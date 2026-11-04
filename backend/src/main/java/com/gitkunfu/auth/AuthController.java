package com.gitkunfu.auth;

import com.gitkunfu.common.ApiException;
import com.gitkunfu.common.AuthContext;
import com.gitkunfu.user.UserDtos.AuthResponse;
import com.gitkunfu.user.UserDtos.LoginRequest;
import com.gitkunfu.user.UserDtos.RegisterRequest;
import com.gitkunfu.user.UserRecord;
import com.gitkunfu.user.UserRepository;
import com.gitkunfu.user.UserView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String[] AVATAR_COLORS = {
            "#8a6f5a", "#7a8a6a", "#8b7a9b", "#a07a6a", "#6f8a8a", "#9a8a5f", "#7d7f94", "#8d6f6f"
    };

    private static final java.util.Set<String> RESERVED_USERNAMES = java.util.Set.of(
            "api", "git", "assets", "login", "register", "explore", "dashboard",
            "new", "settings", "u", "error", "favicon.ico", "me", "admin", "root");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        String displayName = request.displayName() == null || request.displayName().isBlank()
                ? username
                : request.displayName().trim();

        if (RESERVED_USERNAMES.contains(username.toLowerCase(Locale.ROOT))) {
            throw ApiException.conflict("这个用户名为系统保留，请换一个");
        }
        if (userRepository.existsByUsername(username)) {
            throw ApiException.conflict("这个用户名已经被使用了");
        }
        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("这个邮箱已经注册过了");
        }

        String color = AVATAR_COLORS[Math.floorMod(username.toLowerCase(Locale.ROOT).hashCode(), AVATAR_COLORS.length)];
        long id = userRepository.insert(username, email, passwordEncoder.encode(request.password()), displayName, color);
        UserRecord user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR));
        String token = jwtService.issue(user.id(), user.username());
        return new AuthResponse(token, UserView.from(user, true));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        UserRecord user = userRepository.findByLogin(request.login().trim())
                .orElseThrow(() -> ApiException.unauthorized("用户名或密码不正确"));
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw ApiException.unauthorized("用户名或密码不正确");
        }
        String token = jwtService.issue(user.id(), user.username());
        return new AuthResponse(token, UserView.from(user, true));
    }

    @GetMapping("/me")
    public AuthResponse me(HttpServletRequest request) {
        Long userId = AuthContext.requireUserId(request);
        UserRecord user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("登录状态已失效，请重新登录"));
        return new AuthResponse(null, UserView.from(user, true));
    }
}
