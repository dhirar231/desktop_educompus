package com.educompus.controller.back;

import com.educompus.model.SessionLive;
import com.educompus.model.SessionStatut;
import com.educompus.repository.SessionLiveRepository;
import com.educompus.service.JcefBrowserService;
import com.educompus.service.SessionNotificationService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Vue Calendrier JavaFX — affiche les sessions live comme Google Calendar.
 *
 * Utilisation : instancier et appeler {@link #getRootNode()} pour l'insérer dans un conteneur.
 */
public final class BackCalendarController {

    private final SessionLiveRepository sessionRepo = new SessionLiveRepository();

    private YearMonth moisCourant = YearMonth.now();

    private final VBox rootBox       = new VBox(0);
    private final GridPane grille    = new GridPane();
    private final Label moisLabel    = new Label();

    private static final String[] JOURS = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};

    public BackCalendarController() {
        buildUI();
        chargerMois();
    }

    public VBox getRootNode() { return rootBox; }

    // ── Construction de l'interface ───────────────────────────────────────────

    private void buildUI() {
        rootBox.setStyle("-fx-background-color: white; -fx-background-radius: 14px;" +
                         "-fx-border-color: #e8eaed; -fx-border-radius: 14px; -fx-border-width: 1;");
        rootBox.setPadding(new Insets(16));
        rootBox.setSpacing(12);

        // ── Header navigation ──
        HBox nav = new HBox(12);
        nav.setAlignment(Pos.CENTER_LEFT);

        Button prevBtn = new Button("◀");
        prevBtn.setStyle(navBtnStyle());
        prevBtn.setOnAction(e -> { moisCourant = moisCourant.minusMonths(1); chargerMois(); });

        Button nextBtn = new Button("▶");
        nextBtn.setStyle(navBtnStyle());
        nextBtn.setOnAction(e -> { moisCourant = moisCourant.plusMonths(1); chargerMois(); });

        Button todayBtn = new Button("Aujourd'hui");
        todayBtn.setStyle(
            "-fx-background-color: #4a90d9; -fx-text-fill: white; -fx-font-weight: 700;" +
            "-fx-background-radius: 8px; -fx-padding: 5 12 5 12; -fx-cursor: hand; -fx-font-size: 11px;");
        todayBtn.setOnAction(e -> { moisCourant = YearMonth.now(); chargerMois(); });

        moisLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #202124;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        nav.getChildren().addAll(prevBtn, nextBtn, moisLabel, spacer, todayBtn);

        // ── En-têtes des jours ──
        HBox jourHeaders = new HBox(0);
        jourHeaders.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8px; -fx-padding: 6 0 6 0;");
        for (String jour : JOURS) {
            Label lbl = new Label(jour);
            lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #5f6368;" +
                         "-fx-alignment: center; -fx-min-width: 0;");
            lbl.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(lbl, Priority.ALWAYS);
            lbl.setAlignment(Pos.CENTER);
            jourHeaders.getChildren().add(lbl);
        }

        // ── Grille des jours ──
        grille.setHgap(4);
        grille.setVgap(4);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setMinWidth(80);
            grille.getColumnConstraints().add(cc);
        }

        rootBox.getChildren().addAll(nav, jourHeaders, grille);
    }

    // ── Chargement du mois ────────────────────────────────────────────────────

    private void chargerMois() {
        grille.getChildren().clear();

        // Mettre à jour le label du mois
        String nomMois = moisCourant.getMonth()
            .getDisplayName(TextStyle.FULL, Locale.FRENCH);
        moisLabel.setText(nomMois.substring(0, 1).toUpperCase() + nomMois.substring(1) +
                          " " + moisCourant.getYear());

        // Charger toutes les sessions du mois
        List<SessionLive> sessions = sessionRepo.getAllSessions().stream()
            .filter(s -> s.getDate() != null &&
                         YearMonth.from(s.getDate()).equals(moisCourant))
            .collect(Collectors.toList());

        // Premier jour du mois (1 = Lundi, 7 = Dimanche)
        LocalDate premierJour = moisCourant.atDay(1);
        int debutColonne = premierJour.getDayOfWeek().getValue() - 1; // 0-based

        int nbJours = moisCourant.lengthOfMonth();
        LocalDate today = LocalDate.now();

        int col = debutColonne;
        int row = 0;

        for (int jour = 1; jour <= nbJours; jour++) {
            LocalDate date = moisCourant.atDay(jour);
            List<SessionLive> sessionsJour = sessions.stream()
                .filter(s -> date.equals(s.getDate()))
                .collect(Collectors.toList());

            VBox cellule = buildCellule(jour, date, sessionsJour, today);
            grille.add(cellule, col, row);

            col++;
            if (col == 7) { col = 0; row++; }
        }
    }

    private VBox buildCellule(int jour, LocalDate date, List<SessionLive> sessions, LocalDate today) {
        VBox cell = new VBox(3);
        cell.setPadding(new Insets(6, 6, 6, 6));
        cell.setMinHeight(80);

        boolean isToday = date.equals(today);
        boolean hasSessions = !sessions.isEmpty();

        cell.setStyle(
            "-fx-background-color: " + (isToday ? "rgba(74,144,217,0.08)" : "white") + ";" +
            "-fx-background-radius: 8px;" +
            "-fx-border-color: " + (isToday ? "#4a90d9" : "#f0f0f0") + ";" +
            "-fx-border-width: " + (isToday ? "2" : "1") + ";" +
            "-fx-border-radius: 8px;"
        );

        // Numéro du jour
        Label numLabel = new Label(String.valueOf(jour));
        numLabel.setStyle(
            "-fx-font-size: 12px; -fx-font-weight: " + (isToday ? "800" : "600") + ";" +
            "-fx-text-fill: " + (isToday ? "#4a90d9" : "#333") + ";" +
            (isToday ? "-fx-background-color: #4a90d9; -fx-text-fill: white;" +
                       "-fx-background-radius: 50%; -fx-min-width: 22; -fx-min-height: 22;" +
                       "-fx-alignment: center;" : "")
        );
        if (isToday) numLabel.setAlignment(Pos.CENTER);

        cell.getChildren().add(numLabel);

        // Sessions du jour (max 3 affichées)
        int count = 0;
        for (SessionLive session : sessions) {
            if (count >= 3) {
                Label more = new Label("+" + (sessions.size() - 3) + " autres");
                more.setStyle("-fx-font-size: 9px; -fx-text-fill: #999;");
                cell.getChildren().add(more);
                break;
            }
            cell.getChildren().add(buildSessionChip(session));
            count++;
        }

        return cell;
    }

    private Label buildSessionChip(SessionLive session) {
        String heure = session.getHeure() != null
            ? session.getHeure().format(DateTimeFormatter.ofPattern("HH:mm")) + " "
            : "";

        Label chip = new Label(heure + safe(session.getNomCours()));
        chip.setMaxWidth(Double.MAX_VALUE);
        chip.setWrapText(false);
        chip.setEllipsisString("…");

        // Couleur selon statut
        String bg, fg;
        switch (session.getStatut()) {
            case EN_COURS  -> { bg = "rgba(41,182,216,0.15)";  fg = "#29b6d8"; }
            case PLANIFIEE -> { bg = "rgba(74,144,217,0.12)";  fg = "#4a90d9"; }
            case TERMINEE  -> { bg = "rgba(150,150,150,0.12)"; fg = "#888"; }
            default        -> { bg = "rgba(214,41,62,0.12)";   fg = "#d6293e"; }
        }

        // Badge sync
        String syncIcon = session.estSynchroniseeCalendar() ? "📅 " : "";

        chip.setText(syncIcon + heure + safe(session.getNomCours()));
        chip.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + fg + ";" +
            "-fx-font-size: 10px; -fx-font-weight: 700;" +
            "-fx-background-radius: 4px; -fx-padding: 2 5 2 5;" +
            "-fx-cursor: hand;"
        );

        // Tooltip avec détails
        javafx.scene.control.Tooltip tip = new javafx.scene.control.Tooltip(
            session.getStatut().icone() + " " + session.getLibelleStatut() + "\n" +
            "📚 " + safe(session.getNomCours()) + "\n" +
            "🕐 " + session.getDateHeureFormatee() + "\n" +
            "🔗 " + safe(session.getLien()) + "\n" +
            (session.estSynchroniseeCalendar() ? "✅ Synchronisé Google Calendar" : "⚠ Non synchronisé")
        );
        javafx.scene.control.Tooltip.install(chip, tip);

        chip.setOnMouseClicked(e -> {
            if (session.getStatut() == SessionStatut.EN_COURS && session.aLienValide()) {
                try {
                    JcefBrowserService.getInstance().openMeetingDialog("Session - " + safe(session.getNomCours()), session.getLien());
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });

        return chip;
    }

    // ── Utilitaires ───────────────────────────────────────────────────────────

    private static String navBtnStyle() {
        return "-fx-background-color: #f8f9fa; -fx-text-fill: #333; -fx-font-weight: 700;" +
               "-fx-background-radius: 8px; -fx-border-color: #e8eaed; -fx-border-radius: 8px;" +
               "-fx-border-width: 1; -fx-padding: 5 10 5 10; -fx-cursor: hand;";
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }
}
