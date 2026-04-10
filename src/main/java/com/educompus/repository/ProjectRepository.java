package com.educompus.repository;

import com.educompus.model.Project;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class ProjectRepository {
    public List<Project> listAll(String query) {
        String q = query == null ? "" : query.trim();
        String sql = """
                SELECT id, title, description, deadline, deliverables, created_by_id, is_published, created_at
                FROM project
                %s
                ORDER BY created_at DESC, id DESC
                """.formatted(q.isBlank() ? "" : "WHERE title LIKE ? OR description LIKE ?");

        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!q.isBlank()) {
                String like = "%" + q + "%";
                ps.setString(1, like);
                ps.setString(2, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Project> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list projects: " + safeMsg(e), e);
        }
    }

    public List<Project> listPublished(String query) {
        String q = query == null ? "" : query.trim();
        String sql = """
                SELECT id, title, description, deadline, deliverables, created_by_id, is_published, created_at
                FROM project
                WHERE is_published = 1
                %s
                ORDER BY created_at DESC, id DESC
                """.formatted(q.isBlank() ? "" : "AND (title LIKE ? OR description LIKE ?)");

        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!q.isBlank()) {
                String like = "%" + q + "%";
                ps.setString(1, like);
                ps.setString(2, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Project> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(map(rs));
                }
                return out;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list published projects: " + safeMsg(e), e);
        }
    }

    public void create(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("project is null");
        }
        String sql = """
                INSERT INTO project (title, description, deadline, deliverables, created_by_id, is_published, created_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW())
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, project.getTitle());
            ps.setString(2, project.getDescription());
            ps.setString(3, project.getDeadline());
            ps.setString(4, project.getDeliverables());
            ps.setInt(5, project.getCreatedById());
            ps.setBoolean(6, project.isPublished());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    project.setId(keys.getInt(1));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create project: " + safeMsg(e), e);
        }
    }

    public void update(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("project is null");
        }
        String sql = """
                UPDATE project
                SET title = ?, description = ?, deadline = ?, deliverables = ?, is_published = ?
                WHERE id = ?
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, project.getTitle());
            ps.setString(2, project.getDescription());
            ps.setString(3, project.getDeadline());
            ps.setString(4, project.getDeliverables());
            ps.setBoolean(5, project.isPublished());
            ps.setInt(6, project.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update project: " + safeMsg(e), e);
        }
    }

    public void setPublished(int projectId, boolean published) {
        String sql = "UPDATE project SET is_published = ? WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, published);
            ps.setInt(2, projectId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update publication state: " + safeMsg(e), e);
        }
    }

    public void delete(int projectId) {
        String sql = "DELETE FROM project WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete project: " + safeMsg(e), e);
        }
    }

    private static Project map(ResultSet rs) throws Exception {
        Project p = new Project();
        p.setId(rs.getInt("id"));
        p.setTitle(rs.getString("title"));
        p.setDescription(rs.getString("description"));
        p.setDeadline(rs.getString("deadline"));
        p.setDeliverables(rs.getString("deliverables"));
        p.setCreatedById(rs.getInt("created_by_id"));
        p.setPublished(rs.getBoolean("is_published"));
        p.setCreatedAt(rs.getString("created_at"));
        return p;
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

