package com.educompus.controller.front;

import com.educompus.app.AppState;
import com.educompus.model.SessionLive;
import com.educompus.model.SessionStatut;
import com.educompus.repository.SessionLiveRepository;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
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
 * Contrôleur du calendrier côté étudiant.
 * Affiche les sessions live auxquelles l'étudiant peut participer.
 */
public final class FrontCalendarController {

    @FXML private Label pageTitle;
    @FXML private Label pageSubtitle;
    @FXML private VBox calendarContainer;
    @FXML private VBox upcomingSessionsBox;

    private final SessionLiveRepository sessionRepo = new SessionLiveRepository();
    private YearMonth moisCourant = YearMonth.now();
    private GridPane grille;
    private Label moisLabel;

    private static final String[] JOURS = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};

    @FXML
    private void initialize() {
        if (pageTitle != null) {
            pageTitle.setText("📅 Mon Calendrier");
        }
        if (pageSubtitle != null) {
            pageSubtitle.setText("Consultez vos sessions live et événements à venir");
        }

        buildCalendar();
        chargerMois();
        afficherSessionsAVenir();
    }

    /**
     * Construit l'interface du calendrier.
     */
    private void buildCalendar() {
        if (calendarContainer == null) return;

        VBox calendarBox = new VBox(16);
        calendarBox.getStyleClass().add("card");
        calendarBox.setPadding(new Insets(20));

        // ── En-tête avec navigation ──
        HBox nav = new HBox(12);
        nav.setAlignment(Pos.CENTER_LEFT);

        Button prevBtn = new Button("◀");
        prevBtn.getStyleClass().add("btn-rgb-outline");
        prevBtn.setStyle("-fx-min-width: 40px; -fx-min-height: 40px;");
        prevBtn.setOnAction(e -> {
            moisCourant = moisCourant.minusMonths(1);
            chargerMois();
        });

        Button nextBtn = new Button("▶");
        nextBtn.getStyleClass().add("btn-rgb-outline");
        nextBtn.setStyle("-fx-min-width: 40px; -fx-min-height: 40px;");
        nextBtn.setOnAction(e -> {
            moisCourant = moisCourant.plusMonths(1);
            chargerMois();
        });

        Button todayBtn = new Button("Aujourd'hui");
        todayBtn.getStyleClass().add("btn-rgb");
        todayBtn.setOnAction(e -> {
            moisCourant = YearMonth.now();
            chargerMois();
        });

        moisLabel = new Label();
        moisLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: -edu-primary;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        nav.getChildren().addAll(prevBtn, nextBtn, moisLabel, spacer, todayBtn);

        // ── En-têtes des jours ──
        HBox jourHeaders = new HBox(0);
        jourHeaders.setStyle("-fx-background-color: rgba(6,106,201,0.08); -fx-background-radius: 8px; -fx-padding: 10 0 10 0;");
        for (String jour : JOURS) {
            Label lbl = new Label(jour);
            lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: -edu-primary; -fx-alignment: center;");
            lbl.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(lbl, Priority.ALWAYS);
            lbl.setAlignment(Pos.CENTER);
            jourHeaders.getChildren().add(lbl);
        }

        // ── Grille des jours ──
        grille = new GridPane();
        grille.setHgap(6);
        grille.setVgap(6);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setMinWidth(100);
            grille.getColumnConstraints().add(cc);
        }

        calendarBox.getChildren().addAll(nav, jourHeaders, grille);
        calendarContainer.getChildren().add(calendarBox);
    }

    /**
     * Charge et affiche le mois courant.
     */
    private void chargerMois() {
        if (grille == null) return;
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
            if (col == 7) {
                col = 0;
                row++;
            }
        }
    }

    /**
     * Construit une cellule du calendrier pour un jour donné.
     */
    private VBox buildCellule(int jour, LocalDate date, List<SessionLive> sessions, LocalDate today) {
        VBox cell = new VBox(4);
        cell.setPadding(new Insets(8));
        cell.setMinHeight(90);
        cell.setMaxHeight(120);

        boolean isToday = date.equals(today);
        boolean hasSessions = !sessions.isEmpty();

        // Style de la cellule
        String bgColor = isToday ? "rgba(6,106,201,0.08)" : "white";
        String borderColor = isToday ? "-edu-primary" : "#e0e0e0";
        String borderWidth = isToday ? "2" : "1";

        cell.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-background-radius: 8px;" +
                        "-fx-border-color: " + borderColor + ";" +
                        "-fx-border-width: " + borderWidth + ";" +
                        "-fx-border-radius: 8px;"
        );

        // Numéro du jour
        Label numLabel = new Label(String.valueOf(jour));
        if (isToday) {
            numLabel.setStyle(
                    "-fx-background-color: -edu-primary; -fx-text-fill: white;" +
                            "-fx-font-size: 13px; -fx-font-weight: 800;" +
                            "-fx-background-radius: 50%; -fx-min-width: 26; -fx-min-height: 26;" +
                            "-fx-alignment: center;"
            );
            numLabel.setAlignment(Pos.CENTER);
        } else {
            numLabel.setStyle(
                    "-fx-font-size: 13px; -fx-font-weight: " + (hasSessions ? "700" : "600") + ";" +
                            "-fx-text-fill: " + (hasSessions ? "-edu-primary" : "-edu-text") + ";"
            );
        }

        cell.getChildren().add(numLabel);

        // Sessions du jour (max 2 affichées dans la cellule)
        int count = 0;
        for (SessionLive session : sessions) {
            if (count >= 2) {
                Label more = new Label("+" + (sessions.size() - 2) + " autre" + (sessions.size() - 2 > 1 ? "s" : ""));
                more.setStyle("-fx-font-size: 10px; -fx-text-fill: -edu-text-secondary; -fx-font-weight: 600;");
                cell.getChildren().add(more);
                break;
            }
            cell.getChildren().add(buildSessionChip(session));
            count++;
        }

        // Rendre la cellule cliquable si elle a des sessions
        if (hasSessions) {
            cell.setStyle(cell.getStyle() + "-fx-cursor: hand;");
            cell.setOnMouseClicked(e -> afficherDetailsSessions(date, sessions));
        }

        return cell;
    }

    /**
     * Construit un chip pour une session.
     */
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
            case EN_COURS -> {
                bg = "rgba(41,182,216,0.15)";
                fg = "#29b6d8";
            }
            case PLANIFIEE -> {
                bg = "rgba(6,106,201,0.12)";
                fg = "-edu-primary";
            }
            case TERMINEE -> {
                bg = "rgba(150,150,150,0.12)";
                fg = "#888";
            }
            default -> {
                bg = "rgba(214,41,62,0.12)";
                fg = "#d6293e";
            }
        }

        // Badge sync
        String syncIcon = session.estSynchroniseeCalendar() ? "📅 " : "";

        chip.setText(syncIcon + heure + safe(session.getNomCours()));
        chip.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-text-fill: " + fg + ";" +
                        "-fx-font-size: 10px; -fx-font-weight: 700;" +
                        "-fx-background-radius: 4px; -fx-padding: 3 6 3 6;" +
                        "-fx-cursor: hand;"
        );

        // Tooltip avec détails
        Tooltip tip = new Tooltip(
                session.getStatut().icone() + " " + session.getLibelleStatut() + "\n" +
                        "📚 " + safe(session.getNomCours()) + "\n" +
                        "🕐 " + session.getDateHeureFormatee() + "\n" +
                        (session.estSynchroniseeCalendar() ? "✅ Synchronisé Google Calendar" : "")
        );
        Tooltip.install(chip, tip);

        return chip;
    }

    /**
     * Affiche les sessions à venir dans la section dédiée.
     */
    private void afficherSessionsAVenir() {
        if (upcomingSessionsBox == null) return;
        upcomingSessionsBox.getChildren().clear();

        LocalDate today = LocalDate.now();
        LocalDate dans7Jours = today.plusDays(7);

        List<SessionLive> sessionsAVenir = sessionRepo.getAllSessions().stream()
                .filter(s -> s.getDate() != null &&
                        !s.getDate().isBefore(today) &&
                        !s.getDate().isAfter(dans7Jours))
                .sorted((s1, s2) -> {
                    int dateComp = s1.getDate().compareTo(s2.getDate());
                    if (dateComp != 0) return dateComp;
                    if (s1.getHeure() == null) return 1;
                    if (s2.getHeure() == null) return -1;
                    return s1.getHeure().compareTo(s2.getHeure());
                })
                .collect(Collectors.toList());

        if (sessionsAVenir.isEmpty()) {
            Label empty = new Label("Aucune session prévue dans les 7 prochains jours");
            empty.getStyleClass().add("page-subtitle");
            empty.setStyle("-fx-text-align: center; -fx-padding: 20px;");
            upcomingSessionsBox.getChildren().add(empty);
            return;
        }

        for (SessionLive session : sessionsAVenir) {
            upcomingSessionsBox.getChildren().add(buildSessionCard(session));
        }
    }

    /**
     * Construit une carte pour une session à venir.
     */
    private VBox buildSessionCard(SessionLive session) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setStyle("-fx-cursor: hand;");

        // En-tête avec statut
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label statusIcon = new Label(session.getStatut().icone());
        statusIcon.setStyle("-fx-font-size: 20px;");

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label titre = new Label(safe(session.getNomCours()));
        titre.getStyleClass().add("resource-name");
        titre.setStyle("-fx-font-size: 16px; -fx-font-weight: 700;");

        Label dateHeure = new Label("🕐 " + session.getDateHeureFormatee());
        dateHeure.getStyleClass().add("page-subtitle");

        info.getChildren().addAll(titre, dateHeure);

        // Badge statut
        Label statusBadge = new Label(session.getLibelleStatut());
        statusBadge.getStyleClass().add("chip");
        switch (session.getStatut()) {
            case EN_COURS -> statusBadge.getStyleClass().add("chip-success");
            case PLANIFIEE -> statusBadge.getStyleClass().add("chip-info");
            case TERMINEE -> statusBadge.getStyleClass().add("chip-secondary");
            default -> statusBadge.getStyleClass().add("chip-danger");
        }

        header.getChildren().addAll(statusIcon, info, statusBadge);

        // Boutons d'action
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);

        if (session.getStatut() == SessionStatut.EN_COURS && session.aLienValide()) {
            Button joinBtn = new Button("🔴 Rejoindre la session");
            joinBtn.getStyleClass().add("btn-rgb");
            joinBtn.setStyle("-fx-background-color: #29b6d8; -fx-font-weight: 700;");
            joinBtn.setOnAction(e -> ouvrirSession(session));
            actions.getChildren().add(joinBtn);
        } else if (session.getStatut() == SessionStatut.PLANIFIEE) {
            Button detailsBtn = new Button("📋 Détails");
            detailsBtn.getStyleClass().add("btn-rgb-outline");
            detailsBtn.setOnAction(e -> afficherDetailsSession(session));
            actions.getChildren().add(detailsBtn);
        }

        // Badge Google Calendar
        if (session.estSynchroniseeCalendar()) {
            Label calBadge = new Label("📅 Dans Google Calendar");
            calBadge.getStyleClass().addAll("chip", "chip-success");
            calBadge.setStyle("-fx-font-size: 10px;");
            actions.getChildren().add(calBadge);
        }

        card.getChildren().addAll(header, actions);

        // Clic sur la carte
        card.setOnMouseClicked(e -> {
            if (session.getStatut() == SessionStatut.EN_COURS && session.aLienValide()) {
                ouvrirSession(session);
            } else {
                afficherDetailsSession(session);
            }
        });

        return card;
    }

    /**
     * Ouvre une session live dans le navigateur.
     */
    private void ouvrirSession(SessionLive session) {
        if (!session.aLienValide()) return;
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(session.getLien()));
        } catch (Exception e) {
            e.printStackTrace();
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR
            );
            alert.setTitle("Erreur");
            alert.setHeaderText("Impossible d'ouvrir la session");
            alert.setContentText("Lien : " + session.getLien() + "\n\nErreur : " + e.getMessage());
            alert.showAndWait();
        }
    }

    /**
     * Affiche les détails d'une session dans un dialogue.
     */
    private void afficherDetailsSession(SessionLive session) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION
        );
        alert.setTitle("Détails de la session");
        alert.setHeaderText(session.getStatut().icone() + " " + safe(session.getNomCours()));

        StringBuilder content = new StringBuilder();
        content.append("📅 Date : ").append(session.getDate()).append("\n");
        content.append("🕐 Heure : ").append(session.getHeure()).append("\n");
        content.append("📊 Statut : ").append(session.getLibelleStatut()).append("\n");
        if (session.aLienValide()) {
            content.append("🔗 Lien : ").append(session.getLien()).append("\n");
        }
        if (session.estSynchroniseeCalendar()) {
            content.append("\n✅ Cette session est synchronisée avec Google Calendar");
        }

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    /**
     * Affiche les détails des sessions d'un jour donné.
     */
    private void afficherDetailsSessions(LocalDate date, List<SessionLive> sessions) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION
        );
        alert.setTitle("Sessions du " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        alert.setHeaderText(sessions.size() + " session" + (sessions.size() > 1 ? "s" : "") + " prévue" + (sessions.size() > 1 ? "s" : ""));

        StringBuilder content = new StringBuilder();
        for (SessionLive session : sessions) {
            content.append(session.getStatut().icone()).append(" ");
            content.append(safe(session.getNomCours())).append("\n");
            content.append("   🕐 ").append(session.getHeure()).append("\n");
            content.append("   📊 ").append(session.getLibelleStatut()).append("\n");
            if (session.aLienValide()) {
                content.append("   🔗 ").append(session.getLien()).append("\n");
            }
            content.append("\n");
        }

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
