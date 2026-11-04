package com.gitkunfu.repo;

import com.gitkunfu.config.GitProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.ReceivePack;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class GitHttpConfig {

    @Bean
    public ServletRegistrationBean<GitServlet> gitServlet(RepoRepository repoRepository,
                                                         RepoService repoService,
                                                         GitProperties properties) throws IOException {
        Path root = Path.of(properties.repositoryRoot()).toAbsolutePath().normalize();
        Files.createDirectories(root);

        GitServlet servlet = new GitServlet();
        servlet.setRepositoryResolver((HttpServletRequest request, String name) -> {
            String[] pair = GitPathParser.parse(name);
            if (pair == null) {
                return null;
            }
            RepoRecord record = repoRepository.findByOwnerAndName(pair[0], pair[1]).orElse(null);
            if (record == null) {
                return null;
            }
            Path gitDir = root.resolve(record.id() + ".git");
            try {
                return openRepository(gitDir);
            } catch (IOException ex) {
                throw new org.eclipse.jgit.errors.RepositoryNotFoundException(gitDir.toString(), ex);
            }
        });
        servlet.setReceivePackFactory((HttpServletRequest request, Repository db) -> {
            ReceivePack receivePack = new ReceivePack(db);
            String[] pair = GitPathParser.parse(request.getRequestURI());
            if (pair != null) {
                repoRepository.findByOwnerAndName(pair[0], pair[1]).ifPresent(record ->
                        receivePack.setPostReceiveHook((pack, commands) -> repoService.onPush(record.id())));
            }
            return receivePack;
        });

        ServletRegistrationBean<GitServlet> registration = new ServletRegistrationBean<>(servlet, "/git/*");
        registration.setName("gitServlet");
        registration.setLoadOnStartup(1);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<GitAuthFilter> gitAuthFilterRegistration(GitAuthFilter filter) {
        FilterRegistrationBean<GitAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/git/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    private Repository openRepository(Path gitDir) throws IOException {
        return new org.eclipse.jgit.storage.file.FileRepositoryBuilder()
                .setGitDir(gitDir.toFile())
                .setBare()
                .build();
    }
}
