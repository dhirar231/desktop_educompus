package com.educompus.service;

import com.educompus.model.Project;
import com.educompus.repository.EducompusDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServiceProject {

    public void ajouter(Project project) throws Exception {
        String sql = "INSERT INTO project (title, description, deadline, deliverables, created_by_id, is_published, created_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";
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
                if (keys.next()) project.setId(keys.getInt(1));
            }
        }
    }

    public void update(Project project) throws Exception {
        if (project == null) throw new IllegalArgumentException("project is null");
        String sql = "UPDATE project SET title = ?, description = ?, deadline = ?, deliverables = ?, is_published = ? WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, project.getTitle());
            ps.setString(2, project.getDescription());
            ps.setString(3, project.getDeadline());
            ps.setString(4, project.getDeliverables());
            ps.setBoolean(5, project.isPublished());
            ps.setInt(6, project.getId());
            ps.executeUpdate();
        }
    }

    public void setPublished(int projectId, boolean published) throws Exception {
        String sql = "UPDATE project SET is_published = ? WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, published);
            ps.setInt(2, projectId);
            ps.executeUpdate();
        }
    }

    public void delete(int projectId) throws Exception {
        String sql = "DELETE FROM project WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, projectId);
            ps.executeUpdate();
        }
    }

    public List<Project> afficherAll() throws Exception {
        List<Project> out = new ArrayList<>();
        String sql = "SELECT id, title, description, deadline, deliverables, created_by_id, is_published, created_at FROM project ORDER BY created_at DESC, id DESC";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(mapRow(rs));
        }
        return out;
    }

    public List<Project> listPublished() throws Exception {
        List<Project> out = new ArrayList<>();
        String sql = "SELECT id, title, description, deadline, deliverables, created_by_id, is_published, created_at FROM project WHERE is_published = 1 ORDER BY created_at DESC, id DESC";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(mapRow(rs));
        }
        return out;
    }

    public Project getById(int id) throws Exception {
        String sql = "SELECT id, title, description, deadline, deliverables, created_by_id, is_published, created_at FROM project WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    private Project mapRow(ResultSet rs) throws Exception {
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
}
