package com.educompus.controller.front;

import com.educompus.app.AppState;
import com.educompus.service.JcefBrowserService;
import com.educompus.service.MeetingService;
import com.educompus.util.Dialogs;
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

public final class FrontCalendarController {
    @FXML
    private VBox root;

    @FXML
    private Label accessLabel;

    @FXML
    private TextField roomField;

    @FXML
    private TextField linkField;

    @FXML
    private Label helperLabel;

    @FXML
    private SwingNode meetingBrowserHost;

    private final MeetingService meetingService = new MeetingService();
    private final JcefBrowserService browserService = JcefBrowserService.getInstance();
    private String currentUrl = "";
    private JcefBrowserService.BrowserDialogHandle browserHandle;

    @FXML
    private void initialize() {
        boolean allowed = !AppState.isAdmin();
        if (root != null) {
            root.setDisable(!allowed);
        }
        if (accessLabel != null) {
            accessLabel.setVisible(!allowed);
            accessLabel.setManaged(!allowed);
        }
        if (helperLabel != null) {
            helperLabel.setText("Domaine Jitsi: " + meetingService.getJitsiDomain());
        }
        if (meetingBrowserHost != null) {
            meetingBrowserHost.setVisible(false);
            meetingBrowserHost.setManaged(false);
        }
    }

    @FXML
    private void joinMeeting() {
        try {
            currentUrl = meetingService.buildMeetingUrl(roomField == null ? "" : roomField.getText());
            if (linkField != null) {
                linkField.setText(currentUrl);
            }
            loadBrowser(currentUrl);
        } catch (Exception ex) {
            Dialogs.error("Meeting", safe(ex.getMessage()));
        }
    }

    @FXML
    private void copyLink() {
        try {
            if (currentUrl.isBlank()) {
                currentUrl = meetingService.buildMeetingUrl(roomField == null ? "" : roomField.getText());
                if (linkField != null) {
                    linkField.setText(currentUrl);
                }
            }
            ClipboardContent content = new ClipboardContent();
            content.putString(currentUrl);
            Clipboard.getSystemClipboard().setContent(content);
            helperLabel.setText("Lien copie: " + currentUrl);
        } catch (Exception ex) {
            Dialogs.error("Meeting", safe(ex.getMessage()));
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void loadBrowser(String url) {
        String meetingUrl = safe(url);
        System.out.println("[Meeting] Loading Jitsi URL in student view: " + meetingUrl);
        new Thread(() -> {
            try {
                if (browserHandle == null || !browserHandle.isShowing()) {
                    browserHandle = browserService.openMeetingDialog("Salle Jitsi etudiant", meetingUrl);
                } else {
                    browserHandle.show();
                }
                browserHandle.load(meetingUrl);
                if (helperLabel != null) {
                    javafx.application.Platform.runLater(() -> helperLabel.setText("Salle ouverte dans une fenetre separee: " + meetingUrl));
                }
            } catch (Exception ex) {
                try {
                    if (java.awt.Desktop.isDesktopSupported()) {
                        java.awt.Desktop.getDesktop().browse(java.net.URI.create(meetingUrl));
                        javafx.application.Platform.runLater(() -> helperLabel.setText("Salle ouverte dans le navigateur: " + meetingUrl));
                        return;
                    }
                } catch (Exception browseEx) {
                    // ignore and show original error below
                }
                javafx.application.Platform.runLater(() -> Dialogs.error("Meeting", "Initialisation JCEF impossible: " + safe(ex.getMessage())));
            }
        }, "jcef-opener-calendar").start();
    }
}
