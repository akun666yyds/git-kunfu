package com.gitkunfu.pull;

import com.gitkunfu.common.ApiException;
import com.gitkunfu.common.AuthorView;
import com.gitkunfu.pull.PullDtos.CreatePullRequest;
import com.gitkunfu.pull.PullDtos.MergePullRequest;
import com.gitkunfu.pull.PullDtos.PullDetailView;
import com.gitkunfu.pull.PullDtos.PullListResponse;
import com.gitkunfu.pull.PullDtos.PullView;
import com.gitkunfu.repo.GitStorageService;
import com.gitkunfu.repo.RepoDtos.CompareView;
import com.gitkunfu.repo.RepoDtos.MergeResult;
import com.gitkunfu.repo.RepoRecord;
import com.gitkunfu.repo.RepoService;
import com.gitkunfu.user.UserRecord;
import com.gitkunfu.user.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PullService {

    private final PullRepository pullRepository;
    private final RepoService repoService;
    private final GitStorageService storage;
    private final UserRepository userRepository;

    public PullService(PullRepository pullRepository, RepoService repoService,
                       GitStorageService storage, UserRepository userRepository) {
        this.pullRepository = pullRepository;
        this.repoService = repoService;
        this.storage = storage;
        this.userRepository = userRepository;
    }

    public PullListResponse list(RepoRecord repo, String state) {
        String filter = state == null ? "OPEN" : state.toUpperCase();
        List<PullView> items = pullRepository.list(repo.id(), filter).stream().map(this::toView).toList();
        return new PullListResponse(items,
                pullRepository.count(repo.id(), "OPEN"),
                pullRepository.count(repo.id(), "CLOSED"),
                pullRepository.count(repo.id(), "MERGED"));
    }

    public PullRecord require(RepoRecord repo, int number) {
        return pullRepository.find(repo.id(), number)
                .orElseThrow(() -> ApiException.notFound("合并请求不存在"));
    }

    public PullDetailView detail(RepoRecord repo, int number, Long viewerId) {
        PullRecord pull = require(repo, number);
        CompareView compare = null;
        try {
            compare = storage.compare(repo.id(), pull.baseBranch(), pull.headBranch());
        } catch (ApiException ex) {
            // 分支被删除后，合并请求本身仍可查看
        }
        boolean canMerge = viewerId != null && viewerId.equals(repo.ownerId())
                && "OPEN".equals(pull.state()) && compare != null && compare.mergeBase() != null;
        return new PullDetailView(toView(pull), compare, canMerge);
    }

    public PullDetailView create(RepoRecord repo, Long userId, CreatePullRequest request) {
        String base = request.base().trim();
        String head = request.head().trim();
        if (base.equals(head)) {
            throw ApiException.badRequest("来源分支和目标分支不能相同");
        }
        if (!storage.branchExists(repo.id(), base)) {
            throw ApiException.badRequest("目标分支不存在");
        }
        if (!storage.branchExists(repo.id(), head)) {
            throw ApiException.badRequest("来源分支不存在");
        }
        CompareView compare = storage.compare(repo.id(), base, head);
        if (compare.aheadBy() == 0) {
            throw ApiException.badRequest("来源分支还没有新的提交");
        }
        int number = pullRepository.nextNumber(repo.id());
        pullRepository.insert(repo.id(), number, request.title().trim(),
                request.body() == null ? "" : request.body().trim(), userId, base, head);
        PullRecord pull = require(repo, number);
        return new PullDetailView(toView(pull), compare, false);
    }

    public MergeResult merge(RepoRecord repo, int number, Long userId, MergePullRequest request) {
        PullRecord pull = require(repo, number);
        if (!"OPEN".equals(pull.state())) {
            throw ApiException.conflict("这个合并请求已经结束了");
        }
        UserRecord owner = userRepository.findById(repo.ownerId())
                .orElseThrow(() -> ApiException.notFound("仓库所有者不存在"));
        String message = request != null && request.message() != null && !request.message().isBlank()
                ? request.message().trim()
                : "合并请求 #" + pull.number() + "：" + pull.title();
        MergeResult result = storage.merge(repo.id(), pull.baseBranch(), pull.headBranch(),
                owner.displayName(), owner.email(), message);
        if ("MERGED".equals(result.status()) || "FAST_FORWARD".equals(result.status())
                || "UP_TO_DATE".equals(result.status())) {
            pullRepository.markMerged(pull.id(), result.sha(), userId);
        }
        return result;
    }

    public PullDetailView close(RepoRecord repo, int number, Long userId) {
        PullRecord pull = require(repo, number);
        boolean canClose = userId != null && (userId.equals(pull.authorId()) || userId.equals(repo.ownerId()));
        if (!canClose) {
            throw ApiException.forbidden("只有合并请求作者或仓库所有者可以关闭");
        }
        if ("MERGED".equals(pull.state())) {
            throw ApiException.conflict("已合并的请求不能关闭");
        }
        if ("OPEN".equals(pull.state())) {
            pullRepository.markClosed(pull.id());
        }
        PullRecord updated = require(repo, number);
        return detail(repo, number, userId);
    }

    private PullView toView(PullRecord record) {
        return new PullView(
                record.id(),
                record.number(),
                record.title(),
                record.body(),
                record.state(),
                new AuthorView(record.authorId(), record.authorUsername(),
                        record.authorDisplayName(), record.authorAvatarColor()),
                record.baseBranch(),
                record.headBranch(),
                record.mergeCommitSha(),
                record.createdAt(),
                record.updatedAt(),
                record.mergedAt(),
                record.mergedByUsername()
        );
    }
}
