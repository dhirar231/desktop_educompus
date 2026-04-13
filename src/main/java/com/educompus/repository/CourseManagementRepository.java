package com.educompus.repository;

import com.educompus.model.Chapitre;
import com.educompus.model.Cours;
import com.educompus.model.Td;
import com.educompus.model.VideoExplicative;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class CourseManagementRepository {
    public CourseManagementRepository() {
        ensureCoursSchema();
    }

    public List<Cours> listCours(String query) {
        String q = safe(query);
        String sql = """
                SELECT c.id, c.titre, c.description, c.niveau, c.domaine, c.image, c.date_creation,
                       c.nom_formateur, c.duree_totale_heures, COUNT(ch.id) AS chapitre_count
                FROM cours c
                LEFT JOIN chapitre ch ON ch.cours_id = c.id
                %s
                GROUP BY c.id, c.titre, c.description, c.niveau, c.domaine, c.image, c.date_creation,
                         c.nom_formateur, c.duree_totale_heures
                ORDER BY c.date_creation DESC, c.id DESC
                """.formatted(q.isBlank() ? "" : """
                        WHERE c.titre LIKE ? OR c.description LIKE ? OR c.niveau LIKE ? OR c.domaine LIKE ?
                           OR c.nom_formateur LIKE ?
                        """);

        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!q.isBlank()) {
                String like = "%" + q + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
                ps.setString(4, like);
                ps.setString(5, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Cours> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapCours(rs));
                }
                return out;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger les cours: " + safeMsg(e), e);
        }
    }

    public List<Chapitre> listChapitres(String query) {
        String q = safe(query);
        String sql = """
                SELECT ch.id, ch.titre, ch.ordre, ch.description, ch.fichier_c, ch.date_creation, ch.cours_id,
                       ch.niveau, ch.domaine, c.titre AS cours_titre,
                       (SELECT COUNT(*) FROM td t WHERE t.chapitre_id = ch.id) AS td_count,
                       (SELECT COUNT(*) FROM video_explicative v WHERE v.chapitre_id = ch.id) AS video_count
                FROM chapitre ch
                INNER JOIN cours c ON c.id = ch.cours_id
                %s
                ORDER BY ch.date_creation DESC, ch.id DESC
                """.formatted(q.isBlank() ? "" : "WHERE ch.titre LIKE ? OR ch.description LIKE ? OR c.titre LIKE ? OR ch.niveau LIKE ? OR ch.domaine LIKE ?");

        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!q.isBlank()) {
                String like = "%" + q + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
                ps.setString(4, like);
                ps.setString(5, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Chapitre> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapChapitre(rs));
                }
                return out;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger les chapitres: " + safeMsg(e), e);
        }
    }

    public List<Chapitre> listChapitresByCoursId(int coursId) {
        String sql = """
                SELECT ch.id, ch.titre, ch.ordre, ch.description, ch.fichier_c, ch.date_creation, ch.cours_id,
                       ch.niveau, ch.domaine, c.titre AS cours_titre,
                       (SELECT COUNT(*) FROM td t WHERE t.chapitre_id = ch.id) AS td_count,
                       (SELECT COUNT(*) FROM video_explicative v WHERE v.chapitre_id = ch.id) AS video_count
                FROM chapitre ch
                INNER JOIN cours c ON c.id = ch.cours_id
                WHERE ch.cours_id = ?
                ORDER BY ch.ordre ASC, ch.date_creation DESC, ch.id DESC
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, coursId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Chapitre> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapChapitre(rs));
                }
                return out;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger les chapitres du cours: " + safeMsg(e), e);
        }
    }

    public List<Td> listTds(String query) {
        String q = safe(query);
        String sql = """
                SELECT t.id, t.titre, t.description, t.fichier, t.date_creation, t.niveau, t.cours_id, t.chapitre_id,
                       t.domaine, c.titre AS cours_titre, ch.titre AS chapitre_titre
                FROM td t
                INNER JOIN cours c ON c.id = t.cours_id
                INNER JOIN chapitre ch ON ch.id = t.chapitre_id
                %s
                ORDER BY t.date_creation DESC, t.id DESC
                """.formatted(q.isBlank() ? "" : "WHERE t.titre LIKE ? OR t.description LIKE ? OR c.titre LIKE ? OR ch.titre LIKE ? OR t.niveau LIKE ? OR t.domaine LIKE ?");

        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!q.isBlank()) {
                String like = "%" + q + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
                ps.setString(4, like);
                ps.setString(5, like);
                ps.setString(6, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Td> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapTd(rs));
                }
                return out;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger les TD: " + safeMsg(e), e);
        }
    }

    public List<Td> listTdsByCoursId(int coursId) {
        String sql = """
                SELECT t.id, t.titre, t.description, t.fichier, t.date_creation, t.niveau, t.cours_id, t.chapitre_id,
                       t.domaine, c.titre AS cours_titre, ch.titre AS chapitre_titre
                FROM td t
                INNER JOIN cours c ON c.id = t.cours_id
                INNER JOIN chapitre ch ON ch.id = t.chapitre_id
                WHERE t.cours_id = ?
                ORDER BY t.date_creation DESC, t.id DESC
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, coursId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Td> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapTd(rs));
                }
                return out;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger les TD du cours: " + safeMsg(e), e);
        }
    }

    public List<VideoExplicative> listVideos(String query) {
        String q = safe(query);
        String sql = """
                SELECT v.id, v.titre, v.url_video, v.description, v.date_creation, v.niveau,
                       v.cours_id, v.chapitre_id, v.domaine,
                       c.titre AS cours_titre, ch.titre AS chapitre_titre
                FROM video_explicative v
                INNER JOIN cours c ON c.id = v.cours_id
                INNER JOIN chapitre ch ON ch.id = v.chapitre_id
                %s
                ORDER BY v.date_creation DESC, v.id DESC
                """.formatted(q.isBlank() ? "" : "WHERE v.titre LIKE ? OR v.description LIKE ? OR c.titre LIKE ? OR ch.titre LIKE ? OR v.niveau LIKE ? OR v.domaine LIKE ?");

        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!q.isBlank()) {
                String like = "%" + q + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                ps.setString(3, like);
                ps.setString(4, like);
                ps.setString(5, like);
                ps.setString(6, like);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<VideoExplicative> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapVideo(rs));
                }
                return out;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger les videos: " + safeMsg(e), e);
        }
    }

    public List<VideoExplicative> listVideosByCoursId(int coursId) {
        String sql = """
                SELECT v.id, v.titre, v.url_video, v.description, v.date_creation, v.niveau,
                       v.cours_id, v.chapitre_id, v.domaine,
                       c.titre AS cours_titre, ch.titre AS chapitre_titre
                FROM video_explicative v
                INNER JOIN cours c ON c.id = v.cours_id
                INNER JOIN chapitre ch ON ch.id = v.chapitre_id
                WHERE v.cours_id = ?
                ORDER BY v.date_creation DESC, v.id DESC
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, coursId);
            try (ResultSet rs = ps.executeQuery()) {
                List<VideoExplicative> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(mapVideo(rs));
                }
                return out;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger les videos du cours: " + safeMsg(e), e);
        }
    }

    public void createCours(Cours cours) {
        String sql = """
                INSERT INTO cours (titre, description, niveau, domaine, image, date_creation, nom_formateur, duree_totale_heures)
                VALUES (?, ?, ?, ?, ?, NOW(), ?, ?)
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cours.getTitre());
            ps.setString(2, cours.getDescription());
            ps.setString(3, cours.getNiveau());
            ps.setString(4, cours.getDomaine());
            ps.setString(5, emptyToNull(cours.getImage()));
            ps.setString(6, cours.getNomFormateur());
            ps.setInt(7, cours.getDureeTotaleHeures());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    cours.setId(keys.getInt(1));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'ajouter le cours: " + safeMsg(e), e);
        }
    }

    public void updateCours(Cours cours) {
        String sql = """
                UPDATE cours
                SET titre = ?, description = ?, niveau = ?, domaine = ?, image = ?, nom_formateur = ?, duree_totale_heures = ?
                WHERE id = ?
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cours.getTitre());
            ps.setString(2, cours.getDescription());
            ps.setString(3, cours.getNiveau());
            ps.setString(4, cours.getDomaine());
            ps.setString(5, emptyToNull(cours.getImage()));
            ps.setString(6, cours.getNomFormateur());
            ps.setInt(7, cours.getDureeTotaleHeures());
            ps.setInt(8, cours.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de modifier le cours: " + safeMsg(e), e);
        }
    }

    public void deleteCours(int coursId) {
        executeDelete("DELETE FROM cours WHERE id = ?", coursId, "Impossible de supprimer le cours");
    }

    public void createChapitre(Chapitre chapitre) {
        String sql = """
                INSERT INTO chapitre (titre, ordre, description, fichier_c, date_creation, cours_id, niveau, domaine)
                VALUES (?, ?, ?, ?, NOW(), ?, ?, ?)
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, chapitre.getTitre());
            ps.setInt(2, chapitre.getOrdre());
            ps.setString(3, chapitre.getDescription());
            ps.setString(4, emptyToNull(chapitre.getFichierC()));
            ps.setInt(5, chapitre.getCoursId());
            ps.setString(6, emptyToNull(chapitre.getNiveau()));
            ps.setString(7, emptyToNull(chapitre.getDomaine()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    chapitre.setId(keys.getInt(1));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'ajouter le chapitre: " + safeMsg(e), e);
        }
    }

    public void updateChapitre(Chapitre chapitre) {
        String sql = """
                UPDATE chapitre
                SET titre = ?, ordre = ?, description = ?, fichier_c = ?, cours_id = ?, niveau = ?, domaine = ?
                WHERE id = ?
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, chapitre.getTitre());
            ps.setInt(2, chapitre.getOrdre());
            ps.setString(3, chapitre.getDescription());
            ps.setString(4, emptyToNull(chapitre.getFichierC()));
            ps.setInt(5, chapitre.getCoursId());
            ps.setString(6, emptyToNull(chapitre.getNiveau()));
            ps.setString(7, emptyToNull(chapitre.getDomaine()));
            ps.setInt(8, chapitre.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de modifier le chapitre: " + safeMsg(e), e);
        }
    }

    public void deleteChapitre(int chapitreId) {
        executeDelete("DELETE FROM chapitre WHERE id = ?", chapitreId, "Impossible de supprimer le chapitre");
    }

    public void createTd(Td td) {
        String sql = """
                INSERT INTO td (titre, description, fichier, date_creation, niveau, cours_id, chapitre_id, domaine)
                VALUES (?, ?, ?, NOW(), ?, ?, ?, ?)
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, td.getTitre());
            ps.setString(2, td.getDescription());
            ps.setString(3, td.getFichier());
            ps.setString(4, td.getNiveau());
            ps.setInt(5, td.getCoursId());
            ps.setInt(6, td.getChapitreId());
            ps.setString(7, emptyToNull(td.getDomaine()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    td.setId(keys.getInt(1));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'ajouter le TD: " + safeMsg(e), e);
        }
    }

    public void updateTd(Td td) {
        String sql = """
                UPDATE td
                SET titre = ?, description = ?, fichier = ?, niveau = ?, cours_id = ?, chapitre_id = ?, domaine = ?
                WHERE id = ?
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, td.getTitre());
            ps.setString(2, td.getDescription());
            ps.setString(3, td.getFichier());
            ps.setString(4, td.getNiveau());
            ps.setInt(5, td.getCoursId());
            ps.setInt(6, td.getChapitreId());
            ps.setString(7, emptyToNull(td.getDomaine()));
            ps.setInt(8, td.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de modifier le TD: " + safeMsg(e), e);
        }
    }

    public void deleteTd(int tdId) {
        executeDelete("DELETE FROM td WHERE id = ?", tdId, "Impossible de supprimer le TD");
    }

    public void createVideo(VideoExplicative video) {
        String sql = """
                INSERT INTO video_explicative (titre, url_video, description, date_creation, niveau,
                                               cours_id, chapitre_id, domaine)
                VALUES (?, ?, ?, NOW(), ?, ?, ?, ?)
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, video.getTitre());
            ps.setString(2, emptyToNull(video.getUrlVideo()));
            ps.setString(3, video.getDescription());
            ps.setString(4, emptyToNull(video.getNiveau()));
            ps.setInt(5, video.getCoursId());
            ps.setInt(6, video.getChapitreId());
            ps.setString(7, emptyToNull(video.getDomaine()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    video.setId(keys.getInt(1));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'ajouter la video: " + safeMsg(e), e);
        }
    }

    public void updateVideo(VideoExplicative video) {
        String sql = """
                UPDATE video_explicative
                SET titre = ?, url_video = ?, description = ?, niveau = ?,
                    cours_id = ?, chapitre_id = ?, domaine = ?
                WHERE id = ?
                """;
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, video.getTitre());
            ps.setString(2, emptyToNull(video.getUrlVideo()));
            ps.setString(3, video.getDescription());
            ps.setString(4, emptyToNull(video.getNiveau()));
            ps.setInt(5, video.getCoursId());
            ps.setInt(6, video.getChapitreId());
            ps.setString(7, emptyToNull(video.getDomaine()));
            ps.setInt(8, video.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de modifier la video: " + safeMsg(e), e);
        }
    }

    public void deleteVideo(int videoId) {
        executeDelete("DELETE FROM video_explicative WHERE id = ?", videoId, "Impossible de supprimer la video");
    }

    private void ensureCoursSchema() {
        try (Connection conn = EducompusDB.getConnection()) {
            // Create tables if they don't exist
            executeIgnore(conn, """
                    CREATE TABLE IF NOT EXISTS cours (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        titre VARCHAR(255) NOT NULL,
                        description TEXT,
                        niveau VARCHAR(32),
                        domaine VARCHAR(64),
                        image VARCHAR(255),
                        date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        nom_formateur VARCHAR(255),
                        duree_totale_heures INT NOT NULL DEFAULT 0
                    )
                    """);
            executeIgnore(conn, """
                    CREATE TABLE IF NOT EXISTS chapitre (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        titre VARCHAR(255) NOT NULL,
                        description TEXT,
                        ordre INT NOT NULL DEFAULT 1,
                        cours_id INT NOT NULL,
                        fichier_c VARCHAR(512),
                        date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        niveau VARCHAR(32),
                        domaine VARCHAR(64),
                        FOREIGN KEY (cours_id) REFERENCES cours(id) ON DELETE CASCADE
                    )
                    """);
            executeIgnore(conn, """
                    CREATE TABLE IF NOT EXISTS td (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        titre VARCHAR(255) NOT NULL,
                        description TEXT,
                        fichier VARCHAR(512),
                        date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        chapitre_id INT NOT NULL,
                        cours_id INT,
                        niveau VARCHAR(32),
                        domaine VARCHAR(64),
                        FOREIGN KEY (chapitre_id) REFERENCES chapitre(id) ON DELETE CASCADE
                    )
                    """);
            executeIgnore(conn, """
                    CREATE TABLE IF NOT EXISTS video_explicative (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        titre VARCHAR(255) NOT NULL,
                        url_video VARCHAR(512),
                        description TEXT,
                        date_creation DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        chapitre_id INT NOT NULL,
                        cours_id INT,
                        niveau VARCHAR(32),
                        domaine VARCHAR(64),
                        FOREIGN KEY (chapitre_id) REFERENCES chapitre(id) ON DELETE CASCADE
                    )
                    """);

            // Alter existing tables if needed
            if (!columnExists(conn, "cours", "nom_formateur")) {
                executeIgnore(conn, "ALTER TABLE cours ADD COLUMN nom_formateur VARCHAR(255) NOT NULL DEFAULT ''");
            }
            if (!columnExists(conn, "cours", "duree_totale_heures")) {
                executeIgnore(conn, "ALTER TABLE cours ADD COLUMN duree_totale_heures INT NOT NULL DEFAULT 0");
            }
            if (!columnExists(conn, "cours", "image")) {
                executeIgnore(conn, "ALTER TABLE cours ADD COLUMN image VARCHAR(255) NULL");
            }
            if (!columnExists(conn, "chapitre", "niveau")) {
                executeIgnore(conn, "ALTER TABLE chapitre ADD COLUMN niveau VARCHAR(32) NULL");
            }
            if (!columnExists(conn, "chapitre", "domaine")) {
                executeIgnore(conn, "ALTER TABLE chapitre ADD COLUMN domaine VARCHAR(64) NULL");
            }
            if (!columnExists(conn, "td", "cours_id")) {
                executeIgnore(conn, "ALTER TABLE td ADD COLUMN cours_id INT NULL");
            }
            if (!columnExists(conn, "td", "niveau")) {
                executeIgnore(conn, "ALTER TABLE td ADD COLUMN niveau VARCHAR(32) NULL");
            }
            if (!columnExists(conn, "td", "domaine")) {
                executeIgnore(conn, "ALTER TABLE td ADD COLUMN domaine VARCHAR(64) NULL");
            }
            if (!columnExists(conn, "video_explicative", "cours_id")) {
                executeIgnore(conn, "ALTER TABLE video_explicative ADD COLUMN cours_id INT NULL");
            }
            if (!columnExists(conn, "video_explicative", "niveau")) {
                executeIgnore(conn, "ALTER TABLE video_explicative ADD COLUMN niveau VARCHAR(32) NULL");
            }
            if (!columnExists(conn, "video_explicative", "domaine")) {
                executeIgnore(conn, "ALTER TABLE video_explicative ADD COLUMN domaine VARCHAR(64) NULL");
            }
        } catch (Exception ignored) {
            // Keep app usable even if schema migration cannot be performed.
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
            // Ignore schema migration failures.
        }
    }

    private void executeDelete(String sql, int id, String label) {
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException(label + ": " + safeMsg(e), e);
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
        return cours;
    }

    private static Chapitre mapChapitre(ResultSet rs) throws Exception {
        Chapitre chapitre = new Chapitre();
        chapitre.setId(rs.getInt("id"));
        chapitre.setTitre(rs.getString("titre"));
        chapitre.setOrdre(rs.getInt("ordre"));
        chapitre.setDescription(rs.getString("description"));
        chapitre.setFichierC(rs.getString("fichier_c"));
        chapitre.setDateCreation(rs.getString("date_creation"));
        chapitre.setCoursId(rs.getInt("cours_id"));
        chapitre.setCoursTitre(rs.getString("cours_titre"));
        chapitre.setNiveau(rs.getString("niveau"));
        chapitre.setDomaine(rs.getString("domaine"));
        chapitre.setTdCount(rs.getInt("td_count"));
        chapitre.setVideoCount(rs.getInt("video_count"));
        return chapitre;
    }

    private static Td mapTd(ResultSet rs) throws Exception {
        Td td = new Td();
        td.setId(rs.getInt("id"));
        td.setTitre(rs.getString("titre"));
        td.setDescription(rs.getString("description"));
        td.setFichier(rs.getString("fichier"));
        td.setDateCreation(rs.getString("date_creation"));
        td.setNiveau(rs.getString("niveau"));
        td.setCoursId(rs.getInt("cours_id"));
        td.setCoursTitre(rs.getString("cours_titre"));
        td.setChapitreId(rs.getInt("chapitre_id"));
        td.setChapitreTitre(rs.getString("chapitre_titre"));
        td.setDomaine(rs.getString("domaine"));
        return td;
    }

    private static VideoExplicative mapVideo(ResultSet rs) throws Exception {
        VideoExplicative video = new VideoExplicative();
        video.setId(rs.getInt("id"));
        video.setTitre(rs.getString("titre"));
        video.setUrlVideo(rs.getString("url_video"));
        video.setDescription(rs.getString("description"));
        video.setDateCreation(rs.getString("date_creation"));
        video.setNiveau(rs.getString("niveau"));
        video.setCoursId(rs.getInt("cours_id"));
        video.setCoursTitre(rs.getString("cours_titre"));
        video.setChapitreId(rs.getInt("chapitre_id"));
        video.setChapitreTitre(rs.getString("chapitre_titre"));
        video.setDomaine(rs.getString("domaine"));
        return video;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String emptyToNull(String value) {
        String v = safe(value);
        return v.isBlank() ? null : v;
    }

    private static String safeMsg(Exception e) {
        if (e == null) {
            return "erreur inconnue";
        }
        String msg = String.valueOf(e.getMessage()).replace('\n', ' ').replace('\r', ' ').trim();
        if (msg.length() > 220) {
            msg = msg.substring(0, 220) + "...";
        }
        return msg;
    }
}
