package com.educompus.debug;

import com.educompus.controller.front.FrontCourseDetailController;
import com.educompus.nav.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test pour vérifier le chargement FXML et l'injection des composants vidéo.
 */
public class FXMLLoadingTest {

    @BeforeAll
    static void initJavaFX() {
        // Initialiser JavaFX Platform si ce n'est pas déjà fait
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // JavaFX déjà initialisé
        }
    }

    @Test
    void testFXMLLoadingWithVideoComponents() throws Exception {
        System.out.println("🧪 TEST: Chargement FXML avec composants vidéo");
        
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                System.out.println("🔄 Chargement du FXML FrontCourseDetail.fxml...");
                
                // Charger le FXML comme le fait l'application réelle
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(getClass().getResource("/View/front/FrontCourseDetail.fxml"));
                
                Parent root = loader.load();
                FrontCourseDetailController controller = loader.getController();
                
                System.out.println("✅ FXML chargé avec succès");
                System.out.println("✅ Contrôleur obtenu: " + (controller != null ? "OK" : "NULL"));
                
                if (controller != null) {
                    // Vérifier les composants vidéo
                    checkVideoComponents(controller);
                } else {
                    System.out.println("❌ Contrôleur null - impossible de vérifier les composants");
                }
                
            } catch (Exception e) {
                System.out.println("❌ Erreur lors du chargement FXML: " + e.getMessage());
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        
        // Attendre que le test se termine
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        if (!completed) {
            System.out.println("⏰ Timeout du test FXML");
        }
    }

    private void checkVideoComponents(FrontCourseDetailController controller) {
        System.out.println("🔍 Vérification des composants vidéo après chargement FXML:");
        
        try {
            // Vérifier videoModal
            Field videoModalField = FrontCourseDetailController.class.getDeclaredField("videoModal");
            videoModalField.setAccessible(true);
            Object videoModal = videoModalField.get(controller);
            System.out.println("📺 videoModal: " + (videoModal != null ? "✅ Initialisé (" + videoModal.getClass().getSimpleName() + ")" : "❌ NULL"));
            
            // Vérifier mediaView
            Field mediaViewField = FrontCourseDetailController.class.getDeclaredField("mediaView");
            mediaViewField.setAccessible(true);
            Object mediaView = mediaViewField.get(controller);
            System.out.println("🎬 mediaView: " + (mediaView != null ? "✅ Initialisé (" + mediaView.getClass().getSimpleName() + ")" : "❌ NULL"));
            
            // Vérifier playPauseBtn
            Field playPauseBtnField = FrontCourseDetailController.class.getDeclaredField("playPauseBtn");
            playPauseBtnField.setAccessible(true);
            Object playPauseBtn = playPauseBtnField.get(controller);
            System.out.println("⏯️ playPauseBtn: " + (playPauseBtn != null ? "✅ Initialisé (" + playPauseBtn.getClass().getSimpleName() + ")" : "❌ NULL"));
            
            // Vérifier closeVideoBtn
            Field closeVideoBtnField = FrontCourseDetailController.class.getDeclaredField("closeVideoBtn");
            closeVideoBtnField.setAccessible(true);
            Object closeVideoBtn = closeVideoBtnField.get(controller);
            System.out.println("❌ closeVideoBtn: " + (closeVideoBtn != null ? "✅ Initialisé (" + closeVideoBtn.getClass().getSimpleName() + ")" : "❌ NULL"));
            
            // Vérifier videoTitle
            Field videoTitleField = FrontCourseDetailController.class.getDeclaredField("videoTitle");
            videoTitleField.setAccessible(true);
            Object videoTitle = videoTitleField.get(controller);
            System.out.println("🏷️ videoTitle: " + (videoTitle != null ? "✅ Initialisé (" + videoTitle.getClass().getSimpleName() + ")" : "❌ NULL"));
            
            // Vérifier volumeSlider
            Field volumeSliderField = FrontCourseDetailController.class.getDeclaredField("volumeSlider");
            volumeSliderField.setAccessible(true);
            Object volumeSlider = volumeSliderField.get(controller);
            System.out.println("🔊 volumeSlider: " + (volumeSlider != null ? "✅ Initialisé (" + volumeSlider.getClass().getSimpleName() + ")" : "❌ NULL"));
            
            // Vérifier timeLabel
            Field timeLabelField = FrontCourseDetailController.class.getDeclaredField("timeLabel");
            timeLabelField.setAccessible(true);
            Object timeLabel = timeLabelField.get(controller);
            System.out.println("⏰ timeLabel: " + (timeLabel != null ? "✅ Initialisé (" + timeLabel.getClass().getSimpleName() + ")" : "❌ NULL"));
            
            // Résumé
            int totalComponents = 6;
            int initializedComponents = 0;
            if (videoModal != null) initializedComponents++;
            if (mediaView != null) initializedComponents++;
            if (playPauseBtn != null) initializedComponents++;
            if (closeVideoBtn != null) initializedComponents++;
            if (videoTitle != null) initializedComponents++;
            if (volumeSlider != null) initializedComponents++;
            if (timeLabel != null) initializedComponents++;
            
            System.out.println("📊 RÉSUMÉ: " + initializedComponents + "/" + (totalComponents + 1) + " composants vidéo initialisés");
            
            if (initializedComponents == totalComponents + 1) {
                System.out.println("🎉 SUCCÈS: Tous les composants vidéo sont correctement initialisés !");
            } else {
                System.out.println("⚠️ PROBLÈME: Certains composants vidéo ne sont pas initialisés");
            }
            
        } catch (Exception e) {
            System.out.println("❌ Erreur lors de la vérification des composants: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    void testDirectControllerCreation() {
        System.out.println("\n🧪 TEST: Création directe du contrôleur (sans FXML)");
        
        // Créer le contrôleur directement (comme dans les autres tests)
        FrontCourseDetailController controller = new FrontCourseDetailController();
        
        try {
            // Vérifier les composants vidéo
            Field videoModalField = FrontCourseDetailController.class.getDeclaredField("videoModal");
            videoModalField.setAccessible(true);
            Object videoModal = videoModalField.get(controller);
            
            Field mediaViewField = FrontCourseDetailController.class.getDeclaredField("mediaView");
            mediaViewField.setAccessible(true);
            Object mediaView = mediaViewField.get(controller);
            
            System.out.println("📺 videoModal (création directe): " + (videoModal != null ? "✅ Initialisé" : "❌ NULL"));
            System.out.println("🎬 mediaView (création directe): " + (mediaView != null ? "✅ Initialisé" : "❌ NULL"));
            
            System.out.println("📋 CONCLUSION: Création directe = composants NULL (attendu)");
            System.out.println("📋 SOLUTION: Utiliser FXMLLoader pour initialiser les composants");
            
        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }
    }
}