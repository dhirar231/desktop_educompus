package com.educompus.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistance des tâches todo par date.
 * Stockage local dans ~/.educompus/todo/{date}.json
 * Aucune base de données.
 */
public class TodoService {

    private static final Path BASE_DIR = Path.of(
            System.getProperty("user.home"), ".educompus", "todo");

    public static class Tache {
        public String texte;
        public boolean faite;

        public Tache(String texte, boolean faite) {
            this.texte = texte;
            this.faite = faite;
        }
    }

    // ── Charger ───────────────────────────────────────────────────────────────

    public List<Tache> charger(LocalDate date) {
        List<Tache> liste = new ArrayList<>();
        Path fichier = fichier(date);
        if (!Files.exists(fichier)) return liste;
        try {
            String json = Files.readString(fichier, StandardCharsets.UTF_8);
            String[] blocs = json.split("\\},\\s*\\{");
            for (String bloc : blocs) {
                String texte = extraire(bloc, "texte");
                String faiteStr = extraire(bloc, "faite");
                if (texte != null)
                    liste.add(new Tache(texte, "true".equals(faiteStr)));
            }
        } catch (Exception e) {
            System.err.println("[Todo] Erreur chargement : " + e.getMessage());
        }
        return liste;
    }

    // ── Sauvegarder ───────────────────────────────────────────────────────────

    public void sauvegarder(LocalDate date, List<Tache> taches) {
        try {
            Files.createDirectories(BASE_DIR);
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < taches.size(); i++) {
                Tache t = taches.get(i);
                sb.append("  {")
                  .append("\"texte\":").append(jsonStr(t.texte)).append(",")
                  .append("\"faite\":").append(t.faite)
                  .append("}");
                if (i < taches.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("]");
            Files.writeString(fichier(date), sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            System.err.println("[Todo] Erreur sauvegarde : " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Path fichier(LocalDate date) {
        return BASE_DIR.resolve(date.toString() + ".json");
    }

    private String extraire(String bloc, String cle) {
        String pattern = "\"" + cle + "\":";
        int idx = bloc.indexOf(pattern);
        if (idx == -1) return null;
        int debut = idx + pattern.length();
        // Valeur string (entre guillemets) ou booléen
        if (debut < bloc.length() && bloc.charAt(debut) == '"') {
            debut++;
            int fin = bloc.indexOf("\"", debut);
            return fin == -1 ? null : bloc.substring(debut, fin)
                    .replace("\\n", "\n").replace("\\\"", "\"");
        } else {
            int fin = bloc.indexOf(",", debut);
            if (fin == -1) fin = bloc.indexOf("}", debut);
            return fin == -1 ? null : bloc.substring(debut, fin).trim();
        }
    }

    private String jsonStr(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                       .replace("\n", "\\n").replace("\r", "") + "\"";
    }
}
