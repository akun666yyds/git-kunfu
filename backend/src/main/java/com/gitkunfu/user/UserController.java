package com.gitkunfu.user;

import com.gitkunfu.common.ApiException;
import com.gitkunfu.common.AuthContext;
import com.gitkunfu.repo.RepoRepository;
import com.gitkunfu.user.UserDtos.ProfileResponse;
import com.gitkunfu.user.UserDtos.UpdateProfileRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final RepoRepository repoRepository;

    public UserController(UserRepository userRepository, RepoRepository repoRepository) {
        this.userRepository = userRepository;
        this.repoRepository = repoRepository;
    }

    @GetMapping("/{username}")
    public ProfileResponse profile(@PathVariable String username, HttpServletRequest request) {
        UserRecord user = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("没有找到这位用户"));
        Long viewerId = AuthContext.optionalUserId(request);
        boolean self = viewerId != null && viewerId.equals(user.id());
        long count = repoRepository.countVisibleByOwner(user.id(), viewerId);
        return new ProfileResponse(UserView.from(user, self), count);
    }

    @PatchMapping("/me")
    public UserView update(@Valid @RequestBody UpdateProfileRequest body, HttpServletRequest request) {
        Long userId = AuthContext.requireUserId(request);
        String bio = body.bio() == null ? "" : body.bio().trim();
        userRepository.updateProfile(userId, body.displayName().trim(), bio);
        UserRecord user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("用户不存在"));
        return UserView.from(user, true);
    }
}
