package com.gitkunfu.repo;

import com.gitkunfu.common.AuthContext;
import com.gitkunfu.repo.RepoDtos.BlobView;
import com.gitkunfu.repo.RepoDtos.BranchView;
import com.gitkunfu.repo.RepoDtos.CommitDetailView;
import com.gitkunfu.repo.RepoDtos.CommitPage;
import com.gitkunfu.repo.RepoDtos.CompareView;
import com.gitkunfu.repo.RepoDtos.CreateRepoRequest;
import com.gitkunfu.repo.RepoDtos.ReadmeView;
import com.gitkunfu.repo.RepoDtos.RepoDetailView;
import com.gitkunfu.repo.RepoDtos.RepoListResponse;
import com.gitkunfu.repo.RepoDtos.RepoView;
import com.gitkunfu.repo.RepoDtos.TreeEntryView;
import com.gitkunfu.repo.RepoDtos.UpdateRepoRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
public class RepoController {

    private final RepoService repoService;
    private final GitStorageService storage;

    public RepoController(RepoService repoService, GitStorageService storage) {
        this.repoService = repoService;
        this.storage = storage;
    }

    @GetMapping("/repos")
    public RepoListResponse explore(@RequestParam(required = false) String q,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "20") int size) {
        return repoService.explore(q, page, size);
    }

    @GetMapping("/repos/featured")
    public List<RepoView> featured(@RequestParam(defaultValue = "6") int limit) {
        return repoService.listFeatured(limit);
    }

    @GetMapping("/user/repos")
    public List<RepoView> mine(HttpServletRequest request) {
        return repoService.listMine(AuthContext.requireUserId(request));
    }

    @GetMapping("/users/{username}/repos")
    public List<RepoView> byUser(@PathVariable String username, HttpServletRequest request) {
        return repoService.listByUser(username, AuthContext.optionalUserId(request));
    }

    @PostMapping("/repos")
    public RepoDetailView create(@Valid @RequestBody CreateRepoRequest body, HttpServletRequest request) {
        return repoService.create(AuthContext.requireUserId(request), body);
    }

    @GetMapping("/repos/{owner}/{name}")
    public RepoDetailView detail(@PathVariable String owner, @PathVariable String name,
                                 HttpServletRequest request) {
        RepoRecord record = repoService.requireVisible(owner, name, AuthContext.optionalUserId(request));
        return repoService.detail(record);
    }

    @PatchMapping("/repos/{owner}/{name}")
    public RepoDetailView update(@PathVariable String owner, @PathVariable String name,
                                 @Valid @RequestBody UpdateRepoRequest body, HttpServletRequest request) {
        return repoService.update(AuthContext.requireUserId(request), owner, name, body);
    }

    @DeleteMapping("/repos/{owner}/{name}")
    public ResponseEntity<Void> delete(@PathVariable String owner, @PathVariable String name,
                                       HttpServletRequest request) {
        repoService.delete(AuthContext.requireUserId(request), owner, name);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/repos/{owner}/{name}/branches")
    public List<BranchView> branches(@PathVariable String owner, @PathVariable String name,
                                     HttpServletRequest request) {
        RepoRecord record = repoService.requireVisible(owner, name, AuthContext.optionalUserId(request));
        return storage.listBranches(record.id(), record.defaultBranch());
    }

    @GetMapping("/repos/{owner}/{name}/commits")
    public CommitPage commits(@PathVariable String owner, @PathVariable String name,
                              @RequestParam(required = false) String branch,
                              @RequestParam(defaultValue = "1") int page,
                              @RequestParam(defaultValue = "20") int size,
                              HttpServletRequest request) {
        RepoRecord record = repoService.requireVisible(owner, name, AuthContext.optionalUserId(request));
        String target = branch == null || branch.isBlank() ? record.defaultBranch() : branch;
        return storage.listCommits(record.id(), target, Math.max(page, 1), Math.min(Math.max(size, 1), 100));
    }

    @GetMapping("/repos/{owner}/{name}/commits/{sha}")
    public CommitDetailView commit(@PathVariable String owner, @PathVariable String name,
                                   @PathVariable String sha, HttpServletRequest request) {
        RepoRecord record = repoService.requireVisible(owner, name, AuthContext.optionalUserId(request));
        return storage.commitDetail(record.id(), sha);
    }

    @GetMapping("/repos/{owner}/{name}/tree")
    public List<TreeEntryView> tree(@PathVariable String owner, @PathVariable String name,
                                    @RequestParam(required = false) String branch,
                                    @RequestParam(required = false) String path,
                                    HttpServletRequest request) {
        RepoRecord record = repoService.requireVisible(owner, name, AuthContext.optionalUserId(request));
        String target = branch == null || branch.isBlank() ? record.defaultBranch() : branch;
        return storage.listTree(record.id(), target, path);
    }

    @GetMapping("/repos/{owner}/{name}/blob")
    public BlobView blob(@PathVariable String owner, @PathVariable String name,
                         @RequestParam String path,
                         @RequestParam(required = false) String branch,
                         HttpServletRequest request) {
        RepoRecord record = repoService.requireVisible(owner, name, AuthContext.optionalUserId(request));
        String target = branch == null || branch.isBlank() ? record.defaultBranch() : branch;
        return storage.readBlob(record.id(), target, path);
    }

    @GetMapping("/repos/{owner}/{name}/raw")
    public ResponseEntity<byte[]> raw(@PathVariable String owner, @PathVariable String name,
                                      @RequestParam String path,
                                      @RequestParam(required = false) String branch,
                                      HttpServletRequest request) {
        RepoRecord record = repoService.requireVisible(owner, name, AuthContext.optionalUserId(request));
        String target = branch == null || branch.isBlank() ? record.defaultBranch() : branch;
        byte[] content = storage.rawBlob(record.id(), target, path);
        String fileName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }

    @GetMapping("/repos/{owner}/{name}/readme")
    public ResponseEntity<ReadmeView> readme(@PathVariable String owner, @PathVariable String name,
                                             @RequestParam(required = false) String branch,
                                             HttpServletRequest request) {
        RepoRecord record = repoService.requireVisible(owner, name, AuthContext.optionalUserId(request));
        String target = branch == null || branch.isBlank() ? record.defaultBranch() : branch;
        return storage.readReadme(record.id(), target)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }

    @GetMapping("/repos/{owner}/{name}/compare")
    public CompareView compare(@PathVariable String owner, @PathVariable String name,
                               @RequestParam String base, @RequestParam String head,
                               HttpServletRequest request) {
        RepoRecord record = repoService.requireVisible(owner, name, AuthContext.optionalUserId(request));
        return storage.compare(record.id(), base, head);
    }
}
