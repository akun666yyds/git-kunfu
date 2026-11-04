package com.gitkunfu.pull;

import com.gitkunfu.common.AuthContext;
import com.gitkunfu.pull.PullDtos.CreatePullRequest;
import com.gitkunfu.pull.PullDtos.MergePullRequest;
import com.gitkunfu.pull.PullDtos.PullDetailView;
import com.gitkunfu.pull.PullDtos.PullListResponse;
import com.gitkunfu.repo.RepoDtos.MergeResult;
import com.gitkunfu.repo.RepoRecord;
import com.gitkunfu.repo.RepoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repos/{owner}/{name}/pulls")
public class PullController {

    private final PullService pullService;
    private final RepoService repoService;

    public PullController(PullService pullService, RepoService repoService) {
        this.pullService = pullService;
        this.repoService = repoService;
    }

    @GetMapping
    public PullListResponse list(@PathVariable String owner, @PathVariable String name,
                                 @RequestParam(required = false) String state,
                                 HttpServletRequest request) {
        RepoRecord repo = repoService.requireVisible(owner, name, AuthContext.optionalUserId(request));
        return pullService.list(repo, state);
    }

    @PostMapping
    public PullDetailView create(@PathVariable String owner, @PathVariable String name,
                                 @Valid @RequestBody CreatePullRequest body,
                                 HttpServletRequest request) {
        Long userId = AuthContext.requireUserId(request);
        RepoRecord repo = repoService.requireVisible(owner, name, userId);
        return pullService.create(repo, userId, body);
    }

    @GetMapping("/{number}")
    public PullDetailView detail(@PathVariable String owner, @PathVariable String name,
                                 @PathVariable int number, HttpServletRequest request) {
        RepoRecord repo = repoService.requireVisible(owner, name, AuthContext.optionalUserId(request));
        return pullService.detail(repo, number, AuthContext.optionalUserId(request));
    }

    @PostMapping("/{number}/merge")
    public MergeResult merge(@PathVariable String owner, @PathVariable String name,
                             @PathVariable int number,
                             @RequestBody(required = false) MergePullRequest body,
                             HttpServletRequest request) {
        Long userId = AuthContext.requireUserId(request);
        RepoRecord repo = repoService.requireOwned(owner, name, userId);
        return pullService.merge(repo, number, userId, body);
    }

    @PostMapping("/{number}/close")
    public PullDetailView close(@PathVariable String owner, @PathVariable String name,
                                @PathVariable int number, HttpServletRequest request) {
        Long userId = AuthContext.requireUserId(request);
        RepoRecord repo = repoService.requireVisible(owner, name, userId);
        return pullService.close(repo, number, userId);
    }
}
