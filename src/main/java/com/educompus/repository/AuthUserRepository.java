package com.educompus.repository;

import com.educompus.model.AuthUser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AuthUserRepository {

    public List<AuthUser> findAll() throws SQLException {
        String sql = """
                SELECT id, email, name, last_name, image_url, roles
                FROM `user`
                ORDER BY id DESC
                """;

        List<AuthUser> users = new ArrayList<>();
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(mapRow(rs));
            }
        }
        return users;
    }

    public int create(AuthUser user, String passwordHash) throws SQLException {
        String sql = """
                INSERT INTO `user` (email, roles, password, name, last_name, image_url)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        NameParts names = splitDisplayName(user.displayName());
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, normalizeEmail(user.email()));
            ps.setString(2, encodeRoles(user.admin(), user.teacher()));
            ps.setString(3, passwordHash);
            ps.setString(4, names.name());
            ps.setString(5, names.lastName());
            ps.setString(6, normalizeNullable(user.imageUrl()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return 0;
    }

    public void update(AuthUser user) throws SQLException {
        String sql = """
                UPDATE `user`
                SET email = ?, roles = ?, name = ?, last_name = ?, image_url = ?
                WHERE id = ?
                """;

        NameParts names = splitDisplayName(user.displayName());
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizeEmail(user.email()));
            ps.setString(2, encodeRoles(user.admin(), user.teacher()));
            ps.setString(3, names.name());
            ps.setString(4, names.lastName());
            ps.setString(5, normalizeNullable(user.imageUrl()));
            ps.setInt(6, user.id());
            ps.executeUpdate();
        }
    }

    public void updatePassword(int userId, String passwordHash) throws SQLException {
        String sql = "UPDATE `user` SET password = ? WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void delete(int userId) throws SQLException {
        String sql = "DELETE FROM `user` WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

<<<<<<< HEAD
=======
    public AuthUser findByEmail(String email) throws SQLException {
        String sql = """
                SELECT id, email, name, last_name, image_url, roles
                FROM `user`
                WHERE email = ?
                LIMIT 1
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizeEmail(email));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public AuthUser findById(int id) throws SQLException {
        String sql = """
                SELECT id, email, name, last_name, image_url, roles
                FROM `user`
                WHERE id = ?
                LIMIT 1
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

>>>>>>> origin/main
    public boolean emailExists(String email, Integer excludeUserId) throws SQLException {
        String sql = "SELECT 1 FROM `user` WHERE email = ? AND (? IS NULL OR id <> ?) LIMIT 1";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizeEmail(email));
            if (excludeUserId == null) {
                ps.setNull(2, java.sql.Types.INTEGER);
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setInt(2, excludeUserId);
                ps.setInt(3, excludeUserId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private AuthUser mapRow(ResultSet rs) throws SQLException {
        String roles = safe(rs.getString("roles"));
        return new AuthUser(
                rs.getInt("id"),
                safe(rs.getString("email")),
                buildDisplayName(rs.getString("name"), rs.getString("last_name"), rs.getString("email")),
                safe(rs.getString("image_url")),
                hasRole(roles, "ROLE_ADMIN"),
                hasRole(roles, "ROLE_TEACHER")
        );
    }

    private static String buildDisplayName(String name, String lastName, String email) {
        String full = (safe(name) + " " + safe(lastName)).trim();
        if (!full.isBlank()) {
            return full;
        }

        String mail = safe(email);
        int at = mail.indexOf('@');
        if (at > 0) {
            return mail.substring(0, at);
        }
        return mail.isBlank() ? "Utilisateur" : mail;
    }

    private static boolean hasRole(String rolesRaw, String role) {
        return !rolesRaw.isBlank() && !role.isBlank() && rolesRaw.contains(role);
    }

    private static String encodeRoles(boolean admin, boolean teacher) {
        Set<String> roles = new LinkedHashSet<>();
        roles.add("ROLE_USER");
        if (admin) {
            roles.add("ROLE_ADMIN");
        }
        if (teacher) {
            roles.add("ROLE_TEACHER");
        }

        StringBuilder out = new StringBuilder("[");
        int index = 0;
        for (String role : roles) {
            if (index++ > 0) {
                out.append(',');
            }
            out.append('"').append(role).append('"');
        }
        out.append(']');
        return out.toString();
    }

    private static NameParts splitDisplayName(String displayName) {
        String value = safe(displayName);
        if (value.isBlank()) {
            return new NameParts("Utilisateur", "");
        }

        String[] parts = value.split("\\s+", 2);
        String first = parts[0];
        String last = parts.length > 1 ? parts[1] : "";
        return new NameParts(first, last);
    }

    private static String normalizeEmail(String email) {
        return safe(email).toLowerCase();
    }

    private static String normalizeNullable(String value) {
        String normalized = safe(value);
        return normalized.isBlank() ? null : normalized;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record NameParts(String name, String lastName) {
    }
}
