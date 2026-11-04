package com.gitkunfu.repo;

public final class GitPathParser {

    private GitPathParser() {
    }

    /**
     * 从 "/git/owner/repo.git/info/refs" 这样的路径中解析出 owner 和 repo。
     */
    public static String[] parse(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }
        String path = rawPath.trim();
        int gitIndex = path.indexOf("/git/");
        if (gitIndex >= 0) {
            path = path.substring(gitIndex + 5);
        } else if (path.startsWith("git/")) {
            path = path.substring(4);
        } else if (path.startsWith("/")) {
            path = path.substring(1);
        }
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        int slash = path.indexOf('/');
        if (slash <= 0) {
            return null;
        }
        String owner = path.substring(0, slash);
        String rest = path.substring(slash + 1);
        int nextSlash = rest.indexOf('/');
        String repo = nextSlash >= 0 ? rest.substring(0, nextSlash) : rest;
        if (repo.endsWith(".git")) {
            repo = repo.substring(0, repo.length() - 4);
        }
        if (owner.isBlank() || repo.isBlank()) {
            return null;
        }
        return new String[]{owner, repo};
    }
}
