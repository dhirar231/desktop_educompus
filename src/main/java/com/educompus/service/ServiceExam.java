package com.educompus.service;

import com.educompus.model.ExamCatalogueItem;
import com.educompus.repository.EducompusDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ServiceExam {

    public void addExam(ExamCatalogueItem item) throws Exception {
        String sql = "INSERT INTO exam (titre, description, niveau, domaine, cours_id, is_published, date_creation) VALUES (?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getExamTitle());
            ps.setString(2, item.getExamDescription());
            ps.setString(3, item.getLevelLabel());
            ps.setString(4, item.getDomainLabel());
            ps.setInt(5, item.getCourseId());
            ps.setBoolean(6, item.isPublished());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) item.setExamId(keys.getInt(1));
            }
        }
    }

    public void updateExam(ExamCatalogueItem item) throws Exception {
        if (item == null) throw new IllegalArgumentException("item is null");
        String sql = "UPDATE exam SET titre = ?, description = ?, niveau = ?, domaine = ?, cours_id = ?, is_published = ? WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getExamTitle());
            ps.setString(2, item.getExamDescription());
            ps.setString(3, item.getLevelLabel());
            ps.setString(4, item.getDomainLabel());
            ps.setInt(5, item.getCourseId());
            ps.setBoolean(6, item.isPublished());
            ps.setInt(7, item.getExamId());
            ps.executeUpdate();
        }
    }

    public void setPublished(int examId, boolean published) throws Exception {
        String sql = "UPDATE exam SET is_published = ? WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, published);
            ps.setInt(2, examId);
            ps.executeUpdate();
        }
    }

    public void deleteExam(int id) throws Exception {
        String sql = "DELETE FROM exam WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public ExamCatalogueItem getExamById(int id) throws Exception {
        String sql = "SELECT id, titre, description, niveau, domaine, cours_id, is_published, date_creation FROM exam WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<ExamCatalogueItem> listAll() throws Exception {
        List<ExamCatalogueItem> out = new ArrayList<>();
        String sql = "SELECT id, titre, description, niveau, domaine, cours_id, is_published, date_creation FROM exam ORDER BY date_creation DESC";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(mapRow(rs));
        }
        return out;
    }

    public List<ExamCatalogueItem> listPublished() throws Exception {
        List<ExamCatalogueItem> out = new ArrayList<>();
        String sql = "SELECT id, titre, description, niveau, domaine, cours_id, is_published, date_creation FROM exam WHERE is_published = 1 ORDER BY date_creation DESC";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(mapRow(rs));
        }
        return out;
    }

    private ExamCatalogueItem mapRow(ResultSet rs) throws Exception {
        ExamCatalogueItem it = new ExamCatalogueItem();
        it.setExamId(rs.getInt("id"));
        it.setExamTitle(rs.getString("titre"));
        it.setExamDescription(rs.getString("description"));
        it.setLevelLabel(rs.getString("niveau"));
        it.setDomainLabel(rs.getString("domaine"));
        it.setCourseId(rs.getInt("cours_id"));
        it.setPublished(rs.getBoolean("is_published"));
        return it;
    }
}
