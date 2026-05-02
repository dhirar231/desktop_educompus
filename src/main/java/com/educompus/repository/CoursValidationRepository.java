package com.educompus.repository;

import com.educompus.model.Cours;
import com.educompus.model.CoursStatut;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class CoursValidationRepository {

    public CoursValidationRepository() {
        ensureValidationSchema();
    }

    private void ensureValidationSchema() {
        try (Connection conn = EducompusDB.getConnection()) {
            if (!columnExists(conn, "cours", "statut")) {
                executeIgnore(conn, "ALTER TABLE cours ADD COLUMN statut VARCHAR(32) NOT NULL DEFAULT 'EN_ATTENTE'");
            }
            if (!columnExists(conn, "cours", "commentaire_admin")) {
                executeIgnore(conn, "ALTER TABLE cours ADD COLUMN commentaire_admin TEXT NULL");
            }
            if (!columnExists(conn, "cours", "created_by_id")) {
                executeIgnore(conn, "ALTER TABLE cours ADD COLUMN created_by_id INT NOT NULL DEFAULT 0");
            }
        } catch (Exception ignored) {}
    }

    public Cours findById(int coursId) {
        String sql = "SELECT c.id, c.titre, c.description, c.niveau, c.domaine, c.image, c.date_creation,"
                + " c.nom_formateur, c.duree_totale_heures, c.statut, c.commentaire_admin, c.created_by_id,"
                + " COUNT(ch.id) AS chapitre_count"
                + " FROM cours c LEFT JOIN chapitre ch ON ch.cours_id = c.id"
                + " WHERE c.id = ?"
                + " GROUP BY c.id, c.titre, c.description, c.niveau, c.domaine, c.image, c.date_creation,"
                + " c.nom_formateur, c.duree_totale_heures, c.statut, c.commentaire_admin, c.created_by_id";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, coursId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapCours(rs);
                return null;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger le cours: " + safeMsg(e), e);
        }
    }

    public List<Cours> listCoursEnAttente() {
        return queryByStatut("EN_ATTENTE");
    }

    public List<Cours> listCoursApprouves(String query) {
        String q = safe(query);
        String filter = q.isBlank() ? "" : " AND (c.titre LIKE ? OR c.domaine LIKE ? OR c.nom_formateur LIKE ?)";
        String sql = "SELECT c.id, c.titre, c.description, c.niveau, c.domaine, c.image, c.date_creation,"
                + " c.nom_formateur, c.duree_totale_heures, c.statut, c.commentaire_admin, c.created_by_id,"
                + " COUNT(ch.id) AS chapitre_count"
                + " FROM cours c LEFT JOIN chapitre ch ON ch.cours_id = c.id"
                + " WHERE c.statut = 'APPROUVE'" + filter
                + " GROUP BY c.id, c.titre, c.description, c.niveau, c.domaine, c.image, c.date_creation,"
                + " c.nom_formateur, c.duree_totale_heures, c.statut, c.commentaire_admin, c.created_by_id"
                + " ORDER BY c.date_creation DESC, c.id DESC";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!q.isBlank()) {
                String like = "%" + q + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Cours> out = new ArrayList<>();
                while (rs.next()) out.add(mapCours(rs));
                return out;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger les cours approuvés: " + safeMsg(e), e);
        }
    }

    public List<Cours> listCoursByEnseignant(int enseignantId) {
        String sql = "SELECT c.id, c.titre, c.description, c.niveau, c.domaine, c.image, c.date_creation,"
                + " c.nom_formateur, c.duree_totale_heures, c.statut, c.commentaire_admin, c.created_by_id,"
                + " COUNT(ch.id) AS chapitre_count"
                + " FROM cours c LEFT JOIN chapitre ch ON ch.cours_id = c.id"
                + " WHERE c.created_by_id = ?"
                + " GROUP BY c.id, c.titre, c.description, c.niveau, c.domaine, c.image, c.date_creation,"
                + " c.nom_formateur, c.duree_totale_heures, c.statut, c.commentaire_admin, c.created_by_id"
                + " ORDER BY c.date_creation DESC, c.id DESC";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, enseignantId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Cours> out = new ArrayList<>();
                while (rs.next()) out.add(mapCours(rs));
                return out;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger les cours de l'enseignant: " + safeMsg(e), e);
        }
    }

    public void approuver(int coursId) {
        Cours cours = findById(coursId);
        if (cours == null) throw new IllegalArgumentException("Cours introuvable.");
        if (cours.getStatut() != CoursStatut.EN_ATTENTE) throw new IllegalStateException("Transition de statut invalide.");
        String sql = "UPDATE cours SET statut = 'APPROUVE' WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, coursId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'approuver le cours: " + safeMsg(e), e);
        }
    }

    public void refuser(int coursId, String commentaire) {
        Cours cours = findById(coursId);
        if (cours == null) throw new IllegalArgumentException("Cours introuvable.");
        if (cours.getStatut() != CoursStatut.EN_ATTENTE) throw new IllegalStateException("Transition de statut invalide.");
        String sql = "UPDATE cours SET statut = 'REFUSE', commentaire_admin = ? WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, commentaire);
            ps.setInt(2, coursId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de refuser le cours: " + safeMsg(e), e);
        }
    }

    public void reinitialiserStatut(int coursId) {
        String sql = "UPDATE cours SET statut = 'EN_ATTENTE', commentaire_admin = NULL WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, coursId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de réinitialiser le statut: " + safeMsg(e), e);
        }
    }

    public void mettreAJourEtRemettreEnAttente(Cours cours) {
        String sql = "UPDATE cours SET titre = ?, description = ?, niveau = ?, domaine = ?,"
                + " nom_formateur = ?, duree_totale_heures = ?, statut = 'EN_ATTENTE',"
                + " commentaire_admin = NULL WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cours.getTitre());
            ps.setString(2, cours.getDescription());
            ps.setString(3, cours.getNiveau());
            ps.setString(4, cours.getDomaine());
            ps.setString(5, cours.getNomFormateur());
            ps.setInt(6, cours.getDureeTotaleHeures());
            ps.setInt(7, cours.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de mettre à jour le cours: " + safeMsg(e), e);
        }
    }

    public List<Cours> listCoursRefuses() {
        return queryByStatut("REFUSE");
    }

    private List<Cours> queryByStatut(String statut) {
        String sql = "SELECT c.id, c.titre, c.description, c.niveau, c.domaine, c.image, c.date_creation,"
                + " c.nom_formateur, c.duree_totale_heures, c.statut, c.commentaire_admin, c.created_by_id,"
                + " COUNT(ch.id) AS chapitre_count"
                + " FROM cours c LEFT JOIN chapitre ch ON ch.cours_id = c.id"
                + " WHERE c.statut = '" + statut + "'"
                + " GROUP BY c.id, c.titre, c.description, c.niveau, c.domaine, c.image, c.date_creation,"
                + " c.nom_formateur, c.duree_totale_heures, c.statut, c.commentaire_admin, c.created_by_id"
                + " ORDER BY c.date_creation DESC, c.id DESC";
        try (Connection conn = EducompusDB.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            List<Cours> out = new ArrayList<>();
            while (rs.next()) out.add(mapCours(rs));
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger les cours: " + safeMsg(e), e);
        }
    }

    private static Cours mapCours(ResultSet rs) throws Exception {
        Cours cours = new Cours();
        cours.setId(rs.getInt("id"));
        cours.setTitre(rs.getString("titre"));
        cours.setDescription(rs.getString("description"));
        cours.setNiveau(rs.getString("niveau"));
        cours.setDomaine(rs.getString("domaine"));
        cours.setImage(rs.getString("image"));
        cours.setDateCreation(rs.getString("date_creation"));
        cours.setNomFormateur(rs.getString("nom_formateur"));
        cours.setDureeTotaleHeures(rs.getInt("duree_totale_heures"));
        cours.setChapitreCount(rs.getInt("chapitre_count"));
        try { cours.setStatut(CoursStatut.fromString(rs.getString("statut"))); } catch (Exception ignored) {}
        try { cours.setCommentaireAdmin(rs.getString("commentaire_admin")); } catch (Exception ignored) {}
        try { cours.setCreatedById(rs.getInt("created_by_id")); } catch (Exception ignored) {}
        return cours;
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws Exception {
        String sql = "SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    private static void executeIgnore(Connection conn, String sql) {
        try (Statement st = conn.createStatement()) { st.execute(sql); } catch (Exception ignored) {}
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }

    private static String safeMsg(Exception e) {
        if (e == null) return "erreur inconnue";
        String msg = String.valueOf(e.getMessage()).replace('\n', ' ').trim();
        return msg.length() > 220 ? msg.substring(0, 220) + "..." : msg;
    }
}
