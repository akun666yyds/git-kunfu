package com.gitkunfu.common;

import jakarta.servlet.http.HttpServletRequest;

public final class AuthContext {

    public static final String USER_ID = "gitkunfu.userId";
    public static final String USERNAME = "gitkunfu.username";

    private AuthContext() {
    }

    public static Long optionalUserId(HttpServletRequest request) {
        Object value = request.getAttribute(USER_ID);
        return value instanceof Long id ? id : null;
    }

    public static Long requireUserId(HttpServletRequest request) {
        Long id = optionalUserId(request);
        if (id == null) {
            throw ApiException.unauthorized("请先登录后再继续");
        }
        return id;
    }

    public static String optionalUsername(HttpServletRequest request) {
        Object value = request.getAttribute(USERNAME);
        return value instanceof String name ? name : null;
    }

    public static String requireUsername(HttpServletRequest request) {
        requireUserId(request);
        return optionalUsername(request);
    }
}
