package com.educompus.controller.back;

import com.educompus.app.AppState;
import com.educompus.service.JcefBrowserService;
import com.educompus.service.MeetingService;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

public final class BackSessionsController {
    @FXML private VBox root;
    @FXML private Label accessLabel;
    @FXML private TextField meetingTitleField;
    @FXML private TextField meetingRoomField;
    @FXML private TextField meetingLinkField;
    @FXML private Label meetingMetaLabel;
    @FXML private SwingNode meetingBrowserHost;

    private final MeetingService meetingService = new MeetingService();

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
}
