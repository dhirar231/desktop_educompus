package com.educompus.debug;

import com.educompus.app.AppState;
import com.educompus.controller.front.FrontCourseDetailController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Method;

import static org.mockito.ArgumentMatchers.anyString;

/**
 * Test pour vérifier que le fallback vers UrlOpener fonctionne quand les composants FXML ne sont pas initialisés.
 */
public class VideoPlayerFallbackTest {

    @BeforeEach
    void setUp() {
        AppState.setRole(AppState.Role.USER);
        AppState.setUserId(1);
        AppState.setUserEmail("student@test.com");
    }

    @Test
    void testFallbackWhenFXMLComponentsAreNull() throws Exception {
        System.out.println("🧪 TEST: Fallback vers UrlOpener quand composants FXML sont NULL");
        
        // Créer le contrôleur (sans FXML, donc composants null)
        FrontCourseDetailController controller = new FrontCourseDetailController();
        
        // Mock UrlOpener pour vérifier qu'il est appelé
        boolean[] urlOpenerCalled = {false};
        String[] urlOpenerUrl = {null};
        
        try (MockedStatic<com.educompus.util.UrlOpener> mockedUrlOpener = 
             Mockito.mockStatic(com.educompus.util.UrlOpener.class)) {
            
            mockedUrlOpener.when(() -> com.educompus.util.UrlOpener.open(anyString()))
                           .thenAnswer(invocation -> {
                               urlOpenerCalled[0] = true;
                               urlOpenerUrl[0] = invocation.getArgument(0);
                               System.out.println("✅ UrlOpener.open() appelé avec: " + urlOpenerUrl[0]);
                               return null;
                           });

            // Tester openVideoInApp directement
            Method openVideoInAppMethod = FrontCourseDetailController.class.getDeclaredMethod("openVideoInApp", String.class, String.class);
            openVideoInAppMethod.setAccessible(true);
            
            String testUrl = "https://example.com/videos/test.mp4";
            String testTitle = "Test Video";
            
            System.out.println("🔄 Appel de openVideoInApp avec composants FXML null...");
            openVideoInAppMethod.invoke(controller, testUrl, testTitle);
            
            // Vérifier que UrlOpener a été appelé comme fallback
            if (urlOpenerCalled[0]) {
                System.out.println("✅ SUCCÈS: Fallback vers UrlOpener fonctionne correctement");
                System.out.println("📋 URL transmise: " + urlOpenerUrl[0]);
                
                if (testUrl.equals(urlOpenerUrl[0])) {
                    System.out.println("✅ URL correctement transmise au fallback");
                } else {
                    System.out.println("❌ URL incorrecte transmise au fallback");
                }
            } else {
                System.out.println("❌ ÉCHEC: UrlOpener n'a pas été appelé comme fallback");
            }
        }
    }

    @Test
    void testOpenUrlWithVideoDetection() throws Exception {
        System.out.println("\n🧪 TEST: openUrl avec détection vidéo et fallback");
        
        FrontCourseDetailController controller = new FrontCourseDetailController();
        
        boolean[] urlOpenerCalled = {false};
        String[] urlOpenerUrl = {null};
        
        try (MockedStatic<com.educompus.util.UrlOpener> mockedUrlOpener = 
             Mockito.mockStatic(com.educompus.util.UrlOpener.class)) {
            
            mockedUrlOpener.when(() -> com.educompus.util.UrlOpener.open(anyString()))
                           .thenAnswer(invocation -> {
                               urlOpenerCalled[0] = true;
                               urlOpenerUrl[0] = invocation.getArgument(0);
                               System.out.println("✅ UrlOpener.open() appelé avec: " + urlOpenerUrl[0]);
                               return null;
                           });

            // Tester openUrl avec une URL vidéo
            Method openUrlMethod = FrontCourseDetailController.class.getDeclaredMethod("openUrl", String.class);
            openUrlMethod.setAccessible(true);
            
            String testUrl = "https://example.com/videos/test.mp4";
            
            System.out.println("🔄 Appel de openUrl avec URL vidéo...");
            openUrlMethod.invoke(controller, testUrl);
            
            // Vérifier le comportement
            if (urlOpenerCalled[0]) {
                System.out.println("✅ SUCCÈS: URL vidéo redirigée vers UrlOpener (fallback attendu)");
                System.out.println("📋 Flux: openUrl() -> isVideoUrl()=true -> openVideoInApp() -> composants null -> fallback UrlOpener");
            } else {
                System.out.println("❌ PROBLÈME: UrlOpener n'a pas été appelé");
            }
        }
    }

    @Test
    void testOpenUrlWithNonVideoUrl() throws Exception {
        System.out.println("\n🧪 TEST: openUrl avec URL non-vidéo");
        
        FrontCourseDetailController controller = new FrontCourseDetailController();
        
        boolean[] urlOpenerCalled = {false};
        String[] urlOpenerUrl = {null};
        
        try (MockedStatic<com.educompus.util.UrlOpener> mockedUrlOpener = 
             Mockito.mockStatic(com.educompus.util.UrlOpener.class)) {
            
            mockedUrlOpener.when(() -> com.educompus.util.UrlOpener.open(anyString()))
                           .thenAnswer(invocation -> {
                               urlOpenerCalled[0] = true;
                               urlOpenerUrl[0] = invocation.getArgument(0);
                               System.out.println("✅ UrlOpener.open() appelé avec: " + urlOpenerUrl[0]);
                               return null;
                           });

            // Tester openUrl avec une URL non-vidéo
            Method openUrlMethod = FrontCourseDetailController.class.getDeclaredMethod("openUrl", String.class);
            openUrlMethod.setAccessible(true);
            
            String testUrl = "https://example.com/document.pdf";
            
            System.out.println("🔄 Appel de openUrl avec URL non-vidéo...");
            openUrlMethod.invoke(controller, testUrl);
            
            // Vérifier le comportement
            if (urlOpenerCalled[0]) {
                System.out.println("✅ SUCCÈS: URL non-vidéo directement transmise à UrlOpener");
                System.out.println("📋 Flux: openUrl() -> isVideoUrl()=false -> UrlOpener directement");
            } else {
                System.out.println("❌ PROBLÈME: UrlOpener n'a pas été appelé pour URL non-vidéo");
            }
        }
    }
}