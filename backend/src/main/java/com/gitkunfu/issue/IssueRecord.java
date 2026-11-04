package com.gitkunfu.issue;

import java.time.LocalDateTime;

public record IssueRecord(
        Long id,
        Long repoId,
        int number,
        String title,
        String body,
        String state,
        Long authorId,
        String authorUsername,
        String authorDisplayName,
        String authorAvatarColor,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime closedAt,
        long commentCount
) {
}
