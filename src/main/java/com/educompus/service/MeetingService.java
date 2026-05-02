package com.educompus.service;

import com.educompus.model.Project;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

public final class MeetingService {
    private static final String DEFAULT_DOMAIN = "meet.jit.si";

    public String createRoom(String title, String room) {
        String explicitRoom = normalizeRoom(room);
        if (!explicitRoom.isBlank()) {
            validateRoom(explicitRoom);
            return explicitRoom;
        }

        String base = slugify(title);
        if (base.isBlank()) {
            base = "meeting";
        }
        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public void validateRoom(String room) {
        String value = normalizeRoom(room);
        if (!value.matches("^[a-zA-Z0-9][a-zA-Z0-9_-]{2,120}$")) {
            throw new IllegalArgumentException("Salle invalide. Utilisez 3 a 121 caracteres: lettres, chiffres, _ ou -.");
        }
    }

    public String buildMeetingUrl(String room) {
        String value = normalizeRoom(room);
        validateRoom(value);
        return "https://" + getJitsiDomain() + "/" + value;
    }

    public String buildMeetingUrl(String room, String displayName, boolean startAudioMuted, boolean startVideoMuted) {
        String baseUrl = buildMeetingUrl(room);
        String safeDisplayName = safe(displayName);
        StringBuilder fragment = new StringBuilder();
        if (!safeDisplayName.isBlank()) {
            fragment.append("userInfo.displayName=%22")
                    .append(urlValue(safeDisplayName))
                    .append("%22");
        }
        appendFragment(fragment, "config.startWithAudioMuted", String.valueOf(startAudioMuted));
        appendFragment(fragment, "config.startWithVideoMuted", String.valueOf(startVideoMuted));
        appendFragment(fragment, "config.prejoinPageEnabled", "true");
        return fragment.length() == 0 ? baseUrl : (baseUrl + "#" + fragment);
    }

    public String createProjectRoom(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Projet introuvable.");
        }
        int id = Math.max(0, project.getId());
        String slug = slugify(project.getTitle());
        if (slug.isBlank()) {
            slug = "project";
        }
        String room = (id > 0 ? ("project-" + id + "-" + slug) : ("project-" + slug));
        if (room.length() > 121) {
            room = room.substring(0, 121).replaceAll("[-_]+$", "");
        }
        validateRoom(room);
        return room;
    }

    public String getJitsiDomain() {
        String domain = firstNonBlank(
                System.getProperty("educompus.jitsi.domain"),
                System.getenv("EDUCOMPUS_JITSI_DOMAIN"),
                DEFAULT_DOMAIN
        );
        return domain.trim();
    }

    private String slugify(String value) {
        return Normalizer.normalize(safe(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^[-_]+|[-_]+$", "");
    }

    private static String normalizeRoom(String room) {
        return safe(room).replace(' ', '-');
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.trim().isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static void appendFragment(StringBuilder fragment, String key, String value) {
        if (fragment.length() > 0) {
            fragment.append('&');
        }
        fragment.append(key).append('=').append(value);
    }

    private static String urlValue(String value) {
        return safe(value).replace("%", "%25").replace(" ", "%20");
    }
}
