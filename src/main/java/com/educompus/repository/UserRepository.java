package com.educompus.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

public final class UserRepository {
    private static final String PROFILES_FILE = "var/user_profiles.properties";

    public boolean hasCompletedPlacement(String email) {
        try (java.sql.Connection conn = EducompusDB.getConnection()) {
            // prefer DB-stored flag if table exists
            if (tableExists(conn, "user_meta")) {
                try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT value FROM user_meta WHERE user_email = ? AND meta_key = 'placement_done' LIMIT 1")) {
                    ps.setString(1, email);
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return Boolean.parseBoolean(rs.getString(1));
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        // fallback to file-based storage
        try {
            Properties p = new Properties();
            File f = new File(PROFILES_FILE);
            if (f.exists()) try (FileInputStream in = new FileInputStream(f)) { p.load(in); }
            return Boolean.parseBoolean(p.getProperty(placementKey(email), "false"));
        } catch (Exception e) {
            return false;
        }
    }

    public void setPlacementResult(String email, int enScorePercent, int frScorePercent) {
        // Try to persist in DB first if possible
        try (java.sql.Connection conn = EducompusDB.getConnection()) {
            if (tableExists(conn, "user_meta")) {
                upsertMeta(conn, email, "placement_done", "true");
                upsertMeta(conn, email, "placement_en_score", String.valueOf(enScorePercent));
                upsertMeta(conn, email, "placement_fr_score", String.valueOf(frScorePercent));
                return;
            }
        } catch (Exception ignored) {}

        // fallback to file-based storage
        try {
            Properties p = new Properties();
            File f = new File(PROFILES_FILE);
            if (f.exists()) try (FileInputStream in = new FileInputStream(f)) { p.load(in); }
            p.setProperty(placementKey(email), "true");
            p.setProperty(scoreKey(email, "en"), String.valueOf(enScorePercent));
            p.setProperty(scoreKey(email, "fr"), String.valueOf(frScorePercent));
            File parent = f.getParentFile(); if (parent != null && !parent.exists()) parent.mkdirs();
            try (FileOutputStream out = new FileOutputStream(f)) { p.store(out, "User placement results"); }
        } catch (Exception ignored) {}
    }

    public int[] getPlacementScores(String email) {
        try (java.sql.Connection conn = EducompusDB.getConnection()) {
            if (tableExists(conn, "user_meta")) {
                try (java.sql.PreparedStatement ps = conn.prepareStatement("SELECT meta_key, value FROM user_meta WHERE user_email = ? AND meta_key IN ('placement_en_score','placement_fr_score')")) {
                    ps.setString(1, email);
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        int en = -1; int fr = -1;
                        while (rs.next()) {
                            String k = rs.getString(1);
                            String v = rs.getString(2);
                            if ("placement_en_score".equals(k)) en = Integer.parseInt(v == null ? "-1" : v);
                            if ("placement_fr_score".equals(k)) fr = Integer.parseInt(v == null ? "-1" : v);
                        }
                        return new int[]{en, fr};
                    }
                }
            }
        } catch (Exception ignored) {}

        try {
            Properties p = new Properties();
            File f = new File(PROFILES_FILE);
            if (!f.exists()) return new int[]{-1, -1};
            try (FileInputStream in = new FileInputStream(f)) { p.load(in); }
            int en = Integer.parseInt(p.getProperty(scoreKey(email, "en"), "-1"));
            int fr = Integer.parseInt(p.getProperty(scoreKey(email, "fr"), "-1"));
            return new int[]{en, fr};
        } catch (Exception e) {
            return new int[]{-1, -1};
        }
    }

    private static void upsertMeta(java.sql.Connection conn, String email, String key, String value) throws Exception {
        String sel = "SELECT 1 FROM user_meta WHERE user_email = ? AND meta_key = ? LIMIT 1";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sel)) {
            ps.setString(1, email);
            ps.setString(2, key);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    try (java.sql.PreparedStatement ups = conn.prepareStatement("UPDATE user_meta SET value = ? WHERE user_email = ? AND meta_key = ?")) {
                        ups.setString(1, value);
                        ups.setString(2, email);
                        ups.setString(3, key);
                        ups.executeUpdate();
                        return;
                    }
                }
            }
        }
        try (java.sql.PreparedStatement ins = conn.prepareStatement("INSERT INTO user_meta (user_email, meta_key, value) VALUES (?, ?, ?)") ) {
            ins.setString(1, email);
            ins.setString(2, key);
            ins.setString(3, value);
            ins.executeUpdate();
        }
    }

    private static boolean tableExists(java.sql.Connection conn, String tableName) throws Exception {
        java.sql.DatabaseMetaData meta = conn.getMetaData();
        try (java.sql.ResultSet rs = meta.getTables(conn.getCatalog(), null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private static String placementKey(String email) { return "placement_done." + sanitize(email); }
    private static String scoreKey(String email, String lang) { return "placement." + lang + "." + sanitize(email); }
    private static String sanitize(String s) { return (s == null ? "anon" : s.replaceAll("[^a-zA-Z0-9@.-]","_")); }
}
