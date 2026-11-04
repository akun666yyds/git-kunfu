package com.gitkunfu.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class RepoRepository {

    private static final String BASE_SELECT = """
            SELECT r.*, u.username AS owner_username, u.display_name AS owner_display_name,
                   u.avatar_color AS owner_avatar_color,
                   (SELECT COUNT(*) FROM issues i WHERE i.repo_id = r.id AND i.state = 'OPEN') AS open_issues,
                   (SELECT COUNT(*) FROM pull_requests p WHERE p.repo_id = r.id AND p.state = 'OPEN') AS open_pulls
            FROM repositories r
            JOIN users u ON u.id = r.owner_id
            """;

    private static final RowMapper<RepoRecord> MAPPER = (rs, rowNum) -> new RepoRecord(
            rs.getLong("id"),
            rs.getLong("owner_id"),
            rs.getString("owner_username"),
            rs.getString("owner_display_name"),
            rs.getString("owner_avatar_color"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("visibility"),
            rs.getString("default_branch"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime(),
            rs.getTimestamp("pushed_at") == null ? null : rs.getTimestamp("pushed_at").toLocalDateTime(),
            rs.getLong("open_issues"),
            rs.getLong("open_pulls")
    );

    private final JdbcTemplate jdbc;

    public RepoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<RepoRecord> findByOwnerAndName(String owner, String name) {
        List<RepoRecord> rows = jdbc.query(BASE_SELECT + " WHERE u.username = ? AND r.name = ?",
                MAPPER, owner, name);
        return rows.stream().findFirst();
    }

    public Optional<RepoRecord> findById(Long id) {
        List<RepoRecord> rows = jdbc.query(BASE_SELECT + " WHERE r.id = ?", MAPPER, id);
        return rows.stream().findFirst();
    }

    public List<RepoRecord> listByOwner(Long ownerId, boolean includePrivate) {
        String sql = BASE_SELECT + " WHERE r.owner_id = ?";
        if (!includePrivate) {
            sql += " AND r.visibility = 'PUBLIC'";
        }
        sql += " ORDER BY r.updated_at DESC, r.id DESC";
        return jdbc.query(sql, MAPPER, ownerId);
    }

    public List<RepoRecord> searchPublic(String query, int page, int size) {
        String like = "%" + (query == null ? "" : query.trim()) + "%";
        List<Object> args = new ArrayList<>();
        String sql = BASE_SELECT
                + " WHERE r.visibility = 'PUBLIC'"
                + " AND (r.name LIKE ? OR r.description LIKE ? OR u.username LIKE ?)";
        args.add(like);
        args.add(like);
        args.add(like);
        sql += " ORDER BY r.updated_at DESC, r.id DESC LIMIT ? OFFSET ?";
        args.add(size);
        args.add((page - 1) * size);
        return jdbc.query(sql, MAPPER, args.toArray());
    }

    public long countPublic(String query) {
        String like = "%" + (query == null ? "" : query.trim()) + "%";
        Long value = jdbc.queryForObject("""
                SELECT COUNT(*) FROM repositories r JOIN users u ON u.id = r.owner_id
                WHERE r.visibility = 'PUBLIC'
                  AND (r.name LIKE ? OR r.description LIKE ? OR u.username LIKE ?)
                """, Long.class, like, like, like);
        return value == null ? 0 : value;
    }

    public List<RepoRecord> listFeatured(int limit) {
        return jdbc.query(BASE_SELECT
                + " WHERE r.visibility = 'PUBLIC' ORDER BY r.updated_at DESC, r.id DESC LIMIT ?",
                MAPPER, limit);
    }

    public long countVisibleByOwner(Long ownerId, Long viewerId) {
        if (viewerId != null && viewerId.equals(ownerId)) {
            Long total = jdbc.queryForObject("SELECT COUNT(*) FROM repositories WHERE owner_id = ?",
                    Long.class, ownerId);
            return total == null ? 0 : total;
        }
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM repositories WHERE owner_id = ? AND visibility = 'PUBLIC'",
                Long.class, ownerId);
        return total == null ? 0 : total;
    }

    public long insert(Long ownerId, String name, String description, String visibility, String defaultBranch) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO repositories (owner_id, name, description, visibility, default_branch) "
                            + "VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, ownerId);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setString(4, visibility);
            ps.setString(5, defaultBranch);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("无法创建仓库");
        }
        return key.longValue();
    }

    public void updateMeta(Long id, String description, String visibility, String defaultBranch) {
        jdbc.update("UPDATE repositories SET description = ?, visibility = ?, default_branch = ?, "
                + "updated_at = ? WHERE id = ?", description, visibility, defaultBranch,
                Timestamp.valueOf(LocalDateTime.now()), id);
    }

    public void updateDefaultBranch(Long id, String branch) {
        jdbc.update("UPDATE repositories SET default_branch = ? WHERE id = ?", branch, id);
    }

    public void touchPushed(Long id) {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        jdbc.update("UPDATE repositories SET pushed_at = ?, updated_at = ? WHERE id = ?", now, now, id);
    }

    public void delete(Long id) {
        jdbc.update("DELETE FROM repositories WHERE id = ?", id);
    }
}
