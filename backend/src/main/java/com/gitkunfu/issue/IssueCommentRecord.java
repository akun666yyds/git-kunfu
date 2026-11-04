package com.gitkunfu.issue;

import java.time.LocalDateTime;

public record IssueCommentRecord(
        Long id,
        Long issueId,
        String body,
        Long authorId,
        String authorUsername,
        String authorDisplayName,
        String authorAvatarColor,
        LocalDateTime createdAt
) {
}
