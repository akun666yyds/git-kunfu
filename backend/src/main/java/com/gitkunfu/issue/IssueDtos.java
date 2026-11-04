package com.gitkunfu.issue;

import com.gitkunfu.common.AuthorView;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class IssueDtos {

    private IssueDtos() {
    }

    public record IssueView(
            Long id,
            int number,
            String title,
            String body,
            String state,
            AuthorView author,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime closedAt,
            long commentCount
    ) {
    }

    public record IssueCommentView(
            Long id,
            String body,
            AuthorView author,
            LocalDateTime createdAt
    ) {
    }

    public record IssueDetailView(IssueView issue, List<IssueCommentView> comments) {
    }

    public record IssueListResponse(List<IssueView> items, long openCount, long closedCount) {
    }

    public record CreateIssueRequest(
            @NotBlank(message = "请填写议题标题")
            @Size(max = 255, message = "标题最多 255 个字符") String title,
            @Size(max = 20000, message = "正文过长") String body
    ) {
    }

    public record UpdateIssueRequest(
            @Size(max = 255, message = "标题最多 255 个字符") String title,
            @Size(max = 20000, message = "正文过长") String body,
            @Pattern(regexp = "OPEN|CLOSED", message = "状态参数不正确") String state
    ) {
    }

    public record CreateCommentRequest(
            @NotBlank(message = "评论内容不能为空")
            @Size(max = 20000, message = "评论过长") String body
    ) {
    }
}
