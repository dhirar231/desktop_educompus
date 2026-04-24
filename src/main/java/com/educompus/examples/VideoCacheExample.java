package com.educompus.examples;

import com.educompus.service.VideoCache;

import java.util.concurrent.CompletableFuture;

/**
 * Exemple d'utilisation du cache intelligent pour les vidéos IA.
 * Démontre comment éviter les régénérations inutiles.
 */
public class VideoCacheExample {

    public static void main(String[] args) {
        System.out.println("💾 DÉMONSTRATION DU CACHE INTELLIGENT");
        System.out.println("=====================================");
        System.out.println("Objectif: Générer UNE fois, réutiliser TOUJOURS");
        System.out.println();

        // Exemple 1: Première génération (lente)
        demonstrationPremiereGeneration();

        // Exemple 2: Réutilisation du cache (rapide)
        demonstrationReutilisationCache();

        // Exemple 3: Différents paramètres (nouvelle génération)
        demonstrationParametresDifferents();

        // Exemple 4: Force régénération
        demonstrationForceRegeneration();

        // Exemple 5: Statistiques du cache
        demonstrationStatistiques();
    }

    /**
     * Première génération - sera lente car nouvelle.
     */
    private static void demonstrationPremiereGeneration() {
        System.out.println("🔄 PREMIÈRE GÉNÉRATION (LENTE)");
        System.out.println("-------------------------------");

        VideoCache cache = new VideoCache();
        
        VideoCache.ParametresCache parametres = new VideoCache.ParametresCache(
            "Introduction aux algorithmes de tri en informatique",
            "MADISON_1",
            "fr-FR-DeniseNeural",
            "medium",
            false
        );

        String hash = parametres.genererHash();
        System.out.println("🔑 Hash généré: " + hash);
        
        long debut = System.currentTimeMillis();
        
        try {
            CompletableFuture<VideoCache.CacheEntry> future = cache.obtenirVideo(parametres);
            VideoCache.CacheEntry entree = future.get();
            
            long duree = System.currentTimeMillis() - debut;
            
            if (entree != null) {
                System.out.println("✅ Vidéo générée avec succès !");
                System.out.println("📹 URL: " + entree.getUrlVideo());
                System.out.println("⏱️ Temps: " + duree + "ms");
                System.out.println("💾 Hash: " + entree.getHash());
            } else {
                System.out.println("❌ Échec de la génération");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * Réutilisation du cache - sera rapide.
     */
    private static void demonstrationReutilisationCache() {
        System.out.println("⚡ RÉUTILISATION DU CACHE (RAPIDE)");
        System.out.println("----------------------------------");

        VideoCache cache = new VideoCache();
        
        // MÊMES paramètres que la première génération
        VideoCache.ParametresCache parametres = new VideoCache.ParametresCache(
            "Introduction aux algorithmes de tri en informatique", // Même contenu
            "MADISON_1", // Même avatar
            "fr-FR-DeniseNeural", // Même voix
            "medium", // Même qualité
            false // Pas de force régénération
        );

        String hash = parametres.genererHash();
        System.out.println("🔑 Hash généré: " + hash + " (identique à avant)");
        
        long debut = System.currentTimeMillis();
        
        try {
            CompletableFuture<VideoCache.CacheEntry> future = cache.obtenirVideo(parametres);
            VideoCache.CacheEntry entree = future.get();
            
            long duree = System.currentTimeMillis() - debut;
            
            if (entree != null) {
                System.out.println("✅ Vidéo obtenue depuis le cache !");
                System.out.println("📹 URL: " + entree.getUrlVideo());
                System.out.println("⚡ Temps: " + duree + "ms (BEAUCOUP plus rapide !)");
                System.out.println("💾 Hash: " + entree.getHash());
                System.out.println("📅 Date création: " + entree.getDateCreation());
            } else {
                System.out.println("❌ Échec de l'obtention depuis le cache");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * Paramètres différents - nouvelle génération nécessaire.
     */
    private static void demonstrationParametresDifferents() {
        System.out.println("🆕 PARAMÈTRES DIFFÉRENTS (NOUVELLE GÉNÉRATION)");
        System.out.println("-----------------------------------------------");

        VideoCache cache = new VideoCache();
        
        // Paramètres DIFFÉRENTS
        VideoCache.ParametresCache parametres = new VideoCache.ParametresCache(
            "Introduction aux algorithmes de tri en informatique", // Même contenu
            "MADISON_2", // Avatar différent !
            "42d00d4aac5441279d8536cd6b52c53c", // Voix différente (Hope)!
            "high", // Qualité différente !
            false
        );

        String hash = parametres.genererHash();
        System.out.println("🔑 Hash généré: " + hash + " (différent car paramètres changés)");
        
        long debut = System.currentTimeMillis();
        
        try {
            CompletableFuture<VideoCache.CacheEntry> future = cache.obtenirVideo(parametres);
            VideoCache.CacheEntry entree = future.get();
            
            long duree = System.currentTimeMillis() - debut;
            
            if (entree != null) {
                System.out.println("✅ Nouvelle vidéo générée (paramètres différents) !");
                System.out.println("📹 URL: " + entree.getUrlVideo());
                System.out.println("⏱️ Temps: " + duree + "ms");
                System.out.println("💾 Hash: " + entree.getHash());
            } else {
                System.out.println("❌ Échec de la génération");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * Force la régénération même si le cache existe.
     */
    private static void demonstrationForceRegeneration() {
        System.out.println("🔄 FORCE RÉGÉNÉRATION");
        System.out.println("----------------------");

        VideoCache cache = new VideoCache();
        
        VideoCache.ParametresCache parametres = new VideoCache.ParametresCache(
            "Introduction aux algorithmes de tri en informatique",
            "MADISON_1",
            "fr-FR-DeniseNeural",
            "medium",
            true // FORCE la régénération !
        );

        String hash = parametres.genererHash();
        System.out.println("🔑 Hash: " + hash);
        System.out.println("🔄 Force régénération activée");
        
        long debut = System.currentTimeMillis();
        
        try {
            CompletableFuture<VideoCache.CacheEntry> future = cache.regenererVideo(parametres);
            VideoCache.CacheEntry entree = future.get();
            
            long duree = System.currentTimeMillis() - debut;
            
            if (entree != null) {
                System.out.println("✅ Vidéo régénérée avec succès !");
                System.out.println("📹 URL: " + entree.getUrlVideo());
                System.out.println("⏱️ Temps: " + duree + "ms");
                System.out.println("🆕 Nouvelle version créée");
            } else {
                System.out.println("❌ Échec de la régénération");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * Affiche les statistiques du cache.
     */
    private static void demonstrationStatistiques() {
        System.out.println("📊 STATISTIQUES DU CACHE");
        System.out.println("-------------------------");

        VideoCache cache = new VideoCache();
        
        try {
            String statistiques = cache.getStatistiques();
            System.out.println(statistiques);
            
            // Nettoyage du cache
            System.out.println("🧹 Nettoyage du cache...");
            cache.nettoyerCache();
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors des statistiques: " + e.getMessage());
        }
        
        System.out.println();
    }

    /**
     * Démontre les avantages du cache.
     */
    public static void demonstrationAvantages() {
        System.out.println("✨ AVANTAGES DU CACHE INTELLIGENT");
        System.out.println("==================================");
        
        System.out.println("🚀 PERFORMANCE:");
        System.out.println("   • Première génération: 30-120 secondes");
        System.out.println("   • Accès cache: < 1 seconde");
        System.out.println("   • Gain: 30x à 120x plus rapide !");
        System.out.println();
        
        System.out.println("💰 ÉCONOMIES:");
        System.out.println("   • Évite les appels API répétés");
        System.out.println("   • Réduit les coûts HeyGen/Gemini");
        System.out.println("   • Optimise l'utilisation des quotas");
        System.out.println();
        
        System.out.println("🛡️ FIABILITÉ:");
        System.out.println("   • Pas de dépendance réseau pour le contenu existant");
        System.out.println("   • Fonctionne même si les APIs sont indisponibles");
        System.out.println("   • Cohérence garantie (même contenu = même vidéo)");
        System.out.println();
        
        System.out.println("🔧 MAINTENANCE:");
        System.out.println("   • Nettoyage automatique des fichiers expirés");
        System.out.println("   • Gestion intelligente de l'espace disque");
        System.out.println("   • Statistiques détaillées");
        System.out.println();
        
        System.out.println("📈 SCALABILITÉ:");
        System.out.println("   • Cache multi-niveaux (mémoire → disque → BD)");
        System.out.println("   • Hash unique pour éviter les doublons");
        System.out.println("   • Support de milliers de vidéos");
    }

    /**
     * Cas d'usage typiques.
     */
    public static void casDusageTypiques() {
        System.out.println("🎯 CAS D'USAGE TYPIQUES");
        System.out.println("========================");
        
        System.out.println("1️⃣ COURS EN LIGNE:");
        System.out.println("   • Même chapitre consulté par 100+ étudiants");
        System.out.println("   • Génération: 1 fois, affichage: 100+ fois");
        System.out.println("   • Économie: 99% des appels API évités");
        System.out.println();
        
        System.out.println("2️⃣ RÉVISIONS:");
        System.out.println("   • Étudiant revoit le même contenu plusieurs fois");
        System.out.println("   • Accès instantané à chaque consultation");
        System.out.println("   • Expérience utilisateur optimale");
        System.out.println();
        
        System.out.println("3️⃣ DÉMONSTRATIONS:");
        System.out.println("   • Présentation du système à des clients");
        System.out.println("   • Pas d'attente, pas de risque d'échec API");
        System.out.println("   • Démonstration fluide et professionnelle");
        System.out.println();
        
        System.out.println("4️⃣ DÉVELOPPEMENT:");
        System.out.println("   • Tests répétés pendant le développement");
        System.out.println("   • Pas de consommation de quotas API");
        System.out.println("   • Cycle de développement accéléré");
    }
}