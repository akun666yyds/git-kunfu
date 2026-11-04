package com.gitkunfu.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private static final RowMapper<UserRecord> MAPPER = (rs, rowNum) -> new UserRecord(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getString("display_name"),
            rs.getString("bio"),
            rs.getString("avatar_color"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserRecord> findById(Long id) {
        List<UserRecord> rows = jdbc.query("SELECT * FROM users WHERE id = ?", MAPPER, id);
        return rows.stream().findFirst();
    }

    public Optional<UserRecord> findByUsername(String username) {
        List<UserRecord> rows = jdbc.query("SELECT * FROM users WHERE username = ?", MAPPER, username);
        return rows.stream().findFirst();
    }

    public Optional<UserRecord> findByLogin(String login) {
        List<UserRecord> rows = jdbc.query(
                "SELECT * FROM users WHERE username = ? OR email = ?", MAPPER, login, login);
        return rows.stream().findFirst();
    }

    public boolean existsByUsername(String username) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE username = ?", Integer.class, username);
        return count != null && count > 0;
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
        return count != null && count > 0;
    }

    public long insert(String username, String email, String passwordHash,
                       String displayName, String avatarColor) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (username, email, password_hash, display_name, avatar_color) "
                            + "VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setString(4, displayName);
            ps.setString(5, avatarColor);
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("无法创建用户");
        }
        return key.longValue();
    }

    public void updateProfile(Long id, String displayName, String bio) {
        jdbc.update("UPDATE users SET display_name = ?, bio = ? WHERE id = ?", displayName, bio, id);
    }
}
