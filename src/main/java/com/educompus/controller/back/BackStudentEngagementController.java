package com.educompus.controller.back;

import com.educompus.model.Cours;
import com.educompus.model.StudentEngagementRisk;
import com.educompus.model.StudentEngagementRisk.RiskLevel;
import com.educompus.repository.CourseManagementRepository;
import com.educompus.service.StudentEngagementService;
import com.educompus.service.StudentEngagementService.EngagementStats;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Contrôleur pour la détection des étudiants désengagés.
 */
public final class BackStudentEngagementController {
    
    @FXML private ComboBox<Cours> courseCombo;
    @FXML private GridPane statsCardsBox;
    @FXML private VBox studentsListBox;
    @FXML private Label lblTotalStudents;
    @FXML private Label lblActiveStudents;
    @FXML private Label lblAtRiskStudents;
    @FXML private Label lblDisengagedStudents;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label emptyStateLabel;
    
    private final StudentEngagementService engagementService = new StudentEngagementService();
    private final CourseManagementRepository courseRepo = new CourseManagementRepository();
    
    private List<StudentEngagementRisk> currentRisks;
    
    @FXML
    private void initialize() {
        loadCourses();
        
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }
        
        if (emptyStateLabel != null) {
            emptyStateLabel.setVisible(true);
            emptyStateLabel.setText("Sélectionnez un cours pour analyser l'engagement des étudiants");
        }
    }
    
    /**
     * Charge la liste des cours dans le ComboBox.
     */
    private void loadCourses() {
        if (courseCombo == null) return;
        
        List<Cours> courses = courseRepo.listCours("");
        courseCombo.setItems(FXCollections.observableArrayList(courses));
        
        // Afficher le titre du cours
        courseCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Cours cours, boolean empty) {
                super.updateItem(cours, empty);
                setText(empty || cours == null ? "Sélectionner un cours..." : cours.getTitre());
            }
        });
        
        courseCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Cours cours, boolean empty) {
                super.updateItem(cours, empty);
                setText(empty || cours == null ? null : cours.getTitre());
            }
        });
        
        // Listener pour charger les données quand un cours est sélectionné
        courseCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                analyzeEngagement(newVal.getId());
            }
        });
    }
    
    /**
     * Analyse l'engagement des étudiants pour un cours.
     */
    @FXML
    private void analyzeEngagement(int courseId) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(true);
        }
        
        if (emptyStateLabel != null) {
            emptyStateLabel.setVisible(false);
        }
        
        // Exécuter l'analyse dans un thread séparé
        javafx.concurrent.Task<List<StudentEngagementRisk>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<StudentEngagementRisk> call() {
                return engagementService.analyzeStudentEngagement(courseId);
            }
        };
        
        task.setOnSucceeded(e -> {
            currentRisks = task.getValue();
            displayResults(currentRisks, courseId);
            
            if (loadingIndicator != null) {
                loadingIndicator.setVisible(false);
            }
        });
        
        task.setOnFailed(e -> {
            if (loadingIndicator != null) {
                loadingIndicator.setVisible(false);
            }
            
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur lors de l'analyse");
            alert.setContentText("Impossible d'analyser l'engagement des étudiants.");
            alert.showAndWait();
        });
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    /**
     * Affiche les résultats de l'analyse.
     */
    private void displayResults(List<StudentEngagementRisk> risks, int courseId) {
        // Calculer les statistiques
        EngagementStats stats = engagementService.getEngagementStats(courseId);
        displayStats(stats);
        
        // Afficher la liste des étudiants
        displayStudentsList(risks);
    }
    
    /**
     * Affiche les statistiques dans les cartes.
     */
    private void displayStats(EngagementStats stats) {
        if (lblTotalStudents != null) {
            lblTotalStudents.setText(String.valueOf(stats.totalStudents));
        }
        
        if (lblActiveStudents != null) {
            lblActiveStudents.setText(stats.activeStudents + " (" + 
                String.format("%.0f%%", stats.getActivePercentage()) + ")");
        }
        
        if (lblAtRiskStudents != null) {
            lblAtRiskStudents.setText(stats.atRiskStudents + " (" + 
                String.format("%.0f%%", stats.getAtRiskPercentage()) + ")");
        }
        
        if (lblDisengagedStudents != null) {
            lblDisengagedStudents.setText(stats.disengagedStudents + " (" + 
                String.format("%.0f%%", stats.getDisengagedPercentage()) + ")");
        }
    }
    
    /**
     * Affiche la liste des étudiants avec leur niveau de risque.
     */
    private void displayStudentsList(List<StudentEngagementRisk> risks) {
        if (studentsListBox == null) return;
        
        studentsListBox.getChildren().clear();
        
        if (risks.isEmpty()) {
            Label empty = new Label("Aucun étudiant trouvé pour ce cours");
            empty.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");
            studentsListBox.getChildren().add(empty);
            return;
        }
        
        // Trier par score de risque décroissant
        risks.sort((r1, r2) -> Integer.compare(r2.getRiskScore(), r1.getRiskScore()));
        
        for (StudentEngagementRisk risk : risks) {
            studentsListBox.getChildren().add(buildStudentCard(risk));
        }
    }
    
    /**
     * Construit une carte pour un étudiant.
     */
    private VBox buildStudentCard(StudentEngagementRisk risk) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setStyle(card.getStyle() + "-fx-border-left-width: 4px; -fx-border-left-color: " + 
            risk.getRiskLevel().color + ";");
        
        // En-tête avec nom et score
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        // Indicateur de niveau
        Circle indicator = new Circle(8);
        indicator.setFill(javafx.scene.paint.Color.web(risk.getRiskLevel().color));
        
        // Nom de l'étudiant
        VBox nameBox = new VBox(2);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        
        Label nameLabel = new Label(risk.getStudentName());
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        
        Label emailLabel = new Label(risk.getStudentEmail());
        emailLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        
        nameBox.getChildren().addAll(nameLabel, emailLabel);
        
        // Badge de niveau de risque
        Label riskBadge = new Label(risk.getRiskLevel().label);
        riskBadge.setStyle(
            "-fx-background-color: " + risk.getRiskLevel().color + ";" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: 700;" +
            "-fx-padding: 4 10 4 10;" +
            "-fx-background-radius: 12px;"
        );
        
        // Score
        Label scoreLabel = new Label("Score: " + risk.getRiskScore());
        scoreLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: " + 
            risk.getRiskLevel().color + ";");
        
        header.getChildren().addAll(indicator, nameBox, riskBadge, scoreLabel);
        
        // Métriques d'engagement
        HBox metricsBox = new HBox(20);
        metricsBox.setAlignment(Pos.CENTER_LEFT);
        
        metricsBox.getChildren().addAll(
            buildMetric("📅", "Absences", risk.getLiveSessionAbsences() + " sessions"),
            buildMetric("🔌", "Dernière connexion", 
                risk.getDaysSinceLastConnection() == 999 ? "Jamais" : 
                risk.getDaysSinceLastConnection() + " jours"),
            buildMetric("📚", "Chapitres", 
                (risk.getTotalChapters() - risk.getUnopenedChapters()) + "/" + risk.getTotalChapters())
        );
        
        // Raisons du risque
        if (!risk.getRiskReasons().isEmpty()) {
            VBox reasonsBox = new VBox(6);
            reasonsBox.setStyle(
                "-fx-background-color: rgba(231,76,60,0.1);" +
                "-fx-padding: 10;" +
                "-fx-background-radius: 6px;"
            );
            
            Label reasonsTitle = new Label("⚠️ Raisons du risque :");
            reasonsTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #e74c3c;");
            
            reasonsBox.getChildren().add(reasonsTitle);
            
            for (String reason : risk.getRiskReasons()) {
                Label reasonLabel = new Label("• " + reason);
                reasonLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #c0392b;");
                reasonsBox.getChildren().add(reasonLabel);
            }
            
            card.getChildren().add(reasonsBox);
        }
        
        // Boutons d'action
        HBox actionsBox = new HBox(8);
        actionsBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button sendReminderBtn = new Button("📧 Envoyer un rappel");
        sendReminderBtn.getStyleClass().add("btn-rgb");
        sendReminderBtn.setStyle("-fx-font-size: 12px;");
        sendReminderBtn.setOnAction(e -> sendReminder(risk));
        
        Button viewDetailsBtn = new Button("👁️ Voir détails");
        viewDetailsBtn.getStyleClass().add("btn-rgb-outline");
        viewDetailsBtn.setStyle("-fx-font-size: 12px;");
        viewDetailsBtn.setOnAction(e -> viewStudentDetails(risk));
        
        actionsBox.getChildren().addAll(sendReminderBtn, viewDetailsBtn);
        
        card.getChildren().addAll(header, metricsBox, actionsBox);
        
        return card;
    }
    
    /**
     * Construit une métrique d'engagement.
     */
    private VBox buildMetric(String icon, String label, String value) {
        VBox metric = new VBox(4);
        metric.setAlignment(Pos.CENTER_LEFT);
        
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 18px;");
        
        Label labelLabel = new Label(label);
        labelLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
        
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        
        metric.getChildren().addAll(iconLabel, labelLabel, valueLabel);
        
        return metric;
    }
    
    /**
     * Envoie un rappel à l'étudiant.
     */
    private void sendReminder(StudentEngagementRisk risk) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Envoyer un rappel");
        alert.setHeaderText("Rappel à " + risk.getStudentName());
        alert.setContentText(
            "Fonctionnalité d'envoi d'email à venir.\n\n" +
            "Email : " + risk.getStudentEmail() + "\n" +
            "Niveau de risque : " + risk.getRiskLevel().label + "\n" +
            "Score : " + risk.getRiskScore()
        );
        alert.showAndWait();
    }
    
    /**
     * Affiche les détails d'un étudiant.
     */
    private void viewStudentDetails(StudentEngagementRisk risk) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de l'étudiant");
        alert.setHeaderText(risk.getStudentName());
        
        StringBuilder content = new StringBuilder();
        content.append("📧 Email : ").append(risk.getStudentEmail()).append("\n\n");
        content.append("📊 Score de risque : ").append(risk.getRiskScore()).append("\n");
        content.append("🎯 Niveau : ").append(risk.getRiskLevel().label).append("\n\n");
        content.append("📅 Absences aux sessions live : ").append(risk.getLiveSessionAbsences()).append("\n");
        content.append("🔌 Jours depuis dernière connexion : ").append(risk.getDaysSinceLastConnection()).append("\n");
        content.append("📚 Chapitres non consultés : ").append(risk.getUnopenedChapters()).append("/").append(risk.getTotalChapters()).append("\n\n");
        
        if (!risk.getRiskReasons().isEmpty()) {
            content.append("⚠️ Raisons du risque :\n");
            for (String reason : risk.getRiskReasons()) {
                content.append("  • ").append(reason).append("\n");
            }
        }
        
        alert.setContentText(content.toString());
        alert.showAndWait();
    }
    
    /**
     * Exporte les données en CSV.
     */
    @FXML
    private void exportToCSV() {
        if (currentRisks == null || currentRisks.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Aucune donnée");
            alert.setHeaderText("Aucune donnée à exporter");
            alert.setContentText("Veuillez d'abord analyser un cours.");
            alert.showAndWait();
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export CSV");
        alert.setHeaderText("Fonctionnalité à venir");
        alert.setContentText("L'export CSV sera disponible prochainement.");
        alert.showAndWait();
    }
}
