package com.educompus.util;

import java.io.InputStream;
import java.util.Properties;

/**
 * Lecteur centralisé de la configuration Marketplace.
 * Toutes les clés API sont dans src/main/resources/configMarketplace.properties
 */
public final class MarketplaceConfig {

    private static final String CONFIG_FILE = "/configMarketplace.properties";
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream is = MarketplaceConfig.class.getResourceAsStream(CONFIG_FILE)) {
            if (is != null) PROPS.load(is);
        } catch (Exception ignored) {}
    }

    private MarketplaceConfig() {}

    /**
     * Lit une clé depuis configMarketplace.properties.
     * Fallback sur System.getenv() si la valeur commence par "REMPLACE" ou est vide.
     * @throws IllegalStateException si la clé est absente des deux sources
     */
    public static String get(String key) {
        String val = PROPS.getProperty(key);
        if (val == null || val.isBlank() || val.startsWith("REMPLACE"))
            val = System.getenv(key);
        if (val == null || val.isBlank())
            throw new IllegalStateException(
                "Clé manquante : " + key + "\nConfigurez src/main/resources/configMarketplace.properties");
        return val.trim();
    }

    /** Comme get() mais retourne defaut si absent (clé optionnelle). */
    public static String getOrDefault(String key, String defaut) {
        try { return get(key); }
        catch (IllegalStateException ignored) { return defaut; }
    }
}
