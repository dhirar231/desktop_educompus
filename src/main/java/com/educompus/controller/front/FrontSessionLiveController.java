package com.educompus.controller.front;

import com.educompus.app.AppState;
import com.educompus.model.ActionType;
import com.educompus.model.SessionAction;
import com.educompus.model.SessionLive;
import com.educompus.model.SessionParticipant;
import com.educompus.model.SessionStatut;
import com.educompus.repository.SessionLiveRepository;
import com.educompus.service.SessionLiveMetierService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

/**
 * Contrôleur côté étudiant pour le module Session Live.
 *
 * <p>Gère l'affichage des sessions d'un cours et les interactions :
 * rejoindre, lever la main, quitter, voir le journal en temps réel.
 *
 * <p>Ce composant est embarqué dans {@link FrontCourseDetailController}
 * via la méthode {@link #setCours(int, String)}.
 */
public final class FrontSessionLiveController {

    // ── Services ──────────────────────────────────────────────────────────────
    private final SessionLiveRepository    sessionRepo   = new SessionLiveRepository();
    private final SessionLiveMetierService metierService = new SessionLiveMetierService();

    // ── État courant ──────────────────────────────────────────────────────────
    private int    coursId;
    private String coursTitre;
    private SessionLive sessionActive;   // session rejointe par l'étudiant
    private Timeline    refreshTimeline; // polling toutes les 5s

    // ── Conteneur racine (injecté dans le FXML parent) ────────────────────────
    private final VBox rootBox = new VBox(12);

    // ── Zones dynamiques ──────────────────────────────────────────────────────
    private final VBox sessionListBox   = new VBox(8);
    private final VBox livePanel        = new VBox(10);  // panneau "en session"
    private final VBox journalBox       = new VBox(4);   // journal des actions
    private final Label statusLabel     = new Label();
    private final Label participantsLbl = new Label();
    private final Label mainsLeveesLbl  = new Label();
    private Button raiseHandBtn;
    private Button leaveBtn;

    // ── Initialisation ────────────────────────────────────────────────────────

    public FrontSessionLiveController() {
        buildUI();
    }

    /** Retourne le nœud racine à insérer dans le FXML parent. */
    public VBox getRootNode() {
        return rootBox;
    }

    /**
     * Définit le cours courant et charge les sessions associées.
     *
     * @param coursId    ID du cours
     * @param coursTitre Titre du cours (pour affichage)
     */
    public void setCours(int coursId, String coursTitre) {
        this.coursId    = coursId;
        this.coursTitre = coursTitre;
        chargerSessions();
    }

    // ── Construction de l'interface ───────────────────────────────────────────

    private void buildUI() {
        rootBox.setPadding(new Insets(0));
        rootBox.getStyleClass().add("session-live-section");

        // ── Titre de section ──
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label titre = new Label("🔴  Sessions Live");
        titre.getStyleClass().add("page-title");
        titre.setStyle("-fx-font-size: 16px;");
        Label badge = new Label("LIVE");
        badge.getStyleClass().addAll("chip", "chip-success");
        badge.setStyle("-fx-font-size: 10px; -fx-font-weight: 800;");
        header.getChildren().addAll(titre, badge);

        // ── Info interactive ──
        Label infoLbl = new Label(
            "💡 Les interactions (lever la main, chat, audio) sont disponibles après connexion à la session.");
        infoLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c63ff; -fx-wrap-text: true;");
        infoLbl.setWrapText(true);

        // ── Liste des sessions ──
        sessionListBox.setPadding(new Insets(0));

        // ── Panneau "en session" (masqué par défaut) ──
        buildLivePanel();
        livePanel.setVisible(false);
        livePanel.setManaged(false);

        rootBox.getChildren().addAll(header, infoLbl, sessionListBox, livePanel);
    }

    private void buildLivePanel() {
        livePanel.getStyleClass().add("card");
        livePanel.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 14px; -fx-padding: 18;" +
            "-fx-border-color: #e8eaed; -fx-border-radius: 14px; -fx-border-width: 1;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);");

        // ── Header panneau live ──
        HBox liveHeader = new HBox(10);
        liveHeader.setAlignment(Pos.CENTER_LEFT);
        Label liveDot = new Label("🔴");
        liveDot.setStyle("-fx-font-size: 18px;");
        Label liveTitle = new Label("Session en cours");
        liveTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #202124;");
        liveHeader.getChildren().addAll(liveDot, liveTitle);

        // ── Statut ──
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #5f6368;");

        // ── Compteurs ──
        HBox counters = new HBox(16);
        counters.setAlignment(Pos.CENTER_LEFT);
        participantsLbl.getStyleClass().addAll("chip", "chip-info");
        participantsLbl.setStyle("-fx-font-size: 11px;");
        mainsLeveesLbl.getStyleClass().addAll("chip", "chip-warning");
        mainsLeveesLbl.setStyle("-fx-font-size: 11px;");
        counters.getChildren().addAll(participantsLbl, mainsLeveesLbl);

        // ── Boutons d'interaction ──
        raiseHandBtn = new Button("✋  Lever la main");
        raiseHandBtn.getStyleClass().add("btn-rgb");
        raiseHandBtn.setStyle("-fx-font-size: 13px; -fx-font-weight: 700;");
        raiseHandBtn.setOnAction(e -> toggleMainLevee());

        leaveBtn = new Button("🚪  Quitter la session");
        leaveBtn.setStyle(
            "-fx-background-color: rgba(214,41,62,0.08); -fx-text-fill: #d6293e;" +
            "-fx-border-color: #d6293e; -fx-border-width: 1.5; -fx-border-radius: 8px;" +
            "-fx-background-radius: 8px; -fx-font-size: 12px; -fx-font-weight: 700; -fx-cursor: hand;");
        leaveBtn.setOnAction(e -> quitterSession());

        HBox actions = new HBox(12, raiseHandBtn, leaveBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        // ── Journal des actions ──
        Label journalTitle = new Label("📋  Journal de la session");
        journalTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #444;");

        journalBox.setStyle(
            "-fx-background-color: #f8f9fa; -fx-background-radius: 8px;" +
            "-fx-border-color: #e8eaed; -fx-border-radius: 8px; -fx-border-width: 1; -fx-padding: 10;");
        journalBox.setMaxHeight(150);

        ScrollPane journalScroll = new ScrollPane(journalBox);
        journalScroll.setFitToWidth(true);
        journalScroll.setMaxHeight(150);
        journalScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        livePanel.getChildren().addAll(
            liveHeader, statusLabel, counters, actions, journalTitle, journalScroll);
    }

    // ── Chargement des sessions ───────────────────────────────────────────────

    private void chargerSessions() {
        sessionListBox.getChildren().clear();

        List<SessionLive> sessions;
        try {
            sessions = sessionRepo.getSessionsByCoursId(coursId);
        } catch (Exception e) {
            // Table pas encore créée ou autre erreur DB — afficher silencieusement
            System.err.println("[SessionLive] Impossible de charger les sessions: " + e.getMessage());
            sessions = List.of();
        }

        if (sessions.isEmpty()) {
            Label empty = new Label("Aucune session live planifiée pour ce cours.");
            empty.setStyle("-fx-font-size: 12px; -fx-text-fill: #888; -fx-padding: 8 0 0 0;");
            sessionListBox.getChildren().add(empty);
            return;
        }

        for (SessionLive session : sessions) {
            sessionListBox.getChildren().add(buildSessionCard(session));
        }
    }

    private VBox buildSessionCard(SessionLive session) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14, 16, 14, 16));

        // ── Ligne principale ──
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        // Badge statut
        Label statutBadge = new Label(session.getIconeStatut() + "  " + session.getLibelleStatut());
        statutBadge.getStyleClass().add("chip");
        switch (session.getStatut()) {
            case EN_COURS  -> statutBadge.getStyleClass().add("chip-success");
            case PLANIFIEE -> statutBadge.getStyleClass().add("chip-info");
            case TERMINEE  -> statutBadge.getStyleClass().add("chip-warning");
            case ANNULEE   -> statutBadge.getStyleClass().add("chip-danger");
        }

        // Infos
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label nomLbl = new Label(safe(session.getNomCours()));
        nomLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 700;");
        Label dateLbl = new Label("📅  " + session.getDateHeureFormatee());
        dateLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        info.getChildren().addAll(nomLbl, dateLbl);

        // Bouton rejoindre
        Button joinBtn = buildJoinButton(session);

        row.getChildren().addAll(statutBadge, info, joinBtn);

        // ── Message interactif si EN_COURS ──
        if (session.getStatut() == SessionStatut.EN_COURS) {
            Label interactifLbl = new Label(
                "✅  Session interactive disponible — Lever la main, chat et audio disponibles après connexion.");
            interactifLbl.setStyle(
                "-fx-font-size: 10px; -fx-text-fill: #29b6d8; -fx-wrap-text: true;" +
                "-fx-background-color: rgba(41,182,216,0.08); -fx-background-radius: 6px; -fx-padding: 6 10 6 10;");
            interactifLbl.setWrapText(true);
            card.getChildren().addAll(row, interactifLbl);
        } else {
            card.getChildren().add(row);
        }

        return card;
    }

    private Button buildJoinButton(SessionLive session) {
        Button btn;
        if (session.getStatut() == SessionStatut.EN_COURS) {
            btn = new Button("🔗  Rejoindre");
            btn.getStyleClass().add("btn-rgb");
            btn.setStyle("-fx-font-size: 13px; -fx-font-weight: 700;");
            btn.setOnAction(e -> rejoindreSession(session));
        } else if (session.getStatut() == SessionStatut.PLANIFIEE) {
            btn = new Button("📅  Planifiée");
            btn.setStyle(
                "-fx-background-color: rgba(108,99,255,0.1); -fx-text-fill: #6c63ff;" +
                "-fx-border-color: #6c63ff; -fx-border-width: 1; -fx-border-radius: 8px;" +
                "-fx-background-radius: 8px; -fx-font-size: 12px; -fx-cursor: default;");
            btn.setDisable(true);
        } else {
            btn = new Button(session.getStatut() == SessionStatut.TERMINEE ? "✅  Terminée" : "❌  Annulée");
            btn.setStyle(
                "-fx-background-color: rgba(150,150,150,0.1); -fx-text-fill: #999;" +
                "-fx-border-color: #ccc; -fx-border-width: 1; -fx-border-radius: 8px;" +
                "-fx-background-radius: 8px; -fx-font-size: 12px; -fx-cursor: default;");
            btn.setDisable(true);
        }
        return btn;
    }

    // ── Actions métier ────────────────────────────────────────────────────────

    /**
     * L'étudiant rejoint une session :
     * 1. Ouvre le lien externe (Google Meet / Zoom)
     * 2. Enregistre JOIN dans session_actions
     * 3. Ajoute l'étudiant dans session_participants
     * 4. Affiche le panneau interactif
     */
    private void rejoindreSession(SessionLive session) {
        int etudiantId   = AppState.getUserId();
        String nomEtudiant = nomEtudiant();

        // 1. Ouvrir le lien externe
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(session.getLien()));
        } catch (Exception e) {
            // Fallback : copier le lien
            javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(session.getLien());
            cb.setContent(cc);
            showInfo("Lien copié",
                "Impossible d'ouvrir le navigateur.\nLien copié dans le presse-papier :\n" + session.getLien());
        }

        // 2 & 3. Enregistrer JOIN + participant (en arrière-plan)
        Thread t = new Thread(() -> {
            try {
                metierService.joinSession(session.getId(), etudiantId, nomEtudiant);
            } catch (Exception ex) {
                System.err.println("[SessionLive] Erreur JOIN: " + ex.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();

        // 4. Afficher le panneau interactif
        sessionActive = session;
        afficherPanneauLive(session);
        demarrerPolling();
    }

    /** Bascule l'état "main levée" de l'étudiant. */
    private void toggleMainLevee() {
        if (sessionActive == null) return;
        int etudiantId = AppState.getUserId();
        String nom     = nomEtudiant();

        // Vérifier l'état actuel
        boolean mainActuellementLevee = raiseHandBtn != null &&
            raiseHandBtn.getText().contains("Baisser");

        Thread t = new Thread(() -> {
            try {
                if (mainActuellementLevee) {
                    metierService.lowerHand(sessionActive.getId(), etudiantId, nom);
                } else {
                    metierService.raiseHand(sessionActive.getId(), etudiantId, nom);
                }
                Platform.runLater(() -> {
                    if (raiseHandBtn != null) {
                        if (mainActuellementLevee) {
                            raiseHandBtn.setText("✋  Lever la main");
                            raiseHandBtn.setStyle("-fx-font-size: 13px; -fx-font-weight: 700;");
                        } else {
                            raiseHandBtn.setText("👇  Baisser la main");
                            raiseHandBtn.setStyle(
                                "-fx-background-color: #f0a500; -fx-text-fill: white;" +
                                "-fx-font-size: 13px; -fx-font-weight: 700;");
                        }
                    }
                    rafraichirJournal();
                });
            } catch (Exception ex) {
                System.err.println("[SessionLive] Erreur RAISE_HAND: " + ex.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /** L'étudiant quitte la session. */
    private void quitterSession() {
        if (sessionActive == null) return;
        int etudiantId = AppState.getUserId();
        String nom     = nomEtudiant();

        Thread t = new Thread(() -> {
            try {
                metierService.leaveSession(sessionActive.getId(), etudiantId);
            } catch (Exception ex) {
                System.err.println("[SessionLive] Erreur LEAVE: " + ex.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();

        arreterPolling();
        sessionActive = null;
        masquerPanneauLive();
        chargerSessions(); // Rafraîchir la liste
    }

    // ── Panneau live ──────────────────────────────────────────────────────────

    private void afficherPanneauLive(SessionLive session) {
        statusLabel.setText("Connecté à : " + safe(session.getNomCours()) +
                            "  •  " + session.getDateHeureFormatee());
        livePanel.setVisible(true);
        livePanel.setManaged(true);
        rafraichirStatut();
    }

    private void masquerPanneauLive() {
        livePanel.setVisible(false);
        livePanel.setManaged(false);
        journalBox.getChildren().clear();
        if (raiseHandBtn != null) raiseHandBtn.setText("✋  Lever la main");
    }

    private void rafraichirStatut() {
        if (sessionActive == null) return;
        Thread t = new Thread(() -> {
            try {
                SessionLiveMetierService.LiveStatus status =
                    metierService.getLiveStatus(sessionActive.getId());
                Platform.runLater(() -> {
                    participantsLbl.setText("👥  " + status.getNbPresents() + " présent(s)");
                    mainsLeveesLbl.setText("✋  " + status.getNbMainsLevees() + " main(s) levée(s)");
                    afficherJournal(status.getActionsRecentes());
                });
            } catch (Exception ex) {
                System.err.println("[SessionLive] Erreur statut: " + ex.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void rafraichirJournal() {
        if (sessionActive == null) return;
        Thread t = new Thread(() -> {
            try {
                List<SessionAction> actions = metierService.getJournal(sessionActive.getId());
                // Prendre les 10 dernières
                List<SessionAction> recentes = actions.size() > 10
                    ? actions.subList(actions.size() - 10, actions.size())
                    : actions;
                Platform.runLater(() -> afficherJournal(recentes));
            } catch (Exception ex) {
                System.err.println("[SessionLive] Erreur journal: " + ex.getMessage());
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void afficherJournal(List<SessionAction> actions) {
        journalBox.getChildren().clear();
        if (actions == null || actions.isEmpty()) {
            Label vide = new Label("Aucune action pour l'instant...");
            vide.setStyle("-fx-font-size: 11px; -fx-text-fill: #999; -fx-font-style: italic;");
            journalBox.getChildren().add(vide);
            return;
        }
        for (SessionAction action : actions) {
            Label lbl = new Label(action.getResume());
            lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #444; -fx-wrap-text: true;");
            lbl.setWrapText(true);
            journalBox.getChildren().add(lbl);
        }
    }

    // ── Polling ───────────────────────────────────────────────────────────────

    private void demarrerPolling() {
        arreterPolling();
        refreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(5), e -> rafraichirStatut())
        );
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void arreterPolling() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
            refreshTimeline = null;
        }
    }

    /** Arrête le polling proprement (à appeler quand la vue est détruite). */
    public void dispose() {
        arreterPolling();
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private String nomEtudiant() {
        String nom = AppState.getUserDisplayName();
        return (nom == null || nom.isBlank()) ? "Étudiant #" + AppState.getUserId() : nom;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private void showInfo(String titre, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titre);
            alert.setHeaderText(titre);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
