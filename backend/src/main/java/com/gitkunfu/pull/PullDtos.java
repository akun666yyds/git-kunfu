package com.gitkunfu.pull;

import com.gitkunfu.common.AuthorView;
import com.gitkunfu.repo.RepoDtos.CompareView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class PullDtos {

    private PullDtos() {
    }

    public record PullView(
            Long id,
            int number,
            String title,
            String body,
            String state,
            AuthorView author,
            String baseBranch,
            String headBranch,
            String mergeCommitSha,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime mergedAt,
            String mergedByUsername
    ) {
    }

    public record PullDetailView(PullView pull, CompareView compare, boolean canMerge) {
    }

    public record PullListResponse(List<PullView> items, long openCount, long closedCount, long mergedCount) {
    }

    public record CreatePullRequest(
            @NotBlank(message = "请填写合并请求标题")
            @Size(max = 255, message = "标题最多 255 个字符") String title,
            @Size(max = 20000, message = "正文过长") String body,
            @NotBlank(message = "请选择目标分支") String base,
            @NotBlank(message = "请选择来源分支") String head
    ) {
    }

    public record MergePullRequest(
            @Size(max = 500, message = "合并说明过长") String message
    ) {
    }
}
