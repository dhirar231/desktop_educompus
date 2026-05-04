package com.educompus.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.io.InputStream;
import java.util.Properties;

import com.educompus.util.MarketplaceConfig;

public class TwilioSmsService {

    private final String fromNumber;
    private static boolean initialized = false;

    public TwilioSmsService() {
        String sid   = MarketplaceConfig.get("TWILIO_ACCOUNT_SID");
        String token = MarketplaceConfig.get("TWILIO_AUTH_TOKEN");
        this.fromNumber = MarketplaceConfig.get("TWILIO_FROM_NUMBER");
        if (!initialized) {
            Twilio.init(sid, token);
            initialized = true;
        }
    }

    /**
     * Envoie un SMS au numéro destinataire.
     * @param toNumber numéro au format E.164 ex: +21620123456
     * @param message  texte du SMS (max ~160 caractères pour un seul segment)
     */
    public void envoyer(String toNumber, String message) {
        // Normaliser le numéro tunisien si besoin
        String dest = normaliser(toNumber);
        Message.creator(new PhoneNumber(dest), new PhoneNumber(fromNumber), message).create();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String normaliser(String numero) {
        if (numero == null) throw new IllegalArgumentException("Numéro de téléphone manquant.");
        String clean = numero.replaceAll("[\\s\\-\\.]", "");
        // Ajouter +216 si c'est un numéro tunisien à 8 chiffres
        if (clean.matches("[2-9]\\d{7}")) return "+216" + clean;
        if (clean.matches("216\\d{8}"))   return "+" + clean;
        return clean; // déjà au format E.164 ou international
    }

    private static Properties chargerProperties() {
        Properties props = new Properties();
        try (InputStream is = TwilioSmsService.class.getResourceAsStream("/twilio/twilio.properties")) {
            if (is != null) props.load(is);
        } catch (Exception ignored) {}
        return props;
    }

    private static String lire(Properties props, String key) {
        String val = props.getProperty(key);
        if (val == null || val.isBlank() || val.startsWith("REMPLACE")) {
            val = System.getenv(key);
        }
        if (val == null || val.isBlank()) {
            throw new IllegalStateException("Clé Twilio manquante : " + key
                    + ". Configure src/main/resources/twilio/twilio.properties");
        }
        return val.trim();
    }
}
