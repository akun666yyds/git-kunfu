package com.gitkunfu.repo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class GitAuthFilter extends OncePerRequestFilter {

    private final RepoRepository repoRepository;
    private final com.gitkunfu.user.UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public GitAuthFilter(RepoRepository repoRepository,
                         com.gitkunfu.user.UserRepository userRepository,
                         PasswordEncoder passwordEncoder) {
        this.repoRepository = repoRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/git/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        String uri = request.getRequestURI();
        String[] pair = GitPathParser.parse(uri);
        if (pair == null) {
            filterChain.doFilter(request, response);
            return;
        }
        RepoRecord repo = repoRepository.findByOwnerAndName(pair[0], pair[1]).orElse(null);
        if (repo == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String query = request.getQueryString();
        boolean receive = uri.contains("git-receive-pack")
                || (query != null && query.contains("service=git-receive-pack"));
        com.gitkunfu.user.UserRecord user = authenticate(request);

        if (receive) {
            if (user == null) {
                challenge(response, "请输入功夫吉特的账号和密码");
                return;
            }
            if (!user.id().equals(repo.ownerId())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "只有仓库所有者可以推送");
                return;
            }
        } else if (repo.isPrivate()) {
            if (user == null) {
                challenge(response, "私有仓库需要登录后访问");
                return;
            }
            if (!user.id().equals(repo.ownerId())) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Repository not found");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private com.gitkunfu.user.UserRecord authenticate(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Basic ", 0, 6)) {
            return null;
        }
        try {
            String decoded = new String(
                    Base64.getDecoder().decode(header.substring(6).trim()), StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            if (colon < 0) {
                return null;
            }
            String username = decoded.substring(0, colon);
            String password = decoded.substring(colon + 1);
            return userRepository.findByUsername(username)
                    .filter(user -> passwordEncoder.matches(password, user.passwordHash()))
                    .orElse(null);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private void challenge(HttpServletResponse response, String message) throws IOException {
        response.setHeader("WWW-Authenticate", "Basic realm=\"gitKunfu\", charset=\"UTF-8\"");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
    }
}
