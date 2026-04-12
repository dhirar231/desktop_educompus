package com.educompus.service;

import com.educompus.model.ExamAnswer;
import com.educompus.model.ExamQuestion;
import com.educompus.repository.EducompusDB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServiceExamQuestion {

    public void addQuestion(ExamQuestion q, int examId) throws Exception {
        String sql = "INSERT INTO question (texte, duree, date_creation, exam_id) VALUES (?, ?, NOW(), ?)";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, q.getText());
            ps.setInt(2, q.getDurationSeconds());
            ps.setInt(3, examId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) q.setId(rs.getInt(1));
            }
        }
    }

    public void updateQuestion(ExamQuestion q) throws Exception {
        String sql = "UPDATE question SET texte=?, duree=? WHERE id=?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q.getText());
            ps.setInt(2, q.getDurationSeconds());
            ps.setInt(3, q.getId());
            ps.executeUpdate();
        }
    }

    public void deleteQuestion(int id) throws Exception {
        String sql = "DELETE FROM question WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void addAnswer(ExamAnswer a, int questionId) throws Exception {
        try (Connection conn = EducompusDB.getConnection()) {
            String countSql = "SELECT COUNT(*) FROM reponse WHERE question_id = ?";
            try (PreparedStatement cps = conn.prepareStatement(countSql)) {
                cps.setInt(1, questionId);
                try (ResultSet crs = cps.executeQuery()) {
                    if (crs.next() && crs.getInt(1) >= 4) {
                        throw new IllegalStateException("Chaque question ne peut avoir que 4 réponses maximum");
                    }
                }
            }

            String sql = "INSERT INTO reponse (texte, correcte, question_id, date_creation) VALUES (?, ?, ?, NOW())";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, a.getText());
                ps.setBoolean(2, a.isCorrect());
                ps.setInt(3, questionId);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) a.setId(rs.getInt(1));
                }
            }
        }
    }

    public void updateAnswer(ExamAnswer a) throws Exception {
        String sql = "UPDATE reponse SET texte=?, correcte=? WHERE id=?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.getText());
            ps.setBoolean(2, a.isCorrect());
            ps.setInt(3, a.getId());
            ps.executeUpdate();
        }
    }

    public void deleteAnswer(int id) throws Exception {
        String sql = "DELETE FROM reponse WHERE id = ?";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<ExamQuestion> listQuestionsByExamId(int examId) throws Exception {
        Map<Integer, ExamQuestion> questions = new LinkedHashMap<>();
        String sql = "SELECT q.id AS question_id, q.texte AS question_text, q.duree AS question_duration, r.id AS answer_id, r.texte AS answer_text, r.correcte AS answer_correct FROM question q LEFT JOIN reponse r ON r.question_id = q.id WHERE q.exam_id = ? ORDER BY q.id ASC, r.id ASC";
        try (Connection conn = EducompusDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int qid = rs.getInt("question_id");
                    ExamQuestion q = questions.computeIfAbsent(qid, id -> {
                        ExamQuestion qq = new ExamQuestion();
                        qq.setId(id);
                        return qq;
                    });
                    q.setText(rs.getString("question_text"));
                    q.setDurationSeconds(rs.getInt("question_duration"));
                    int aid = rs.getInt("answer_id");
                    if (aid > 0) {
                        ExamAnswer a = new ExamAnswer();
                        a.setId(aid);
                        a.setText(rs.getString("answer_text"));
                        a.setCorrect(rs.getBoolean("answer_correct"));
                        q.getAnswers().add(a);
                    }
                }
            }
        }
        return new ArrayList<>(questions.values());
    }
}
