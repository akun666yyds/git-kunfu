package com.gitkunfu.auth;

import com.gitkunfu.common.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;

    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            jwtService.verify(header.substring(7).trim()).ifPresent(user -> {
                request.setAttribute(AuthContext.USER_ID, user.id());
                request.setAttribute(AuthContext.USERNAME, user.username());
            });
        }
        return true;
    }
}
