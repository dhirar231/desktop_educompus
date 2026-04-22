package com.educompus.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashSet;
import java.util.Set;

public final class CourseFavoriteRepository {

    public CourseFavoriteRepository() {
        ensureSchema();
    }

    public Set<Integer> listFavoriteCourseIds(int studentId) {
        Set<Integer> ids = new LinkedHashSet<>();
        if (studentId <= 0) {
            return ids;
        }

        String sql = """
                SELECT cours_id
                FROM cours_favori
                WHERE student_id = ?
                ORDER BY created_at DESC, id DESC
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("cours_id"));
                }
            }
            return ids;
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger les favoris: " + safeMsg(e), e);
        }
    }

    public boolean isFavorite(int studentId, int coursId) {
        if (studentId <= 0 || coursId <= 0) {
            return false;
        }
        String sql = "SELECT 1 FROM cours_favori WHERE student_id = ? AND cours_id = ? LIMIT 1";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, coursId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de verifier le favori: " + safeMsg(e), e);
        }
    }

    public void addFavorite(int studentId, int coursId) {
        if (studentId <= 0 || coursId <= 0) {
            throw new IllegalArgumentException("Etudiant ou cours invalide.");
        }
        String sql = """
                INSERT INTO cours_favori (student_id, cours_id, created_at)
                VALUES (?, ?, NOW())
                ON DUPLICATE KEY UPDATE created_at = VALUES(created_at)
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, coursId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'ajouter le favori: " + safeMsg(e), e);
        }
    }

    public void removeFavorite(int studentId, int coursId) {
        if (studentId <= 0 || coursId <= 0) {
            throw new IllegalArgumentException("Etudiant ou cours invalide.");
        }
        String sql = "DELETE FROM cours_favori WHERE student_id = ? AND cours_id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, coursId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de retirer le favori: " + safeMsg(e), e);
        }
    }

    private void ensureSchema() {
        try (Connection conn = EducompusDB.getConnection()) {
            executeIgnore(conn, """
                    CREATE TABLE IF NOT EXISTS cours_favori (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        student_id INT NOT NULL,
                        cours_id INT NOT NULL,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        UNIQUE KEY uk_cours_favori_student_course (student_id, cours_id),
                        CONSTRAINT fk_cours_favori_cours FOREIGN KEY (cours_id) REFERENCES cours(id) ON DELETE CASCADE
                    )
                    """);
        } catch (Exception ignored) {
            // Keep app usable even if schema migration cannot be performed.
        }
    }

    private static void executeIgnore(Connection conn, String sql) {
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (Exception ignored) {
            // Ignore schema migration failures.
        }
    }

    private static String safeMsg(Exception e) {
        if (e == null) {
            return "erreur inconnue";
        }
        String msg = String.valueOf(e.getMessage()).replace('\n', ' ').replace('\r', ' ').trim();
        if (msg.length() > 220) {
            msg = msg.substring(0, 220) + "...";
        }
        return msg;
    }
}
