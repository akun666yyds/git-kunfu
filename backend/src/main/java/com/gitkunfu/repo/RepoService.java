package com.gitkunfu.repo;

import com.gitkunfu.common.ApiException;
import com.gitkunfu.config.GitProperties;
import com.gitkunfu.repo.RepoDtos.CreateRepoRequest;
import com.gitkunfu.repo.RepoDtos.RepoDetailView;
import com.gitkunfu.repo.RepoDtos.RepoListResponse;
import com.gitkunfu.repo.RepoDtos.RepoView;
import com.gitkunfu.repo.RepoDtos.UpdateRepoRequest;
import com.gitkunfu.user.UserRecord;
import com.gitkunfu.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
public class RepoService {

    private final RepoRepository repoRepository;
    private final UserRepository userRepository;
    private final GitStorageService storage;
    private final GitProperties properties;

    public RepoService(RepoRepository repoRepository, UserRepository userRepository,
                       GitStorageService storage, GitProperties properties) {
        this.repoRepository = repoRepository;
        this.userRepository = userRepository;
        this.storage = storage;
        this.properties = properties;
    }

    public RepoView toView(RepoRecord record) {
        return new RepoView(
                record.id(),
                record.name(),
                record.ownerUsername(),
                record.ownerDisplayName(),
                record.ownerAvatarColor(),
                record.description(),
                record.visibility(),
                record.defaultBranch(),
                record.createdAt(),
                record.updatedAt(),
                record.pushedAt(),
                record.openIssues(),
                record.openPulls(),
                cloneUrl(record.ownerUsername(), record.name())
        );
    }

    public String cloneUrl(String owner, String name) {
        String base = properties.baseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/git/" + owner + "/" + name + ".git";
    }

    public RepoRecord requireVisible(String owner, String name, Long viewerId) {
        RepoRecord record = repoRepository.findByOwnerAndName(owner, name)
                .orElseThrow(() -> ApiException.notFound("仓库不存在"));
        if (record.isPrivate() && (viewerId == null || !viewerId.equals(record.ownerId()))) {
            throw ApiException.notFound("仓库不存在");
        }
        return record;
    }

    public RepoRecord requireOwned(String owner, String name, Long viewerId) {
        RepoRecord record = repoRepository.findByOwnerAndName(owner, name)
                .orElseThrow(() -> ApiException.notFound("仓库不存在"));
        if (viewerId == null || !viewerId.equals(record.ownerId())) {
            throw ApiException.forbidden("只有仓库所有者才能执行这个操作");
        }
        return record;
    }

    public RepoDetailView detail(RepoRecord record) {
        boolean empty = storage.isEmpty(record.id());
        long commits = empty ? 0 : storage.countCommits(record.id(), record.defaultBranch());
        long branches = empty ? 0 : storage.branchNames(record.id()).size();
        return new RepoDetailView(toView(record), empty, commits, branches);
    }

    public RepoListResponse explore(String query, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        List<RepoView> items = repoRepository.searchPublic(query, safePage, safeSize).stream()
                .map(this::toView)
                .toList();
        return new RepoListResponse(items, repoRepository.countPublic(query), safePage, safeSize);
    }

    public List<RepoView> listFeatured(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 24);
        return repoRepository.listFeatured(safeLimit).stream().map(this::toView).toList();
    }

    public List<RepoView> listByUser(String username, Long viewerId) {
        UserRecord owner = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("没有找到这位用户"));
        boolean self = viewerId != null && viewerId.equals(owner.id());
        return repoRepository.listByOwner(owner.id(), self).stream().map(this::toView).toList();
    }

    public List<RepoView> listMine(Long userId) {
        return repoRepository.listByOwner(userId, true).stream().map(this::toView).toList();
    }

    @Transactional
    public RepoDetailView create(Long userId, CreateRepoRequest request) {
        String name = request.name().trim();
        if (name.endsWith(".git")) {
            throw ApiException.badRequest("仓库名不需要以 .git 结尾");
        }
        UserRecord user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("登录状态已失效"));
        String owner = user.username();
        if (repoRepository.findByOwnerAndName(owner, name).isPresent()) {
            throw ApiException.conflict("这个仓库名已经被使用了");
        }
        String visibility = request.visibility() == null ? "PUBLIC" : request.visibility();
        String description = request.description() == null ? "" : request.description().trim();
        boolean withReadme = request.initialReadme() == null || request.initialReadme();

        long id = repoRepository.insert(userId, name, description, visibility, "main");
        try {
            storage.createRepository(id, name, description, user.displayName(), user.email(),
                    cloneUrl(owner, name), withReadme, "main");
        } catch (ApiException ex) {
            storage.deleteRepository(id);
            throw ex;
        } catch (Exception ex) {
            storage.deleteRepository(id);
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "仓库初始化失败，请稍后再试");
        }
        RepoRecord record = repoRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("仓库不存在"));
        return detail(record);
    }

    public RepoDetailView update(Long userId, String owner, String name, UpdateRepoRequest request) {
        RepoRecord record = requireOwned(owner, name, userId);
        String description = request.description() == null ? record.description() : request.description().trim();
        String visibility = request.visibility() == null ? record.visibility() : request.visibility();
        String defaultBranch = request.defaultBranch() == null || request.defaultBranch().isBlank()
                ? record.defaultBranch() : request.defaultBranch().trim();
        if (!storage.isEmpty(record.id()) && !storage.branchExists(record.id(), defaultBranch)) {
            throw ApiException.badRequest("默认分支不存在");
        }
        repoRepository.updateMeta(record.id(), description, visibility, defaultBranch);
        RepoRecord updated = repoRepository.findById(record.id()).orElseThrow();
        return detail(updated);
    }

    public void delete(Long userId, String owner, String name) {
        RepoRecord record = requireOwned(owner, name, userId);
        repoRepository.delete(record.id());
        storage.deleteRepository(record.id());
    }

    public void onPush(Long repoId) {
        RepoRecord record = repoRepository.findById(repoId).orElse(null);
        if (record == null) {
            return;
        }
        repoRepository.touchPushed(repoId);
        if (!storage.branchExists(repoId, record.defaultBranch())) {
            List<String> branches = storage.branchNames(repoId);
            if (!branches.isEmpty()) {
                String pick = branches.contains("main") ? "main" : branches.get(0);
                repoRepository.updateDefaultBranch(repoId, pick);
            }
        }
    }
}
