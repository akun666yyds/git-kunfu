package com.gitkunfu.issue;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class IssueRepository {

    private static final String SELECT = """
            SELECT i.*, u.username AS author_username, u.display_name AS author_display_name,
                   u.avatar_color AS author_avatar_color,
                   (SELECT COUNT(*) FROM issue_comments c WHERE c.issue_id = i.id) AS comment_count
            FROM issues i JOIN users u ON u.id = i.author_id
            """;

    private static final RowMapper<IssueRecord> MAPPER = (rs, rowNum) -> new IssueRecord(
            rs.getLong("id"),
            rs.getLong("repo_id"),
            rs.getInt("number"),
            rs.getString("title"),
            rs.getString("body"),
            rs.getString("state"),
            rs.getLong("author_id"),
            rs.getString("author_username"),
            rs.getString("author_display_name"),
            rs.getString("author_avatar_color"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime(),
            rs.getTimestamp("closed_at") == null ? null : rs.getTimestamp("closed_at").toLocalDateTime(),
            rs.getLong("comment_count")
    );

    private static final RowMapper<IssueCommentRecord> COMMENT_MAPPER = (rs, rowNum) -> new IssueCommentRecord(
            rs.getLong("id"),
            rs.getLong("issue_id"),
            rs.getString("body"),
            rs.getLong("author_id"),
            rs.getString("author_username"),
            rs.getString("author_display_name"),
            rs.getString("author_avatar_color"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    private final JdbcTemplate jdbc;

    public IssueRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<IssueRecord> list(Long repoId, String state) {
        if (state == null || state.isBlank() || "all".equalsIgnoreCase(state)) {
            return jdbc.query(SELECT + " WHERE i.repo_id = ? ORDER BY i.created_at DESC, i.id DESC",
                    MAPPER, repoId);
        }
        return jdbc.query(SELECT + " WHERE i.repo_id = ? AND i.state = ? "
                        + "ORDER BY i.created_at DESC, i.id DESC",
                MAPPER, repoId, state.toUpperCase());
    }

    public long count(Long repoId, String state) {
        Long value = jdbc.queryForObject(
                "SELECT COUNT(*) FROM issues WHERE repo_id = ? AND state = ?",
                Long.class, repoId, state);
        return value == null ? 0 : value;
    }

    public Optional<IssueRecord> find(Long repoId, int number) {
        List<IssueRecord> rows = jdbc.query(SELECT + " WHERE i.repo_id = ? AND i.number = ?",
                MAPPER, repoId, number);
        return rows.stream().findFirst();
    }

    public int nextNumber(Long repoId) {
        Integer value = jdbc.queryForObject(
                "SELECT COALESCE(MAX(number), 0) + 1 FROM issues WHERE repo_id = ?",
                Integer.class, repoId);
        return value == null ? 1 : value;
    }

    public long insert(Long repoId, int number, String title, String body, Long authorId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO issues (repo_id, number, title, body, author_id) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, repoId);
            ps.setInt(2, number);
            ps.setString(3, title);
            ps.setString(4, body);
            ps.setLong(5, authorId);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("无法创建议题");
        }
        return key.longValue();
    }

    public void update(Long id, String title, String body, String state) {
        Timestamp closedAt = "CLOSED".equals(state) ? Timestamp.valueOf(LocalDateTime.now()) : null;
        jdbc.update("UPDATE issues SET title = ?, body = ?, state = ?, closed_at = ?, updated_at = ? WHERE id = ?",
                title, body, state, closedAt, Timestamp.valueOf(LocalDateTime.now()), id);
    }

    public void toggleState(Long id, String state) {
        Timestamp closedAt = "CLOSED".equals(state) ? Timestamp.valueOf(LocalDateTime.now()) : null;
        jdbc.update("UPDATE issues SET state = ?, closed_at = ?, updated_at = ? WHERE id = ?",
                state, closedAt, Timestamp.valueOf(LocalDateTime.now()), id);
    }

    public List<IssueCommentRecord> listComments(Long issueId) {
        return jdbc.query("""
                SELECT c.*, u.username AS author_username, u.display_name AS author_display_name,
                       u.avatar_color AS author_avatar_color
                FROM issue_comments c JOIN users u ON u.id = c.author_id
                WHERE c.issue_id = ? ORDER BY c.created_at ASC, c.id ASC
                """, COMMENT_MAPPER, issueId);
    }

    public long addComment(Long issueId, Long authorId, String body) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO issue_comments (issue_id, author_id, body) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, issueId);
            ps.setLong(2, authorId);
            ps.setString(3, body);
            return ps;
        }, keyHolder);
        jdbc.update("UPDATE issues SET updated_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now()), issueId);
        Number key = keyHolder.getKey();
        return key == null ? 0 : key.longValue();
    }
}
