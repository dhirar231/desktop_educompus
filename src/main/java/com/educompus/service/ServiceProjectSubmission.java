package com.educompus.service;

import com.educompus.model.ProjectSubmission;
import com.educompus.repository.EducompusDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServiceProjectSubmission {

    public void addSubmission(ProjectSubmission s) throws Exception {
        String sql = "INSERT INTO project_submission (project_id, student_id, text_response, cahier_path, dossier_path, submitted_at) VALUES (?, ?, ?, ?, ?, NOW())";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getProjectId());
            ps.setInt(2, s.getStudentId());
            ps.setString(3, s.getTextResponse());
            ps.setString(4, s.getCahierPath());
            ps.setString(5, s.getDossierPath());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) s.setId(rs.getInt(1));
            }
        }
    }

    public void updateSubmission(ProjectSubmission s) throws Exception {
        String sql = "UPDATE project_submission SET text_response=?, cahier_path=?, dossier_path=? WHERE id=?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getTextResponse());
            ps.setString(2, s.getCahierPath());
            ps.setString(3, s.getDossierPath());
            ps.setInt(4, s.getId());
            ps.executeUpdate();
        }
    }

    public void deleteSubmission(int id) throws Exception {
        String sql = "DELETE FROM project_submission WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<ProjectSubmission> listByProject(int projectId) throws Exception {
        List<ProjectSubmission> list = new ArrayList<>();
        String sql = "SELECT id, project_id, student_id, text_response, cahier_path, dossier_path, submitted_at FROM project_submission WHERE project_id = ? ORDER BY id ASC";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProjectSubmission s = new ProjectSubmission();
                    s.setId(rs.getInt("id"));
                    s.setProjectId(rs.getInt("project_id"));
                    s.setStudentId(rs.getInt("student_id"));
                    s.setTextResponse(rs.getString("text_response"));
                    s.setCahierPath(rs.getString("cahier_path"));
                    s.setDossierPath(rs.getString("dossier_path"));
                    list.add(s);
                }
            }
        }
        return list;
    }

    public List<ProjectSubmission> listByStudent(int studentId) throws Exception {
        List<ProjectSubmission> list = new ArrayList<>();
        String sql = "SELECT id, project_id, student_id, text_response, cahier_path, dossier_path, submitted_at FROM project_submission WHERE student_id = ? ORDER BY id ASC";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProjectSubmission s = new ProjectSubmission();
                    s.setId(rs.getInt("id"));
                    s.setProjectId(rs.getInt("project_id"));
                    s.setStudentId(rs.getInt("student_id"));
                    s.setTextResponse(rs.getString("text_response"));
                    s.setCahierPath(rs.getString("cahier_path"));
                    s.setDossierPath(rs.getString("dossier_path"));
                    list.add(s);
                }
            }
        }
        return list;
    }
}
