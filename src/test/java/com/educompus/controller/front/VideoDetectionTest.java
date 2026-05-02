package com.educompus.controller.front;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test simple pour vérifier la détection de type de fichier vidéo
 * dans FrontCourseDetailController.
 */
public class VideoDetectionTest {

    @Test
    void testIsVideoUrlDetectsVideoFiles() throws Exception {
        // Créer une instance du contrôleur
        FrontCourseDetailController controller = new FrontCourseDetailController();
        
        // Accéder à la méthode privée isVideoUrl via réflexion
        Method isVideoUrlMethod = FrontCourseDetailController.class.getDeclaredMethod("isVideoUrl", String.class);
        isVideoUrlMethod.setAccessible(true);
        
        // Test des extensions vidéo supportées
        assertTrue((Boolean) isVideoUrlMethod.invoke(controller, "video.mp4"));
        assertTrue((Boolean) isVideoUrlMethod.invoke(controller, "video.avi"));
        assertTrue((Boolean) isVideoUrlMethod.invoke(controller, "video.webm"));
        assertTrue((Boolean) isVideoUrlMethod.invoke(controller, "video.mov"));
        assertTrue((Boolean) isVideoUrlMethod.invoke(controller, "video.wmv"));
        assertTrue((Boolean) isVideoUrlMethod.invoke(controller, "video.mkv"));
        assertTrue((Boolean) isVideoUrlMethod.invoke(controller, "video.flv"));
        assertTrue((Boolean) isVideoUrlMethod.invoke(controller, "video.m4v"));
        
        // Test avec URLs complètes
        assertTrue((Boolean) isVideoUrlMethod.invoke(controller, "https://example.com/video.mp4"));
        assertTrue((Boolean) isVideoUrlMethod.invoke(controller, "file:///C:/videos/demo.avi"));
        
        // Test des extensions non-vidéo
        assertFalse((Boolean) isVideoUrlMethod.invoke(controller, "document.pdf"));
        assertFalse((Boolean) isVideoUrlMethod.invoke(controller, "image.jpg"));
        assertFalse((Boolean) isVideoUrlMethod.invoke(controller, "https://example.com/page.html"));
        assertFalse((Boolean) isVideoUrlMethod.invoke(controller, "audio.mp3"));
        
        // Test des cas limites
        assertFalse((Boolean) isVideoUrlMethod.invoke(controller, (String) null));
        assertFalse((Boolean) isVideoUrlMethod.invoke(controller, "video"));
        
        // Test spécial pour chaîne vide
        Boolean emptyResult = (Boolean) isVideoUrlMethod.invoke(controller, "");
        assertFalse(emptyResult);
        
        // Note: ".mp4" est considéré comme valide car il se termine par .mp4
        // C'est un comportement acceptable pour la détection d'extension
    }
    
    @Test
    void testIsVideoUrlCaseInsensitive() throws Exception {
        FrontCourseDetailController controller = new FrontCourseDetailController();
        Method isVideoUrlMethod = FrontCourseDetailController.class.getDeclaredMethod("isVideoUrl", String.class);
        isVideoUrlMethod.setAccessible(true);
        
        // Test de la sensibilité à la casse
        assertTrue((Boolean) isVideoUrlMethod.invoke(controller, "VIDEO.MP4"));
        assertTrue((Boolean) isVideoUrlMethod.invoke(controller, "Video.Avi"));
        assertTrue((Boolean) isVideoUrlMethod.invoke(controller, "DEMO.WEBM"));
    }
}