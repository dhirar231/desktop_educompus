package com.educompus.debug;

import com.educompus.app.AppState;

/**
 * Outil de diagnostic pour le lecteur vidéo.
 * Aide à identifier les problèmes de chargement FXML en temps réel.
 */
public final class VideoPlayerDiagnostic {
    
    private VideoPlayerDiagnostic() {}
    
    /**
     * Affiche un rapport de diagnostic complet sur l'état du lecteur vidéo.
     */
    public static void afficherRapportDiagnostic(Object controller, String contexte) {
        System.out.println("\n🔍 DIAGNOSTIC LECTEUR VIDÉO - " + contexte);
        System.out.println("═══════════════════════════════════════════════════════════");
        
        // Informations utilisateur
        System.out.println("👤 UTILISATEUR:");
        System.out.println("   Rôle: " + AppState.getRole());
        System.out.println("   ID: " + AppState.getUserId());
        System.out.println("   Email: " + AppState.getUserEmail());
        System.out.println("   Admin: " + AppState.isAdmin());
        System.out.println("   Teacher: " + AppState.isTeacher());
        
        // Informations contrôleur
        System.out.println("\n🎮 CONTRÔLEUR:");
        System.out.println("   Type: " + (controller != null ? controller.getClass().getSimpleName() : "NULL"));
        System.out.println("   Instance: " + (controller != null ? "✅ OK" : "❌ NULL"));
        
        if (controller != null) {
            // Vérifier les composants FXML via réflexion
            verifierComposantsFXML(controller);
        }
        
        System.out.println("\n📋 RECOMMANDATIONS:");
        if (controller == null) {
            System.out.println("   ❌ Contrôleur null - vérifier le chargement FXML");
        } else {
            System.out.println("   ✅ Contrôleur initialisé correctement");
        }
        
        System.out.println("═══════════════════════════════════════════════════════════\n");
    }
    
    /**
     * Vérifie l'état des composants FXML du lecteur vidéo.
     */
    private static void verifierComposantsFXML(Object controller) {
        System.out.println("\n📺 COMPOSANTS VIDÉO:");
        
        try {
            Class<?> clazz = controller.getClass();
            
            // Liste des composants à vérifier
            String[] composants = {
                "videoModal", "mediaView", "playPauseBtn", "closeVideoBtn", 
                "videoTitle", "volumeSlider", "timeLabel", "progressSlider", "fullscreenBtn"
            };
            
            int initialises = 0;
            int total = composants.length;
            
            for (String nomComposant : composants) {
                try {
                    java.lang.reflect.Field field = clazz.getDeclaredField(nomComposant);
                    field.setAccessible(true);
                    Object valeur = field.get(controller);
                    boolean ok = valeur != null;
                    
                    System.out.println("   " + nomComposant + ": " + (ok ? "✅ OK" : "❌ NULL"));
                    if (ok) initialises++;
                    
                } catch (NoSuchFieldException e) {
                    System.out.println("   " + nomComposant + ": ⚠️ CHAMP INEXISTANT");
                } catch (Exception e) {
                    System.out.println("   " + nomComposant + ": ❌ ERREUR (" + e.getMessage() + ")");
                }
            }
            
            System.out.println("\n📊 RÉSUMÉ: " + initialises + "/" + total + " composants initialisés");
            
            if (initialises == total) {
                System.out.println("🎉 STATUT: Tous les composants sont correctement initialisés");
            } else if (initialises == 0) {
                System.out.println("🚨 STATUT: Aucun composant initialisé - FXML non chargé");
            } else {
                System.out.println("⚠️ STATUT: Chargement FXML partiel - problème de configuration");
            }
            
        } catch (Exception e) {
            System.out.println("   ❌ Erreur lors de la vérification: " + e.getMessage());
        }
    }
    
    /**
     * Affiche un message de diagnostic rapide pour le débogage.
     */
    public static void logDiagnosticRapide(String action, boolean composantsOK) {
        String statut = composantsOK ? "✅ OK" : "❌ FXML NULL";
        System.out.println("🔍 DIAGNOSTIC: " + action + " - Composants: " + statut + 
                          " | Rôle: " + AppState.getRole() + 
                          " | User: " + AppState.getUserId());
    }
    
    /**
     * Vérifie si les composants vidéo sont initialisés.
     */
    public static boolean verifierComposantsInitialises(Object controller) {
        if (controller == null) return false;
        
        try {
            Class<?> clazz = controller.getClass();
            
            // Vérifier les composants critiques
            String[] composantsCritiques = {"videoModal", "mediaView"};
            
            for (String nom : composantsCritiques) {
                java.lang.reflect.Field field = clazz.getDeclaredField(nom);
                field.setAccessible(true);
                Object valeur = field.get(controller);
                if (valeur == null) {
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
}