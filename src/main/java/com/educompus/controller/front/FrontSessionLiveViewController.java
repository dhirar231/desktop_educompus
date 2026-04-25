package com.educompus.controller.front;

import com.educompus.model.SessionLive;
import com.educompus.model.SessionStatut;
import com.educompus.repository.SessionLiveRepository;
import com.educompus.service.SessionLiveMetierService;
import com.educompus.util.SessionLiveErrorHandler;
import com.educompus.nav.Navigator;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Contrôleur de la vue dédiée Sessions Live (FrontSessionLive.fxml).
 * Affiche toutes les sessions d'un cours avec filtres et interactions.
 */
public final class FrontSessionLiveViewController {

    @FXML private Label   titreCoursLabel;
    @FXML private Label   sousTitreLabel;
    @FXML private Label   badgeEnCours;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statutFilterCombo;
    @FXML private VBox    sessionsContainer;
    @FXML private VBox    emptyState;
    @FXML private Label   statPlanifieeLabel;
    @FXML private Label   statEnCoursLabel;
    @FXML private Label   statTermineeLabel;

    private final SessionLiveRepository    sessionRepo   = new SessionLiveRepository();
    private final SessionLiveMetierService metierService = new SessionLiveMetierService();

    private int    coursId;
    private String coursTitre;

    @FXML
    private void initialize() {
        // Filtres
        if (statutFilterCombo != null) {
            statutFilterCombo.getItems().setAll("Tous", "📅 Planifiée", "🔴 En cours", "✅ Terminée");
            statutFilterCombo.setValue("Tous");
            statutFilterCombo.valueProperty().addListener((obs, o, n) -> chargerSessions());
        }
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> chargerSessions());
        }
    }

    /**
     * Initialise la vue avec le cours sélectionné.
     */
    public void setCours(int coursId, String coursTitre) {
        this.coursId    = coursId;
        this.coursTitre = coursTitre;
        if (titreCoursLabel != null) titreCoursLabel.setText("Sessions — " + safe(coursTitre));
        if (sousTitreLabel  != null) sousTitreLabel.setText("Sessions live du cours : " + safe(coursTitre));
        chargerSessions();
    }

    @FXML
    private void onBack() {
        Navigator.goRoot("View/front/FrontCourses.fxml");
    }

    // ── Chargement ────────────────────────────────────────────────────────────

    private void chargerSessions() {
        List<SessionLive> sessions = sessionRepo.getSessionsByCoursId(coursId);

        // Filtre statut
        String filtre = statutFilterCombo != null ? safe(statutFilterCombo.getValue()) : "Tous";
        if (!filtre.equals("Tous") && !filtre.isBlank()) {
            SessionStatut s = null;
            if (filtre.contains("Planifiée")) s = SessionStatut.PLANIFIEE;
            else if (filtre.contains("En cours")) s = SessionStatut.EN_COURS;
            else if (filtre.contains("Terminée")) s = SessionStatut.TERMINEE;
            if (s != null) {
                final SessionStatut fs = s;
                sessions = sessions.stream().filter(sess -> sess.getStatut() == fs).collect(Collectors.toList());
            }
        }

        // Filtre recherche
        if (searchField != null && !searchField.getText().isBlank()) {
            String q = searchField.getText().trim().toLowerCase();
            sessions = sessions.stream()
                .filter(sess -> safe(sess.getNomCours()).toLowerCase().contains(q))
                .collect(Collectors.toList());
        }

        // Compteurs
        long planifiees = sessions.stream().filter(s -> s.getStatut() == SessionStatut.PLANIFIEE).count();
        long enCours    = sessions.stream().filter(s -> s.getStatut() == SessionStatut.EN_COURS).count();
        long terminees  = sessions.stream().filter(s -> s.getStatut() == SessionStatut.TERMINEE).count();

        if (statPlanifieeLabel != null) statPlanifieeLabel.setText("📅 " + planifiees + " planifiée(s)");
        if (statEnCoursLabel   != null) statEnCoursLabel.setText("🔴 " + enCours + " en cours");
        if (statTermineeLabel  != null) statTermineeLabel.setText("✅ " + terminees + " terminée(s)");

        // Badge EN COURS
        if (badgeEnCours != null) {
            badgeEnCours.setVisible(enCours > 0);
            badgeEnCours.setManaged(enCours > 0);
        }

        // Construire les cartes
        if (sessionsContainer != null) {
            sessionsContainer.getChildren().clear();
            if (sessions.isEmpty()) {
                if (emptyState != null) { emptyState.setVisible(true); emptyState.setManaged(true); }
            } else {
                if (emptyState != null) { emptyState.setVisible(false); emptyState.setManaged(false); }
                for (SessionLive session : sessions) {
                    sessionsContainer.getChildren().add(buildSessionCard(session));
                }
            }
        }
    }

    // ── Construction des cartes ───────────────────────────────────────────────

    private VBox buildSessionCard(SessionLive session) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16, 18, 16, 18));

        boolean enCours = session.getStatut() == SessionStatut.EN_COURS;
        if (enCours) {
            card.setStyle("-fx-border-color: #29b6d8; -fx-border-width: 2; -fx-border-radius: 12px;");
        }

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
        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nomLbl = new Label(safe(session.getNomCours()));
        nomLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #202124;");

        Label dateLbl = new Label("📅  " + session.getDateHeureFormatee());
        dateLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        Label lienLbl = new Label("🔗  " + safe(session.getLien()));
        lienLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #29b6d8;");
        lienLbl.setWrapText(false);

        info.getChildren().addAll(nomLbl, dateLbl, lienLbl);

        // Bouton rejoindre
        Button joinBtn = buildJoinButton(session);

        row.getChildren().addAll(statutBadge, info, joinBtn);
        card.getChildren().add(row);

        // ── Message interactif si EN_COURS ──
        if (enCours) {
            Label interactifLbl = new Label(
                "✅  Session interactive disponible — Lever la main, chat et audio disponibles après connexion.");
            interactifLbl.setStyle(
                "-fx-font-size: 10px; -fx-text-fill: #29b6d8; -fx-wrap-text: true;" +
                "-fx-background-color: rgba(41,182,216,0.08); -fx-background-radius: 6px; -fx-padding: 6 10 6 10;");
            interactifLbl.setWrapText(true);
            card.getChildren().add(interactifLbl);
        }

        return card;
    }

    private Button buildJoinButton(SessionLive session) {
        if (session.getStatut() == SessionStatut.EN_COURS) {
            Button btn = new Button("🔗  Rejoindre la session");
            btn.getStyleClass().add("btn-rgb");
            btn.setStyle("-fx-font-size: 13px; -fx-font-weight: 700;");
            btn.setOnAction(e -> rejoindreSession(session));
            return btn;
        } else if (session.getStatut() == SessionStatut.PLANIFIEE) {
            Button btn = new Button("📅  Planifiée");
            btn.setStyle(
                "-fx-background-color: rgba(41,182,216,0.1); -fx-text-fill: #29b6d8;" +
                "-fx-border-color: #29b6d8; -fx-border-width: 1; -fx-border-radius: 8px;" +
                "-fx-background-radius: 8px; -fx-font-size: 12px;");
            btn.setDisable(true);
            return btn;
        } else {
            Button btn = new Button(session.getStatut() == SessionStatut.TERMINEE ? "✅  Terminée" : "❌  Annulée");
            btn.setStyle(
                "-fx-background-color: rgba(150,150,150,0.1); -fx-text-fill: #999;" +
                "-fx-border-color: #ccc; -fx-border-width: 1; -fx-border-radius: 8px;" +
                "-fx-background-radius: 8px; -fx-font-size: 12px;");
            btn.setDisable(true);
            return btn;
        }
    }

    private void rejoindreSession(SessionLive session) {
        SessionLiveErrorHandler.ouvrirLienAvecFallback(session.getLien());
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }
}
