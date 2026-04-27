package com.educompus.controller.back;

import com.educompus.model.Commande;
import com.educompus.service.ServiceCommande;
import com.educompus.service.ServiceLivraison;
import com.educompus.service.TodoService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BackCalendrierCommandesController {

    @FXML private GridPane   grilleCalendrier;
    @FXML private GridPane   headerJours;
    @FXML private Label      lblMoisAnnee;
    @FXML private Label      lblTotalMois;
    @FXML private ScrollPane mainScroll;
    @FXML private VBox       panneauDetail;
    @FXML private Label      lblDetailDate;
    @FXML private ListView<String> listeDetailCommandes;

    private final ServiceCommande  serviceCommande  = new ServiceCommande();
    private final ServiceLivraison serviceLivraison = new ServiceLivraison();
    private final TodoService      todoService      = new TodoService();

    private YearMonth moisCourant = YearMonth.now();
    private final Map<LocalDate, List<Commande>> commandesParJour = new HashMap<>();
    private final Map<Integer, String>           statutsLivraison = new HashMap<>();

    private LocalDate              jourSelectionne;
    private List<TodoService.Tache> tachesJour = new ArrayList<>();
    private VBox                   todoBox;

    private static final DateTimeFormatter FMT_TITRE  = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter FMT_DETAIL = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter FMT_HEURE  = DateTimeFormatter.ofPattern("HH:mm");

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
            serviceCommande.afficherAll().forEach(c -> {
                if (c.getDateCommande() != null)
                    commandesParJour.computeIfAbsent(c.getDateCommande().toLocalDate(), k -> new ArrayList<>()).add(c);
            });
            serviceLivraison.afficherAll().forEach(l -> {
                if (l.getCommandeId() != null) statutsLivraison.put(l.getCommandeId(), l.getStatusLivraison());
            });
            afficherCalendrier();
        } catch (Exception e) {
            System.err.println("[Calendrier] Erreur : " + e.getMessage());
        }
    }

    // ── Calendrier ────────────────────────────────────────────────────────────

    private void afficherCalendrier() {
        grilleCalendrier.getChildren().clear();
        grilleCalendrier.getColumnConstraints().clear();
        grilleCalendrier.getRowConstraints().clear();

        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setPercentWidth(100.0 / 7);
            grilleCalendrier.getColumnConstraints().add(cc);
        }

        String titre = moisCourant.format(FMT_TITRE);
        lblMoisAnnee.setText(titre.substring(0, 1).toUpperCase() + titre.substring(1));

        LocalDate premierJour = moisCourant.atDay(1);
        int decalage = premierJour.getDayOfWeek().getValue() - 1;
        int nbJours  = moisCourant.lengthOfMonth();
        int nbLignes = (int) Math.ceil((decalage + nbJours) / 7.0);

        for (int r = 0; r < nbLignes; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            rc.setMinHeight(80);
            grilleCalendrier.getRowConstraints().add(rc);
        }

        int totalMois = 0;
        for (int jour = 1; jour <= nbJours; jour++) {
            LocalDate date = moisCourant.atDay(jour);
            int pos = decalage + jour - 1;
            List<Commande> cmds = commandesParJour.getOrDefault(date, Collections.emptyList());
            totalMois += cmds.size();
            grilleCalendrier.add(buildCellule(date, cmds), pos % 7, pos / 7);
        }

        lblTotalMois.setText(totalMois + " commande" + (totalMois > 1 ? "s" : "") + " ce mois");
        panneauDetail.setVisible(false);
        panneauDetail.setManaged(false);
    }

    private VBox buildCellule(LocalDate date, List<Commande> cmds) {
        VBox cell = new VBox(3);
        cell.setPadding(new Insets(6));
        cell.setMaxWidth(Double.MAX_VALUE);
        cell.setMaxHeight(Double.MAX_VALUE);

        boolean estAujourdhui = date.equals(LocalDate.now());
        boolean estWeekend    = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;

        cell.setStyle("-fx-background-color: " + (estAujourdhui ? "rgba(6,106,201,0.08)" : estWeekend ? "rgba(0,0,0,0.02)" : "transparent") + ";" +
                "-fx-border-color: -edu-border; -fx-border-width: 0.5; -fx-cursor: hand;");

        Label lblJour = new Label(String.valueOf(date.getDayOfMonth()));
        lblJour.setStyle(estAujourdhui
                ? "-fx-font-weight: 900; -fx-font-size: 12px; -fx-background-color: #2563eb; -fx-text-fill: white;" +
                  "-fx-background-radius: 999px; -fx-min-width: 22px; -fx-min-height: 22px; -fx-alignment: CENTER; -fx-padding: 2;"
                : "-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " + (estWeekend ? "#94a3b8" : "-edu-text") + ";");
        cell.getChildren().add(lblJour);

        if (!cmds.isEmpty()) {
            VBox pastilles = new VBox(2);
            for (int i = 0; i < Math.min(cmds.size(), 2); i++) {
                String statut = statutsLivraison.getOrDefault(cmds.get(i).getId(), "en_attente");
                Label p = new Label(String.format("%.0f TND", cmds.get(i).getTotal()));
                p.setMaxWidth(Double.MAX_VALUE);
                p.setStyle("-fx-background-color: " + couleurStatut(statut) + "; -fx-text-fill: white;" +
                        "-fx-background-radius: 4px; -fx-padding: 1 4 1 4; -fx-font-size: 9px; -fx-font-weight: 700;");
                pastilles.getChildren().add(p);
            }
            if (cmds.size() > 2) {
                Label plus = new Label("+" + (cmds.size() - 2));
                plus.setStyle("-fx-font-size: 8px; -fx-text-fill: #94a3b8;");
                pastilles.getChildren().add(plus);
            }
            cell.getChildren().add(pastilles);
        }

        // Indicateur todo
        List<TodoService.Tache> taches = todoService.charger(date);
        if (!taches.isEmpty()) {
            long restantes = taches.stream().filter(t -> !t.faite).count();
            Label ind = new Label("📝 " + restantes + "/" + taches.size());
            ind.setStyle("-fx-font-size: 8px; -fx-text-fill: #7c3aed; -fx-font-weight: 700;");
            cell.getChildren().add(ind);
        }

        cell.setOnMouseClicked(e -> afficherDetail(date, cmds));
        return cell;
    }

    // ── Détail + Todo ─────────────────────────────────────────────────────────

    private void afficherDetail(LocalDate date, List<Commande> cmds) {
        jourSelectionne = date;
        tachesJour = todoService.charger(date);

        String titreFmt = date.format(FMT_DETAIL);
        lblDetailDate.setText(titreFmt.substring(0, 1).toUpperCase() + titreFmt.substring(1)
                + " — " + cmds.size() + " commande" + (cmds.size() > 1 ? "s" : ""));

        listeDetailCommandes.getItems().clear();
        listeDetailCommandes.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label lbl = new Label(item); lbl.setWrapText(true); lbl.setStyle("-fx-font-size: 12px;");
                setGraphic(lbl);
            }
        });
        for (Commande c : cmds) {
            String statut = statutsLivraison.getOrDefault(c.getId(), "en_attente");
            String heure  = c.getDateCommande() != null ? c.getDateCommande().format(FMT_HEURE) : "—";
            listeDetailCommandes.getItems().add(
                    emojiStatut(statut) + "  #" + c.getId()
                    + "  |  " + String.format("%.2f TND", c.getTotal())
                    + "  |  " + heure + "  |  " + statut.replace("_", " "));
        }

        panneauDetail.getChildren().removeIf(n -> "todoSection".equals(n.getId()));
        VBox todoSection = buildTodoSection();
        todoSection.setId("todoSection");
        panneauDetail.getChildren().add(todoSection);

        panneauDetail.setVisible(true);
        panneauDetail.setManaged(true);
        javafx.application.Platform.runLater(() -> { if (mainScroll != null) mainScroll.setVvalue(1.0); });
    }

    private VBox buildTodoSection() {
        VBox section = new VBox(10);
        section.setPadding(new Insets(12, 0, 0, 0));

        Label titreTodo = new Label("📝  Todo du jour");
        titreTodo.setStyle("-fx-font-weight: 800; -fx-font-size: 13px; -fx-text-fill: #7c3aed;");
        section.getChildren().add(titreTodo);

        todoBox = new VBox(6);
        rafraichirTodoBox();
        section.getChildren().add(todoBox);

        TextField fieldTache = new TextField();
        fieldTache.setPromptText("Ajouter une tâche...");
        fieldTache.getStyleClass().add("field");

        Button btnAjouter = new Button("+ Ajouter");
        btnAjouter.getStyleClass().add("btn-rgb-compact");

        Runnable ajouterTache = () -> {
            String texte = fieldTache.getText().trim();
            if (texte.isBlank()) return;
            tachesJour.add(new TodoService.Tache(texte, false));
            todoService.sauvegarder(jourSelectionne, tachesJour);
            fieldTache.clear();
            rafraichirTodoBox();
        };

        btnAjouter.setOnAction(e -> ajouterTache.run());
        fieldTache.setOnAction(e -> ajouterTache.run());

        HBox saisie = new HBox(8, fieldTache, btnAjouter);
        HBox.setHgrow(fieldTache, Priority.ALWAYS);
        section.getChildren().add(saisie);
        return section;
    }

    private void rafraichirTodoBox() {
        if (todoBox == null) return;
        todoBox.getChildren().clear();

        if (tachesJour.isEmpty()) {
            Label vide = new Label("Aucune tâche pour ce jour.");
            vide.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-style: italic;");
            todoBox.getChildren().add(vide);
            return;
        }

        for (int i = 0; i < tachesJour.size(); i++) {
            final int idx = i;
            TodoService.Tache tache = tachesJour.get(i);

            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 10, 6, 10));
            row.setStyle("-fx-background-color: " + (tache.faite ? "rgba(16,185,129,0.08)" : "rgba(124,58,237,0.05)") + ";" +
                    "-fx-background-radius: 8px; -fx-border-color: " +
                    (tache.faite ? "rgba(16,185,129,0.2)" : "rgba(124,58,237,0.15)") +
                    "; -fx-border-radius: 8px; -fx-border-width: 1;");

            CheckBox cb = new CheckBox();
            cb.setSelected(tache.faite);
            cb.setOnAction(e -> {
                tache.faite = cb.isSelected();
                todoService.sauvegarder(jourSelectionne, tachesJour);
                rafraichirTodoBox();
            });

            Label lblTexte = new Label(tache.texte);
            lblTexte.setWrapText(true);
            HBox.setHgrow(lblTexte, Priority.ALWAYS);
            lblTexte.setStyle("-fx-font-size: 12px; -fx-text-fill: " +
                    (tache.faite ? "#94a3b8" : "#1e293b") + ";" +
                    (tache.faite ? "-fx-strikethrough: true;" : ""));

            Button btnSuppr = new Button("x");
            btnSuppr.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444;" +
                    "-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 0 4 0 4;");
            btnSuppr.setOnAction(e -> {
                tachesJour.remove(idx);
                todoService.sauvegarder(jourSelectionne, tachesJour);
                rafraichirTodoBox();
            });

            row.getChildren().addAll(cb, lblTexte, btnSuppr);
            todoBox.getChildren().add(row);
        }

        long faites = tachesJour.stream().filter(t -> t.faite).count();
        Label resume = new Label(faites + "/" + tachesJour.size() + " tache" +
                (tachesJour.size() > 1 ? "s" : "") + " completee" + (faites > 1 ? "s" : ""));
        resume.setStyle("-fx-font-size: 10px; -fx-text-fill: #7c3aed; -fx-font-weight: 700;");
        todoBox.getChildren().add(resume);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML private void onMoisPrecedent(ActionEvent e) { moisCourant = moisCourant.minusMonths(1); afficherCalendrier(); }
    @FXML private void onMoisSuivant(ActionEvent e)   { moisCourant = moisCourant.plusMonths(1);  afficherCalendrier(); }
    @FXML private void onAujourdhui(ActionEvent e)    { moisCourant = YearMonth.now();             afficherCalendrier(); }
    @FXML private void onActualiser(ActionEvent e)    { chargerDonnees(); }
    @FXML private void onFermerDetail(ActionEvent e)  { panneauDetail.setVisible(false); panneauDetail.setManaged(false); }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String couleurStatut(String s) {
        return switch (s) { case "livree" -> "#10b981"; case "expediee" -> "#3b82f6"; case "annulee" -> "#ef4444"; default -> "#f59e0b"; };
    }

    private String emojiStatut(String s) {
        return switch (s) { case "livree" -> "OK"; case "expediee" -> "En route"; case "annulee" -> "Annulee"; default -> "En attente"; };
    }
}
