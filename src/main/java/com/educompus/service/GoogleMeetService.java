package com.educompus.service;

import java.util.UUID;

/**
 * Service pour générer des liens Google Meet.
 * 
 * Note : Google Meet ne fournit pas d'API publique pour créer des meetings programmatiquement.
 * Cette classe génère des liens au format Google Meet qui peuvent être utilisés manuellement.
 */
public final class GoogleMeetService {

    /**
     * Génère un lien Google Meet avec un code de réunion aléatoire.
     * 
     * Format : https://meet.google.com/xxx-yyyy-zzz
     * 
     * @param meetingName Nom de la réunion (utilisé pour générer un code lisible)
     * @return URL Google Meet
     */
    public String generateMeetLink(String meetingName) {
        // Générer un code de réunion au format Google Meet : xxx-yyyy-zzz
        String code = generateMeetingCode();
        return "https://meet.google.com/" + code;
    }

    /**
     * Génère un code de réunion au format Google Meet.
     * Format : xxx-yyyy-zzz (3 segments de 3-4 caractères séparés par des tirets)
     * 
     * @return Code de réunion
     */
    private String generateMeetingCode() {
        // Générer 3 segments aléatoires
        String segment1 = generateSegment(3);
        String segment2 = generateSegment(4);
        String segment3 = generateSegment(3);
        
        return segment1 + "-" + segment2 + "-" + segment3;
    }

    /**
     * Génère un segment aléatoire de caractères alphanumériques.
     * 
     * @param length Longueur du segment
     * @return Segment aléatoire
     */
    private String generateSegment(int length) {
        // Caractères autorisés dans les codes Google Meet (lettres minuscules)
        String chars = "abcdefghijklmnopqrstuvwxyz";
        StringBuilder segment = new StringBuilder();
        
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * chars.length());
            segment.append(chars.charAt(index));
        }
        
        return segment.toString();
    }

    /**
     * Génère un lien Google Meet avec un UUID court.
     * Alternative plus simple mais moins "Google Meet-like".
     * 
     * @return URL Google Meet
     */
    public String generateMeetLinkSimple() {
        String uuid = UUID.randomUUID().toString().substring(0, 10).replace("-", "");
        return "https://meet.google.com/" + uuid;
    }

    /**
     * Valide si un lien est un lien Google Meet valide.
     * 
     * @param link Lien à valider
     * @return true si le lien est un lien Google Meet valide
     */
    public boolean isValidGoogleMeetLink(String link) {
        if (link == null || link.isBlank()) {
            return false;
        }
        
        // Format attendu : https://meet.google.com/xxx-yyyy-zzz
        return link.matches("https://meet\\.google\\.com/[a-z]{3}-[a-z]{4}-[a-z]{3}") ||
               link.matches("https://meet\\.google\\.com/[a-z0-9]+");
    }

    /**
     * Extrait le code de réunion d'un lien Google Meet.
     * 
     * @param link Lien Google Meet
     * @return Code de réunion, ou null si le lien est invalide
     */
    public String extractMeetingCode(String link) {
        if (link == null || link.isBlank()) {
            return null;
        }
        
        // Extraire le code après le dernier /
        int lastSlash = link.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < link.length() - 1) {
            return link.substring(lastSlash + 1);
        }
        
        return null;
    }
}
