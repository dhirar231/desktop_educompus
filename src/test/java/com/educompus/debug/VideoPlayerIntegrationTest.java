package com.educompus.debug;

import com.educompus.app.AppState;
import com.educompus.controller.front.FrontCourseDetailController;
import com.educompus.nav.Navigator;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test d'intégration pour vérifier le comportement réel du lecteur vidéo
 * dans différents contextes d'utilisation.
 */
public class VideoPlayerIntegrationTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // JavaFX déjà initialisé
        }
    }

    @Test
    void testVideoPlayerWithProperFXMLLoading() throws Exception {
        System.out.println("🧪 TEST INTÉGRATION: Lecteur vidéo avec chargement FXML complet");
        
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                // Simuler le chargement comme dans l'application réelle
                System.out.println("🔄 Chargement FXML via Navigator.loader()...");
                
                FXMLLoader loader = Navigator.loader("View/front/FrontCourseDetail.fxml");
                Parent root = loader.load();
                FrontCourseDetailController controller = loader.getController();
                
                System.out.println("✅ FXML chargé via Navigator");
                System.out.println("✅ Contrôleur obtenu: " + (controller != null ? "OK" : "NULL"));
                
                if (controller != null) {
                    // Vérifier les composants vidéo
                    boolean allComponentsInitialized = checkVideoComponents(controller, "NAVIGATOR");
                    
                    if (allComponentsInitialized) {
                        // Tester le comportement vidéo complet
                        testVideoPlaybackBehavior(controller);
                    }
                } else {
                    System.out.println("❌ Contrôleur null - impossible de continuer");
                }
                
            } catch (Exception e) {
                System.out.println("❌ Erreur lors du test d'intégration: " + e.getMessage());
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        
        boolean completed = latch.await(15, TimeUnit.SECONDS);
        if (!completed) {
            System.out.println("⏰ Timeout du test d'intégration");
        }
    }

    @Test
    void testVideoPlayerAsStudentVsAdmin() throws Exception {
        System.out.println("\n🧪 TEST COMPARAISON: Étudiant vs Admin avec FXML");
        
        CountDownLatch latch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                // Test en tant qu'étudiant
                System.out.println("\n👤 TEST ÉTUDIANT:");
                AppState.setRole(AppState.Role.USER);
                AppState.setUserId(1);
                AppState.setUserEmail("student@test.com");
                
                FXMLLoader studentLoader = Navigator.loader("View/front/FrontCourseDetail.fxml");
                Parent studentRoot = studentLoader.load();
                FrontCourseDetailController studentController = studentLoader.getController();
                
                boolean studentComponentsOK = checkVideoComponents(studentController, "ÉTUDIANT");
                
                // Test en tant qu'admin
                System.out.println("\n🔒 TEST ADMIN:");
                AppState.setRole(AppState.Role.ADMIN);
                AppState.setUserId(2);
                AppState.setUserEmail("admin@test.com");
                
                FXMLLoader adminLoader = Navigator.loader("View/front/FrontCourseDetail.fxml");
                Parent adminRoot = adminLoader.load();
                FrontCourseDetailController adminController = adminLoader.getController();
                
                boolean adminComponentsOK = checkVideoComponents(adminController, "ADMIN");
                
                // Comparaison
                System.out.println("\n📊 RÉSULTATS COMPARAISON:");
                System.out.println("👤 Étudiant - Composants OK: " + studentComponentsOK);
                System.out.println("🔒 Admin - Composants OK: " + adminComponentsOK);
                
                if (studentComponentsOK && adminComponentsOK) {
                    System.out.println("🎉 CONCLUSION: Aucune différence entre étudiant et admin");
                    System.out.println("📋 Le problème n'est PAS lié au rôle utilisateur");
                } else if (!studentComponentsOK && !adminComponentsOK) {
                    System.out.println("⚠️ CONCLUSION: Problème général de chargement FXML");
                } else {
                    System.out.println("🚨 CONCLUSION: Différence détectée entre rôles!");
                }
                
            } catch (Exception e) {
                System.out.println("❌ Erreur lors du test de comparaison: " + e.getMessage());
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        
        boolean completed = latch.await(15, TimeUnit.SECONDS);
        if (!completed) {
            System.out.println("⏰ Timeout du test de comparaison");
        }
    }

    private boolean checkVideoComponents(FrontCourseDetailController controller, String context) {
        System.out.println("🔍 Vérification composants vidéo (" + context + "):");
        
        try {
            int initializedCount = 0;
            int totalCount = 7;
            
            // Vérifier videoModal
            Field videoModalField = FrontCourseDetailController.class.getDeclaredField("videoModal");
            videoModalField.setAccessible(true);
            Object videoModal = videoModalField.get(controller);
            boolean videoModalOK = videoModal != null;
            System.out.println("📺 videoModal: " + (videoModalOK ? "✅ OK" : "❌ NULL"));
            if (videoModalOK) initializedCount++;
            
            // Vérifier mediaView
            Field mediaViewField = FrontCourseDetailController.class.getDeclaredField("mediaView");
            mediaViewField.setAccessible(true);
            Object mediaView = mediaViewField.get(controller);
            boolean mediaViewOK = mediaView != null;
            System.out.println("🎬 mediaView: " + (mediaViewOK ? "✅ OK" : "❌ NULL"));
            if (mediaViewOK) initializedCount++;
            
            // Vérifier playPauseBtn
            Field playPauseBtnField = FrontCourseDetailController.class.getDeclaredField("playPauseBtn");
            playPauseBtnField.setAccessible(true);
            Object playPauseBtn = playPauseBtnField.get(controller);
            boolean playPauseBtnOK = playPauseBtn != null;
            System.out.println("⏯️ playPauseBtn: " + (playPauseBtnOK ? "✅ OK" : "❌ NULL"));
            if (playPauseBtnOK) initializedCount++;
            
            // Vérifier closeVideoBtn
            Field closeVideoBtnField = FrontCourseDetailController.class.getDeclaredField("closeVideoBtn");
            closeVideoBtnField.setAccessible(true);
            Object closeVideoBtn = closeVideoBtnField.get(controller);
            boolean closeVideoBtnOK = closeVideoBtn != null;
            System.out.println("❌ closeVideoBtn: " + (closeVideoBtnOK ? "✅ OK" : "❌ NULL"));
            if (closeVideoBtnOK) initializedCount++;
            
            // Vérifier videoTitle
            Field videoTitleField = FrontCourseDetailController.class.getDeclaredField("videoTitle");
            videoTitleField.setAccessible(true);
            Object videoTitle = videoTitleField.get(controller);
            boolean videoTitleOK = videoTitle != null;
            System.out.println("🏷️ videoTitle: " + (videoTitleOK ? "✅ OK" : "❌ NULL"));
            if (videoTitleOK) initializedCount++;
            
            // Vérifier volumeSlider
            Field volumeSliderField = FrontCourseDetailController.class.getDeclaredField("volumeSlider");
            volumeSliderField.setAccessible(true);
            Object volumeSlider = volumeSliderField.get(controller);
            boolean volumeSliderOK = volumeSlider != null;
            System.out.println("🔊 volumeSlider: " + (volumeSliderOK ? "✅ OK" : "❌ NULL"));
            if (volumeSliderOK) initializedCount++;
            
            // Vérifier timeLabel
            Field timeLabelField = FrontCourseDetailController.class.getDeclaredField("timeLabel");
            timeLabelField.setAccessible(true);
            Object timeLabel = timeLabelField.get(controller);
            boolean timeLabelOK = timeLabel != null;
            System.out.println("⏰ timeLabel: " + (timeLabelOK ? "✅ OK" : "❌ NULL"));
            if (timeLabelOK) initializedCount++;
            
            System.out.println("📊 TOTAL: " + initializedCount + "/" + totalCount + " composants initialisés");
            
            return initializedCount == totalCount;
            
        } catch (Exception e) {
            System.out.println("❌ Erreur lors de la vérification: " + e.getMessage());
            return false;
        }
    }

    private void testVideoPlaybackBehavior(FrontCourseDetailController controller) {
        System.out.println("\n🎬 Test du comportement de lecture vidéo:");
        
        try {
            // Tester isVideoUrl
            Method isVideoUrlMethod = FrontCourseDetailController.class.getDeclaredMethod("isVideoUrl", String.class);
            isVideoUrlMethod.setAccessible(true);
            
            String testUrl = "https://example.com/test.mp4";
            boolean isDetected = (Boolean) isVideoUrlMethod.invoke(controller, testUrl);
            System.out.println("🔍 Détection URL vidéo: " + (isDetected ? "✅ OK" : "❌ ÉCHEC"));
            
            // Tester openVideoInApp (sans vraiment ouvrir de vidéo)
            Method openVideoInAppMethod = FrontCourseDetailController.class.getDeclaredMethod("openVideoInApp", String.class, String.class);
            openVideoInAppMethod.setAccessible(true);
            
            System.out.println("🎬 Test openVideoInApp...");
            try {
                openVideoInAppMethod.invoke(controller, testUrl, "Test Video");
                System.out.println("✅ openVideoInApp exécuté sans erreur");
            } catch (Exception e) {
                System.out.println("⚠️ openVideoInApp a généré une exception (normal sans vraie vidéo): " + e.getCause().getClass().getSimpleName());
            }
            
        } catch (Exception e) {
            System.out.println("❌ Erreur lors du test de comportement: " + e.getMessage());
        }
    }
}