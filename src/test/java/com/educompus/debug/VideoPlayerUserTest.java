package com.educompus.debug;

import com.educompus.app.AppState;
import com.educompus.controller.front.FrontCourseDetailController;
import com.educompus.model.Cours;
import com.educompus.nav.Navigator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Test utilisateur pour reproduire le problème de lecteur vidéo.
 * 
 * INSTRUCTIONS POUR L'UTILISATEUR:
 * 1. Exécuter ce test
 * 2. Cliquer sur "▶ Regarder" pour une vidéo
 * 3. Observer les messages de diagnostic dans la console
 * 4. Rapporter si la vidéo s'ouvre dans le lecteur intégré ou le navigateur externe
 */
public class VideoPlayerUserTest extends Application {

    public static void main(String[] args) {
        System.out.println("🧪 DÉMARRAGE TEST UTILISATEUR - Lecteur Vidéo");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("📋 INSTRUCTIONS:");
        System.out.println("   1. Une fenêtre va s'ouvrir avec les détails d'un cours");
        System.out.println("   2. Cherchez une vidéo et cliquez sur '▶ Regarder'");
        System.out.println("   3. Observez les messages de diagnostic dans cette console");
        System.out.println("   4. Notez si la vidéo s'ouvre dans le lecteur intégré ou le navigateur");
        System.out.println("═══════════════════════════════════════════════════════════\n");
        
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("🚀 Initialisation de l'application de test...");
        
        // Configurer comme étudiant par défaut
        AppState.setRole(AppState.Role.USER);
        AppState.setUserId(1);
        AppState.setUserEmail("student@test.com");
        AppState.setUserDisplayName("Étudiant Test");
        
        System.out.println("👤 Configuration utilisateur:");
        System.out.println("   Rôle: " + AppState.getRole());
        System.out.println("   ID: " + AppState.getUserId());
        System.out.println("   Email: " + AppState.getUserEmail());
        
        // Initialiser Navigator
        Navigator.init(primaryStage, "styles/educompus.css");
        
        try {
            System.out.println("\n🔄 Chargement de FrontCourseDetail.fxml...");
            
            // Charger le FXML comme dans l'application réelle
            FXMLLoader loader = Navigator.loader("View/front/FrontCourseDetail.fxml");
            Parent root = loader.load();
            FrontCourseDetailController controller = loader.getController();
            
            System.out.println("✅ FXML chargé avec succès");
            
            if (controller != null) {
                // Créer un cours de test avec des vidéos
                Cours coursTest = creerCoursTest();
                controller.setCours(coursTest);
                
                System.out.println("✅ Cours de test configuré");
                System.out.println("📋 Le cours contient des vidéos de test pour tester le lecteur");
            }
            
            // Créer et afficher la scène
            Scene scene = new Scene(root, 1200, 800);
            primaryStage.setTitle("Test Lecteur Vidéo - EduCompus");
            primaryStage.setScene(scene);
            primaryStage.show();
            
            System.out.println("🎬 Interface utilisateur affichée");
            System.out.println("\n📋 PRÊT POUR LE TEST:");
            System.out.println("   - Cherchez une section 'Vidéos explicatives'");
            System.out.println("   - Cliquez sur '▶ Regarder' pour une vidéo");
            System.out.println("   - Observez les messages de diagnostic ci-dessous");
            System.out.println("═══════════════════════════════════════════════════════════");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement: " + e.getMessage());
            e.printStackTrace();
            
            // Afficher une fenêtre d'erreur simple
            Platform.runLater(() -> {
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Erreur de chargement");
                alert.setHeaderText("Impossible de charger l'interface");
                alert.setContentText("Erreur: " + e.getMessage());
                alert.showAndWait();
                Platform.exit();
            });
        }
    }

    /**
     * Crée un cours de test avec des vidéos pour tester le lecteur.
     */
    private Cours creerCoursTest() {
        Cours cours = new Cours();
        cours.setId(999);
        cours.setTitre("Cours de Test - Lecteur Vidéo");
        cours.setDescription("Ce cours de test contient des vidéos pour tester le lecteur vidéo intégré. " +
                           "Cliquez sur '▶ Regarder' pour tester le lecteur.");
        cours.setNiveau("Test");
        cours.setDomaine("Informatique");
        cours.setNomFormateur("Assistant Test");
        cours.setDureeTotaleHeures(2);
        cours.setChapitreCount(1);
        
        return cours;
    }

    @Override
    public void stop() throws Exception {
        System.out.println("\n🛑 Arrêt de l'application de test");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("📋 RÉSUMÉ DU TEST:");
        System.out.println("   - Vérifiez les messages de diagnostic ci-dessus");
        System.out.println("   - Si vous voyez 'COMPOSANTS FXML NON INITIALISÉS', le problème est confirmé");
        System.out.println("   - Si tous les composants sont 'OK', le problème est ailleurs");
        System.out.println("═══════════════════════════════════════════════════════════");
        super.stop();
    }
}