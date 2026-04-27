package com.educompus.controller.back;

import com.educompus.app.AppState;
import com.educompus.service.JcefBrowserService;
import com.educompus.service.MeetingService;
import com.educompus.util.Dialogs;
import javafx.fxml.FXML;
import javafx.embed.swing.SwingNode;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

public final class BackSessionsController {
    @FXML
    private VBox root;

    @FXML
    private Label accessLabel;

    @FXML
    private TextField meetingTitleField;

    @FXML
    private TextField meetingRoomField;

    @FXML
    private TextField meetingLinkField;

    @FXML
    private Label meetingMetaLabel;

    @FXML
    private SwingNode meetingBrowserHost;

    private final MeetingService meetingService = new MeetingService();
    private final JcefBrowserService browserService = JcefBrowserService.getInstance();
    private String currentMeetingUrl = "";
    private JcefBrowserService.BrowserDialogHandle browserHandle;

    @FXML
    private void initialize() {
        boolean teacherAccess = AppState.isTeacher();
        if (root != null) {
            root.setDisable(!teacherAccess);
        }
        if (accessLabel != null) {
            accessLabel.setVisible(!teacherAccess);
            accessLabel.setManaged(!teacherAccess);
        }
        if (!teacherAccess) {
            return;
        }
        if (meetingMetaLabel != null) {
            meetingMetaLabel.setText("Domaine Jitsi: " + meetingService.getJitsiDomain());
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
            currentMeetingUrl = meetingService.buildMeetingUrl(room);
            meetingRoomField.setText(room);
            meetingLinkField.setText(currentMeetingUrl);
            meetingMetaLabel.setText("Salle " + room + " | " + meetingService.getJitsiDomain());
            loadBrowser(currentMeetingUrl);
        } catch (Exception ex) {
            Dialogs.error("Meeting", safe(ex.getMessage()));
        }
    }

    @FXML
    private void loadMeeting() {
        ensureMeetingCreated();
        if (currentMeetingUrl.isBlank()) {
            return;
        }
        loadBrowser(currentMeetingUrl);
    }

    @FXML
    private void copyMeetingLink() {
        ensureMeetingCreated();
        if (currentMeetingUrl.isBlank()) {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(currentMeetingUrl);
        Clipboard.getSystemClipboard().setContent(content);
        meetingMetaLabel.setText("Lien copie: " + currentMeetingUrl);
    }

    private void ensureMeetingCreated() {
        if (currentMeetingUrl.isBlank()) {
            createMeeting();
        }
    }

    private void loadBrowser(String url) {
        String meetingUrl = safe(url);
        System.out.println("[Meeting] Loading Jitsi URL in teacher view: " + meetingUrl);
        new Thread(() -> {
            try {
                if (browserHandle == null || !browserHandle.isShowing()) {
                    browserHandle = browserService.openMeetingDialog("Salle Jitsi enseignant", meetingUrl);
                } else {
                    browserHandle.show();
                }
                browserHandle.load(meetingUrl);
                if (meetingMetaLabel != null) {
                    javafx.application.Platform.runLater(() -> meetingMetaLabel.setText("Salle ouverte dans une fenetre separee: " + meetingUrl));
                }
            } catch (Exception ex) {
                try {
                    if (java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop.getDesktop().browse(java.net.URI.create(meetingUrl));
                        javafx.application.Platform.runLater(() -> meetingMetaLabel.setText("Salle ouverte dans le navigateur: " + meetingUrl));
                        return;
                    }
                } catch (Exception browseEx) {
                    // ignore and show original error below
                }
                javafx.application.Platform.runLater(() -> Dialogs.error("Meeting", "Initialisation JCEF impossible: " + safe(ex.getMessage())));
            }
        }, "jcef-opener-sessions").start();
    }

    private static String text(TextField field) {
        return field == null ? "" : safe(field.getText());
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
