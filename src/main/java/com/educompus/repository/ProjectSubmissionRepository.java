package com.educompus.repository;

import com.educompus.model.ProjectSubmission;
import com.educompus.model.ProjectSubmissionView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ProjectSubmissionRepository {
    public Map<Integer, Integer> countByProject() {
        String sql = "SELECT project_id, COUNT(*) AS cnt FROM project_submission GROUP BY project_id";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Map<Integer, Integer> out = new HashMap<>();
            while (rs.next()) {
                out.put(rs.getInt("project_id"), rs.getInt("cnt"));
            }
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to count submissions by project: " + safeMsg(e), e);
        }
    }

    public void create(ProjectSubmission submission) {
        if (submission == null) {
            throw new IllegalArgumentException("submission is null");
        }
        String sql = """
                INSERT INTO project_submission (project_id, student_id, text_response, cahier_path, dossier_path, submitted_at)
                VALUES (?, ?, ?, ?, ?, NOW())
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, submission.getProjectId());
            ps.setInt(2, submission.getStudentId());
            ps.setString(3, submission.getTextResponse());
            ps.setString(4, submission.getCahierPath());
            ps.setString(5, submission.getDossierPath());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    submission.setId(keys.getInt(1));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create submission: " + safeMsg(e), e);
        }
    }

    public void updateMine(ProjectSubmission submission) {
        if (submission == null) {
            throw new IllegalArgumentException("submission is null");
        }
        if (submission.getId() <= 0) {
            throw new IllegalArgumentException("submission.id is required");
        }
        if (submission.getStudentId() <= 0) {
            throw new IllegalArgumentException("submission.studentId is required");
        }

        String sql = """
                UPDATE project_submission
                SET text_response = ?,
                    cahier_path = ?,
                    dossier_path = ?,
                    submitted_at = NOW()
                WHERE id = ?
                  AND student_id = ?
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, submission.getTextResponse());
            ps.setString(2, submission.getCahierPath());
            ps.setString(3, submission.getDossierPath());
            ps.setInt(4, submission.getId());
            ps.setInt(5, submission.getStudentId());
            int updated = ps.executeUpdate();
            if (updated <= 0) {
                throw new IllegalStateException("No submission updated (not found or not yours).");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update submission: " + safeMsg(e), e);
        }
    }

    public void deleteMine(int submissionId, int studentId) {
        int id = Math.max(0, submissionId);
        int sid = Math.max(0, studentId);
        if (id <= 0 || sid <= 0) {
            throw new IllegalArgumentException("submissionId and studentId are required");
        }
        String sql = "DELETE FROM project_submission WHERE id = ? AND student_id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.setInt(2, sid);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete submission: " + safeMsg(e), e);
        }
    }

    public List<ProjectSubmissionView> listAll() {
        String sql = """
                SELECT ps.id,
                       ps.project_id,
                       p.title AS project_title,
                       ps.student_id,
                       u.email AS student_email,
                       ps.text_response,
                       ps.cahier_path,
                       ps.dossier_path,
                       ps.submitted_at
                FROM project_submission ps
                LEFT JOIN project p ON p.id = ps.project_id
                LEFT JOIN `user` u ON u.id = ps.student_id
                ORDER BY ps.submitted_at DESC, ps.id DESC
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<ProjectSubmissionView> out = new ArrayList<>();
            while (rs.next()) {
                out.add(mapView(rs));
            }
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list submissions: " + safeMsg(e), e);
        }
    }

    public List<ProjectSubmissionView> listByStudentId(int studentId) {
        String sql = """
                SELECT ps.id,
                       ps.project_id,
                       p.title AS project_title,
                       ps.student_id,
                       u.email AS student_email,
                       ps.text_response,
                       ps.cahier_path,
                       ps.dossier_path,
                       ps.submitted_at
                FROM project_submission ps
                LEFT JOIN project p ON p.id = ps.project_id
                LEFT JOIN `user` u ON u.id = ps.student_id
                WHERE ps.student_id = ?
                ORDER BY ps.submitted_at DESC, ps.id DESC
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ProjectSubmissionView> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapView(rs));
                }
                return out;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list your submissions: " + safeMsg(e), e);
        }
    }

    public List<ProjectSubmissionView> listMine(int studentId, String email) {
        int id = Math.max(0, studentId);
        String mail = email == null ? "" : email.trim().toLowerCase();
        if (id <= 0 && mail.isBlank()) {
            return List.of();
        }

        String sql = """
                SELECT ps.id,
                       ps.project_id,
                       p.title AS project_title,
                       ps.student_id,
                       u.email AS student_email,
                       ps.text_response,
                       ps.cahier_path,
                       ps.dossier_path,
                       ps.submitted_at
                FROM project_submission ps
                LEFT JOIN project p ON p.id = ps.project_id
                LEFT JOIN `user` u ON u.id = ps.student_id
                WHERE %s
                ORDER BY ps.submitted_at DESC, ps.id DESC
                """.formatted(
                id > 0 && !mail.isBlank()
                        ? "(ps.student_id = ? AND LOWER(u.email) = ?)"
                        : (id > 0 ? "ps.student_id = ?" : "LOWER(u.email) = ?")
        );
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (id > 0 && !mail.isBlank()) {
                ps.setInt(1, id);
                ps.setString(2, mail);
            } else if (id > 0) {
                ps.setInt(1, id);
            } else {
                ps.setString(1, mail);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<ProjectSubmissionView> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapView(rs));
                }
                return out;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list your submissions: " + safeMsg(e), e);
        }
    }

    public boolean hasSubmission(int projectId, int studentId) {
        String sql = "SELECT 1 FROM project_submission WHERE project_id = ? AND student_id = ? LIMIT 1";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to check submission: " + safeMsg(e), e);
        }
    }

    public boolean hasSubmissionMine(int projectId, int studentId, String email) {
        int pid = Math.max(0, projectId);
        int sid = Math.max(0, studentId);
        String mail = email == null ? "" : email.trim().toLowerCase();
        if (pid <= 0) {
            return false;
        }
        if (mail.isBlank()) {
            return sid > 0 && hasSubmission(pid, sid);
        }
        String sql = """
                SELECT 1
                FROM project_submission ps
                JOIN `user` u ON u.id = ps.student_id
                WHERE ps.project_id = ?
                  AND (%s)
                LIMIT 1
                """.formatted(sid > 0 ? "(ps.student_id = ? AND LOWER(u.email) = ?)" : "LOWER(u.email) = ?");
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pid);
            if (sid > 0) {
                ps.setInt(2, sid);
                ps.setString(3, mail);
            } else {
                ps.setString(2, mail);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to check submission: " + safeMsg(e), e);
        }
    }

    private static ProjectSubmissionView mapView(ResultSet rs) throws Exception {
        ProjectSubmissionView s = new ProjectSubmissionView();
        s.setId(rs.getInt("id"));
        s.setProjectId(rs.getInt("project_id"));
        s.setProjectTitle(rs.getString("project_title"));
        s.setStudentId(rs.getInt("student_id"));
        s.setStudentEmail(rs.getString("student_email"));
        s.setTextResponse(rs.getString("text_response"));
        s.setCahierPath(rs.getString("cahier_path"));
        s.setDossierPath(rs.getString("dossier_path"));
        s.setSubmittedAt(rs.getString("submitted_at"));
        return s;
    }

    private static String safeMsg(Exception e) {
        if (e == null) {
            return "unknown error";
        }
        String msg = String.valueOf(e.getMessage());
        msg = msg.replace('\n', ' ').replace('\r', ' ').trim();
        if (msg.length() > 180) {
            msg = msg.substring(0, 180) + "...";
        }
        return msg;
    }
}
