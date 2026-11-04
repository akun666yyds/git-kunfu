CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(39)  NOT NULL,
    email         VARCHAR(190) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(80)  NOT NULL,
    bio           VARCHAR(255) NOT NULL DEFAULT '',
    avatar_color  VARCHAR(16)  NOT NULL DEFAULT '#7d8b74',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS repositories (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    owner_id       BIGINT       NOT NULL,
    name           VARCHAR(100) NOT NULL,
    description    VARCHAR(500) NOT NULL DEFAULT '',
    visibility     VARCHAR(10)  NOT NULL DEFAULT 'PUBLIC',
    default_branch VARCHAR(100) NOT NULL DEFAULT 'main',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    pushed_at      DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_repos_owner_name (owner_id, name),
    KEY idx_repos_updated (updated_at),
    CONSTRAINT fk_repos_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS issues (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    repo_id    BIGINT       NOT NULL,
    number     INT          NOT NULL,
    title      VARCHAR(255) NOT NULL,
    body       TEXT         NOT NULL,
    state      VARCHAR(10)  NOT NULL DEFAULT 'OPEN',
    author_id  BIGINT       NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at  DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_issues_repo_number (repo_id, number),
    KEY idx_issues_repo_state (repo_id, state),
    CONSTRAINT fk_issues_repo FOREIGN KEY (repo_id) REFERENCES repositories (id) ON DELETE CASCADE,
    CONSTRAINT fk_issues_author FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS issue_comments (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    issue_id   BIGINT   NOT NULL,
    author_id  BIGINT   NOT NULL,
    body       TEXT     NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_comments_issue (issue_id),
    CONSTRAINT fk_comments_issue FOREIGN KEY (issue_id) REFERENCES issues (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS pull_requests (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    repo_id           BIGINT       NOT NULL,
    number            INT          NOT NULL,
    title             VARCHAR(255) NOT NULL,
    body              TEXT         NOT NULL,
    state             VARCHAR(10)  NOT NULL DEFAULT 'OPEN',
    author_id         BIGINT       NOT NULL,
    base_branch       VARCHAR(100) NOT NULL,
    head_branch       VARCHAR(100) NOT NULL,
    merge_commit_sha  VARCHAR(40)  NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    merged_at         DATETIME     NULL,
    merged_by         BIGINT       NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pulls_repo_number (repo_id, number),
    KEY idx_pulls_repo_state (repo_id, state),
    CONSTRAINT fk_pulls_repo FOREIGN KEY (repo_id) REFERENCES repositories (id) ON DELETE CASCADE,
    CONSTRAINT fk_pulls_author FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT fk_pulls_merger FOREIGN KEY (merged_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
