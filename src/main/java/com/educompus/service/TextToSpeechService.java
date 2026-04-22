package com.educompus.service;

/**
 * Text-to-Speech sans dépendance externe.
 * Windows : PowerShell SAPI.SpVoice (voix système)
 * macOS   : commande "say"
 * Linux   : commande "espeak"
 */
public class TextToSpeechService {

    private static Process processEnCours = null;

    /** Lit le texte à voix haute (bloquant — appeler dans un thread séparé). */
    public static void lire(String texte) {
        if (texte == null || texte.isBlank()) return;
        arreter();

        String os = System.getProperty("os.name", "").toLowerCase();
        try {
            ProcessBuilder pb;
            if (os.contains("win")) {
                // On utilise -c pour que PowerShell soit interruptible
                String script = "Add-Type -AssemblyName System.Speech; " +
                        "$s = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                        "$s.Speak('" + echapper(texte) + "');";
                pb = new ProcessBuilder(
                        "powershell", "-NoProfile", "-NonInteractive", "-Command", script);
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("say", texte);
            } else {
                pb = new ProcessBuilder("espeak", "-v", "fr", texte);
            }
            pb.redirectErrorStream(true);
            processEnCours = pb.start();
            processEnCours.waitFor(); // bloquant dans le thread appelant
        } catch (InterruptedException ignored) {
            // Interrompu par arreter() → normal
        } catch (Exception e) {
            System.err.println("[TTS] Erreur : " + e.getMessage());
        }
    }

    /** Arrête immédiatement la lecture en cours. */
    public static void arreter() {
        Process p = processEnCours;
        processEnCours = null;
        if (p != null) {
            // Tuer le processus PowerShell ET ses enfants (le moteur vocal)
            p.descendants().forEach(ProcessHandle::destroyForcibly);
            p.destroyForcibly();
            try { p.waitFor(); } catch (InterruptedException ignored) {}
        }
    }

    private static String echapper(String s) {
        return s.replace("'", " ").replace("\"", " ")
                .replace("\n", " ").replace("\r", "");
    }
}
