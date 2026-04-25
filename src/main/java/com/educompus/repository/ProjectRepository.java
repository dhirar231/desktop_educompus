package com.educompus.repository;

import com.educompus.model.Project;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class ProjectRepository {
    public ProjectRepository() {
        ensureMeetingColumns();
    }

    public List<Project> listAll(String query) {
        String q = query == null ? "" : query.trim();
        String sql = """
                SELECT id, title, description, deadline, deliverables, created_by_id, is_published, created_at,
                       meeting_room, meeting_url, meeting_active, meeting_started_by_id, meeting_started_at
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
                SELECT id, title, description, deadline, deliverables, created_by_id, is_published, created_at,
                       meeting_room, meeting_url, meeting_active, meeting_started_by_id, meeting_started_at
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

    public Project getById(int id) {
        String sql = """
                SELECT id, title, description, deadline, deliverables, created_by_id, is_published, created_at,
                       meeting_room, meeting_url, meeting_active, meeting_started_by_id, meeting_started_at
                FROM project
                WHERE id = ?
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
            return null;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get project: " + safeMsg(e), e);
        }
    }

    public Project activateMeeting(int projectId, String room, String url, int startedById) {
        String sql = """
                UPDATE project
                SET meeting_room = ?,
                    meeting_url = ?,
                    meeting_active = 1,
                    meeting_started_by_id = ?,
                    meeting_started_at = NOW()
                WHERE id = ?
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, room);
            ps.setString(2, url);
            ps.setInt(3, startedById);
            ps.setInt(4, projectId);
            ps.executeUpdate();
            return getById(projectId);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to activate project meeting: " + safeMsg(e), e);
        }
    }

    public Project deactivateMeeting(int projectId) {
        String sql = """
                UPDATE project
                SET meeting_active = 0,
                    meeting_started_at = NOW()
                WHERE id = ?
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.executeUpdate();
            return getById(projectId);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to close project meeting: " + safeMsg(e), e);
        }
    }

    private void ensureMeetingColumns() {
        try (Connection conn = EducompusDB.getConnection()) {
            if (!columnExists(conn, "project", "meeting_room")) {
                executeIgnore(conn, "ALTER TABLE project ADD COLUMN meeting_room VARCHAR(160) NULL");
            }
            if (!columnExists(conn, "project", "meeting_url")) {
                executeIgnore(conn, "ALTER TABLE project ADD COLUMN meeting_url VARCHAR(255) NULL");
            }
            if (!columnExists(conn, "project", "meeting_active")) {
                executeIgnore(conn, "ALTER TABLE project ADD COLUMN meeting_active TINYINT(1) NOT NULL DEFAULT 0");
            }
            if (!columnExists(conn, "project", "meeting_started_by_id")) {
                executeIgnore(conn, "ALTER TABLE project ADD COLUMN meeting_started_by_id INT NULL");
            }
            if (!columnExists(conn, "project", "meeting_started_at")) {
                executeIgnore(conn, "ALTER TABLE project ADD COLUMN meeting_started_at DATETIME NULL");
            }
        } catch (Exception ignored) {
            // Keep app usable even when automatic schema alignment fails.
        }
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws Exception {
        String sql = "SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = ? AND column_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void executeIgnore(Connection conn, String sql) {
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (Exception ignored) {
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
        p.setMeetingRoom(rs.getString("meeting_room"));
        p.setMeetingUrl(rs.getString("meeting_url"));
        p.setMeetingActive(rs.getBoolean("meeting_active"));
        p.setMeetingStartedById(rs.getInt("meeting_started_by_id"));
        p.setMeetingStartedAt(rs.getString("meeting_started_at"));
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

