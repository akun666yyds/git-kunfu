package com.gitkunfu.user;

import java.time.LocalDateTime;

public record UserView(
        Long id,
        String username,
        String displayName,
        String email,
        String bio,
        String avatarColor,
        LocalDateTime createdAt
) {
    public static UserView from(UserRecord user, boolean includeEmail) {
        return new UserView(
                user.id(),
                user.username(),
                user.displayName(),
                includeEmail ? user.email() : null,
                user.bio(),
                user.avatarColor(),
                user.createdAt()
        );
    }
}
