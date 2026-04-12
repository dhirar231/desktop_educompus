package com.educompus.service;

import com.educompus.model.AuthUser;
import com.educompus.repository.EducompusDB;

import at.favre.lib.crypto.bcrypt.BCrypt;
import de.mkammerer.argon2.Argon2Factory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public final class DbAuthService {
    private DbAuthService() {
    }

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception ignored) {
        }
    }

    public static AuthUser authenticate(String email, String plainPassword) {
        String mail = email == null ? "" : email.trim().toLowerCase();
        String pass = plainPassword == null ? "" : plainPassword;
        if (mail.isBlank() || pass.isBlank()) {
            return null;
        }

        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, email, name, last_name, image_url, roles, password FROM `user` WHERE email = ? LIMIT 1"
             )) {
            ps.setString(1, mail);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                int id = rs.getInt("id");
                String dbEmail = rs.getString("email");
                String name = rs.getString("name");
                String lastName = rs.getString("last_name");
                String imageUrl = rs.getString("image_url");
                String roles = rs.getString("roles");
                String hash = rs.getString("password");

                if (!verifyPassword(pass, hash)) {
                    return null;
                }

                boolean admin = hasRole(roles, "ROLE_ADMIN");
                boolean teacher = hasRole(roles, "ROLE_TEACHER");
                String displayName = buildDisplayName(name, lastName, dbEmail);
                return new AuthUser(id, dbEmail, displayName, imageUrl, admin, teacher);
            }
        } catch (Exception e) {
            throw new IllegalStateException("DB authentication failed: " + safeMessage(e), e);
        }
    }

    public static boolean emailExists(String email) {
        String mail = email == null ? "" : email.trim().toLowerCase();
        if (mail.isBlank()) {
            return false;
        }

        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM `user` WHERE email = ? LIMIT 1"
             )) {
            ps.setString(1, mail);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new IllegalStateException("DB check failed: " + safeMessage(e), e);
        }
    }

    public static void registerUser(String fullName, String email, String plainPassword) {
        String name = fullName == null ? "" : fullName.trim();
        String mail = email == null ? "" : email.trim().toLowerCase();
        String pass = plainPassword == null ? "" : plainPassword;

        if (name.isBlank()) {
            throw new IllegalArgumentException("Le nom est obligatoire.");
        }
        if (mail.isBlank()) {
            throw new IllegalArgumentException("L'email est obligatoire.");
        }
        if (pass.isBlank()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire.");
        }
        if (emailExists(mail)) {
            throw new IllegalStateException("Cet email existe deja.");
        }

        String hash = BCrypt.withDefaults().hashToString(12, pass.toCharArray());
        String roles = "[\"ROLE_USER\"]";

        String sql = """
                INSERT INTO `user` (email, roles, password, name, last_name, image_url)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, mail);
            ps.setString(2, roles);
            ps.setString(3, hash);
            ps.setString(4, name);
            ps.setString(5, "");
            ps.setString(6, null);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("DB inscription impossible: " + safeMessage(e), e);
        }
    }

    private static String safeMessage(Exception e) {
        if (e == null) {
            return "unknown error";
        }
        String msg = String.valueOf(e.getMessage());
        if (msg.length() > 160) {
            msg = msg.substring(0, 160) + "...";
        }
        return msg;
    }

    private static boolean verifyPassword(String plain, String hash) {
        if (hash == null || hash.isBlank()) {
            return false;
        }

        String h = hash.trim();
        if (h.startsWith("$2a$") || h.startsWith("$2b$") || h.startsWith("$2y$")) {
            return BCrypt.verifyer().verify(plain.toCharArray(), h).verified;
        }

        if (h.startsWith("$argon2id$") || h.startsWith("$argon2i$") || h.startsWith("$argon2d$")) {
            var argon2 = Argon2Factory.create();
            try {
                return argon2.verify(h, plain.toCharArray());
            } finally {
                argon2.wipeArray(plain.toCharArray());
            }
        }

        return false;
    }

    private static boolean hasRole(String rolesRaw, String role) {
        if (rolesRaw == null || rolesRaw.isBlank() || role == null || role.isBlank()) {
            return false;
        }
        return rolesRaw.contains(role);
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
        return mail.isBlank() ? "Compte" : mail;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
