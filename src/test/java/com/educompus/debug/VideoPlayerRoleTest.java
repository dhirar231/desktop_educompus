package com.educompus.debug;

import com.educompus.app.AppState;
import com.educompus.controller.front.FrontCourseDetailController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Test de débogage pour identifier pourquoi le lecteur vidéo ne fonctionne pas côté étudiant.
 */
public class VideoPlayerRoleTest {

    @BeforeEach
    void setUp() {
        // Réinitialiser l'état de l'application
        AppState.setRole(AppState.Role.USER);
        AppState.setUserId(1);
        AppState.setUserEmail("student@test.com");
    }

    @Test
    void testVideoPlayerAsStudent() throws Exception {
        System.out.println("🧪 TEST: Lecteur vidéo en tant qu'ÉTUDIANT");
        
        // Configurer comme étudiant
        AppState.setRole(AppState.Role.USER);
        System.out.println("👤 Rôle: " + AppState.getRole());
        System.out.println("🆔 User ID: " + AppState.getUserId());
        System.out.println("📧 Email: " + AppState.getUserEmail());
        System.out.println("🔒 Is Admin: " + AppState.isAdmin());
        System.out.println("👨‍🏫 Is Teacher: " + AppState.isTeacher());
        
        // Créer le contrôleur
        FrontCourseDetailController controller = new FrontCourseDetailController();
        
        // Vérifier l'état des composants FXML
        checkFXMLComponents(controller, "ÉTUDIANT");
        
        // Tester la détection d'URL vidéo
        testVideoUrlDetection(controller);
        
        // Tester l'appel de openUrl
        testOpenUrl(controller);
    }

    @Test
    void testVideoPlayerAsAdmin() throws Exception {
        System.out.println("\n🧪 TEST: Lecteur vidéo en tant qu'ADMIN");
        
        // Configurer comme admin
        AppState.setRole(AppState.Role.ADMIN);
        System.out.println("👤 Rôle: " + AppState.getRole());
        System.out.println("🆔 User ID: " + AppState.getUserId());
        System.out.println("📧 Email: " + AppState.getUserEmail());
        System.out.println("🔒 Is Admin: " + AppState.isAdmin());
        System.out.println("👨‍🏫 Is Teacher: " + AppState.isTeacher());
        
        // Créer le contrôleur
        FrontCourseDetailController controller = new FrontCourseDetailController();
        
        // Vérifier l'état des composants FXML
        checkFXMLComponents(controller, "ADMIN");
        
        // Tester la détection d'URL vidéo
        testVideoUrlDetection(controller);
        
        // Tester l'appel de openUrl
        testOpenUrl(controller);
    }

    @Test
    void testVideoPlayerAsTeacher() throws Exception {
        System.out.println("\n🧪 TEST: Lecteur vidéo en tant qu'TEACHER");
        
        // Configurer comme teacher
        AppState.setRole(AppState.Role.TEACHER);
        System.out.println("👤 Rôle: " + AppState.getRole());
        System.out.println("🆔 User ID: " + AppState.getUserId());
        System.out.println("📧 Email: " + AppState.getUserEmail());
        System.out.println("🔒 Is Admin: " + AppState.isAdmin());
        System.out.println("👨‍🏫 Is Teacher: " + AppState.isTeacher());
        
        // Créer le contrôleur
        FrontCourseDetailController controller = new FrontCourseDetailController();
        
        // Vérifier l'état des composants FXML
        checkFXMLComponents(controller, "TEACHER");
        
        // Tester la détection d'URL vidéo
        testVideoUrlDetection(controller);
        
        // Tester l'appel de openUrl
        testOpenUrl(controller);
    }

    private void checkFXMLComponents(FrontCourseDetailController controller, String role) throws Exception {
        System.out.println("🔍 Vérification des composants FXML pour " + role + ":");
        
        // Vérifier videoModal
        Field videoModalField = FrontCourseDetailController.class.getDeclaredField("videoModal");
        videoModalField.setAccessible(true);
        Object videoModal = videoModalField.get(controller);
        System.out.println("📺 videoModal: " + (videoModal != null ? "✅ Initialisé" : "❌ NULL"));
        
        // Vérifier mediaView
        Field mediaViewField = FrontCourseDetailController.class.getDeclaredField("mediaView");
        mediaViewField.setAccessible(true);
        Object mediaView = mediaViewField.get(controller);
        System.out.println("🎬 mediaView: " + (mediaView != null ? "✅ Initialisé" : "❌ NULL"));
        
        // Vérifier playPauseBtn
        Field playPauseBtnField = FrontCourseDetailController.class.getDeclaredField("playPauseBtn");
        playPauseBtnField.setAccessible(true);
        Object playPauseBtn = playPauseBtnField.get(controller);
        System.out.println("⏯️ playPauseBtn: " + (playPauseBtn != null ? "✅ Initialisé" : "❌ NULL"));
        
        // Vérifier closeVideoBtn
        Field closeVideoBtnField = FrontCourseDetailController.class.getDeclaredField("closeVideoBtn");
        closeVideoBtnField.setAccessible(true);
        Object closeVideoBtn = closeVideoBtnField.get(controller);
        System.out.println("❌ closeVideoBtn: " + (closeVideoBtn != null ? "✅ Initialisé" : "❌ NULL"));
    }

    private void testVideoUrlDetection(FrontCourseDetailController controller) throws Exception {
        Method isVideoUrlMethod = FrontCourseDetailController.class.getDeclaredMethod("isVideoUrl", String.class);
        isVideoUrlMethod.setAccessible(true);
        
        String[] testUrls = {
            "https://example.com/videos/test.mp4",
            "https://player.vimeo.com/video/123456?autoplay=1",
            "https://storage.googleapis.com/heygen-video/test-video.mp4"
        };
        
        for (String url : testUrls) {
            boolean isVideo = (Boolean) isVideoUrlMethod.invoke(controller, url);
            System.out.println("🎬 URL: " + url + " -> Détectée comme vidéo: " + isVideo);
        }
    }

    private void testOpenUrl(FrontCourseDetailController controller) throws Exception {
        Method openUrlMethod = FrontCourseDetailController.class.getDeclaredMethod("openUrl", String.class);
        openUrlMethod.setAccessible(true);
        
        System.out.println("🔄 Test d'ouverture d'URL vidéo...");
        try {
            openUrlMethod.invoke(controller, "https://example.com/videos/test.mp4");
            System.out.println("✅ openUrl() exécuté sans erreur");
        } catch (Exception e) {
            System.out.println("❌ Erreur dans openUrl(): " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("🔍 Cause: " + e.getCause().getMessage());
                e.getCause().printStackTrace();
            }
        }
    }
}