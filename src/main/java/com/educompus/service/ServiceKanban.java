package com.educompus.service;

import com.educompus.model.KanbanTask;
import com.educompus.model.KanbanStatus;
import com.educompus.repository.EducompusDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServiceKanban {

    public void addTask(KanbanTask t) throws Exception {
        String sql = "INSERT INTO kanban_task (title, description, status, position, project_id, student_id, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getTitle());
            ps.setString(2, t.getDescription());
            ps.setString(3, t.getStatus() != null ? t.getStatus().dbValue() : KanbanStatus.TODO.dbValue());
            ps.setInt(4, t.getPosition());
            ps.setInt(5, t.getProjectId());
            ps.setInt(6, t.getStudentId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) t.setId(rs.getInt(1));
            }
        }
    }

    public void updateTask(KanbanTask t) throws Exception {
        String sql = "UPDATE kanban_task SET title=?, description=?, status=?, position=? WHERE id=?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getTitle());
            ps.setString(2, t.getDescription());
            ps.setString(3, t.getStatus() != null ? t.getStatus().dbValue() : KanbanStatus.TODO.dbValue());
            ps.setInt(4, t.getPosition());
            ps.setInt(5, t.getId());
            ps.executeUpdate();
        }
    }

    public void deleteTask(int id) throws Exception {
        String sql = "DELETE FROM kanban_task WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void setStatus(int id, KanbanStatus status) throws Exception {
        String sql = "UPDATE kanban_task SET status = ? WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status != null ? status.dbValue() : KanbanStatus.TODO.dbValue());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public List<KanbanTask> listByProjectAndStudent(int projectId, int studentId) throws Exception {
        List<KanbanTask> list = new ArrayList<>();
        String sql = "SELECT id, title, description, status, position, project_id, student_id, created_at FROM kanban_task WHERE project_id = ? AND student_id = ? ORDER BY position ASC";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    KanbanTask t = new KanbanTask();
                    t.setId(rs.getInt("id"));
                    t.setTitle(rs.getString("title"));
                    t.setDescription(rs.getString("description"));
                    t.setStatus(KanbanStatus.fromDb(rs.getString("status")));
                    t.setPosition(rs.getInt("position"));
                    t.setProjectId(rs.getInt("project_id"));
                    t.setStudentId(rs.getInt("student_id"));
                    list.add(t);
                }
            }
        }
        return list;
    }
}
