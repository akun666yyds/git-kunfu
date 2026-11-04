package com.gitkunfu.user;

import java.time.LocalDateTime;

public record UserRecord(
        Long id,
        String username,
        String email,
        String passwordHash,
        String displayName,
        String bio,
        String avatarColor,
        LocalDateTime createdAt
) {
}
