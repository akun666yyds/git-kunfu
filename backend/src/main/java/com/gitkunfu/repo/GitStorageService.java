package com.gitkunfu.repo;

import com.gitkunfu.common.ApiException;
import com.gitkunfu.config.GitProperties;
import com.gitkunfu.repo.RepoDtos.BlobView;
import com.gitkunfu.repo.RepoDtos.BranchView;
import com.gitkunfu.repo.RepoDtos.CommitPage;
import com.gitkunfu.repo.RepoDtos.CommitView;
import com.gitkunfu.repo.RepoDtos.CompareView;
import com.gitkunfu.repo.RepoDtos.FileDiffView;
import com.gitkunfu.repo.RepoDtos.MergeResult;
import com.gitkunfu.repo.RepoDtos.ReadmeView;
import com.gitkunfu.repo.RepoDtos.TreeEntryView;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.Merger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class GitStorageService {

    private static final Logger log = LoggerFactory.getLogger(GitStorageService.class);
    private static final long MAX_BLOB_BYTES = 2L * 1024 * 1024;

    private final Path root;

    public GitStorageService(GitProperties properties) throws IOException {
        this.root = Path.of(properties.repositoryRoot()).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public Path gitDir(Long repoId) {
        return root.resolve(repoId + ".git");
    }

    public void createRepository(Long repoId, String repoName, String description,
                                 String ownerName, String ownerEmail, String cloneUrl,
                                 boolean initialReadme, String defaultBranch)
            throws IOException, org.eclipse.jgit.api.errors.GitAPIException {
        Path dir = gitDir(repoId);
        if (Files.exists(dir)) {
            throw ApiException.conflict("仓库数据目录已存在");
        }
        Files.createDirectories(dir.getParent());
        try (Git git = Git.init()
                .setBare(true)
                .setInitialBranch(defaultBranch)
                .setDirectory(dir.toFile())
                .call()) {
            Repository repository = git.getRepository();
            RefUpdate head = repository.updateRef(Constants.HEAD);
            head.link("refs/heads/" + defaultBranch);
            if (initialReadme) {
                writeInitialCommit(repository, repoName, description, ownerName, ownerEmail,
                        cloneUrl, defaultBranch);
            }
        } catch (Exception ex) {
            deleteDirectory(dir);
            throw ex;
        }
    }

    private void writeInitialCommit(Repository repository, String repoName, String description,
                                    String ownerName, String ownerEmail, String cloneUrl,
                                    String defaultBranch) throws IOException {
        String readme = "# " + repoName + "\n\n"
                + (description == null || description.isBlank()
                    ? "这里是 **" + repoName + "** 的源码。"
                    : description.trim())
                + "\n\n## 开始使用\n\n"
                + "```bash\n"
                + "git clone " + cloneUrl + "\n"
                + "```\n\n"
                + "愿每一次提交，都有功夫。\n";
        PersonIdent person = new PersonIdent(ownerName, ownerEmail);
        byte[] content = readme.getBytes(StandardCharsets.UTF_8);
        try (ObjectInserter inserter = repository.newObjectInserter()) {
            ObjectId blobId = inserter.insert(Constants.OBJ_BLOB, content);
            org.eclipse.jgit.lib.TreeFormatter treeFormatter = new org.eclipse.jgit.lib.TreeFormatter();
            treeFormatter.append("README.md", FileMode.REGULAR_FILE, blobId);
            ObjectId treeId = inserter.insert(treeFormatter);

            org.eclipse.jgit.lib.CommitBuilder commit = new org.eclipse.jgit.lib.CommitBuilder();
            commit.setTreeId(treeId);
            commit.setAuthor(person);
            commit.setCommitter(person);
            commit.setMessage("初始化仓库");
            ObjectId commitId = inserter.insert(commit);
            inserter.flush();

            RefUpdate update = repository.updateRef("refs/heads/" + defaultBranch);
            update.setNewObjectId(commitId);
            update.setForceUpdate(true);
            update.update();
        }
    }

    public void deleteRepository(Long repoId) {
        try {
            deleteDirectory(gitDir(repoId));
        } catch (IOException ex) {
            log.warn("删除仓库目录失败: {}", repoId, ex);
        }
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        List<Path> paths;
        try (var stream = Files.walk(dir)) {
            paths = stream.sorted(Comparator.reverseOrder()).toList();
        }
        IOException failure = null;
        for (Path path : paths) {
            try {
                deleteWithRetry(path);
            } catch (IOException ex) {
                failure = ex;
            }
        }
        if (failure != null && Files.exists(dir)) {
            throw failure;
        }
    }

    private void deleteWithRetry(Path path) throws IOException {
        IOException last = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                // JGit 会把对象文件标记为只读，Windows 上需先恢复可写才能删除
                path.toFile().setWritable(true);
                Files.deleteIfExists(path);
                return;
            } catch (IOException ex) {
                last = ex;
                try {
                    Thread.sleep(120L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw last;
    }

    public Git open(Long repoId) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        builder.setGitDir(gitDir(repoId).toFile());
        builder.setBare();
        Repository repository = builder.build();
        return new ClosingGit(repository);
    }

    /**
     * Git.wrap 不会在 close 时关闭底层的 Repository，Windows 上会一直占用
     * pack 文件句柄，导致仓库删除失败。这里显式负责关闭。
     */
    private static final class ClosingGit extends Git {
        private final Repository repository;

        private ClosingGit(Repository repository) {
            super(repository);
            this.repository = repository;
        }

        @Override
        public void close() {
            try {
                super.close();
            } finally {
                repository.close();
            }
        }
    }

    public boolean isEmpty(Long repoId) {
        try (Git git = open(repoId)) {
            return git.getRepository().getRefDatabase().getRefsByPrefix(Constants.R_HEADS).isEmpty();
        } catch (Exception ex) {
            return true;
        }
    }

    public List<String> branchNames(Long repoId) {
        try (Git git = open(repoId)) {
            return git.branchList().call().stream()
                    .map(ref -> ref.getName().substring(Constants.R_HEADS.length()))
                    .sorted()
                    .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    public boolean branchExists(Long repoId, String branch) {
        if (branch == null || branch.isBlank()) {
            return false;
        }
        try (Git git = open(repoId)) {
            return git.getRepository().resolve("refs/heads/" + branch) != null;
        } catch (Exception ex) {
            return false;
        }
    }

    public List<BranchView> listBranches(Long repoId, String defaultBranch) {
        List<BranchView> branches = new ArrayList<>();
        try (Git git = open(repoId); RevWalk walk = new RevWalk(git.getRepository())) {
            for (Ref ref : git.branchList().call()) {
                RevCommit commit = walk.parseCommit(ref.getObjectId());
                String name = ref.getName().substring(Constants.R_HEADS.length());
                branches.add(new BranchView(
                        name,
                        commit.getName(),
                        commit.getName().substring(0, 7),
                        commit.getShortMessage(),
                        commit.getAuthorIdent().getName(),
                        commit.getAuthorIdent().getWhen().toInstant(),
                        name.equals(defaultBranch)
                ));
            }
        } catch (Exception ex) {
            return List.of();
        }
        branches.sort(Comparator.comparing(BranchView::isDefault).reversed()
                .thenComparing(BranchView::name, String.CASE_INSENSITIVE_ORDER));
        return branches;
    }

    public long countCommits(Long repoId, String branch) {
        try (Git git = open(repoId); RevWalk walk = new RevWalk(git.getRepository())) {
            ObjectId tip = git.getRepository().resolve("refs/heads/" + branch);
            if (tip == null) {
                return 0;
            }
            walk.markStart(walk.parseCommit(tip));
            long count = 0;
            for (RevCommit ignored : walk) {
                count++;
            }
            return count;
        } catch (Exception ex) {
            return 0;
        }
    }

    public CommitPage listCommits(Long repoId, String branch, int page, int size) {
        List<CommitView> items = new ArrayList<>();
        boolean hasMore = false;
        try (Git git = open(repoId); RevWalk walk = new RevWalk(git.getRepository())) {
            ObjectId tip = git.getRepository().resolve("refs/heads/" + branch);
            if (tip == null) {
                return new CommitPage(items, page, size, false);
            }
            walk.markStart(walk.parseCommit(tip));
            int skip = (page - 1) * size;
            int index = 0;
            int collected = 0;
            for (RevCommit commit : walk) {
                if (index++ < skip) {
                    continue;
                }
                if (collected == size) {
                    hasMore = true;
                    break;
                }
                items.add(toCommitView(commit));
                collected++;
            }
        } catch (Exception ex) {
            log.debug("读取提交失败: {}", repoId, ex);
            return new CommitPage(items, page, size, false);
        }
        return new CommitPage(items, page, size, hasMore);
    }

    public RepoDtos.CommitDetailView commitDetail(Long repoId, String sha) {
        try (Git git = open(repoId)) {
            Repository repository = git.getRepository();
            ObjectId id = repository.resolve(sha);
            if (id == null) {
                throw ApiException.notFound("没有找到这次提交");
            }
            try (RevWalk walk = new RevWalk(repository)) {
                RevCommit commit = walk.parseCommit(id);
                RevCommit parent = commit.getParentCount() > 0 ? walk.parseCommit(commit.getParent(0)) : null;
                List<FileDiffView> files = computeDiff(repository, parent, commit);
                int additions = files.stream().mapToInt(FileDiffView::additions).sum();
                int deletions = files.stream().mapToInt(FileDiffView::deletions).sum();
                return new RepoDtos.CommitDetailView(toCommitView(commit), files, additions, deletions);
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "读取提交信息失败");
        }
    }

    public List<TreeEntryView> listTree(Long repoId, String branch, String path) {
        String normalized = normalizePath(path);
        try (Git git = open(repoId); RevWalk walk = new RevWalk(git.getRepository())) {
            Repository repository = git.getRepository();
            ObjectId tip = repository.resolve("refs/heads/" + branch);
            if (tip == null) {
                throw ApiException.notFound("仓库还没有任何提交");
            }
            RevCommit commit = walk.parseCommit(tip);
            ObjectId treeId = commit.getTree().getId();
            if (!normalized.isEmpty()) {
                try (TreeWalk nav = TreeWalk.forPath(repository, normalized, commit.getTree())) {
                    if (nav == null) {
                        throw ApiException.notFound("路径不存在");
                    }
                    if (!nav.isSubtree()) {
                        throw ApiException.badRequest("该路径不是文件夹");
                    }
                    treeId = nav.getObjectId(0);
                }
            }
            List<TreeEntryView> entries = new ArrayList<>();
            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(treeId);
                treeWalk.setRecursive(false);
                while (treeWalk.next()) {
                    String name = treeWalk.getNameString();
                    boolean dir = treeWalk.isSubtree();
                    String childPath = normalized.isEmpty() ? name : normalized + "/" + name;
                    entries.add(new TreeEntryView(childPath, childPath, dir ? "dir" : "file",
                            dir ? 0 : blobSize(treeWalk)));
                }
            }
            entries.sort(Comparator.comparing((TreeEntryView e) -> !"dir".equals(e.type()))
                    .thenComparing(TreeEntryView::name, String.CASE_INSENSITIVE_ORDER));
            return entries;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "读取目录失败");
        }
    }

    public BlobView readBlob(Long repoId, String branch, String path) {
        String normalized = normalizePath(path);
        if (normalized.isEmpty()) {
            throw ApiException.badRequest("请指定文件路径");
        }
        try (Git git = open(repoId); RevWalk walk = new RevWalk(git.getRepository())) {
            Repository repository = git.getRepository();
            ObjectId tip = repository.resolve("refs/heads/" + branch);
            if (tip == null) {
                throw ApiException.notFound("仓库还没有任何提交");
            }
            RevCommit commit = walk.parseCommit(tip);
            try (TreeWalk treeWalk = TreeWalk.forPath(repository, normalized, commit.getTree())) {
                if (treeWalk == null || treeWalk.isSubtree()) {
                    throw ApiException.notFound("文件不存在");
                }
                long size = blobSize(treeWalk);
                if (size > MAX_BLOB_BYTES) {
                    return new BlobView(normalized, fileName(normalized), size, true, true, null);
                }
                byte[] bytes = repository.open(treeWalk.getObjectId(0)).getBytes();
                boolean binary = isBinary(bytes);
                String content = binary ? null : new String(bytes, StandardCharsets.UTF_8);
                return new BlobView(normalized, fileName(normalized), size, binary, false, content);
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "读取文件失败");
        }
    }

    public byte[] rawBlob(Long repoId, String branch, String path) {
        String normalized = normalizePath(path);
        try (Git git = open(repoId); RevWalk walk = new RevWalk(git.getRepository())) {
            Repository repository = git.getRepository();
            ObjectId tip = repository.resolve("refs/heads/" + branch);
            if (tip == null) {
                throw ApiException.notFound("仓库还没有任何提交");
            }
            RevCommit commit = walk.parseCommit(tip);
            try (TreeWalk treeWalk = TreeWalk.forPath(repository, normalized, commit.getTree())) {
                if (treeWalk == null || treeWalk.isSubtree()) {
                    throw ApiException.notFound("文件不存在");
                }
                long size = blobSize(treeWalk);
                if (size > 10L * 1024 * 1024) {
                    throw ApiException.badRequest("文件过大，无法直接下载");
                }
                return repository.open(treeWalk.getObjectId(0)).getBytes();
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "读取文件失败");
        }
    }

    public Optional<ReadmeView> readReadme(Long repoId, String branch) {
        try (Git git = open(repoId); RevWalk walk = new RevWalk(git.getRepository())) {
            Repository repository = git.getRepository();
            ObjectId tip = repository.resolve("refs/heads/" + branch);
            if (tip == null) {
                return Optional.empty();
            }
            RevCommit commit = walk.parseCommit(tip);
            for (String candidate : List.of("README.md", "readme.md", "Readme.md", "README.MD",
                    "README", "README.txt", "README.rst")) {
                try (TreeWalk treeWalk = TreeWalk.forPath(repository, candidate, commit.getTree())) {
                    if (treeWalk != null && !treeWalk.isSubtree()
                            && blobSize(treeWalk) <= 1024 * 1024) {
                        byte[] bytes = repository.open(treeWalk.getObjectId(0)).getBytes();
                        String content = new String(bytes, StandardCharsets.UTF_8);
                        boolean markdown = candidate.toLowerCase().endsWith(".md");
                        return Optional.of(new ReadmeView(candidate, content, markdown));
                    }
                }
            }
            return Optional.empty();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public CompareView compare(Long repoId, String baseBranch, String headBranch) {
        try (Git git = open(repoId); RevWalk walk = new RevWalk(git.getRepository())) {
            Repository repository = git.getRepository();
            ObjectId baseId = repository.resolve("refs/heads/" + baseBranch);
            ObjectId headId = repository.resolve("refs/heads/" + headBranch);
            if (baseId == null) {
                throw ApiException.notFound("目标分支不存在");
            }
            if (headId == null) {
                throw ApiException.notFound("来源分支不存在");
            }
            RevCommit base = walk.parseCommit(baseId);
            RevCommit head = walk.parseCommit(headId);
            boolean fastForward = walk.isMergedInto(base, head);
            boolean behind = walk.isMergedInto(head, base);

            RevCommit mergeBase = findMergeBase(repository, base, head);
            List<FileDiffView> files = computeDiff(repository, mergeBase, head);
            int additions = files.stream().mapToInt(FileDiffView::additions).sum();
            int deletions = files.stream().mapToInt(FileDiffView::deletions).sum();

            List<CommitView> commits = new ArrayList<>();
            try (RevWalk aheadWalk = new RevWalk(repository)) {
                aheadWalk.markStart(aheadWalk.parseCommit(head));
                aheadWalk.markUninteresting(aheadWalk.parseCommit(base));
                for (RevCommit commit : aheadWalk) {
                    commits.add(toCommitView(commit));
                }
            }
            int behindBy = 0;
            if (behind && !fastForward) {
                try (RevWalk behindWalk = new RevWalk(repository)) {
                    behindWalk.markStart(behindWalk.parseCommit(base));
                    behindWalk.markUninteresting(behindWalk.parseCommit(head));
                    for (RevCommit ignored : behindWalk) {
                        behindBy++;
                    }
                }
            }
            String mergeBaseSha = mergeBase == null ? null : mergeBase.getName();
            return new CompareView(baseBranch, headBranch, mergeBaseSha, fastForward, commits.size(),
                    behindBy, commits, files, additions, deletions);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "对比分支失败");
        }
    }

    public MergeResult merge(Long repoId, String baseBranch, String headBranch,
                             String authorName, String authorEmail, String message) {
        try (Git git = open(repoId)) {
            Repository repository = git.getRepository();
            ObjectId baseId = repository.resolve("refs/heads/" + baseBranch);
            ObjectId headId = repository.resolve("refs/heads/" + headBranch);
            if (baseId == null) {
                throw ApiException.notFound("目标分支不存在");
            }
            if (headId == null) {
                throw ApiException.notFound("来源分支不存在");
            }
            if (baseId.equals(headId)) {
                return new MergeResult("UP_TO_DATE", "两个分支已经一致", baseId.getName(), List.of());
            }
            try (RevWalk walk = new RevWalk(repository)) {
                RevCommit base = walk.parseCommit(baseId);
                RevCommit head = walk.parseCommit(headId);
                if (walk.isMergedInto(head, base)) {
                    return new MergeResult("UP_TO_DATE", "来源分支的改动已经在目标分支中", baseId.getName(), List.of());
                }
                if (walk.isMergedInto(base, head)) {
                    RefUpdate update = repository.updateRef("refs/heads/" + baseBranch);
                    update.setNewObjectId(headId);
                    update.setExpectedOldObjectId(baseId);
                    RefUpdate.Result result = update.update();
                    if (result != RefUpdate.Result.FAST_FORWARD && result != RefUpdate.Result.NEW
                            && result != RefUpdate.Result.FORCED) {
                        throw ApiException.conflict("分支已被其他人更新，请刷新后重试");
                    }
                    return new MergeResult("FAST_FORWARD", "已快进合并", headId.getName(), List.of());
                }

                Merger merger = MergeStrategy.RECURSIVE.newMerger(repository, true);
                boolean merged = merger.merge(base, head);
                if (!merged) {
                    List<String> conflicts = new ArrayList<>();
                    if (merger instanceof org.eclipse.jgit.merge.ResolveMerger resolveMerger) {
                        var results = resolveMerger.getMergeResults();
                        if (results != null) {
                            results.forEach((path, result) -> {
                                if (result.containsConflicts()) {
                                    conflicts.add(path);
                                }
                            });
                        }
                    }
                    return new MergeResult("CONFLICT", "分支存在冲突，暂时无法合并",
                            null, conflicts);
                }
                ObjectId treeId = merger.getResultTreeId();
                PersonIdent person = new PersonIdent(authorName, authorEmail);
                try (ObjectInserter inserter = repository.newObjectInserter()) {
                    org.eclipse.jgit.lib.CommitBuilder commit = new org.eclipse.jgit.lib.CommitBuilder();
                    commit.setTreeId(treeId);
                    commit.setParentIds(base, head);
                    commit.setAuthor(person);
                    commit.setCommitter(person);
                    commit.setMessage(message == null || message.isBlank()
                            ? "合并分支 '" + headBranch + "' 到 '" + baseBranch + "'"
                            : message.trim());
                    ObjectId commitId = inserter.insert(commit);
                    inserter.flush();

                    RefUpdate update = repository.updateRef("refs/heads/" + baseBranch);
                    update.setNewObjectId(commitId);
                    update.setExpectedOldObjectId(baseId);
                    RefUpdate.Result result = update.update();
                    if (result != RefUpdate.Result.NEW && result != RefUpdate.Result.FAST_FORWARD
                            && result != RefUpdate.Result.FORCED) {
                        throw ApiException.conflict("分支已被其他人更新，请刷新后重试");
                    }
                    return new MergeResult("MERGED", "已创建合并提交", commitId.getName(), List.of());
                }
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("合并分支失败", ex);
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "合并失败，请稍后再试");
        }
    }

    private RevCommit findMergeBase(Repository repository, RevCommit base, RevCommit head) throws IOException {
        try (RevWalk walk = new RevWalk(repository)) {
            Set<ObjectId> ancestors = new HashSet<>();
            walk.markStart(walk.parseCommit(base));
            for (RevCommit commit : walk) {
                ancestors.add(commit);
            }
            walk.reset();
            walk.markStart(walk.parseCommit(head));
            for (RevCommit commit : walk) {
                if (ancestors.contains(commit)) {
                    return commit;
                }
            }
            return null;
        }
    }

    private List<FileDiffView> computeDiff(Repository repository, RevCommit oldCommit, RevCommit newCommit) throws IOException {
        List<FileDiffView> files = new ArrayList<>();
        try (ObjectReader reader = repository.newObjectReader()) {
            CanonicalTreeParser oldTree = new CanonicalTreeParser();
            if (oldCommit != null) {
                oldTree.reset(reader, oldCommit.getTree().getId());
            }
            CanonicalTreeParser newTree = new CanonicalTreeParser();
            if (newCommit != null) {
                newTree.reset(reader, newCommit.getTree().getId());
            }
            var oldIterator = oldCommit == null ? new EmptyTreeIterator() : oldTree;
            var newIterator = newCommit == null ? new EmptyTreeIterator() : newTree;

            List<DiffEntry> entries;
            try (DiffFormatter scanner = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
                scanner.setRepository(repository);
                scanner.setDetectRenames(true);
                entries = scanner.scan(oldIterator, newIterator);
            }
            for (DiffEntry entry : entries) {
                String patch;
                try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                     DiffFormatter formatter = new DiffFormatter(out)) {
                    formatter.setRepository(repository);
                    formatter.setDetectRenames(true);
                    formatter.setContext(3);
                    formatter.format(entry);
                    patch = out.toString(StandardCharsets.UTF_8);
                }
                int additions = 0;
                int deletions = 0;
                for (String line : patch.split("\n")) {
                    if (line.startsWith("+") && !line.startsWith("+++")) {
                        additions++;
                    } else if (line.startsWith("-") && !line.startsWith("---")) {
                        deletions++;
                    }
                }
                String path = "/dev/null".equals(entry.getNewPath())
                        ? entry.getOldPath() : entry.getNewPath();
                files.add(new FileDiffView(path, entry.getOldPath(),
                        entry.getChangeType().name(), additions, deletions, patch));
            }
        }
        return files;
    }

    private CommitView toCommitView(RevCommit commit) {
        String full = commit.getFullMessage();
        String body = full.contains("\n")
                ? full.substring(full.indexOf('\n') + 1).trim()
                : "";
        List<String> parents = new ArrayList<>();
        for (RevCommit parent : commit.getParents()) {
            parents.add(parent.getName());
        }
        return new CommitView(
                commit.getName(),
                commit.getName().substring(0, 7),
                commit.getShortMessage(),
                body,
                commit.getAuthorIdent().getName(),
                commit.getAuthorIdent().getEmailAddress(),
                commit.getAuthorIdent().getWhen().toInstant(),
                parents
        );
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.contains("..")) {
            throw ApiException.badRequest("路径不合法");
        }
        return normalized;
    }

    private String fileName(String path) {
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    private boolean isBinary(byte[] bytes) {
        int limit = Math.min(bytes.length, 8000);
        for (int i = 0; i < limit; i++) {
            if (bytes[i] == 0) {
                return true;
            }
        }
        return false;
    }

    private long blobSize(TreeWalk treeWalk) {
        try {
            return treeWalk.getObjectReader().getObjectSize(treeWalk.getObjectId(0), Constants.OBJ_BLOB);
        } catch (IOException ex) {
            return 0;
        }
    }
}
