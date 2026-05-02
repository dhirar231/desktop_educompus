package com.educompus.util;

import com.educompus.model.ExamAnswer;
import com.educompus.model.ExamCatalogueItem;
import com.educompus.model.ExamQuestion;
import com.educompus.repository.ExamRepository;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ExcelExamImporter {
    private ExcelExamImporter() {}

    /**
     * Import an Excel file. Expected columns (0-based):
     * 0: Exam Title
     * 1: Exam Description
     * 2: Exam Level
     * 3: Course Id (optional)
     * 4: Question Text
     * 5: Duration (seconds or mm:ss)
     * 6: Choice 1
     * 7: Choice 2
     * 8: Choice 3
     * 9: Choice 4
     * 10: Correct answer (A/B/C/D or 1-4)
     */
    public static String importFromExcel(File file, int defaultCourseId) throws Exception {
        if (file == null || !file.exists()) throw new IllegalArgumentException("Fichier introuvable");
        ExamRepository repo = new ExamRepository();
        Map<String, Integer> exams = new LinkedHashMap<>();
        int createdExams = 0, createdQuestions = 0, createdAnswers = 0;
        StringBuilder debug = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(file); Workbook wb = WorkbookFactory.create(fis)) {
            Sheet sheet = wb.getNumberOfSheets() > 0 ? wb.getSheetAt(0) : null;
            if (sheet == null) throw new IllegalArgumentException("Fichier Excel vide");

            // detect header row (if first cell contains words like 'exam' or 'title')
            int firstRow = sheet.getFirstRowNum();
            Row header = sheet.getRow(firstRow);
            boolean hasHeader = false;
            if (header != null) {
                Cell c = header.getCell(0);
                if (c != null && c.getCellType() == CellType.STRING) {
                    String t = c.getStringCellValue().toLowerCase(Locale.ROOT);
                    if (t.contains("exam") || t.contains("titre") || t.contains("title")) hasHeader = true;
                }
            }
            int start = hasHeader ? firstRow + 1 : firstRow;

            for (int r = start; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String examTitle = getCellString(row, 0).trim();
                if (examTitle.isEmpty()) continue; // skip empty rows
                String examDesc = getCellString(row, 1);
                String examLevel = getCellString(row, 2);
                String courseCell = getCellString(row, 3).trim();
                int courseId = defaultCourseId;
                if (!courseCell.isEmpty()) {
                    try { courseId = Integer.parseInt(courseCell); } catch (Exception ignored) {}
                }
                String qtext = getCellString(row, 4).trim();
                int duration = parseDurationToSeconds(getCellString(row, 5));
                if (duration <= 0) duration = 45;
                String[] choices = new String[4];
                for (int i = 0; i < 4; i++) choices[i] = getCellString(row, 6 + i);
                int correctIndex = parseCorrectIndex(getCellString(row, 10)); // 1..4 or -1

                try {
                    // create exam if not created yet
                    int examId = exams.containsKey(examTitle) ? exams.get(examTitle) : -1;
                    if (examId <= 0) {
                        ExamCatalogueItem item = new ExamCatalogueItem();
                        item.setExamTitle(examTitle);
                        item.setExamDescription(examDesc);
                        item.setLevelLabel(examLevel);
                        item.setCourseId(courseId);
                        item.setPublished(false);
                        repo.addExam(item);
                        examId = item.getExamId();
                        exams.put(examTitle, examId);
                        createdExams++;
                        debug.append("Row ").append(r).append(": created exam '").append(examTitle).append("' -> id=").append(examId).append("\n");
                        if (examId <= 0) debug.append("Row ").append(r).append(": WARNING exam id is 0 (generated keys not returned)\n");
                    }

                    // add question
                    ExamQuestion q = new ExamQuestion();
                    q.setText(qtext);
                    q.setDurationSeconds(duration);
                    repo.addQuestion(q, examId);
                    createdQuestions++;
                    int qid = q.getId();
                    debug.append("Row ").append(r).append(": added question '").append(qtext).append("' -> id=").append(qid).append(" (examId=").append(examId).append(")\n");
                    if (qid <= 0) debug.append("Row ").append(r).append(": WARNING question id is 0 (generated keys not returned)\n");

                    for (int i = 0; i < 4; i++) {
                        String txt = choices[i];
                        if (txt == null || txt.trim().isEmpty()) continue;
                        ExamAnswer a = new ExamAnswer();
                        a.setText(txt.trim());
                        a.setCorrect(correctIndex == (i + 1));
                        repo.addAnswer(a, qid);
                        createdAnswers++;
                        debug.append("Row ").append(r).append(": added answer '").append(a.getText()).append("' -> id=").append(a.getId()).append(" (questionId=").append(qid).append(")\n");
                    }
                } catch (Exception e) {
                    debug.append("Row ").append(r).append(": ERROR - ").append(e.getClass().getSimpleName()).append(" - ").append(e.getMessage()).append("\n");
                }
            }
        }
        // write debug log to var/
        try {
            java.io.File dir = new java.io.File("var");
            if (!dir.exists()) dir.mkdirs();
            java.io.File log = new java.io.File(dir, "excel_import_debug.log");
            try (java.io.FileWriter fw = new java.io.FileWriter(log, true)) {
                fw.write("--- Import at " + java.time.ZonedDateTime.now() + " ---\n");
                fw.write(debug.toString());
                fw.write("Summary: exams=" + createdExams + ", questions=" + createdQuestions + ", answers=" + createdAnswers + "\n\n");
            }
        } catch (Exception ignored) {}

        return String.format("Import terminé — examens: %d, questions: %d, réponses: %d (voir var/excel_import_debug.log pour détails)", createdExams, createdQuestions, createdAnswers);
    }

    private static String getCellString(Row row, int idx) {
        if (row == null) return "";
        Cell c = row.getCell(idx);
        if (c == null) return "";
        try {
            switch (c.getCellType()) {
                case STRING: return c.getStringCellValue();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(c)) return c.getDateCellValue().toString();
                    double d = c.getNumericCellValue();
                    if (Math.floor(d) == d) return String.valueOf((long) d);
                    return String.valueOf(d);
                case BOOLEAN: return String.valueOf(c.getBooleanCellValue());
                case FORMULA:
                    try { return c.getStringCellValue(); } catch (Exception e) { return String.valueOf(c.getNumericCellValue()); }
                default: return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    private static int parseDurationToSeconds(String s) {
        if (s == null) return 0;
        s = s.trim();
        if (s.isEmpty()) return 0;
        if (s.contains(":")) {
            String[] parts = s.split(":");
            try {
                int sec = 0;
                int mult = 1;
                for (int i = parts.length - 1; i >= 0; i--) {
                    int part = Integer.parseInt(parts[i].trim());
                    sec += part * mult;
                    mult *= 60;
                }
                return sec;
            } catch (Exception ignored) {}
        }
        try {
            return Integer.parseInt(s);
        } catch (Exception ignored) {}
        try {
            double v = Double.parseDouble(s);
            return (int) Math.round(v);
        } catch (Exception ignored) {}
        return 0;
    }

    private static int parseCorrectIndex(String s) {
        if (s == null) return -1;
        s = s.trim();
        if (s.isEmpty()) return -1;
        String up = s.toUpperCase(Locale.ROOT);
        if (up.matches("^[A-D]$")) return up.charAt(0) - 'A' + 1;
        try {
            int v = Integer.parseInt(s.replaceAll("[^0-9]", ""));
            if (v >= 1 && v <= 4) return v;
        } catch (Exception ignored) {}
        return -1;
    }
}
