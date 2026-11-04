package com.gitkunfu.pull;

import java.time.LocalDateTime;

public record PullRecord(
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
        String baseBranch,
        String headBranch,
        String mergeCommitSha,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime mergedAt,
        String mergedByUsername
) {
}
