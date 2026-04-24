package com.educompus.controller.front;

import com.educompus.model.VideoExplicative;
import org.junit.jupiter.api.Test;
import javafx.scene.layout.HBox;
import javafx.scene.control.Button;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test pour vérifier que buildVideoRow utilise la logique de détection vidéo.
 */
public class BuildVideoRowTest {

    @Test
    void testBuildVideoRowMethodExists() throws Exception {
        // Vérifier que la méthode buildVideoRow existe
        Method buildVideoRowMethod = FrontCourseDetailController.class.getDeclaredMethod(
            "buildVideoRow", VideoExplicative.class);
        
        assertNotNull(buildVideoRowMethod);
        assertEquals(1, buildVideoRowMethod.getParameterCount());
        assertEquals(VideoExplicative.class, buildVideoRowMethod.getParameterTypes()[0]);
        assertEquals(HBox.class, buildVideoRowMethod.getReturnType());
    }
    
    @Test
    void testBuildVideoRowCreatesButton() throws Exception {
        // Créer une instance du contrôleur
        FrontCourseDetailController controller = new FrontCourseDetailController();
        
        // Créer une vidéo de test
        VideoExplicative video = new VideoExplicative();
        video.setTitre("Test Video");
        video.setDescription("Description de test");
        video.setUrlVideo("https://example.com/video.mp4");
        
        // Accéder à la méthode privée buildVideoRow
        Method buildVideoRowMethod = FrontCourseDetailController.class.getDeclaredMethod(
            "buildVideoRow", VideoExplicative.class);
        buildVideoRowMethod.setAccessible(true);
        
        // Appeler la méthode
        HBox result = (HBox) buildVideoRowMethod.invoke(controller, video);
        
        // Vérifier que le résultat n'est pas null
        assertNotNull(result);
        
        // Vérifier qu'il y a des enfants (icône, info, bouton)
        assertTrue(result.getChildren().size() >= 3);
        
        // Vérifier qu'il y a un bouton "▶ Regarder"
        Button watchButton = result.getChildren().stream()
            .filter(node -> node instanceof Button)
            .map(node -> (Button) node)
            .filter(button -> "▶ Regarder".equals(button.getText()))
            .findFirst()
            .orElse(null);
            
        assertNotNull(watchButton, "Le bouton '▶ Regarder' doit être présent");
        assertFalse(watchButton.isDisabled(), "Le bouton ne doit pas être désactivé pour une URL valide");
    }
    
    @Test
    void testBuildVideoRowWithEmptyUrl() throws Exception {
        // Créer une instance du contrôleur
        FrontCourseDetailController controller = new FrontCourseDetailController();
        
        // Créer une vidéo sans URL
        VideoExplicative video = new VideoExplicative();
        video.setTitre("Test Video Sans URL");
        video.setDescription("Description de test");
        video.setUrlVideo(""); // URL vide
        
        // Accéder à la méthode privée buildVideoRow
        Method buildVideoRowMethod = FrontCourseDetailController.class.getDeclaredMethod(
            "buildVideoRow", VideoExplicative.class);
        buildVideoRowMethod.setAccessible(true);
        
        // Appeler la méthode
        HBox result = (HBox) buildVideoRowMethod.invoke(controller, video);
        
        // Vérifier que le résultat n'est pas null
        assertNotNull(result);
        
        // Vérifier qu'il y a un bouton "▶ Regarder" mais qu'il est désactivé
        Button watchButton = result.getChildren().stream()
            .filter(node -> node instanceof Button)
            .map(node -> (Button) node)
            .filter(button -> "▶ Regarder".equals(button.getText()))
            .findFirst()
            .orElse(null);
            
        assertNotNull(watchButton, "Le bouton '▶ Regarder' doit être présent");
        assertTrue(watchButton.isDisabled(), "Le bouton doit être désactivé pour une URL vide");
    }
}