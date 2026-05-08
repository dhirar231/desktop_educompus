package com.educompus.controller.back;

import com.educompus.app.AppState;
import com.educompus.model.SessionLive;
import com.educompus.model.SessionStatut;
import com.educompus.service.GoogleCalendarService;
import com.educompus.service.GoogleMeetService;
import com.educompus.service.JcefBrowserService;
import com.educompus.service.MeetingService;
import com.educompus.service.SessionLiveService;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class BackSessionsController {
    @FXML private VBox root;
    @FXML private Label accessLabel;
    @FXML private TextField meetingTitleField;
    @FXML private TextField meetingRoomField;
    @FXML private TextField meetingLinkField;
    @FXML private Label meetingMetaLabel;
    @FXML private SwingNode meetingBrowserHost;
    
    // Éléments pour les sessions live
    @FXML private VBox sessionsLiveContainer;
    @FXML private TextField sessionCoursNameField;
    @FXML private TextField sessionLinkField;
    @FXML private DatePicker sessionDatePicker;
    @FXML private Spinner<Integer> sessionHourSpinner;
    @FXML private Spinner<Integer> sessionMinuteSpinner;
    @FXML private ListView<SessionLive> sessionsListView;
    @FXML private Label sessionsCountLabel;

    private final MeetingService meetingService = new MeetingService();
    private final SessionLiveService sessionLiveService = new SessionLiveService();
    private final GoogleCalendarService googleCalendarService = new GoogleCalendarService();
    private final GoogleMeetService googleMeetService = new GoogleMeetService();
    private ObservableList<SessionLive> sessionsList;

    @FXML
    private void initialize() {
        boolean teacherAllowed = AppState.isTeacher();
        if (accessLabel != null) {
            accessLabel.setVisible(!teacherAllowed);
            accessLabel.setManaged(!teacherAllowed);
        }
        if (root != null && !teacherAllowed) {
            root.setDisable(true);
        }
        if (meetingMetaLabel != null) {
            meetingMetaLabel.setText("Aucune salle generee.");
        }
        if (meetingBrowserHost != null) {
            meetingBrowserHost.setVisible(false);
            meetingBrowserHost.setManaged(false);
        }
        
        // Initialiser les sessions live
        initializeSessionsLive();
    }
    
    private void initializeSessionsLive() {
        // Initialiser les spinners pour l'heure et les minutes
        if (sessionHourSpinner != null) {
            sessionHourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, LocalTime.now().getHour()));
        }
        if (sessionMinuteSpinner != null) {
            sessionMinuteSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 15));
        }
        
        // Initialiser le date picker
        if (sessionDatePicker != null) {
            sessionDatePicker.setValue(LocalDate.now());
        }
        
        // Initialiser la liste des sessions
        if (sessionsListView != null) {
            sessionsList = FXCollections.observableArrayList();
            sessionsListView.setItems(sessionsList);
            sessionsListView.setCellFactory(param -> new SessionLiveCell());
            loadSessions();
        }
    }
    
    private void loadSessions() {
        try {
            List<SessionLive> sessions = sessionLiveService.getAllSessions();
            sessionsList.clear();
            sessionsList.addAll(sessions);
            updateSessionsCount();
        } catch (Exception e) {
            error("Erreur", "Impossible de charger les sessions: " + e.getMessage());
        }
    }
    
    private void updateSessionsCount() {
        if (sessionsCountLabel != null) {
            int total = sessionsList.size();
            long active = sessionsList.stream().filter(s -> s.getStatut() == SessionStatut.EN_COURS).count();
            sessionsCountLabel.setText(String.format("Total: %d | Actives: %d", total, active));
        }
    }
    
    @FXML
    private void createSessionLive() {
        try {
            String coursName = text(sessionCoursNameField);
            String link = text(sessionLinkField);
            LocalDate date = sessionDatePicker != null ? sessionDatePicker.getValue() : LocalDate.now();
            
            int hour = sessionHourSpinner != null ? sessionHourSpinner.getValue() : 0;
            int minute = sessionMinuteSpinner != null ? sessionMinuteSpinner.getValue() : 0;
            LocalTime time = LocalTime.of(hour, minute);
            
            if (coursName.isBlank()) {
                info("Validation", "Veuillez entrer le nom du cours.");
                return;
            }
            if (link.isBlank()) {
                info("Validation", "Veuillez entrer le lien de la session.");
                return;
            }
            
            // Créer la session (la synchronisation Google Calendar est automatique dans le service)
            SessionLive session = new SessionLive(coursName, link, date, time);
            sessionLiveService.ajouterSession(session);
            
            // Afficher le message approprié selon le statut de synchronisation
            if (session.estSynchroniseeCalendar()) {
                info("Succès", "Session créée et synchronisée avec Google Calendar ✓");
            } else {
                info("Succès", "Session créée (Google Calendar non configuré).");
            }
            
            // Réinitialiser les champs
            sessionCoursNameField.clear();
            sessionLinkField.clear();
            sessionDatePicker.setValue(LocalDate.now());
            sessionHourSpinner.getValueFactory().setValue(LocalTime.now().getHour());
            sessionMinuteSpinner.getValueFactory().setValue(0);
            
            // Recharger la liste
            loadSessions();
        } catch (Exception e) {
            error("Erreur", "Impossible de créer la session: " + e.getMessage());
        }
    }
    
    @FXML
    private void generateGoogleMeetLink() {
        try {
            // Générer un lien Google Meet
            String meetLink = googleMeetService.generateMeetLink("Session Live");
            
            // Remplir le champ avec le lien généré
            if (sessionLinkField != null) {
                sessionLinkField.setText(meetLink);
            }
            
            // Copier dans le presse-papiers
            ClipboardContent content = new ClipboardContent();
            content.putString(meetLink);
            Clipboard.getSystemClipboard().setContent(content);
            
            info("Google Meet", 
                "Lien Google Meet généré et copié !\n\n" +
                "Lien : " + meetLink + "\n\n" +
                "Note : Vous devrez créer manuellement la réunion sur meet.google.com\n" +
                "en utilisant ce code de réunion.");
        } catch (Exception e) {
            error("Erreur", "Impossible de générer le lien Google Meet: " + e.getMessage());
        }
    }
    
    @FXML
    private void startSelectedSession() {
        SessionLive selected = sessionsListView != null ? sessionsListView.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            info("Sélection", "Veuillez sélectionner une session.");
            return;
        }
        
        try {
            sessionLiveService.demarrerSession(selected.getId());
            loadSessions();
            info("Succès", "Session démarrée.");
        } catch (Exception e) {
            error("Erreur", "Impossible de démarrer la session: " + e.getMessage());
        }
    }
    
    @FXML
    private void endSelectedSession() {
        SessionLive selected = sessionsListView != null ? sessionsListView.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            info("Sélection", "Veuillez sélectionner une session.");
            return;
        }
        
        try {
            sessionLiveService.terminerSession(selected.getId());
            loadSessions();
            info("Succès", "Session terminée.");
        } catch (Exception e) {
            error("Erreur", "Impossible de terminer la session: " + e.getMessage());
        }
    }
    
    @FXML
    private void deleteSelectedSession() {
        SessionLive selected = sessionsListView != null ? sessionsListView.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            info("Sélection", "Veuillez sélectionner une session.");
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la session ?");
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cette session ?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                // Supprimer de Google Calendar si synchronisée
                if (selected.estSynchroniseeCalendar()) {
                    googleCalendarService.supprimerEvenement(selected.getGoogleEventId());
                }
                
                sessionLiveService.supprimerSession(selected.getId());
                loadSessions();
                info("Succès", "Session supprimée.");
            } catch (Exception e) {
                error("Erreur", "Impossible de supprimer la session: " + e.getMessage());
            }
        }
    }

    @FXML
    private void createMeeting() {
        try {
            String room = meetingService.createRoom(text(meetingTitleField), text(meetingRoomField));
            String url = meetingService.buildMeetingUrl(room);
            if (meetingRoomField != null) {
                meetingRoomField.setText(room);
            }
            if (meetingLinkField != null) {
                meetingLinkField.setText(url);
            }
            if (meetingMetaLabel != null) {
                meetingMetaLabel.setText("Salle prete: " + room);
            }
            openDialog(url);
        } catch (Exception e) {
            error("Meeting", e);
        }
    }

    @FXML
    private void loadMeeting() {
        String url = text(meetingLinkField);
        if (url.isBlank()) {
            String room = text(meetingRoomField);
            if (room.isBlank()) {
                info("Meeting", "Generez une salle ou saisissez un Room ID.");
                return;
            }
            try {
                url = meetingService.buildMeetingUrl(room);
                if (meetingLinkField != null) {
                    meetingLinkField.setText(url);
                }
            } catch (Exception e) {
                error("Meeting", e);
                return;
            }
        }
        openDialog(url);
    }

    @FXML
    private void copyMeetingLink() {
        String url = text(meetingLinkField);
        if (url.isBlank()) {
            info("Meeting", "Aucun lien a copier.");
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(url);
        Clipboard.getSystemClipboard().setContent(content);
        info("Meeting", "Lien copie.");
    }

    private void openDialog(String url) {
        try {
            JcefBrowserService.getInstance().openMeetingDialog("Meeting Jitsi", url);
        } catch (Exception e) {
            copyToClipboard(url);
            error("Meeting JCEF", e);
        }
    }

    private static void copyToClipboard(String value) {
        ClipboardContent content = new ClipboardContent();
        content.putString(safe(value));
        Clipboard.getSystemClipboard().setContent(content);
    }

    private static String text(TextField field) {
        return field == null ? "" : safe(field.getText());
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static void error(String title, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(e == null ? "Erreur" : String.valueOf(e.getMessage()));
        alert.showAndWait();
        if (e != null) {
            e.printStackTrace();
        }
    }
    
    private static void error(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // ── Classe interne pour afficher les sessions dans la ListView ──────────
    
    private static class SessionLiveCell extends ListCell<SessionLive> {
        @Override
        protected void updateItem(SessionLive session, boolean empty) {
            super.updateItem(session, empty);
            
            if (empty || session == null) {
                setGraphic(null);
                setText(null);
                return;
            }
            
            // Créer un conteneur pour la session
            VBox container = new VBox(4);
            container.setStyle("-fx-border-color: #e0e0e0; -fx-border-radius: 4; -fx-padding: 8;");
            
            // Titre avec statut
            HBox titleBox = new HBox(8);
            Label titleLabel = new Label(session.getNomCours());
            titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
            
            Label statusLabel = new Label(session.getLibelleStatut());
            statusLabel.setStyle("-fx-padding: 2 6; -fx-border-radius: 3; -fx-font-size: 10; " + 
                                getStatusStyle(session.getStatut()));
            
            titleBox.getChildren().addAll(titleLabel, statusLabel);
            
            // Date et heure
            Label dateTimeLabel = new Label("📅 " + session.getDateHeureFormatee());
            dateTimeLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #666;");
            
            // Lien
            Label linkLabel = new Label("🔗 " + truncate(session.getLien(), 50));
            linkLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #0066cc; -fx-underline: true;");
            linkLabel.setCursor(javafx.scene.Cursor.HAND);
            
            // Google Calendar status
            Label calendarLabel = new Label();
            if (session.estSynchroniseeCalendar()) {
                calendarLabel.setText("✓ Synchronisée avec Google Calendar");
                calendarLabel.setStyle("-fx-font-size: 9; -fx-text-fill: #00aa00;");
            } else {
                calendarLabel.setText("✗ Non synchronisée");
                calendarLabel.setStyle("-fx-font-size: 9; -fx-text-fill: #aa0000;");
            }
            
            container.getChildren().addAll(titleBox, dateTimeLabel, linkLabel, calendarLabel);
            setGraphic(container);
        }
        
        private String getStatusStyle(SessionStatut statut) {
            return switch (statut) {
                case PLANIFIEE -> "-fx-background-color: #e3f2fd; -fx-text-fill: #1976d2;";
                case EN_COURS -> "-fx-background-color: #c8e6c9; -fx-text-fill: #388e3c;";
                case TERMINEE -> "-fx-background-color: #f5f5f5; -fx-text-fill: #666;";
            };
        }
        
        private String truncate(String text, int length) {
            if (text == null) return "";
            return text.length() > length ? text.substring(0, length) + "..." : text;
        }
    }
}
