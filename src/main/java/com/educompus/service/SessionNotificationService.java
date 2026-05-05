package com.educompus.service;

import com.educompus.model.SessionLive;
import com.educompus.model.SessionStatut;
import com.educompus.repository.SessionLiveRepository;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Service de notifications automatiques + statut live automatique.
 *
 * Fonctionnalités :
 * 1. Statut automatique : PLANIFIEE → EN_COURS basé sur l'heure réelle
 * 2. Notification 30 min avant avec bouton "Rejoindre en 1 clic"
 * 3. Notification urgente 5 min avant
 * 4. Callback pour rafraîchir l'UI après changement de statut
 */
public final class SessionNotificationService {

    private static final SessionNotificationService INSTANCE = new SessionNotificationService();

    private static final int CHECK_INTERVAL_SECONDS = 60;
    // Durée par défaut d'une session (1h) avant passage automatique à TERMINEE
    private static final int SESSION_DUREE_MINUTES  = 60;

    private final Set<Integer> notifiedIds30min = new HashSet<>();
    private final Set<Integer> notifiedIds5min  = new HashSet<>();
    private final Set<Integer> autoStartedIds   = new HashSet<>();

    private final SessionLiveRepository    sessionRepo   = new SessionLiveRepository();
    private final SessionLiveMetierService metierService = new SessionLiveMetierService();

    private Timeline checkTimeline;
    private Stage    ownerStage;

    /** Callback appelé quand un statut change automatiquement (pour rafraîchir l'UI). */
    private Consumer<SessionLive> onStatutChange;

    private SessionNotificationService() {}

    public static SessionNotificationService getInstance() { return INSTANCE; }

    // ── Cycle de vie ──────────────────────────────────────────────────────────

    public void demarrer(Stage stage) {
        this.ownerStage = stage;
        arreter();
        checkTimeline = new Timeline(
            new KeyFrame(Duration.seconds(CHECK_INTERVAL_SECONDS), e -> verifierSessions())
        );
        checkTimeline.setCycleCount(Timeline.INDEFINITE);
        checkTimeline.play();
        verifierSessions(); // vérification immédiate
        System.out.println("[Notifications] Service démarré");
    }

    public void arreter() {
        if (checkTimeline != null) { checkTimeline.stop(); checkTimeline = null; }
    }

    /** Enregistre un callback appelé à chaque changement de statut automatique. */
    public void setOnStatutChange(Consumer<SessionLive> callback) {
        this.onStatutChange = callback;
    }

    // ── Vérification principale ───────────────────────────────────────────────

    private void verifierSessions() {
        Thread t = new Thread(() -> {
            try {
                LocalDateTime now = LocalDateTime.now();

                // 1. Sessions PLANIFIEES → vérifier démarrage automatique + notifications
                List<SessionLive> planifiees = sessionRepo.getSessionsByStatut(SessionStatut.PLANIFIEE);
                for (SessionLive session : planifiees) {
                    if (session.getDate() == null || session.getHeure() == null) continue;
                    LocalDateTime debut = LocalDateTime.of(session.getDate(), session.getHeure());
                    long minutesAvant = java.time.Duration.between(now, debut).toMinutes();

                    // ── Statut automatique : démarrer si l'heure est passée ──
                    if (minutesAvant <= 0 && !autoStartedIds.contains(session.getId())) {
                        autoStartedIds.add(session.getId());
                        try {
                            metierService.startSession(session.getId());
                            System.out.println("[AutoStatus] Session " + session.getId() + " → EN_COURS");
                            SessionLive updated = sessionRepo.getSessionById(session.getId());
                            if (onStatutChange != null && updated != null) {
                                Platform.runLater(() -> onStatutChange.accept(updated));
                            }
                        } catch (Exception ex) {
                            System.err.println("[AutoStatus] Erreur: " + ex.getMessage());
                        }
                        continue;
                    }

                    // ── Notification 30 min avant ──
                    if (minutesAvant >= 28 && minutesAvant <= 32 && !notifiedIds30min.contains(session.getId())) {
                        notifiedIds30min.add(session.getId());
                        final int min = (int) minutesAvant;
                        Platform.runLater(() -> afficherNotification(session, min, false));
                    }
                    // ── Notification 5 min avant ──
                    else if (minutesAvant >= 3 && minutesAvant <= 7 && !notifiedIds5min.contains(session.getId())) {
                        notifiedIds5min.add(session.getId());
                        Platform.runLater(() -> afficherNotification(session, 5, true));
                    }
                }

                // 2. Sessions EN_COURS → terminer automatiquement après SESSION_DUREE_MINUTES
                List<SessionLive> enCours = sessionRepo.getSessionsByStatut(SessionStatut.EN_COURS);
                for (SessionLive session : enCours) {
                    if (session.getDate() == null || session.getHeure() == null) continue;
                    LocalDateTime debut = LocalDateTime.of(session.getDate(), session.getHeure());
                    long minutesEcoulees = java.time.Duration.between(debut, now).toMinutes();
                    if (minutesEcoulees >= SESSION_DUREE_MINUTES) {
                        try {
                            metierService.endSession(session.getId());
                            System.out.println("[AutoStatus] Session " + session.getId() + " → TERMINEE");
                            SessionLive updated = sessionRepo.getSessionById(session.getId());
                            if (onStatutChange != null && updated != null) {
                                Platform.runLater(() -> onStatutChange.accept(updated));
                            }
                        } catch (Exception ex) {
                            System.err.println("[AutoStatus] Fin auto erreur: " + ex.getMessage());
                        }
                    }
                }

            } catch (Exception e) {
                System.err.println("[Notifications] Erreur: " + e.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Affichage des notifications ───────────────────────────────────────────

    private void afficherNotification(SessionLive session, int minutesAvant, boolean urgente) {
        if (ownerStage == null || !ownerStage.isShowing()) return;

        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().add(buildNotificationBox(session, minutesAvant, urgente, popup));

        double x = ownerStage.getX() + ownerStage.getWidth() - 360;
        double y = ownerStage.getY() + ownerStage.getHeight() - 180;
        popup.show(ownerStage, x, y);

        new Timeline(new KeyFrame(Duration.seconds(urgente ? 12 : 8), e -> popup.hide()))
            .play();
    }

    private VBox buildNotificationBox(SessionLive session, int minutesAvant,
                                      boolean urgente, Popup popup) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(14, 16, 14, 16));
        box.setMaxWidth(340);
        box.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 12px;" +
            "-fx-border-color: " + (urgente ? "#d6293e" : "#4a90d9") + ";" +
            "-fx-border-width: 2; -fx-border-radius: 12px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 16, 0, 0, 4);"
        );

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon  = new Label(urgente ? "🔴" : "📅");
        icon.setStyle("-fx-font-size: 18px;");
        Label titre = new Label(urgente ? "⚡ Session dans 5 min !" : "📅 Session dans " + minutesAvant + " min");
        titre.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " +
                       (urgente ? "#d6293e" : "#202124") + ";");

        // ── Sync status badge ──
        Label syncBadge = buildSyncBadge(session);

        HBox.setHgrow(titre, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().addAll(icon, titre, syncBadge);

        Label nomCours  = new Label("📚  " + safe(session.getNomCours()));
        nomCours.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #333;");
        Label dateHeure = new Label("🕐  " + session.getDateHeureFormatee());
        dateHeure.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        // ── Bouton rejoindre en 1 clic ──
        Button joinBtn = new Button("🔗  Rejoindre maintenant");
        joinBtn.setStyle(
            "-fx-background-color: " + (urgente ? "#d6293e" : "#4a90d9") + ";" +
            "-fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: 12px;" +
            "-fx-background-radius: 8px; -fx-padding: 8 16 8 16; -fx-cursor: hand;"
        );
        joinBtn.setMaxWidth(Double.MAX_VALUE);
        joinBtn.setOnAction(e -> {
            popup.hide();
            ouvrirLien(session.getLien());
        });

        // Bouton fermer
        Button closeBtn = new Button("✕");
        closeBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #999;" +
            "-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 2 6 2 6;");
        closeBtn.setOnAction(e -> popup.hide());

        HBox footer = new HBox(8, joinBtn, closeBtn);
        footer.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(header, nomCours, dateHeure, footer);
        return box;
    }

    // ── Sync status badge ─────────────────────────────────────────────────────

    /**
     * Retourne un badge SYNCED / NOT SYNCED / UPDATED selon l'état Calendar.
     */
    public static Label buildSyncBadge(SessionLive session) {
        Label badge = new Label();
        badge.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-background-radius: 4px; -fx-padding: 2 6 2 6;");
        if (session.estSynchroniseeCalendar()) {
            badge.setText("✅ SYNCED");
            badge.setStyle(badge.getStyle() +
                "-fx-background-color: rgba(41,182,216,0.15); -fx-text-fill: #29b6d8;");
        } else {
            badge.setText("⚠ NOT SYNCED");
            badge.setStyle(badge.getStyle() +
                "-fx-background-color: rgba(240,165,0,0.15); -fx-text-fill: #f0a500;");
        }
        return badge;
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private static void ouvrirLien(String lien) {
        if (lien == null || lien.isBlank()) return;
        try {
            JcefBrowserService.getInstance().openMeetingDialog("Session live", lien);
        } catch (Exception ex) {
            javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(lien);
            cb.setContent(cc);
        }
    }

    public void testerNotification(SessionLive session) {
        Platform.runLater(() -> afficherNotification(session, 30, false));
    }

    public void reinitialiserNotifications() {
        notifiedIds30min.clear();
        notifiedIds5min.clear();
        autoStartedIds.clear();
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }
}
