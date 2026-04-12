package com.educompus.repository;

import com.educompus.model.ExamAnswer;
import com.educompus.model.ExamCatalogueItem;
import com.educompus.model.ExamQuestion;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ExamRepository {
    private boolean fallbackUsed;
    private String sourceLabel = "";

    public boolean isFallbackUsed() {
        return fallbackUsed;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public List<ExamCatalogueItem> listCatalogue(String query) {
        try {
            List<ExamCatalogueItem> items = loadCatalogueFromDb(query, true);
            fallbackUsed = false;
            sourceLabel = "";
            return items;
        } catch (Exception e) {
            fallbackUsed = true;
            sourceLabel = "";
            return filterCatalogue(fallbackCatalogue(), query);
        }
    }

    public List<ExamCatalogueItem> listAdminRows(String query) {
        try {
            List<ExamCatalogueItem> items = loadCatalogueFromDb(query, false);
            fallbackUsed = false;
            sourceLabel = "";
            return items;
        } catch (Exception e) {
            fallbackUsed = true;
            sourceLabel = "";
            return filterCatalogue(fallbackCatalogue(), query);
        }
    }

    public List<ExamQuestion> listQuestionsByExamId(int examId) {
        try {
            List<ExamQuestion> questions = loadQuestionsFromDb(examId);
            if (!questions.isEmpty()) {
                fallbackUsed = false;
                sourceLabel = "";
                return questions;
            }
        } catch (Exception ignored) {
            // fallback below
        }
        fallbackUsed = true;
        sourceLabel = "";
        return fallbackQuestionsByExamId().getOrDefault(examId, List.of());
    }

    // ----- Question / Answer CRUD (admin) -----
    public void addQuestion(com.educompus.model.ExamQuestion q, int examId) throws Exception {
        try (java.sql.Connection conn = EducompusDB.getConnection()) {
            if (!tableExists(conn, "question")) {
                throw new IllegalStateException("Table question introuvable");
            }
            String sql = "INSERT INTO question (texte, duree, date_creation, exam_id) VALUES (?, ?, NOW(), ?)";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, q.getText());
                ps.setInt(2, q.getDurationSeconds());
                ps.setInt(3, examId);
                ps.executeUpdate();
                try (java.sql.ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) q.setId(rs.getInt(1));
                }
            }
        }
    }

    public void updateQuestion(com.educompus.model.ExamQuestion q, int examId) throws Exception {
        try (java.sql.Connection conn = EducompusDB.getConnection()) {
            if (!tableExists(conn, "question")) {
                throw new IllegalStateException("Table question introuvable");
            }
            String sql = "UPDATE question SET texte=?, duree=?, exam_id=? WHERE id=?";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, q.getText());
                ps.setInt(2, q.getDurationSeconds());
                ps.setInt(3, examId);
                ps.setInt(4, q.getId());
                ps.executeUpdate();
            }
        }
    }

    public void deleteQuestion(int id) throws Exception {
        try (java.sql.Connection conn = EducompusDB.getConnection()) {
            if (!tableExists(conn, "question")) {
                throw new IllegalStateException("Table question introuvable");
            }
            String sql = "DELETE FROM question WHERE id = ?";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        }
    }

    public void addAnswer(com.educompus.model.ExamAnswer a) throws Exception {
        try (java.sql.Connection conn = EducompusDB.getConnection()) {
            if (!tableExists(conn, "reponse") && !tableExists(conn, "response")) {
                throw new IllegalStateException("Table reponse introuvable");
            }
            String sql = "INSERT INTO reponse (texte, correcte, question_id, date_creation) VALUES (?, ?, ?, NOW())";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                // note: ExamAnswer model doesn't store questionId in this project model,
                // caller must provide the question id via a later helper or use updateAnswer which sets question linkage.
                throw new IllegalStateException("Use addAnswer(ExamAnswer a, int questionId) instead");
            }
        }
    }

    public void updateAnswer(com.educompus.model.ExamAnswer a) throws Exception {
        try (java.sql.Connection conn = EducompusDB.getConnection()) {
            if (!tableExists(conn, "reponse") && !tableExists(conn, "response")) {
                throw new IllegalStateException("Table reponse introuvable");
            }
            String sql = "UPDATE reponse SET texte=?, correcte=? WHERE id=?";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, a.getText());
                ps.setBoolean(2, a.isCorrect());
                ps.setInt(3, a.getId());
                ps.executeUpdate();
            }
        }
    }

    public void deleteAnswer(int id) throws Exception {
        try (java.sql.Connection conn = EducompusDB.getConnection()) {
            if (!tableExists(conn, "reponse") && !tableExists(conn, "response")) {
                throw new IllegalStateException("Table reponse introuvable");
            }
            String sql = "DELETE FROM reponse WHERE id = ?";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        }
    }

    public void addAnswer(com.educompus.model.ExamAnswer a, int questionId) throws Exception {
        try (java.sql.Connection conn = EducompusDB.getConnection()) {
            if (!tableExists(conn, "reponse") && !tableExists(conn, "response")) {
                throw new IllegalStateException("Table reponse introuvable");
            }
            // enforce max 4 answers per question
            String countSql = "SELECT COUNT(*) FROM reponse WHERE question_id = ?";
            try (java.sql.PreparedStatement cps = conn.prepareStatement(countSql)) {
                cps.setInt(1, questionId);
                try (java.sql.ResultSet crs = cps.executeQuery()) {
                    if (crs.next() && crs.getInt(1) >= 4) {
                        throw new IllegalStateException("Chaque question ne peut avoir que 4 réponses maximum");
                    }
                }
            }

            String sql = "INSERT INTO reponse (texte, correcte, question_id, date_creation) VALUES (?, ?, ?, NOW())";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, a.getText());
                ps.setBoolean(2, a.isCorrect());
                ps.setInt(3, questionId);
                ps.executeUpdate();
                try (java.sql.ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) a.setId(rs.getInt(1));
                }
            }
        }
    }

    private List<ExamCatalogueItem> loadCatalogueFromDb(String query, boolean publishedOnly) throws Exception {
        try (Connection conn = EducompusDB.getConnection()) {
            if (!tableExists(conn, "exam")) {
                throw new IllegalStateException("Table exam introuvable");
            }

            String courseTable = tableExists(conn, "cours") ? "cours" : (tableExists(conn, "course") ? "course" : null);
            Set<String> courseColumns = courseTable == null ? Set.of() : columnsOf(conn, courseTable);
            Set<String> examColumns = columnsOf(conn, "exam");
            boolean hasQuestions = tableExists(conn, "question");

            String examTitleCol = pickColumn(examColumns, "titre", "title", "name", "nom");
            String examDescCol = pickColumn(examColumns, "description", "details", "contenu", "content");
            String examLevelCol = pickColumn(examColumns, "niveau", "level");
            String examDomainCol = pickColumn(examColumns, "domaine", "domain", "matiere", "subject");
            String examCourseCol = pickColumn(examColumns, "cours_id", "course_id");
            String examPublishedCol = pickColumn(examColumns, "is_published", "published");

            if (examTitleCol == null || examCourseCol == null) {
                throw new IllegalStateException("Colonnes examen insuffisantes");
            }

            String courseTitleExpr = "CONCAT('Cours #', e." + examCourseCol + ")";
            String courseDescExpr = "'Quiz relie a ce cours.'";
            String courseTitleCol = null;
            if (courseTable != null) {
                courseTitleCol = pickColumn(courseColumns, "titre", "title", "nom", "name");
                String courseDescCol = pickColumn(courseColumns, "description", "details", "contenu", "content");
                if (courseTitleCol != null) {
                    courseTitleExpr = "COALESCE(NULLIF(c." + courseTitleCol + ", ''), CONCAT('Cours #', e." + examCourseCol + "))";
                }
                if (courseDescCol != null) {
                    courseDescExpr = "COALESCE(NULLIF(c." + courseDescCol + ", ''), 'Quiz relie a ce cours.')";
                }
            }

            boolean filtered = query != null && !query.isBlank();
            String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ")
                    .append("e.id AS exam_id, ")
                    .append("e.").append(examTitleCol).append(" AS exam_title, ")
                    .append(safeColumnOrLiteral("e", examDescCol, "''")).append(" AS exam_description, ")
                    .append(safeColumnOrLiteral("e", examLevelCol, "''")).append(" AS level_label, ")
                    .append(safeColumnOrLiteral("e", examDomainCol, "''")).append(" AS domain_label, ")
                    .append("e.").append(examCourseCol).append(" AS course_id, ")
                    .append((examPublishedCol == null) ? "0" : "e." + examPublishedCol).append(" AS published_flag, ")
                    .append(courseTitleExpr).append(" AS course_title, ")
                    .append(courseDescExpr).append(" AS course_description, ")
                    .append(hasQuestions ? "COUNT(q.id)" : "0").append(" AS question_count ")
                    .append("FROM exam e ");

            if (courseTable != null) sql.append(" LEFT JOIN ").append(courseTable).append(" c ON c.id = e.").append(examCourseCol);
            if (hasQuestions) sql.append(" LEFT JOIN question q ON q.exam_id = e.id");

            sql.append(" WHERE 1=1 ");
            if (publishedOnly && examPublishedCol != null) sql.append(" AND e.").append(examPublishedCol).append(" = 1 ");
            if (filtered) {
                sql.append(" AND (LOWER(e.").append(examTitleCol).append(") LIKE ? ");
                if (examDescCol != null) sql.append(" OR LOWER(e.").append(examDescCol).append(") LIKE ? ");
                if (courseTable != null && courseTitleCol != null) sql.append(" OR LOWER(c.").append(courseTitleCol).append(") LIKE ? ");
                sql.append(") ");
            }

            sql.append(" GROUP BY e.id ");
            sql.append(" ORDER BY e.date_creation DESC");

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                if (filtered) {
                    String like = "%" + q.toLowerCase(Locale.ROOT) + "%";
                    int idx = 1;
                    ps.setString(idx++, like);
                    if (examDescCol != null) {
                        ps.setString(idx++, like);
                    }
                    if (courseTable != null && courseTitleCol != null) {
                        ps.setString(idx, like);
                    }
                }

                try (ResultSet rs = ps.executeQuery()) {
                    List<ExamCatalogueItem> out = new ArrayList<>();
                    while (rs.next()) {
                        ExamCatalogueItem item = new ExamCatalogueItem();
                        item.setExamId(rs.getInt("exam_id"));
                        item.setCourseId(rs.getInt("course_id"));
                        item.setCourseTitle(rs.getString("course_title"));
                        item.setCourseDescription(rs.getString("course_description"));
                        item.setExamTitle(rs.getString("exam_title"));
                        item.setExamDescription(rs.getString("exam_description"));
                        item.setLevelLabel(rs.getString("level_label"));
                        item.setDomainLabel(rs.getString("domain_label"));
                        item.setPublished(rs.getBoolean("published_flag"));
                        item.setQuestionCount(rs.getInt("question_count"));
                        item.setEstimatedMinutes(Math.max(5, item.getQuestionCount() * 2));
                        out.add(item);
                    }
                    // If there are no rows, return an empty list instead of throwing.
                    // Throwing here triggered the fallback catalogue even when the DB
                    // query executed successfully but simply had no published exams.
                    return out;
                }
            }
        }
    }

    private List<ExamQuestion> loadQuestionsFromDb(int examId) throws Exception {
        try (Connection conn = EducompusDB.getConnection()) {
            if (!tableExists(conn, "question") || !tableExists(conn, "reponse")) {
                throw new IllegalStateException("Tables quiz introuvables");
            }

            Set<String> questionColumns = columnsOf(conn, "question");
            Set<String> answerColumns = columnsOf(conn, "reponse");

            String questionTextCol = pickColumn(questionColumns, "texte", "text", "question");
            String questionDurationCol = pickColumn(questionColumns, "duree", "duration", "duration_seconds");
            String answerTextCol = pickColumn(answerColumns, "texte", "text", "label");
            String answerCorrectCol = pickColumn(answerColumns, "correcte", "is_correct", "correct");

            if (questionTextCol == null || answerTextCol == null || answerCorrectCol == null) {
                throw new IllegalStateException("Colonnes quiz insuffisantes");
            }

            Map<Integer, ExamQuestion> questions = new LinkedHashMap<>();
            String sql = """
                    SELECT
                        q.id AS question_id,
                        q.%s AS question_text,
                        %s AS question_duration,
                        r.id AS answer_id,
                        r.%s AS answer_text,
                        r.%s AS answer_correct
                    FROM question q
                    LEFT JOIN reponse r ON r.question_id = q.id
                    WHERE q.exam_id = ?
                    ORDER BY q.id ASC, r.id ASC
                    """.formatted(
                    questionTextCol,
                    questionDurationCol == null ? "45" : "q." + questionDurationCol,
                    answerTextCol,
                    answerCorrectCol
            );

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, examId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int questionId = rs.getInt("question_id");
                        ExamQuestion question = questions.computeIfAbsent(questionId, id -> {
                            ExamQuestion q = new ExamQuestion();
                            q.setId(id);
                            return q;
                        });
                        question.setText(rs.getString("question_text"));
                        question.setDurationSeconds(rs.getInt("question_duration"));

                        int answerId = rs.getInt("answer_id");
                        if (answerId > 0) {
                            ExamAnswer answer = new ExamAnswer();
                            answer.setId(answerId);
                            answer.setText(rs.getString("answer_text"));
                            answer.setCorrect(rs.getBoolean("answer_correct"));
                            question.getAnswers().add(answer);
                        }
                    }
                }
            }

            List<ExamQuestion> out = new ArrayList<>(questions.values());
            if (out.isEmpty()) {
                throw new IllegalStateException("Quiz vide");
            }
            return out;
        }
    }

    private static boolean tableExists(Connection conn, String tableName) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(conn.getCatalog(), null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private static Set<String> columnsOf(Connection conn, String tableName) throws Exception {
        Set<String> cols = new HashSet<>();
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, tableName, null)) {
            while (rs.next()) {
                cols.add(String.valueOf(rs.getString("COLUMN_NAME")).toLowerCase(Locale.ROOT));
            }
        }
        return cols;
    }

    private static String pickColumn(Set<String> columns, String... candidates) {
        for (String candidate : candidates) {
            if (columns.contains(candidate.toLowerCase(Locale.ROOT))) {
                return candidate;
            }
        }
        return null;
    }

    private static String safeColumnOrLiteral(String alias, String column, String fallbackLiteral) {
        return column == null ? fallbackLiteral : "COALESCE(" + alias + "." + column + ", '')";
    }

    private static List<ExamCatalogueItem> filterCatalogue(List<ExamCatalogueItem> source, String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isBlank()) {
            return source;
        }
        List<ExamCatalogueItem> out = new ArrayList<>();
        for (ExamCatalogueItem item : source) {
            String haystack = (item.getCourseTitle() + " " + item.getCourseDescription() + " "
                    + item.getExamTitle() + " " + item.getExamDescription() + " "
                    + item.getLevelLabel() + " " + item.getDomainLabel()).toLowerCase(Locale.ROOT);
            if (haystack.contains(q)) {
                out.add(item);
            }
        }
        return out;
    }

    private static List<ExamCatalogueItem> fallbackCatalogue() {
        List<ExamCatalogueItem> items = new ArrayList<>();
        items.add(catalogueItem(1001, 201, "Java Fundamentals", "Variables, conditions, boucles et bases POO.",
                "Quiz Java debutant", "Validation rapide des bases Java orientees objet.", "Debutant", "Programmation", 4));
        items.add(catalogueItem(1002, 202, "MySQL Essentials", "Requetes SQL, jointures et modelisation simple.",
                "Quiz SQL relationnel", "Evaluation sur select, join, group by et bonnes pratiques.", "Intermediaire", "Base de donnees", 4));
        items.add(catalogueItem(1003, 203, "Symfony Web", "Routes, controllers, formulaires et services.",
                "Quiz Symfony user flow", "Quiz relie au cours Symfony pour preparer le passage en projet.", "Intermediaire", "Framework", 4));
        return items;
    }

    private static ExamCatalogueItem catalogueItem(int examId, int courseId, String courseTitle, String courseDescription,
                                                   String examTitle, String examDescription, String level,
                                                   String domain, int questionCount) {
        ExamCatalogueItem item = new ExamCatalogueItem();
        item.setExamId(examId);
        item.setCourseId(courseId);
        item.setCourseTitle(courseTitle);
        item.setCourseDescription(courseDescription);
        item.setExamTitle(examTitle);
        item.setExamDescription(examDescription);
        item.setLevelLabel(level);
        item.setDomainLabel(domain);
        item.setQuestionCount(questionCount);
        item.setEstimatedMinutes(Math.max(5, questionCount * 2));
        item.setPublished(true);
        return item;
    }

    private static Map<Integer, List<ExamQuestion>> fallbackQuestionsByExamId() {
        Map<Integer, List<ExamQuestion>> map = new LinkedHashMap<>();
        map.put(1001, List.of(
                question(1, "Quel mot-cle sert a declarer une classe en Java ?", answer("class", true), answer("function", false), answer("module", false)),
                question(2, "Quelle collection preserve l'ordre d'insertion ?", answer("ArrayList", true), answer("HashSet", false), answer("TreeMap", false)),
                question(3, "Quel type convient pour une valeur vraie ou fausse ?", answer("boolean", true), answer("char", false), answer("double", false)),
                question(4, "Quel mot-cle appelle le constructeur parent ?", answer("super", true), answer("this", false), answer("extends", false))
        ));
        map.put(1002, List.of(
                question(5, "Quelle clause relie deux tables ?", answer("JOIN", true), answer("MERGE", false), answer("ORDER", false)),
                question(6, "Quelle clause agrege des lignes ?", answer("GROUP BY", true), answer("LIMIT", false), answer("OFFSET", false)),
                question(7, "Quelle fonction compte les lignes ?", answer("COUNT()", true), answer("SUM()", false), answer("AVG()", false)),
                question(8, "Quelle clause filtre apres aggregation ?", answer("HAVING", true), answer("WHERE", false), answer("ON", false))
        ));
        map.put(1003, List.of(
                question(9, "Quel composant gere les routes Symfony ?", answer("Controller + Route", true), answer("Twig", false), answer("Doctrine only", false)),
                question(10, "Quel service genere les formulaires ?", answer("FormBuilder", true), answer("Mailer", false), answer("Security token", false)),
                question(11, "Ou place-t-on la logique injectable ?", answer("Service", true), answer("Twig template", false), answer("CSS", false)),
                question(12, "Quel composant mappe les entites SQL ?", answer("Doctrine ORM", true), answer("Messenger", false), answer("HttpFoundation", false))
        ));
        return map;
    }

    // --- Admin CRUD operations (simple wrappers using existing DB connection logic) ---
    public ExamCatalogueItem getExamById(int id) throws Exception {
        try (java.sql.Connection conn = EducompusDB.getConnection()) {
            if (!tableExists(conn, "exam")) {
                return null;
            }
            java.util.Set<String> examColumns = columnsOf(conn, "exam");
            String examTitleCol = pickColumn(examColumns, "titre", "title", "name", "nom");
            String examDescCol = pickColumn(examColumns, "description", "details", "contenu", "content");
            String examLevelCol = pickColumn(examColumns, "niveau", "level");
            String examDomainCol = pickColumn(examColumns, "domaine", "domain", "matiere", "subject");
            String examCourseCol = pickColumn(examColumns, "cours_id", "course_id");
            String examPublishedCol = pickColumn(examColumns, "is_published", "published");
            if (examTitleCol == null || examCourseCol == null) return null;

            String sql = "SELECT * FROM exam WHERE id = ?";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        ExamCatalogueItem item = new ExamCatalogueItem();
                        item.setExamId(rs.getInt("id"));
                        item.setExamTitle(rs.getString(examTitleCol));
                        item.setExamDescription(examDescCol == null ? "" : rs.getString(examDescCol));
                        item.setLevelLabel(examLevelCol == null ? "" : rs.getString(examLevelCol));
                        item.setDomainLabel(examDomainCol == null ? "" : rs.getString(examDomainCol));
                        item.setCourseId(rs.getInt(examCourseCol));
                        item.setPublished(examPublishedCol == null || rs.getBoolean(examPublishedCol));
                        // question count can be retrieved separately if needed
                        item.setQuestionCount(0);
                        item.setEstimatedMinutes(0);
                        return item;
                    }
                }
            }
        }
        return null;
    }

    public void addExam(ExamCatalogueItem item) throws Exception {
        try (java.sql.Connection conn = EducompusDB.getConnection()) {
            if (!tableExists(conn, "exam")) {
                throw new IllegalStateException("Table exam introuvable");
            }
            java.util.Set<String> examColumns = columnsOf(conn, "exam");
            String examTitleCol = pickColumn(examColumns, "titre", "title", "name", "nom");
            String examDescCol = pickColumn(examColumns, "description", "details", "contenu", "content");
            String examLevelCol = pickColumn(examColumns, "niveau", "level");
            String examDomainCol = pickColumn(examColumns, "domaine", "domain", "matiere", "subject");
            String examCourseCol = pickColumn(examColumns, "cours_id", "course_id");
            String examPublishedCol = pickColumn(examColumns, "is_published", "published");
            if (examTitleCol == null || examCourseCol == null) {
                throw new IllegalStateException("Colonnes examen insuffisantes");
            }

            StringBuilder cols = new StringBuilder();
            StringBuilder params = new StringBuilder();
            java.util.List<String> provided = new java.util.ArrayList<>();
            cols.append(examTitleCol); params.append("?"); provided.add(examTitleCol);
            if (examDescCol != null) { cols.append(", ").append(examDescCol); params.append(", ?"); provided.add(examDescCol); }
            if (examLevelCol != null) { cols.append(", ").append(examLevelCol); params.append(", ?"); provided.add(examLevelCol); }
            if (examDomainCol != null) { cols.append(", ").append(examDomainCol); params.append(", ?"); provided.add(examDomainCol); }
            if (examPublishedCol != null) { cols.append(", ").append(examPublishedCol); params.append(", ?"); provided.add(examPublishedCol); }
            cols.append(", date_creation, ").append(examCourseCol); params.append(", NOW(), ?");

            String realSql = "INSERT INTO exam (" + cols.toString() + ") VALUES (" + params.toString() + ")";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(realSql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                int idx = 1;
                ps.setString(idx++, item.getExamTitle());
                if (examDescCol != null) ps.setString(idx++, item.getExamDescription());
                if (examLevelCol != null) ps.setString(idx++, item.getLevelLabel());
                if (examDomainCol != null) ps.setString(idx++, item.getDomainLabel());
                if (examPublishedCol != null) ps.setBoolean(idx++, item.isPublished());
                ps.setInt(idx, item.getCourseId());
                ps.executeUpdate();
                try (java.sql.ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) item.setExamId(rs.getInt(1));
                }
            }
        }
    }

    public void updateExam(ExamCatalogueItem item) throws Exception {
        try (java.sql.Connection conn = EducompusDB.getConnection()) {
            if (!tableExists(conn, "exam")) {
                throw new IllegalStateException("Table exam introuvable");
            }
            java.util.Set<String> examColumns = columnsOf(conn, "exam");
            String examTitleCol = pickColumn(examColumns, "titre", "title", "name", "nom");
            String examDescCol = pickColumn(examColumns, "description", "details", "contenu", "content");
            String examLevelCol = pickColumn(examColumns, "niveau", "level");
            String examDomainCol = pickColumn(examColumns, "domaine", "domain", "matiere", "subject");
            String examCourseCol = pickColumn(examColumns, "cours_id", "course_id");
            String examPublishedCol = pickColumn(examColumns, "is_published", "published");
            if (examTitleCol == null || examCourseCol == null) {
                throw new IllegalStateException("Colonnes examen insuffisantes");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("UPDATE exam SET ").append(examTitleCol).append(" = ?");
            if (examDescCol != null) sb.append(", ").append(examDescCol).append(" = ?");
            if (examLevelCol != null) sb.append(", ").append(examLevelCol).append(" = ?");
            if (examDomainCol != null) sb.append(", ").append(examDomainCol).append(" = ?");
            if (examPublishedCol != null) sb.append(", ").append(examPublishedCol).append(" = ?");
            sb.append(", ").append(examCourseCol).append(" = ? WHERE id = ?");

            try (java.sql.PreparedStatement ps = conn.prepareStatement(sb.toString())) {
                int idx = 1;
                ps.setString(idx++, item.getExamTitle());
                if (examDescCol != null) ps.setString(idx++, item.getExamDescription());
                if (examLevelCol != null) ps.setString(idx++, item.getLevelLabel());
                if (examDomainCol != null) ps.setString(idx++, item.getDomainLabel());
                if (examPublishedCol != null) ps.setBoolean(idx++, item.isPublished());
                ps.setInt(idx++, item.getCourseId());
                ps.setInt(idx, item.getExamId());
                ps.executeUpdate();
            }
        }
    }

    public void setPublished(int examId, boolean published) throws Exception {
        try (java.sql.Connection conn = EducompusDB.getConnection()) {
            if (!tableExists(conn, "exam")) {
                throw new IllegalStateException("Table exam introuvable");
            }
            java.util.Set<String> examColumns = columnsOf(conn, "exam");
            String examPublishedCol = pickColumn(examColumns, "is_published", "published");
            if (examPublishedCol == null) {
                throw new IllegalStateException("Colonne de publication introuvable. Ajoutez is_published a la table exam.");
            }

            String sql = "UPDATE exam SET " + examPublishedCol + " = ? WHERE id = ?";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setBoolean(1, published);
                ps.setInt(2, examId);
                ps.executeUpdate();
            }
        }
    }

    public void deleteExam(int id) throws Exception {
        try (java.sql.Connection conn = EducompusDB.getConnection()) {
            if (!tableExists(conn, "exam")) {
                throw new IllegalStateException("Table exam introuvable");
            }
            String sql = "DELETE FROM exam WHERE id = ?";
            try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        }
    }

    private static ExamQuestion question(int id, String text, ExamAnswer... answers) {
        ExamQuestion question = new ExamQuestion();
        question.setId(id);
        question.setText(text);
        question.setDurationSeconds(45);
        for (ExamAnswer answer : answers) {
            question.getAnswers().add(answer);
        }
        return question;
    }

    private static ExamAnswer answer(String text, boolean correct) {
        ExamAnswer answer = new ExamAnswer();
        answer.setText(text);
        answer.setCorrect(correct);
        return answer;
    }

    // --- Simple attempt tracking + certificate generation (file-based persistence) ---
    private static final String ATTEMPTS_FILE = "var/exam_attempts.properties";

    public int getAttemptCount(String userEmail, int examId) {
        try {
            java.util.Properties p = new java.util.Properties();
            java.io.File f = new java.io.File(ATTEMPTS_FILE);
            if (f.exists()) try (java.io.FileInputStream in = new java.io.FileInputStream(f)) { p.load(in); }
            String key = attemptsKey(userEmail, examId);
            return Integer.parseInt(p.getProperty(key, "0"));
        } catch (Exception e) {
            return 0;
        }
    }

    public void recordAttempt(String userEmail, int examId, int scorePercent, boolean passed, String certificatePath) throws Exception {
        java.util.Properties p = new java.util.Properties();
        java.io.File f = new java.io.File(ATTEMPTS_FILE);
        if (f.exists()) try (java.io.FileInputStream in = new java.io.FileInputStream(f)) { p.load(in); }

        String key = attemptsKey(userEmail, examId);
        int prev = Integer.parseInt(p.getProperty(key, "0"));
        p.setProperty(key, String.valueOf(prev + 1));
        p.setProperty(passedKey(userEmail, examId), String.valueOf(passed));
        if (certificatePath != null) p.setProperty(certKey(userEmail, examId), certificatePath);

        // ensure parent dir exists
        java.io.File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(f)) { p.store(out, "Exam attempts and certificates"); }
    }

    public String getCertificatePath(String userEmail, int examId) {
        try {
            java.util.Properties p = new java.util.Properties();
            java.io.File f = new java.io.File(ATTEMPTS_FILE);
            if (!f.exists()) return null;
            try (java.io.FileInputStream in = new java.io.FileInputStream(f)) { p.load(in); }
            return p.getProperty(certKey(userEmail, examId));
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasPassed(String userEmail, int examId) {
        try {
            java.util.Properties p = new java.util.Properties();
            java.io.File f = new java.io.File(ATTEMPTS_FILE);
            if (!f.exists()) return false;
            try (java.io.FileInputStream in = new java.io.FileInputStream(f)) { p.load(in); }
            return Boolean.parseBoolean(p.getProperty(passedKey(userEmail, examId), "false"));
        } catch (Exception e) {
            return false;
        }
    }

    public java.util.List<com.educompus.model.ExamAttemptRecord> listAttemptsForExam(int examId) {
        java.util.List<com.educompus.model.ExamAttemptRecord> out = new java.util.ArrayList<>();
        try {
            java.util.Properties p = new java.util.Properties();
            java.io.File f = new java.io.File(ATTEMPTS_FILE);
            if (!f.exists()) return out;
            try (java.io.FileInputStream in = new java.io.FileInputStream(f)) { p.load(in); }

            for (String key : p.stringPropertyNames()) {
                if (!key.startsWith("attempts.")) continue;
                int lastDot = key.lastIndexOf('.');
                if (lastDot <= "attempts.".length()) continue;
                String examIdStr = key.substring(lastDot + 1);
                int eid;
                try { eid = Integer.parseInt(examIdStr); } catch (NumberFormatException ex) { continue; }
                // if examId == 0 -> return attempts for all exams
                if (examId != 0 && eid != examId) continue;
                String sanitizedEmail = key.substring("attempts.".length(), lastDot);
                int attempts = Integer.parseInt(p.getProperty(key, "0"));
                boolean passed = Boolean.parseBoolean(p.getProperty("passed." + sanitizedEmail + "." + eid, "false"));
                String cert = p.getProperty("certificate." + sanitizedEmail + "." + eid);
                // attempt to resolve exam title and course name from DB
                String examTitle = "Examen #" + eid;
                String courseName = "";
                try (java.sql.Connection conn2 = EducompusDB.getConnection()) {
                    if (tableExists(conn2, "exam")) {
                        String etitleCol = "titre";
                        java.sql.PreparedStatement q = conn2.prepareStatement("SELECT " + etitleCol + " AS t, cours_id FROM exam WHERE id = ?");
                        q.setInt(1, eid);
                        try (java.sql.ResultSet rset = q.executeQuery()) {
                            if (rset.next()) {
                                String t = rset.getString("t");
                                examTitle = t == null || t.isBlank() ? examTitle : t;
                                int cid = 0;
                                try { cid = rset.getInt("cours_id"); } catch (Exception ignored) {}
                                if (cid > 0) {
                                    // try course table
                                    try (java.sql.PreparedStatement cq = conn2.prepareStatement("SELECT titre FROM cours WHERE id = ?")) {
                                        cq.setInt(1, cid);
                                        try (java.sql.ResultSet crows = cq.executeQuery()) {
                                            if (crows.next()) courseName = crows.getString(1);
                                        }
                                    } catch (Exception ignored) {
                                        // try alternative course table name
                                        try (java.sql.PreparedStatement cq2 = conn2.prepareStatement("SELECT title FROM course WHERE id = ?")) {
                                            cq2.setInt(1, cid);
                                            try (java.sql.ResultSet crows2 = cq2.executeQuery()) {
                                                if (crows2.next()) courseName = crows2.getString(1);
                                            }
                                        } catch (Exception ignored2) {}
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}

                com.educompus.model.ExamAttemptRecord r = new com.educompus.model.ExamAttemptRecord(sanitizedEmail, attempts, passed, cert, courseName, examTitle);
                out.add(r);
            }
        } catch (Exception e) {
            // ignore and return what we have
        }
        return out;
    }

    private static String attemptsKey(String email, int examId) { return "attempts." + sanitize(email) + "." + examId; }
    private static String passedKey(String email, int examId) { return "passed." + sanitize(email) + "." + examId; }
    private static String certKey(String email, int examId) { return "certificate." + sanitize(email) + "." + examId; }
    private static String sanitize(String s) { return (s == null ? "anon" : s.replaceAll("[^a-zA-Z0-9@.-]","_")); }

    public String createCertificatePdf(String userName, String userEmail, String examTitle, int scorePercent, int examId) throws Exception {
        // create a simple PDF certificate using PDFBox
        String dir = "var/certificates";
        java.io.File outDir = new java.io.File(dir);
        if (!outDir.exists()) outDir.mkdirs();
        String filename = "certificate_" + sanitize(userEmail) + "_" + examId + ".pdf";
        java.io.File out = new java.io.File(outDir, filename);

        org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument();
        try {
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.LETTER);
            doc.addPage(page);
                org.apache.pdfbox.pdmodel.PDPageContentStream cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page);

                // draw border
                cs.setStrokingColor(30, 144, 255);
                cs.setLineWidth(3);
                cs.addRect(36, 36, page.getMediaBox().getWidth() - 72, page.getMediaBox().getHeight() - 72);
                cs.stroke();

                // logo if exists
                try {
                    java.io.InputStream logoStream = getClass().getResourceAsStream("/assets/images/logo-light.png");
                    if (logoStream != null) {
                        org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject pdImage = org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory.createFromStream(doc, logoStream);
                        float imgW = 120;
                        float imgH = 120f * pdImage.getHeight() / pdImage.getWidth();
                        cs.drawImage(pdImage, (page.getMediaBox().getWidth() - imgW) / 2f, page.getMediaBox().getHeight() - 120 - imgH, imgW, imgH);
                    }
                } catch (Exception ignored) {
                }

                // Title
                cs.beginText();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD, 28);
                cs.newLineAtOffset(72, page.getMediaBox().getHeight() - 200);
                cs.showText("Certificat de réussite");
                cs.endText();

                // body
                cs.beginText();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 14);
                cs.newLineAtOffset(72, page.getMediaBox().getHeight() - 240);
                cs.showText("Ce certificat atteste que : " + userName);
                cs.endText();

                cs.beginText();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 14);
                cs.newLineAtOffset(72, page.getMediaBox().getHeight() - 260);
                cs.showText("Email : " + userEmail);
                cs.endText();

                cs.beginText();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 14);
                cs.newLineAtOffset(72, page.getMediaBox().getHeight() - 280);
                cs.showText("A réussi l'examen : " + examTitle + " avec un score de " + scorePercent + "%.");
                cs.endText();

                // signature line
                cs.beginText();
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_OBLIQUE, 12);
                cs.newLineAtOffset(page.getMediaBox().getWidth() - 250, 100);
                cs.showText("Signature: ____________________");
                cs.endText();

                cs.close();
            doc.save(out);
        } finally {
            try { doc.close(); } catch (Exception ignored) {}
        }
        return out.getAbsolutePath();
    }
}
