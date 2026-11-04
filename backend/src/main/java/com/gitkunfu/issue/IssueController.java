package com.gitkunfu.issue;

import com.gitkunfu.common.AuthContext;
import com.gitkunfu.issue.IssueDtos.CreateCommentRequest;
import com.gitkunfu.issue.IssueDtos.CreateIssueRequest;
import com.gitkunfu.issue.IssueDtos.IssueDetailView;
import com.gitkunfu.issue.IssueDtos.IssueListResponse;
import com.gitkunfu.issue.IssueDtos.UpdateIssueRequest;
import com.gitkunfu.repo.RepoRecord;
import com.gitkunfu.repo.RepoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repos/{owner}/{name}/issues")
public class IssueController {

    private final IssueService issueService;
    private final RepoService repoService;

    public IssueController(IssueService issueService, RepoService repoService) {
        this.issueService = issueService;
        this.repoService = repoService;
    }

    @GetMapping
    public IssueListResponse list(@PathVariable String owner, @PathVariable String name,
                                  @RequestParam(required = false) String state,
                                  HttpServletRequest request) {
        RepoRecord repo = repoService.requireVisible(owner, name, AuthContext.optionalUserId(request));
        return issueService.list(repo, state);
    }

    @PostMapping
    public IssueDetailView create(@PathVariable String owner, @PathVariable String name,
                                  @Valid @RequestBody CreateIssueRequest body,
                                  HttpServletRequest request) {
        Long userId = AuthContext.requireUserId(request);
        RepoRecord repo = repoService.requireVisible(owner, name, userId);
        return issueService.create(repo, userId, body);
    }

    @GetMapping("/{number}")
    public IssueDetailView detail(@PathVariable String owner, @PathVariable String name,
                                  @PathVariable int number, HttpServletRequest request) {
        RepoRecord repo = repoService.requireVisible(owner, name, AuthContext.optionalUserId(request));
        return issueService.detail(repo, number);
    }

    @PatchMapping("/{number}")
    public IssueDetailView update(@PathVariable String owner, @PathVariable String name,
                                  @PathVariable int number,
                                  @Valid @RequestBody UpdateIssueRequest body,
                                  HttpServletRequest request) {
        Long userId = AuthContext.requireUserId(request);
        RepoRecord repo = repoService.requireVisible(owner, name, userId);
        return issueService.update(repo, number, userId, body);
    }

    @PostMapping("/{number}/comments")
    public IssueDetailView comment(@PathVariable String owner, @PathVariable String name,
                                   @PathVariable int number,
                                   @Valid @RequestBody CreateCommentRequest body,
                                   HttpServletRequest request) {
        Long userId = AuthContext.requireUserId(request);
        RepoRecord repo = repoService.requireVisible(owner, name, userId);
        return issueService.addComment(repo, number, userId, body);
    }
}
