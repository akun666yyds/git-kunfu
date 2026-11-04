package com.gitkunfu.pull;

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
public class PullRepository {

    private static final String SELECT = """
            SELECT p.*, u.username AS author_username, u.display_name AS author_display_name,
                   u.avatar_color AS author_avatar_color, m.username AS merged_by_username
            FROM pull_requests p
            JOIN users u ON u.id = p.author_id
            LEFT JOIN users m ON m.id = p.merged_by
            """;

    private static final RowMapper<PullRecord> MAPPER = (rs, rowNum) -> new PullRecord(
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
            rs.getString("base_branch"),
            rs.getString("head_branch"),
            rs.getString("merge_commit_sha"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime(),
            rs.getTimestamp("merged_at") == null ? null : rs.getTimestamp("merged_at").toLocalDateTime(),
            rs.getString("merged_by_username")
    );

    private final JdbcTemplate jdbc;

    public PullRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<PullRecord> list(Long repoId, String state) {
        if (state == null || state.isBlank() || "ALL".equalsIgnoreCase(state)) {
            return jdbc.query(SELECT + " WHERE p.repo_id = ? ORDER BY p.created_at DESC, p.id DESC",
                    MAPPER, repoId);
        }
        if ("CLOSED".equalsIgnoreCase(state)) {
            return jdbc.query(SELECT + " WHERE p.repo_id = ? AND p.state IN ('CLOSED','MERGED') "
                    + "ORDER BY p.created_at DESC, p.id DESC", MAPPER, repoId);
        }
        return jdbc.query(SELECT + " WHERE p.repo_id = ? AND p.state = ? "
                + "ORDER BY p.created_at DESC, p.id DESC", MAPPER, repoId, state.toUpperCase());
    }

    public long count(Long repoId, String state) {
        Long value;
        if ("CLOSED".equalsIgnoreCase(state)) {
            value = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM pull_requests WHERE repo_id = ? AND state IN ('CLOSED','MERGED')",
                    Long.class, repoId);
        } else {
            value = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM pull_requests WHERE repo_id = ? AND state = ?",
                    Long.class, repoId, state);
        }
        return value == null ? 0 : value;
    }

    public Optional<PullRecord> find(Long repoId, int number) {
        List<PullRecord> rows = jdbc.query(SELECT + " WHERE p.repo_id = ? AND p.number = ?",
                MAPPER, repoId, number);
        return rows.stream().findFirst();
    }

    public int nextNumber(Long repoId) {
        Integer value = jdbc.queryForObject(
                "SELECT COALESCE(MAX(number), 0) + 1 FROM pull_requests WHERE repo_id = ?",
                Integer.class, repoId);
        return value == null ? 1 : value;
    }

    public long insert(Long repoId, int number, String title, String body, Long authorId,
                       String baseBranch, String headBranch) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO pull_requests (repo_id, number, title, body, author_id, base_branch, head_branch) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, repoId);
            ps.setInt(2, number);
            ps.setString(3, title);
            ps.setString(4, body);
            ps.setLong(5, authorId);
            ps.setString(6, baseBranch);
            ps.setString(7, headBranch);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("无法创建合并请求");
        }
        return key.longValue();
    }

    public void markMerged(Long id, String sha, Long mergedBy) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbc.update("UPDATE pull_requests SET state = 'MERGED', merge_commit_sha = ?, merged_at = ?, "
                + "merged_by = ?, updated_at = ? WHERE id = ?", sha, now, mergedBy, now, id);
    }

    public void markClosed(Long id) {
        jdbc.update("UPDATE pull_requests SET state = 'CLOSED', updated_at = ? WHERE id = ?",
                Timestamp.valueOf(LocalDateTime.now()), id);
    }
}
