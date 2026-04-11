package com.educompus.util;

/**
 * Ouvre une URL dans le navigateur par défaut.
 * Utilise ProcessBuilder (cmd /c start) sur Windows pour contourner
 * les limitations de java.awt.Desktop dans un contexte JavaFX.
 */
public final class UrlOpener {

    private UrlOpener() {}

    /**
     * Ouvre l'URL dans le navigateur par défaut.
     * @throws Exception si l'ouverture échoue
     */
    public static void open(String url) throws Exception {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL vide ou nulle.");
        }

        String os = System.getProperty("os.name", "").toLowerCase();

        if (os.contains("win")) {
            // Windows : cmd /c start "" "url"
            new ProcessBuilder("cmd", "/c", "start", "", url)
                    .inheritIO()
                    .start();
        } else if (os.contains("mac")) {
            new ProcessBuilder("open", url).start();
        } else {
            // Linux / autres
            new ProcessBuilder("xdg-open", url).start();
        }
    }

    /**
     * Ouvre l'URL sans lever d'exception — retourne false si échec.
     */
    public static boolean openSilent(String url) {
        try {
            open(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
