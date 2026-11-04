package com.gitkunfu.issue;

import com.gitkunfu.common.ApiException;
import com.gitkunfu.common.AuthorView;
import com.gitkunfu.issue.IssueDtos.CreateCommentRequest;
import com.gitkunfu.issue.IssueDtos.CreateIssueRequest;
import com.gitkunfu.issue.IssueDtos.IssueCommentView;
import com.gitkunfu.issue.IssueDtos.IssueDetailView;
import com.gitkunfu.issue.IssueDtos.IssueListResponse;
import com.gitkunfu.issue.IssueDtos.IssueView;
import com.gitkunfu.issue.IssueDtos.UpdateIssueRequest;
import com.gitkunfu.repo.RepoRecord;
import com.gitkunfu.repo.RepoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final RepoService repoService;

    public IssueService(IssueRepository issueRepository, RepoService repoService) {
        this.issueRepository = issueRepository;
        this.repoService = repoService;
    }

    public IssueListResponse list(RepoRecord repo, String state) {
        String normalized = state == null ? "OPEN" : state.toUpperCase();
        List<IssueView> items = issueRepository.list(repo.id(), normalized).stream()
                .map(this::toView).toList();
        return new IssueListResponse(items,
                issueRepository.count(repo.id(), "OPEN"),
                issueRepository.count(repo.id(), "CLOSED"));
    }

    public IssueRecord require(RepoRecord repo, int number) {
        return issueRepository.find(repo.id(), number)
                .orElseThrow(() -> ApiException.notFound("议题不存在"));
    }

    public IssueDetailView detail(RepoRecord repo, int number) {
        IssueRecord issue = require(repo, number);
        List<IssueCommentView> comments = issueRepository.listComments(issue.id()).stream()
                .map(c -> new IssueCommentView(
                        c.id(),
                        c.body(),
                        new AuthorView(c.authorId(), c.authorUsername(), c.authorDisplayName(), c.authorAvatarColor()),
                        c.createdAt()))
                .toList();
        return new IssueDetailView(toView(issue), comments);
    }

    @Transactional
    public IssueDetailView create(RepoRecord repo, Long userId, CreateIssueRequest request) {
        String title = request.title().trim();
        String body = request.body() == null ? "" : request.body().trim();
        int number = issueRepository.nextNumber(repo.id());
        long id = issueRepository.insert(repo.id(), number, title, body, userId);
        IssueRecord issue = issueRepository.find(repo.id(), number)
                .orElseThrow(() -> ApiException.notFound("议题不存在"));
        return new IssueDetailView(toView(issue), List.of());
    }

    public IssueDetailView update(RepoRecord repo, int number, Long userId, UpdateIssueRequest request) {
        IssueRecord issue = require(repo, number);
        boolean canEdit = userId != null && (userId.equals(issue.authorId()) || userId.equals(repo.ownerId()));
        if (!canEdit) {
            throw ApiException.forbidden("只有议题作者或仓库所有者可以修改议题");
        }
        String title = request.title() == null || request.title().isBlank()
                ? issue.title() : request.title().trim();
        String body = request.body() == null ? issue.body() : request.body().trim();
        String state = request.state() == null ? issue.state() : request.state();
        issueRepository.update(issue.id(), title, body, state);
        return detail(repo, number);
    }

    public IssueDetailView addComment(RepoRecord repo, int number, Long userId, CreateCommentRequest request) {
        IssueRecord issue = require(repo, number);
        issueRepository.addComment(issue.id(), userId, request.body().trim());
        return detail(repo, number);
    }

    private IssueView toView(IssueRecord record) {
        return new IssueView(
                record.id(),
                record.number(),
                record.title(),
                record.body(),
                record.state(),
                new AuthorView(record.authorId(), record.authorUsername(),
                        record.authorDisplayName(), record.authorAvatarColor()),
                record.createdAt(),
                record.updatedAt(),
                record.closedAt(),
                record.commentCount()
        );
    }
}
