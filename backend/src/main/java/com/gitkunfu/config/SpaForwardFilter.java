package com.gitkunfu.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 浏览器直接访问前端路由（例如 /akun/repo、/settings）时，
 * 把请求交给单页应用入口，由前端路由接管。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class SpaForwardFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String accept = request.getHeader("Accept");
        boolean pageRequest = "GET".equalsIgnoreCase(request.getMethod())
                && accept != null && accept.contains("text/html")
                && isSpaPath(path);
        if (pageRequest) {
            request.getRequestDispatcher("/index.html").forward(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isSpaPath(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return false;
        }
        return !path.equals("/api") && !path.startsWith("/api/")
                && !path.equals("/git") && !path.startsWith("/git/")
                && !path.equals("/error") && !path.startsWith("/error/")
                && !path.startsWith("/assets/")
                && !path.equals("/index.html")
                && !path.equals("/favicon.ico");
    }
}
