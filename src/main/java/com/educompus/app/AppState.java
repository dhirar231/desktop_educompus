package com.educompus.app;

public final class AppState {
    public enum Role {
        USER,
        ADMIN
    }

    private static Role role = Role.USER;
    private static boolean dark = false;
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

    public static boolean isDark() {
        return dark;
    }

    public static void setDark(boolean value) {
        dark = value;
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
}
