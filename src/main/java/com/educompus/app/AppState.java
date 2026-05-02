package com.educompus.app;

public final class AppState {
    public enum Role {
        USER,
        ADMIN,
        TEACHER
    }

    private static Role role = Role.USER;
    private static boolean dark = false;
    private static int userId = 0;
    private static String userEmail = "";
    private static String userDisplayName = "";
    private static String userImageUrl = "";

    private AppState() {
    }

    public static Role getRole() {
        return role;
    }

    public static void setRole(Role value) {
        role = value == null ? Role.USER : value;
    }

    public static boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public static boolean isTeacher() {
        return role == Role.TEACHER;
    }

    public static boolean isDark() {
        return dark;
    }

    public static void setDark(boolean value) {
        dark = value;
    }

    public static int getUserId() {
        return userId;
    }

    public static void setUserId(int id) {
        userId = Math.max(0, id);
    }

    public static String getUserEmail() {
        return userEmail;
    }

    public static void setUserEmail(String email) {
        userEmail = email == null ? "" : String.valueOf(email).trim();
    }

    public static String getUserDisplayName() {
        return userDisplayName;
    }

    public static void setUserDisplayName(String displayName) {
        userDisplayName = displayName == null ? "" : String.valueOf(displayName).trim();
    }

    public static String getUserImageUrl() {
        return userImageUrl;
    }

    public static void setUserImageUrl(String imageUrl) {
        userImageUrl = imageUrl == null ? "" : String.valueOf(imageUrl).trim();
    }

    // Base URL for the web server (used by QR links and deep links)
    private static String serverBaseUrl = "http://localhost:8000";

    public static String getServerBaseUrl() {
        return serverBaseUrl;
    }

    public static void setServerBaseUrl(String url) {
        serverBaseUrl = url == null ? "" : String.valueOf(url).trim();
    }
}
