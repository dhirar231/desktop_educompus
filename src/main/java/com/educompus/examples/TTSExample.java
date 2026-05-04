package com.educompus.examples;

import com.educompus.service.GoogleTTSService;
import com.educompus.service.GoogleTTSService.ParametresTTS;
import com.educompus.service.GoogleTTSService.ResultatTTS;
import com.educompus.service.GoogleTTSService.VoixFrancaise;

import java.util.concurrent.CompletableFuture;

/**
 * Exemple d'utilisation du service TTS Google.
 */
public class TTSExample {

    public static void main(String[] args) {
        System.out.println("=== Test du service Google TTS ===\n");

        try {
            // 1. Test de configuration
            System.out.println("1. Test de configuration TTS :");
            testConfiguration();
            System.out.println();

            // 2. Conversion simple
            System.out.println("2. Conversion simple :");
            testConversionSimple();
            System.out.println();

            // 3. Test avec différentes voix
            System.out.println("3. Test avec différentes voix :");
            testDifferentesVoix();
            System.out.println();

            // 4. Test avec paramètres personnalisés
            System.out.println("4. Test avec paramètres personnalisés :");
            testParametresPersonnalises();
            System.out.println();

            // 5. Conversion d'un script éducatif
            System.out.println("5. Conversion d'un script éducatif :");
            testScriptEducatif();

        } catch (Exception e) {
            System.err.println("Erreur générale : " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n=== Fin des tests TTS ===");
    }

    private static void testConfiguration() {
        try {
            System.out.println("   Test de la configuration Google TTS...");
            
            ResultatTTS resultat = GoogleTTSService.testerConfiguration();
            
            if (resultat.isSucces()) {
                System.out.println("   ✓ Configuration TTS OK !");
                System.out.println("   Fichier généré: " + resultat.getCheminFichier());
                System.out.println("   Taille: " + resultat.getTailleFormatee());
                System.out.println("   Durée estimée: " + resultat.getDureeFormatee());
            } else {
                System.out.println("   ✗ Erreur de configuration: " + resultat.getMessageErreur());
            }
            
        } catch (Exception e) {
            System.out.println("   ✗ Erreur lors du test: " + e.getMessage());
        }
    }

    private static void testConversionSimple() {
        try {
            String texte = "Bonjour les étudiants ! Aujourd'hui nous allons apprendre les bases de la programmation Java. " +
                          "Java est un langage orienté objet très populaire dans le développement d'applications.";
            
            System.out.println("   Texte à convertir: " + texte.substring(0, Math.min(50, texte.length())) + "...");
            
            ParametresTTS parametres = new ParametresTTS();
            parametres.setVoix(VoixFrancaise.FEMALE_A);
            
            ResultatTTS resultat = GoogleTTSService.convertirTexte(texte, parametres);
            
            if (resultat.isSucces()) {
                System.out.println("   ✓ Conversion réussie !");
                System.out.println("   Fichier: " + resultat.getCheminFichier());
                System.out.println("   Taille: " + resultat.getTailleFormatee());
                System.out.println("   Durée: " + resultat.getDureeFormatee());
            } else {
                System.out.println("   ✗ Erreur: " + resultat.getMessageErreur());
            }
            
        } catch (Exception e) {
            System.out.println("   ✗ Erreur lors de la conversion: " + e.getMessage());
        }
    }

    private static void testDifferentesVoix() {
        String texte = "Ceci est un test des différentes voix françaises disponibles.";
        
        VoixFrancaise[] voixATest = {
            VoixFrancaise.FEMALE_A,
            VoixFrancaise.MALE_B,
            VoixFrancaise.NEURAL_FEMALE
        };
        
        for (VoixFrancaise voix : voixATest) {
            try {
                System.out.println("   Test de la voix: " + voix.getDescription());
                
                ParametresTTS parametres = new ParametresTTS();
                parametres.setVoix(voix);
                
                ResultatTTS resultat = GoogleTTSService.convertirTexte(texte, parametres);
                
                if (resultat.isSucces()) {
                    System.out.println("     ✓ Généré: " + resultat.getCheminFichier());
                } else {
                    System.out.println("     ✗ Erreur: " + resultat.getMessageErreur());
                }
                
            } catch (Exception e) {
                System.out.println("     ✗ Erreur: " + e.getMessage());
            }
        }
    }

    private static void testParametresPersonnalises() {
        try {
            String texte = "Ce texte sera lu avec des paramètres personnalisés : vitesse réduite, tonalité plus grave, et volume augmenté.";
            
            ParametresTTS parametres = new ParametresTTS();
            parametres.setVoix(VoixFrancaise.MALE_D);
            parametres.setVitesse(0.8);  // Plus lent
            parametres.setPitch(-2.0);   // Plus grave
            parametres.setVolumeGain(3.0); // Plus fort
            parametres.setFormat("WAV");
            
            System.out.println("   Paramètres: " + parametres);
            
            ResultatTTS resultat = GoogleTTSService.convertirTexte(texte, parametres);
            
            if (resultat.isSucces()) {
                System.out.println("   ✓ Conversion avec paramètres personnalisés réussie !");
                System.out.println("   Fichier: " + resultat.getCheminFichier());
                System.out.println("   Format: WAV | Taille: " + resultat.getTailleFormatee());
            } else {
                System.out.println("   ✗ Erreur: " + resultat.getMessageErreur());
            }
            
        } catch (Exception e) {
            System.out.println("   ✗ Erreur: " + e.getMessage());
        }
    }

    private static void testScriptEducatif() {
        try {
            String scriptEducatif = """
                Bonjour et bienvenue dans ce cours sur les algorithmes de tri !
                
                Aujourd'hui, nous allons découvrir trois algorithmes fondamentaux :
                le tri à bulles, le tri par insertion, et le tri rapide.
                
                Commençons par le tri à bulles. Cet algorithme compare les éléments adjacents
                et les échange s'ils sont dans le mauvais ordre. Il répète ce processus
                jusqu'à ce que la liste soit complètement triée.
                
                Ensuite, nous verrons le tri par insertion, qui construit la liste triée
                un élément à la fois, en insérant chaque nouvel élément à sa place correcte.
                
                Enfin, le tri rapide utilise une approche diviser pour régner,
                en partitionnant la liste autour d'un pivot.
                
                Ces algorithmes ont des complexités différentes et sont adaptés
                à différents types de données. Merci de votre attention !
                """;
            
            System.out.println("   Génération audio pour un script éducatif complet...");
            
            // Utiliser la méthode optimisée pour les scripts éducatifs
            CompletableFuture<ResultatTTS> future = GoogleTTSService.convertirScriptChapitreAsync(scriptEducatif);
            
            ResultatTTS resultat = future.get(); // Attendre le résultat
            
            if (resultat.isSucces()) {
                System.out.println("   ✓ Script éducatif converti avec succès !");
                System.out.println("   Fichier: " + resultat.getCheminFichier());
                System.out.println("   Taille: " + resultat.getTailleFormatee());
                System.out.println("   Durée estimée: " + resultat.getDureeFormatee());
                System.out.println("   Mots: " + scriptEducatif.split("\\s+").length);
            } else {
                System.out.println("   ✗ Erreur: " + resultat.getMessageErreur());
            }
            
        } catch (Exception e) {
            System.out.println("   ✗ Erreur: " + e.getMessage());
        }
    }

    /**
     * Affiche les voix disponibles.
     */
    public static void afficherVoixDisponibles() {
        System.out.println("=== Voix françaises disponibles ===");
        
        String[] voix = GoogleTTSService.getVoixDisponibles();
        for (int i = 0; i < voix.length; i++) {
            System.out.println((i + 1) + ". " + voix[i]);
        }
    }

    /**
     * Test de validation des paramètres.
     */
    public static void testValidation() {
        System.out.println("=== Test de validation des paramètres ===");
        
        ParametresTTS parametres = new ParametresTTS();
        
        // Test des limites de vitesse
        try {
            parametres.setVitesse(5.0); // Trop rapide
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Validation vitesse: " + e.getMessage());
        }
        
        // Test des limites de pitch
        try {
            parametres.setPitch(25.0); // Trop aigu
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Validation pitch: " + e.getMessage());
        }
        
        // Test des limites de volume
        try {
            parametres.setVolumeGain(20.0); // Trop fort
        } catch (IllegalArgumentException e) {
            System.out.println("✓ Validation volume: " + e.getMessage());
        }
    }
}