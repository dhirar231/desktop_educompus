package com.educompus.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Service pour enregistrer les activités des étudiants.
 * Utilisé par le module de détection des étudiants désengagés.
 */
public final class ActivityTrackingService {
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/educompus";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    
    /**
     * Enregistre qu'un étudiant a consulté un chapitre.
     * 
     * @param userId ID de l'étudiant
     * @param chapitreId ID du chapitre
     */
    public static void logChapterView(int userId, int chapitreId) {
        String sql = """
            INSERT INTO chapitre_progress (user_id, chapitre_id, completed, completed_at)
            VALUES (?, ?, TRUE, NOW())
            ON DUPLICATE KEY UPDATE 
                completed = TRUE, 
                completed_at = NOW()
            """;
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, chapitreId);
            stmt.executeUpdate();
            
            System.out.println("✅ Chapitre " + chapitreId + " marqué comme consulté pour l'étudiant " + userId);
            
        } catch (SQLException e) {
            System.err.println("⚠️ Erreur lors de l'enregistrement de la consultation du chapitre : " + e.getMessage());
        }
    }
    
    /**
     * Enregistre qu'un étudiant a téléchargé un PDF.
     * 
     * @param studentId ID de l'étudiant
     * @param courseId ID du cours
     * @param chapterId ID du chapitre (peut être null)
     * @param pdfType Type de PDF (CHAPTER, TD, VIDEO)
     */
    public static void logPdfDownload(int studentId, int courseId, Integer chapterId, String pdfType) {
        String sql = """
            INSERT INTO pdf_download_log (student_id, course_id, chapter_id, pdf_type, downloaded_at)
            VALUES (?, ?, ?, ?, NOW())
            """;
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, studentId);
            stmt.setInt(2, courseId);
            if (chapterId != null) {
                stmt.setInt(3, chapterId);
            } else {
                stmt.setNull(3, java.sql.Types.INTEGER);
            }
            stmt.setString(4, pdfType);
            stmt.executeUpdate();
            
            System.out.println("✅ Téléchargement PDF enregistré pour l'étudiant " + studentId);
            
        } catch (SQLException e) {
            System.err.println("⚠️ Erreur lors de l'enregistrement du téléchargement PDF : " + e.getMessage());
        }
    }
    
    /**
     * Enregistre une connexion d'utilisateur.
     * 
     * @param userId ID de l'utilisateur
     * @param ipAddress Adresse IP (peut être null)
     */
    public static void logUserConnection(int userId, String ipAddress) {
        String sql = """
            INSERT INTO user_activity_log (user_id, date_connexion, ip_address)
            VALUES (?, NOW(), ?)
            """;
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            stmt.setString(2, ipAddress);
            stmt.executeUpdate();
            
            System.out.println("✅ Connexion enregistrée pour l'utilisateur " + userId);
            
        } catch (SQLException e) {
            System.err.println("⚠️ Erreur lors de l'enregistrement de la connexion : " + e.getMessage());
        }
    }
    
    /**
     * Enregistre la présence d'un étudiant à une session live.
     * 
     * @param sessionId ID de la session
     * @param studentId ID de l'étudiant
     */
    public static void logSessionAttendance(int sessionId, int studentId) {
        String sql = """
            INSERT INTO session_attendance (session_id, student_id, attended_at)
            VALUES (?, ?, NOW())
            ON DUPLICATE KEY UPDATE attended_at = NOW()
            """;
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, sessionId);
            stmt.setInt(2, studentId);
            stmt.executeUpdate();
            
            System.out.println("✅ Présence enregistrée pour l'étudiant " + studentId + " à la session " + sessionId);
            
        } catch (SQLException e) {
            System.err.println("⚠️ Erreur lors de l'enregistrement de la présence : " + e.getMessage());
        }
    }
}
