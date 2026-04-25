package com.educompus.controller.back;

import com.educompus.model.Commande;
import com.educompus.model.Livraison;
import com.educompus.service.ServiceCommande;
import com.educompus.service.ServiceLivraison;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class BackCalendrierCommandesController {

    @FXML private GridPane grilleCalendrier;
    @FXML private GridPane headerJours;
    @FXML private Label    lblMoisAnnee;
    @FXML private Label    lblTotalMois;
    @FXML private ScrollPane mainScroll;
    @FXML private VBox     panneauDetail;
    @FXML private Label    lblDetailDate;
    @FXML private ListView<String> listeDetailCommandes;

    private final ServiceCommande  serviceCommande  = new ServiceCommande();
    private final ServiceLivraison serviceLivraison = new ServiceLivraison();

    private YearMonth moisCourant = YearMonth.now();

    // { date → liste de commandes }
    private Map<LocalDate, List<Commande>> commandesParJour = new HashMap<>();
    // { commande_id → statut livraison }
    private Map<Integer, String> statutsLivraison = new HashMap<>();

    private static final DateTimeFormatter FMT_TITRE =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter FMT_DETAIL =
            DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter FMT_HEURE =
            DateTimeFormatter.ofPattern("HH:mm");

    // ── Init ─────────────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        construireHeaderJours();
        chargerDonnees();
    }

    private void construireHeaderJours() {
        String[] jours = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (int i = 0; i < 7; i++) {
            Label lbl = new Label(jours[i]);
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);
            lbl.setStyle("-fx-font-weight: 800; -fx-font-size: 11px; -fx-text-fill: -edu-text-muted;");
            GridPane.setHgrow(lbl, Priority.ALWAYS);
            headerJours.add(lbl, i, 0);
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setPercentWidth(100.0 / 7);
            if (headerJours.getColumnConstraints().size() <= i)
                headerJours.getColumnConstraints().add(cc);
        }
    }

    // ── Chargement ────────────────────────────────────────────────────────────

    private void chargerDonnees() {
        try {
            commandesParJour.clear();
            statutsLivraison.clear();

            List<Commande> toutes = serviceCommande.afficherAll();
            for (Commande c : toutes) {
                if (c.getDateCommande() != null) {
                    LocalDate date = c.getDateCommande().toLocalDate();
                    commandesParJour.computeIfAbsent(date, k -> new ArrayList<>()).add(c);
                }
            }

            List<Livraison> livraisons = serviceLivraison.afficherAll();
            for (Livraison l : livraisons) {
                if (l.getCommandeId() != null)
                    statutsLivraison.put(l.getCommandeId(), l.getStatusLivraison());
            }

            afficherCalendrier();
        } catch (Exception e) {
            System.err.println("[Calendrier] Erreur : " + e.getMessage());
        }
    }

    // ── Affichage calendrier ──────────────────────────────────────────────────

    private void afficherCalendrier() {
        grilleCalendrier.getChildren().clear();
        grilleCalendrier.getColumnConstraints().clear();
        grilleCalendrier.getRowConstraints().clear();

        // Colonnes égales
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setPercentWidth(100.0 / 7);
            grilleCalendrier.getColumnConstraints().add(cc);
        }

        lblMoisAnnee.setText(moisCourant.format(FMT_TITRE).substring(0, 1).toUpperCase()
                + moisCourant.format(FMT_TITRE).substring(1));

        LocalDate premierJour = moisCourant.atDay(1);
        int decalage = premierJour.getDayOfWeek().getValue() - 1; // Lundi = 0
        int nbJours  = moisCourant.lengthOfMonth();
        int nbLignes = (int) Math.ceil((decalage + nbJours) / 7.0);

        // Lignes de hauteur égale
        for (int r = 0; r < nbLignes; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            rc.setMinHeight(80);
            grilleCalendrier.getRowConstraints().add(rc);
        }

        int totalMois = 0;
        for (int jour = 1; jour <= nbJours; jour++) {
            LocalDate date = moisCourant.atDay(jour);
            int pos  = decalage + jour - 1;
            int col  = pos % 7;
            int row  = pos / 7;

            List<Commande> cmds = commandesParJour.getOrDefault(date, Collections.emptyList());
            totalMois += cmds.size();

            VBox cellule = buildCellule(date, cmds);
            grilleCalendrier.add(cellule, col, row);
        }

        // Compter total du mois
        lblTotalMois.setText(totalMois + " commande" + (totalMois > 1 ? "s" : "") + " ce mois");
        panneauDetail.setVisible(false);
        panneauDetail.setManaged(false);
    }

    private VBox buildCellule(LocalDate date, List<Commande> cmds) {
        VBox cell = new VBox(3);
        cell.setPadding(new Insets(6, 6, 6, 6));
        cell.setMaxWidth(Double.MAX_VALUE);
        cell.setMaxHeight(Double.MAX_VALUE);

        boolean estAujourdhui = date.equals(LocalDate.now());
        boolean estWeekend    = date.getDayOfWeek() == DayOfWeek.SATURDAY
                             || date.getDayOfWeek() == DayOfWeek.SUNDAY;

        String bgColor = estAujourdhui ? "rgba(6,106,201,0.08)"
                       : estWeekend    ? "rgba(0,0,0,0.02)"
                       : "transparent";

        cell.setStyle("-fx-background-color: " + bgColor + ";" +
                "-fx-border-color: -edu-border; -fx-border-width: 0.5;" +
                (cmds.isEmpty() ? "" : "-fx-cursor: hand;"));

        // Numéro du jour
        Label lblJour = new Label(String.valueOf(date.getDayOfMonth()));
        lblJour.setStyle(estAujourdhui
                ? "-fx-font-weight: 900; -fx-font-size: 12px; -fx-text-fill: #2563eb;" +
                  "-fx-background-color: #2563eb; -fx-text-fill: white;" +
                  "-fx-background-radius: 999px; -fx-min-width: 22px; -fx-min-height: 22px;" +
                  "-fx-alignment: CENTER; -fx-padding: 2;"
                : "-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " +
                  (estWeekend ? "#94a3b8" : "-edu-text") + ";");
        cell.getChildren().add(lblJour);

        // Pastilles de commandes (max 3 visibles)
        if (!cmds.isEmpty()) {
            VBox pastilles = new VBox(2);
            int affichees = Math.min(cmds.size(), 3);
            for (int i = 0; i < affichees; i++) {
                Commande c = cmds.get(i);
                String statut = statutsLivraison.getOrDefault(c.getId(), "en_attente");
                Label pastille = new Label(String.format("%.0f TND", c.getTotal()));
                pastille.setMaxWidth(Double.MAX_VALUE);
                pastille.setStyle("-fx-background-color: " + couleurStatut(statut) + ";" +
                        "-fx-text-fill: white; -fx-background-radius: 4px;" +
                        "-fx-padding: 1 4 1 4; -fx-font-size: 9px; -fx-font-weight: 700;");
                pastilles.getChildren().add(pastille);
            }
            if (cmds.size() > 3) {
                Label plus = new Label("+" + (cmds.size() - 3) + " autres");
                plus.setStyle("-fx-font-size: 8px; -fx-text-fill: #94a3b8;");
                pastilles.getChildren().add(plus);
            }
            cell.getChildren().add(pastilles);

            // Clic → afficher détail
            final LocalDate dateFinal = date;
            cell.setOnMouseClicked(e -> afficherDetail(dateFinal, cmds));
        }

        return cell;
    }

    // ── Détail du jour ────────────────────────────────────────────────────────

    private void afficherDetail(LocalDate date, List<Commande> cmds) {
        lblDetailDate.setText(date.format(FMT_DETAIL).substring(0, 1).toUpperCase()
                + date.format(FMT_DETAIL).substring(1)
                + " — " + cmds.size() + " commande" + (cmds.size() > 1 ? "s" : ""));

        listeDetailCommandes.getItems().clear();
        listeDetailCommandes.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label lbl = new Label(item);
                lbl.setWrapText(true);
                lbl.setStyle("-fx-font-size: 12px;");
                setGraphic(lbl);
            }
        });

        for (Commande c : cmds) {
            String statut = statutsLivraison.getOrDefault(c.getId(), "en_attente");
            String emoji  = emojiStatut(statut);
            String heure  = c.getDateCommande() != null
                    ? c.getDateCommande().format(FMT_HEURE) : "—";
            listeDetailCommandes.getItems().add(
                    emoji + "  Commande #" + c.getId()
                    + "  |  " + String.format("%.2f TND", c.getTotal())
                    + "  |  " + heure
                    + "  |  " + statut.replace("_", " "));
        }

        panneauDetail.setVisible(true);
        panneauDetail.setManaged(true);

        // Scroller automatiquement vers le panneau détail
        javafx.application.Platform.runLater(() -> {
            if (mainScroll != null) mainScroll.setVvalue(1.0);
        });
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML private void onMoisPrecedent(ActionEvent e) { moisCourant = moisCourant.minusMonths(1); afficherCalendrier(); }
    @FXML private void onMoisSuivant(ActionEvent e)   { moisCourant = moisCourant.plusMonths(1);  afficherCalendrier(); }
    @FXML private void onAujourdhui(ActionEvent e)    { moisCourant = YearMonth.now();             afficherCalendrier(); }
    @FXML private void onActualiser(ActionEvent e)    { chargerDonnees(); }
    @FXML private void onFermerDetail(ActionEvent e)  {
        panneauDetail.setVisible(false);
        panneauDetail.setManaged(false);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String couleurStatut(String statut) {
        return switch (statut) {
            case "livree"   -> "#10b981";
            case "expediee" -> "#3b82f6";
            case "annulee"  -> "#ef4444";
            default         -> "#f59e0b";
        };
    }

    private String emojiStatut(String statut) {
        return switch (statut) {
            case "livree"   -> "✅";
            case "expediee" -> "🚚";
            case "annulee"  -> "❌";
            default         -> "⏳";
        };
    }
}
