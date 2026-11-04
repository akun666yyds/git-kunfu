package com.gitkunfu.repo;

import java.time.LocalDateTime;

public record RepoRecord(
        Long id,
        Long ownerId,
        String ownerUsername,
        String ownerDisplayName,
        String ownerAvatarColor,
        String name,
        String description,
        String visibility,
        String defaultBranch,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime pushedAt,
        long openIssues,
        long openPulls
) {
    public boolean isPrivate() {
        return "PRIVATE".equals(visibility);
    }
}
