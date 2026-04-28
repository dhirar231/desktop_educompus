package com.educompus.examples;

import com.educompus.service.ResumeChapterService;
import com.educompus.service.ResumeChapterService.ResultatResume;
import com.educompus.service.ResumeChapterService.TypeResume;
import com.educompus.service.ResumeChapterService.LangueResume;

/**
 * Exemple d'utilisation du service de résumé de chapitre.
 * 
 * Pour exécuter cet exemple:
 * mvn exec:java -Dexec.mainClass="com.educompus.examples.ResumeChapterExample"
 */
public class ResumeChapterExample {

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════════");
        System.out.println("  Exemple d'utilisation du service de résumé de chapitre");
        System.out.println("═══════════════════════════════════════════════════════════════\n");

        // Chemin vers un fichier PDF de test
        // Remplacez par un vrai chemin pour tester
        String cheminPDF = "chemin/vers/chapitre.pdf";

        // Exemple 1: Résumé court en français
        System.out.println("📝 Exemple 1: Résumé COURT en FRANÇAIS");
        System.out.println("─────────────────────────────────────");
        demonstrerResume(cheminPDF, TypeResume.COURT, LangueResume.FR);

        System.out.println("\n");

        // Exemple 2: Résumé détaillé en français
        System.out.println("📚 Exemple 2: Résumé DÉTAILLÉ en FRANÇAIS");
        System.out.println("─────────────────────────────────────────");
        demonstrerResume(cheminPDF, TypeResume.DETAILLE, LangueResume.FR);

        System.out.println("\n");

        // Exemple 3: Points clés en français
        System.out.println("🎯 Exemple 3: POINTS CLÉS en FRANÇAIS");
        System.out.println("─────────────────────────────────────");
        demonstrerResume(cheminPDF, TypeResume.POINTS_CLES, LangueResume.FR);

        System.out.println("\n");

        // Exemple 4: Résumé en anglais
        System.out.println("🇬🇧 Exemple 4: Résumé COURT en ANGLAIS");
        System.out.println("──────────────────────────────────────");
        demonstrerResume(cheminPDF, TypeResume.COURT, LangueResume.EN);

        System.out.println("\n");

        // Exemple 5: Résumé en arabe
        System.out.println("🇸🇦 Exemple 5: Résumé COURT en ARABE");
        System.out.println("────────────────────────────────────");
        demonstrerResume(cheminPDF, TypeResume.COURT, LangueResume.AR);

        System.out.println("\n═══════════════════════════════════════════════════════════════");
        System.out.println("  Fin des exemples");
        System.out.println("═══════════════════════════════════════════════════════════════");
    }

    /**
     * Démontre la génération d'un résumé avec les paramètres donnés.
     */
    private static void demonstrerResume(String cheminPDF, TypeResume type, LangueResume langue) {
        System.out.println("Type: " + type.label);
        System.out.println("Langue: " + langue.label);
        System.out.println();

        long debut = System.currentTimeMillis();

        ResultatResume resultat = ResumeChapterService.genererResume(
            cheminPDF,
            type,
            langue
        );

        long duree = System.currentTimeMillis() - debut;

        if (resultat.succes) {
            System.out.println("✅ Résumé généré avec succès en " + duree + "ms");
            System.out.println();
            System.out.println("Résumé:");
            System.out.println("─────────────────────────────────────");
            System.out.println(resultat.texte);
            System.out.println("─────────────────────────────────────");
            System.out.println("Longueur: " + resultat.texte.length() + " caractères");
        } else {
            System.out.println("❌ Erreur: " + resultat.erreur);
        }
    }

    /**
     * Exemple d'utilisation programmatique simple.
     */
    public static void exempleSimple() {
        // Générer un résumé court en français
        ResultatResume resultat = ResumeChapterService.genererResume(
            "chemin/vers/chapitre.pdf",
            TypeResume.COURT,
            LangueResume.FR
        );

        if (resultat.succes) {
            System.out.println("Résumé: " + resultat.texte);
        } else {
            System.err.println("Erreur: " + resultat.erreur);
        }
    }

    /**
     * Exemple avec gestion d'erreurs complète.
     */
    public static void exempleAvecGestionErreurs(String cheminPDF) {
        try {
            // Vérifier que le chemin n'est pas null ou vide
            if (cheminPDF == null || cheminPDF.isBlank()) {
                throw new IllegalArgumentException("Le chemin du PDF ne peut pas être vide");
            }

            // Générer le résumé
            ResultatResume resultat = ResumeChapterService.genererResume(
                cheminPDF,
                TypeResume.DETAILLE,
                LangueResume.FR
            );

            // Vérifier le résultat
            if (resultat.succes) {
                // Traiter le résumé
                String resume = resultat.texte;
                System.out.println("Résumé généré: " + resume.substring(0, Math.min(100, resume.length())) + "...");

                // Sauvegarder dans un fichier
                sauvegarderResume(resume, "resume_output.txt");

            } else {
                // Gérer l'erreur
                System.err.println("Impossible de générer le résumé: " + resultat.erreur);
                
                // Actions alternatives
                System.out.println("Suggestions:");
                System.out.println("- Vérifiez que le fichier PDF existe");
                System.out.println("- Vérifiez que le PDF n'est pas corrompu");
                System.out.println("- Vérifiez votre connexion internet");
            }

        } catch (Exception e) {
            System.err.println("Erreur inattendue: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Sauvegarde un résumé dans un fichier.
     */
    private static void sauvegarderResume(String resume, String nomFichier) {
        try {
            java.nio.file.Files.writeString(
                java.nio.file.Paths.get(nomFichier),
                resume
            );
            System.out.println("✅ Résumé sauvegardé dans: " + nomFichier);
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    /**
     * Exemple de génération de résumés multiples.
     */
    public static void genererResumesMultiples(String cheminPDF) {
        System.out.println("Génération de résumés multiples...\n");

        // Générer tous les types en français
        for (TypeResume type : TypeResume.values()) {
            System.out.println("Génération: " + type.label);
            
            ResultatResume resultat = ResumeChapterService.genererResume(
                cheminPDF,
                type,
                LangueResume.FR
            );

            if (resultat.succes) {
                String nomFichier = "resume_" + type.code + "_fr.txt";
                sauvegarderResume(resultat.texte, nomFichier);
            }
        }

        System.out.println("\n✅ Tous les résumés ont été générés");
    }

    /**
     * Exemple de génération multilingue.
     */
    public static void genererResumesMultilingues(String cheminPDF) {
        System.out.println("Génération de résumés multilingues...\n");

        TypeResume type = TypeResume.COURT;

        // Générer dans toutes les langues
        for (LangueResume langue : LangueResume.values()) {
            System.out.println("Génération en: " + langue.label);
            
            ResultatResume resultat = ResumeChapterService.genererResume(
                cheminPDF,
                type,
                langue
            );

            if (resultat.succes) {
                String nomFichier = "resume_" + langue.code + ".txt";
                sauvegarderResume(resultat.texte, nomFichier);
            }
        }

        System.out.println("\n✅ Résumés générés dans toutes les langues");
    }
}
