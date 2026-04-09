package com.educompus.model;

public record AuthUser(
        String email,
        String displayName,
        String imageUrl,
        boolean admin
) {
}
