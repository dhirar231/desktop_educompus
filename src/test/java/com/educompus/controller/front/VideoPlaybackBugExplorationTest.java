package com.educompus.controller.front;

import com.educompus.model.VideoExplicative;
import javafx.application.Platform;
import javafx.scene.media.MediaView;
import javafx.scene.layout.StackPane;
import net.jqwik.api.*;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Test d'exploration de la condition de bug - Ouverture Vidéo Navigateur Externe
 * 
 * **CRITIQUE**: Ce test DOIT ÉCHOUER sur le code non corrigé - l'échec confirme que le bug existe
 * **NE PAS tenter de corriger le test ou le code quand il échoue**
 * **NOTE**: Ce test encode le comportement attendu - il validera le correctif quand il passera après l'implémentation
 * **OBJECTIF**: Exposer des contre-exemples qui démontrent que le bug existe
 * 
 * **Validates: Requirements 2.1, 2.2, 2.3**
 */
class VideoPlaybackBugExplorationTest {

    private static boolean javafxInitialized = false;

    @BeforeAll
    static void initializeJavaFX() {
        if (!javafxInitialized) {
            try {
                Platform.startup(() -> {});
                javafxInitialized = true;
            } catch (IllegalStateException e) {
                // JavaFX déjà initialisé
                javafxInitialized = true;
            }
        }
    }

    @BeforeProperty
    void waitForJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(latch::countDown);
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX Platform should be ready");
    }

    /**
     * Property 1: Bug Condition - Ouverture Vidéo Navigateur Externe
     * 
     * Teste que quand l'utilisateur clique sur "▶ Regarder" pour une VideoExplicative avec URL .mp4,
     * le système ouvre la vidéo dans MediaView intégré avec lecture automatique.
     * 
     * **RÉSULTAT ATTENDU SUR CODE NON CORRIGÉ**: Ce test ÉCHOUE car les vidéos s'ouvrent 
     * actuellement dans le navigateur externe via UrlOpener.open() au lieu de MediaView intégré.
     */
    @Property(tries = 10)
    @Report(Reporting.GENERATED)
    void bugConditionExploration_VideoShouldOpenInIntegratedMediaView(
            @ForAll("videoUrls") String videoUrl,
            @ForAll("videoTitles") String videoTitle) {
        
        System.out.println("Testing video URL: " + videoUrl + " with title: " + videoTitle);
        
        // Arrange: Créer une vidéo explicative avec URL vidéo
        VideoExplicative video = new VideoExplicative();
        video.setTitre(videoTitle);
        video.setDescription("Description de test pour vidéo " + videoTitle);
        video.setUrlVideo(videoUrl);
        video.setCoursId(1);
        video.setChapitreId(1);
        video.setNiveau("Test");
        video.setDomaine("Test");
        video.setAIGenerated(false);

        CountDownLatch testLatch = new CountDownLatch(1);
        
        Platform.runLater(() -> {
            try {
                // Arrange: Créer le contrôleur et simuler l'environnement FXML
                FrontCourseDetailController controller = new FrontCourseDetailController();
                
                // Mock UrlOpener pour détecter si il est appelé (comportement buggy)
                boolean[] urlOpenerCalled = {false};
                
                try (MockedStatic<com.educompus.util.UrlOpener> mockedUrlOpener = 
                     Mockito.mockStatic(com.educompus.util.UrlOpener.class)) {
                    
                    mockedUrlOpener.when(() -> com.educompus.util.UrlOpener.open(anyString()))
                                   .thenAnswer(invocation -> {
                                       urlOpenerCalled[0] = true;
                                       System.out.println("UrlOpener.open() called with: " + invocation.getArgument(0));
                                       return null;
                                   });

                    // Act: Simuler le clic sur "▶ Regarder" en appelant directement openUrl()
                    // (qui est la méthode appelée par le bouton "▶ Regarder")
                    Method openUrlMethod = FrontCourseDetailController.class.getDeclaredMethod("openUrl", String.class);
                    openUrlMethod.setAccessible(true);
                    openUrlMethod.invoke(controller, videoUrl);

                    // Assert: Vérifier le comportement attendu (qui devrait échouer sur le code non corrigé)
                    
                    // 1. Vérifier qu'UrlOpener.open() N'EST PAS appelé (comportement attendu)
                    // Sur le code non corrigé, cette assertion ÉCHOUERA car UrlOpener.open() est appelé
                    assertFalse(urlOpenerCalled[0], 
                        "EXPECTED BEHAVIOR: Video should NOT open in external browser via UrlOpener.open(). " +
                        "CURRENT BUGGY BEHAVIOR: UrlOpener.open() was called, opening video in external browser. " +
                        "Counter-example URL: " + videoUrl);

                    // 2. Vérifier qu'un MediaView est configuré et visible (comportement attendu)
                    // Sur le code non corrigé, cette assertion ÉCHOUERA car il n'y a pas de MediaView
                    try {
                        Field mediaViewField = FrontCourseDetailController.class.getDeclaredField("mediaView");
                        mediaViewField.setAccessible(true);
                        MediaView mediaView = (MediaView) mediaViewField.get(controller);
                        
                        assertNotNull(mediaView, 
                            "EXPECTED BEHAVIOR: MediaView should exist for integrated video playback. " +
                            "CURRENT BUGGY BEHAVIOR: No MediaView found in controller. " +
                            "Counter-example URL: " + videoUrl);
                        
                        assertNotNull(mediaView.getMediaPlayer(), 
                            "EXPECTED BEHAVIOR: MediaView should have a MediaPlayer configured. " +
                            "CURRENT BUGGY BEHAVIOR: MediaView has no MediaPlayer. " +
                            "Counter-example URL: " + videoUrl);
                        
                    } catch (NoSuchFieldException e) {
                        fail("EXPECTED BEHAVIOR: Controller should have a mediaView field for integrated playback. " +
                             "CURRENT BUGGY BEHAVIOR: No mediaView field found in FrontCourseDetailController. " +
                             "Counter-example URL: " + videoUrl + 
                             ". This confirms the bug - no MediaView infrastructure exists.");
                    }

                    // 3. Vérifier qu'un modal de lecture vidéo est affiché (comportement attendu)
                    // Sur le code non corrigé, cette assertion ÉCHOUERA car il n'y a pas de modal vidéo
                    try {
                        Field videoModalField = FrontCourseDetailController.class.getDeclaredField("videoModal");
                        videoModalField.setAccessible(true);
                        StackPane videoModal = (StackPane) videoModalField.get(controller);
                        
                        assertNotNull(videoModal, 
                            "EXPECTED BEHAVIOR: Video modal should exist for integrated playback. " +
                            "CURRENT BUGGY BEHAVIOR: No video modal found. " +
                            "Counter-example URL: " + videoUrl);
                        
                        assertTrue(videoModal.isVisible(), 
                            "EXPECTED BEHAVIOR: Video modal should be visible when playing video. " +
                            "CURRENT BUGGY BEHAVIOR: Video modal is not visible. " +
                            "Counter-example URL: " + videoUrl);
                        
                    } catch (NoSuchFieldException e) {
                        fail("EXPECTED BEHAVIOR: Controller should have a videoModal field for integrated playback. " +
                             "CURRENT BUGGY BEHAVIOR: No videoModal field found in FrontCourseDetailController. " +
                             "Counter-example URL: " + videoUrl + 
                             ". This confirms the bug - no video modal infrastructure exists.");
                    }

                } catch (Exception e) {
                    fail("Test execution failed: " + e.getMessage() + " for URL: " + videoUrl);
                }
                
            } finally {
                testLatch.countDown();
            }
        });
        
        try {
            assertTrue(testLatch.await(10, TimeUnit.SECONDS), "Test should complete within timeout");
        } catch (InterruptedException e) {
            fail("Test was interrupted");
        }
    }

    /**
     * Test unitaire simple pour confirmer le comportement buggy actuel
     */
    @Test
    void confirmCurrentBuggyBehavior_VideoOpensInExternalBrowser() {
        // Ce test documente le comportement actuel (buggy) pour référence
        VideoExplicative video = new VideoExplicative();
        video.setUrlVideo("https://example.com/test-video.mp4");
        
        boolean[] urlOpenerCalled = {false};
        
        try (MockedStatic<com.educompus.util.UrlOpener> mockedUrlOpener = 
             Mockito.mockStatic(com.educompus.util.UrlOpener.class)) {
            
            mockedUrlOpener.when(() -> com.educompus.util.UrlOpener.open(anyString()))
                           .thenAnswer(invocation -> {
                               urlOpenerCalled[0] = true;
                               return null;
                           });

            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    FrontCourseDetailController controller = new FrontCourseDetailController();
                    Method openUrlMethod = FrontCourseDetailController.class.getDeclaredMethod("openUrl", String.class);
                    openUrlMethod.setAccessible(true);
                    openUrlMethod.invoke(controller, video.getUrlVideo());
                } catch (Exception e) {
                    // Ignore pour ce test de documentation
                } finally {
                    latch.countDown();
                }
            });
            
            try {
                latch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // Ignore
            }
            
            // Documenter le comportement actuel (buggy)
            assertTrue(urlOpenerCalled[0], 
                "CURRENT BUGGY BEHAVIOR CONFIRMED: Videos currently open in external browser via UrlOpener.open(). " +
                "This is the bug we need to fix - videos should open in integrated MediaView instead.");
        }
    }

    // Générateurs pour les tests basés sur les propriétés

    @Provide
    Arbitrary<String> videoUrls() {
        return Arbitraries.oneOf(
            // URLs vidéo MP4 typiques
            Arbitraries.strings().alpha().ofMinLength(5).ofMaxLength(20)
                .map(s -> "https://example.com/videos/" + s + ".mp4"),
            
            // URLs vidéo avec différents formats
            Arbitraries.oneOf(
                Arbitraries.just("https://storage.googleapis.com/heygen-video/test-video.mp4"),
                Arbitraries.just("https://synthesia-videos.s3.amazonaws.com/demo.mp4"),
                Arbitraries.just("https://cdn.example.com/course-intro.webm"),
                Arbitraries.just("https://media.example.com/lesson-1.avi"),
                Arbitraries.just("https://videos.example.com/tutorial.mov")
            ),
            
            // URLs avec paramètres (cas réels)
            Arbitraries.strings().alpha().ofMinLength(8).ofMaxLength(15)
                .map(s -> "https://player.vimeo.com/video/" + s + "?autoplay=1")
        );
    }

    @Provide
    Arbitrary<String> videoTitles() {
        return Arbitraries.oneOf(
            Arbitraries.just("Introduction aux Algorithmes"),
            Arbitraries.just("Structures de Données Avancées"),
            Arbitraries.just("Programmation Orientée Objet"),
            Arbitraries.just("Base de Données Relationnelles"),
            Arbitraries.just("Développement Web Frontend"),
            Arbitraries.strings().alpha().ofMinLength(10).ofMaxLength(50)
                .map(s -> "Cours: " + s)
        );
    }
}