package com.educompus.repository;

import com.educompus.model.KanbanStatus;
import com.educompus.model.KanbanTask;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class KanbanTaskRepository {
    public List<KanbanTask> listByProjectAndStudent(int projectId, int studentId) {
        String sql = """
                SELECT id, title, description, status, position, project_id, student_id, created_at
                FROM kanban_task
                WHERE project_id = ? AND student_id = ?
                ORDER BY status ASC, position ASC, id ASC
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                List<KanbanTask> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list kanban tasks: " + safeMsg(e), e);
        }
    }

    public void create(KanbanTask task) {
        if (task == null) {
            throw new IllegalArgumentException("task is null");
        }
        int nextPos = nextPosition(task.getProjectId(), task.getStudentId(), task.getStatus());
        String sql = """
                INSERT INTO kanban_task (title, description, status, position, project_id, student_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW())
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getStatus().dbValue());
            ps.setInt(4, nextPos);
            ps.setInt(5, task.getProjectId());
            ps.setInt(6, task.getStudentId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    task.setId(keys.getInt(1));
                }
            }
            task.setPosition(nextPos);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create kanban task: " + safeMsg(e), e);
        }
    }

    public void updateStatus(int taskId, KanbanStatus status) {
        if (status == null) {
            status = KanbanStatus.TODO;
        }
        KanbanTask existing = getById(taskId);
        if (existing == null) {
            return;
        }
        int nextPos = nextPosition(existing.getProjectId(), existing.getStudentId(), status);
        String sql = "UPDATE kanban_task SET status = ?, position = ? WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.dbValue());
            ps.setInt(2, nextPos);
            ps.setInt(3, taskId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update task status: " + safeMsg(e), e);
        }
    }

    public void updateMine(KanbanTask task) {
        if (task == null) {
            throw new IllegalArgumentException("task is null");
        }
        if (task.getId() <= 0) {
            throw new IllegalArgumentException("task.id is required");
        }
        if (task.getStudentId() <= 0) {
            throw new IllegalArgumentException("task.studentId is required");
        }
        KanbanStatus status = task.getStatus() == null ? KanbanStatus.TODO : task.getStatus();

        KanbanTask existing = getById(task.getId());
        if (existing == null) {
            return;
        }

        int pos = existing.getPosition();
        if (existing.getStatus() != status) {
            pos = nextPosition(existing.getProjectId(), existing.getStudentId(), status);
        }

        String sql = """
                UPDATE kanban_task
                SET title = ?,
                    description = ?,
                    status = ?,
                    position = ?
                WHERE id = ?
                  AND student_id = ?
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, status.dbValue());
            ps.setInt(4, pos);
            ps.setInt(5, task.getId());
            ps.setInt(6, task.getStudentId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update kanban task: " + safeMsg(e), e);
        }
    }

    public void delete(int taskId) {
        String sql = "DELETE FROM kanban_task WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, taskId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete task: " + safeMsg(e), e);
        }
    }

    private KanbanTask getById(int id) {
        String sql = """
                SELECT id, title, description, status, position, project_id, student_id, created_at
                FROM kanban_task
                WHERE id = ?
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read task: " + safeMsg(e), e);
        }
    }

    private int nextPosition(int projectId, int studentId, KanbanStatus status) {
        String sql = """
                SELECT COALESCE(MAX(position), 0) AS max_pos
                FROM kanban_task
                WHERE project_id = ? AND student_id = ? AND status = ?
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.setInt(2, studentId);
            ps.setString(3, status.dbValue());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("max_pos") + 1;
                }
                return 1;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute kanban position: " + safeMsg(e), e);
        }
    }

    private static KanbanTask map(ResultSet rs) throws Exception {
        KanbanTask t = new KanbanTask();
        t.setId(rs.getInt("id"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));
        t.setStatus(KanbanStatus.fromDb(rs.getString("status")));
        t.setPosition(rs.getInt("position"));
        t.setProjectId(rs.getInt("project_id"));
        t.setStudentId(rs.getInt("student_id"));
        t.setCreatedAt(rs.getString("created_at"));
        return t;
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
