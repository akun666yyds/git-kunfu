package com.gitkunfu.common;

public record AuthorView(
        Long id,
        String username,
        String displayName,
        String avatarColor
) {
}
