package com.educompus.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class EducompusDB {
    private static final String URL = System.getProperty(
            "educompus.jdbcUrl",
            "jdbc:mysql://127.0.0.1:3306/educompus" +
                    "?useUnicode=true&characterEncoding=utf8" +
                    "&serverTimezone=UTC" +
                    "&useSSL=false" +
                    "&allowPublicKeyRetrieval=true"
    );
    private static final String USER = System.getProperty("educompus.dbUser", "root");
    private static final String PASS = System.getProperty("educompus.dbPass", "");

    private EducompusDB() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
