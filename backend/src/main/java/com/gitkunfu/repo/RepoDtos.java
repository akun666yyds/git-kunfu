package com.gitkunfu.repo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public final class RepoDtos {

    private RepoDtos() {
    }

    public record CreateRepoRequest(
            @NotBlank(message = "请填写仓库名称")
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,99}$",
                    message = "仓库名需以字母或数字开头，只能包含字母、数字、点、下划线和短横线")
            @Size(max = 100, message = "仓库名过长")
            String name,
            @Size(max = 500, message = "仓库简介最多 500 个字符")
            String description,
            @Pattern(regexp = "PUBLIC|PRIVATE", message = "可见性参数不正确")
            String visibility,
            Boolean initialReadme
    ) {
    }

    public record UpdateRepoRequest(
            @Size(max = 500, message = "仓库简介最多 500 个字符") String description,
            @Pattern(regexp = "PUBLIC|PRIVATE", message = "可见性参数不正确") String visibility,
            @Size(max = 100, message = "分支名过长") String defaultBranch
    ) {
    }

    public record RepoView(
            Long id,
            String name,
            String owner,
            String ownerDisplayName,
            String ownerAvatarColor,
            String description,
            String visibility,
            String defaultBranch,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime pushedAt,
            long openIssues,
            long openPulls,
            String cloneUrl
    ) {
    }

    public record RepoDetailView(
            RepoView repo,
            boolean empty,
            long commitCount,
            long branchCount
    ) {
    }

    public record RepoListResponse(List<RepoView> items, long total, int page, int size) {
    }

    public record BranchView(
            String name,
            String sha,
            String shortSha,
            String message,
            String authorName,
            Instant updatedAt,
            boolean isDefault
    ) {
    }

    public record TreeEntryView(String name, String path, String type, long size) {
    }

    public record BlobView(
            String path,
            String name,
            long size,
            boolean binary,
            boolean truncated,
            String content
    ) {
    }

    public record ReadmeView(String name, String content, boolean markdown) {
    }

    public record CommitView(
            String sha,
            String shortSha,
            String message,
            String body,
            String authorName,
            String authorEmail,
            Instant authoredAt,
            List<String> parents
    ) {
    }

    public record CommitPage(List<CommitView> items, int page, int size, boolean hasMore) {
    }

    public record FileDiffView(
            String path,
            String oldPath,
            String changeType,
            int additions,
            int deletions,
            String patch
    ) {
    }

    public record CommitDetailView(
            CommitView commit,
            List<FileDiffView> files,
            int additions,
            int deletions
    ) {
    }

    public record CompareView(
            String base,
            String head,
            String mergeBase,
            boolean fastForward,
            int aheadBy,
            int behindBy,
            List<CommitView> commits,
            List<FileDiffView> files,
            int additions,
            int deletions
    ) {
    }

    public record MergeResult(
            String status,
            String message,
            String sha,
            List<String> conflicts
    ) {
    }
}
