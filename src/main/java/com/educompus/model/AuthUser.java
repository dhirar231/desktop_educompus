package com.educompus.model;

public record AuthUser(
                int id,
                String email,
                String displayName,
                String imageUrl,
                boolean admin,
                boolean teacher) {
}
