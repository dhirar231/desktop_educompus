package com.educompus.repository;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;

/**
 * Small test helper that requires a local MySQL and applies the schema.
 * Usage: set environment variables TEST_DB_USER, TEST_DB_PASS and ensure MySQL is running.
 */
public final class TestMySqlHelper {
    private TestMySqlHelper() {}

    public static void init() throws Exception {
        String useLocal = System.getenv("TEST_USE_LOCAL_MYSQL");
        if (!"1".equals(useLocal) && !"true".equalsIgnoreCase(useLocal)) {
            throw new IllegalStateException("Tests require local MySQL. Set TEST_USE_LOCAL_MYSQL=1 in environment.");
        }

        Map<String, String> env = System.getenv();
        String user = env.getOrDefault("TEST_DB_USER", "root");
        String pass = env.getOrDefault("TEST_DB_PASS", "");
        String url = env.getOrDefault("TEST_JDBC_URL", "jdbc:mysql://127.0.0.1:3306/educompus?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true");

        // set system properties expected by EducompusDB
        System.setProperty("educompus.jdbcUrl", url);
        System.setProperty("educompus.dbUser", user);
        System.setProperty("educompus.dbPass", pass);

        // By default do NOT apply SQL schema files. If you explicitly want the test helper
        // to apply the SQL bundle from resources, set TEST_FORCE_SCHEMA_APPLY=1 in environment.
        String force = System.getenv().getOrDefault("TEST_FORCE_SCHEMA_APPLY", "0");
        if (!"1".equals(force) && !"true".equalsIgnoreCase(force)) {
            // Use existing local MySQL schema as-is.
            return;
        }

        // apply schema (idempotent) only when forced
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            try (InputStream in = TestMySqlHelper.class.getResourceAsStream("/sql/projects_schema.sql")) {
                if (in == null) throw new IllegalStateException("Schema resource not found: /sql/projects_schema.sql");
                String sql = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                for (String stmt : sql.split(";")) {
                    String s = stmt.trim();
                    if (s.isEmpty()) continue;
                    try (Statement st = conn.createStatement()) {
                        try {
                            st.execute(s);
                        } catch (Exception ignored) {
                            // ignore (table exists, etc.)
                        }
                    }
                }
            }
        }
    }
}
    