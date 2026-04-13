package com.educompus.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Gère la progression de lecture des chapitres par étudiant.
 */
public final class ChapitreProgressRepository {

    /** Retourne les IDs des chapitres terminés par un étudiant pour un cours donné. */
    public Set<Integer> getCompletedChapitres(int studentId, int coursId) {
        Set<Integer> done = new HashSet<>();
        String sql = """
            SELECT cp.chapitre_id FROM chapitre_progress cp
            JOIN chapitre c ON c.id = cp.chapitre_id
            WHERE cp.student_id = ? AND c.cours_id = ? AND cp.completed = 1
            """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, coursId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) done.add(rs.getInt("chapitre_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return done;
    }

    /** Marque un chapitre comme terminé (ou non) pour un étudiant. */
    public void setCompleted(int studentId, int chapitreId, boolean completed) {
        String sql = """
            INSERT INTO chapitre_progress (chapitre_id, student_id, completed, completed_at)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE completed = VALUES(completed), completed_at = VALUES(completed_at)
            """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, chapitreId);
            ps.setInt(2, studentId);
            ps.setInt(3, completed ? 1 : 0);
            ps.setString(4, completed ? java.time.LocalDateTime.now().toString() : null);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Retourne le nombre de chapitres terminés pour un cours. */
    public int countCompleted(int studentId, int coursId) {
        return getCompletedChapitres(studentId, coursId).size();
    }
}
