package com.educompus.repository;

import com.educompus.model.Project;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class FavoriteRepository {

    public FavoriteRepository() {
        ensureTable();
    }

    public List<Integer> listProjectIdsByUser(int userId) {
        if (userId <= 0) return List.of();
        String sql = "SELECT project_id FROM project_favorite WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Integer> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(rs.getInt("project_id"));
                }
                return out;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list favorites: " + safeMsg(e), e);
        }
    }

    public List<Project> listProjectsByUser(int userId) {
        if (userId <= 0) return List.of();
        String sql = """
                SELECT p.id, p.title, p.description, p.deadline, p.deliverables, p.created_by_id, p.is_published, p.created_at,
                       p.meeting_room, p.meeting_url, p.meeting_active, p.meeting_started_by_id, p.meeting_started_at
                FROM project p
                JOIN project_favorite f ON p.id = f.project_id
                WHERE f.user_id = ? AND p.is_published = 1
                ORDER BY f.created_at DESC
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Project> out = new ArrayList<>();
                while (rs.next()) {
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
                    p.setFavorite(true);
                    out.add(p);
                }
                return out;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list favorite projects: " + safeMsg(e), e);
        }
    }

    public boolean isFavorite(int userId, int projectId) {
        if (userId <= 0 || projectId <= 0) return false;
        String sql = "SELECT 1 FROM project_favorite WHERE user_id = ? AND project_id = ? LIMIT 1";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to check favorite: " + safeMsg(e), e);
        }
    }

    public void setFavorite(int userId, int projectId, boolean favorite) {
        if (userId <= 0 || projectId <= 0) return;
        if (favorite) {
            String sql = "INSERT INTO project_favorite (user_id, project_id, created_at) VALUES (?, ?, NOW())";
            try (Connection conn = EducompusDB.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, projectId);
                try {
                    ps.executeUpdate();
                } catch (Exception ignored) {
                    // ignore duplicates
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to add favorite: " + safeMsg(e), e);
            }
        } else {
            String sql = "DELETE FROM project_favorite WHERE user_id = ? AND project_id = ?";
            try (Connection conn = EducompusDB.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                ps.setInt(2, projectId);
                ps.executeUpdate();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to remove favorite: " + safeMsg(e), e);
            }
        }
    }

    private void ensureTable() {
        try (Connection conn = EducompusDB.getConnection()) {
            executeIgnore(conn, "CREATE TABLE IF NOT EXISTS project_favorite (user_id INT NOT NULL, project_id INT NOT NULL, created_at DATETIME DEFAULT NOW(), PRIMARY KEY(user_id, project_id))");
        } catch (Exception ignored) {
        }
    }

    private static void executeIgnore(Connection conn, String sql) {
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (Exception ignored) {
        }
    }

    private static String safeMsg(Exception e) {
        if (e == null) return "unknown error";
        String msg = String.valueOf(e.getMessage()).replace('\n', ' ').replace('\r', ' ').trim();
        if (msg.length() > 180) msg = msg.substring(0, 180) + "...";
        return msg;
    }
}
