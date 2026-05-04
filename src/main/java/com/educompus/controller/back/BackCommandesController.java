package com.educompus.controller.back;

import com.educompus.service.TwilioSmsService;
import com.educompus.model.Commande;
import com.educompus.model.LigneCommande;
import com.educompus.model.Livraison;
import com.educompus.service.ServiceCommande;
import com.educompus.service.ServiceLigneCommande;
import com.educompus.service.ServiceLivraison;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.layout.GridPane;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackCommandesController {

    @FXML private ListView<Commande>  listeCommandes;
    @FXML private TextField           searchField;
    @FXML private ComboBox<String>    filterStatut;
    @FXML private ComboBox<String>    sortCombo;
    @FXML private Label               lblCount;

    private final ServiceCommande      serviceCommande      = new ServiceCommande();
    private final ServiceLivraison     serviceLivraison     = new ServiceLivraison();
    private final ServiceLigneCommande serviceLigneCommande = new ServiceLigneCommande();

    private final ObservableList<Commande> data     = FXCollections.observableArrayList();
    private FilteredList<Commande>         filtered;
    private SortedList<Commande>           sorted;
    private final Map<Integer, Livraison>  livraisonsMap = new HashMap<>();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Init ─────────────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        filterStatut.getItems().addAll("en_attente", "expediee", "livree", "annulee");
        sortCombo.getItems().addAll(
                "Date récente → ancienne",
                "Date ancienne → récente",
                "Total croissant",
                "Total décroissant"
        );
        filtered = new FilteredList<>(data, c -> true);
        sorted   = new SortedList<>(filtered);
        listeCommandes.setItems(sorted);
        listeCommandes.setCellFactory(lv -> new CommandeCell());
        chargerDonnees();
    }

    // ── CellFactory ───────────────────────────────────────────────────────────

    private class CommandeCell extends ListCell<Commande> {
        private final VBox  root      = new VBox(6);
        private final HBox  headerRow = new HBox(10);
        private final HBox  livRow    = new HBox(10);
        private final Label lblDate   = new Label();
        private final Label lblTotal  = new Label();
        private final Label lblStatut = new Label();
        private final Label lblAdresse= new Label();
        private final Label lblPhone  = new Label();
        private final Button btnDetail = new Button("👁  Détail");

        CommandeCell() {
            root.setPadding(new Insets(12, 16, 12, 16));
            root.setStyle("-fx-border-color: transparent transparent -edu-border transparent;" +
                          "-fx-border-width: 0 0 1 0;");

            lblDate.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: -edu-text;");
            lblTotal.setStyle("-fx-font-weight: 900; -fx-font-size: 14px; -fx-text-fill: -edu-primary;");
            lblAdresse.getStyleClass().add("page-subtitle");
            lblAdresse.setStyle("-fx-font-size: 11px;");
            lblPhone.getStyleClass().add("page-subtitle");
            lblPhone.setStyle("-fx-font-size: 11px;");

            Region sp1 = new Region(); HBox.setHgrow(sp1, Priority.ALWAYS);
            Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);

            btnDetail.getStyleClass().add("btn-ghost");
            btnDetail.setOnAction(e -> ouvrirDetail(getItem()));

            headerRow.setAlignment(Pos.CENTER_LEFT);
            headerRow.getChildren().addAll(lblDate, sp1, lblStatut, lblTotal, btnDetail);

            livRow.setAlignment(Pos.CENTER_LEFT);
            livRow.getChildren().addAll(lblAdresse, sp2, lblPhone);

            root.getChildren().addAll(headerRow, livRow);
        }

        @Override
        protected void updateItem(Commande cmd, boolean empty) {
            super.updateItem(cmd, empty);
            if (empty || cmd == null) { setGraphic(null); return; }

            lblDate.setText("📅  " + (cmd.getDateCommande() != null
                    ? cmd.getDateCommande().format(FMT) : "—"));
            lblTotal.setText(String.format("%.2f TND", cmd.getTotal()));

            Livraison liv = livraisonsMap.get(cmd.getId());
            if (liv != null) {
                lblAdresse.setText("📍  " + liv.getAdresse() + ", " + liv.getVille());
                lblPhone.setText("📞  " + (liv.getPhoneNumber() != null ? liv.getPhoneNumber() : "—"));
                styleStatut(liv.getStatusLivraison());
            } else {
                lblAdresse.setText("Aucune livraison");
                lblPhone.setText("");
                styleStatut("—");
            }
            setGraphic(root);
        }

        private void styleStatut(String statut) {
            lblStatut.setText(statut);
            lblStatut.getStyleClass().setAll("chip");
            switch (statut) {
                case "livree"   -> lblStatut.getStyleClass().add("chip-success");
                case "expediee" -> lblStatut.getStyleClass().add("chip-info");
                case "annulee"  -> lblStatut.setStyle(
                        "-fx-background-color: rgba(214,41,62,0.12); -fx-text-fill: #d6293e;" +
                        "-fx-background-radius: 999px; -fx-padding: 4 10 4 10; -fx-font-weight: 600;");
                default         -> lblStatut.getStyleClass().add("chip-warning");
            }
        }
    }

    // ── Chargement ────────────────────────────────────────────────────────────

    private void chargerDonnees() {
        try {
            livraisonsMap.clear();
            serviceLivraison.afficherAll().forEach(l -> {
                if (l.getCommandeId() != null) livraisonsMap.put(l.getCommandeId(), l);
            });
            data.setAll(serviceCommande.afficherAll());
            appliquerFiltre();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Chargement échoué : " + e.getMessage());
        }
    }

    // ── Filtres ───────────────────────────────────────────────────────────────

    @FXML private void onSearch()     { appliquerFiltre(); }
    @FXML private void onActualiser() { chargerDonnees(); }

    @FXML
    private void onSort() {
        String choix = sortCombo.getValue();
        if (choix == null) { sorted.setComparator(null); return; }
        sorted.setComparator(switch (choix) {
            case "Date récente → ancienne"  -> java.util.Comparator.<Commande, java.time.LocalDateTime>
                    comparing(c -> c.getDateCommande() != null ? c.getDateCommande() : java.time.LocalDateTime.MIN).reversed();
            case "Date ancienne → récente"  -> java.util.Comparator.comparing(
                    c -> c.getDateCommande() != null ? c.getDateCommande() : java.time.LocalDateTime.MIN);
            case "Total croissant"          -> java.util.Comparator.comparingDouble(Commande::getTotal);
            case "Total décroissant"        -> java.util.Comparator.comparingDouble(Commande::getTotal).reversed();
            default -> null;
        });
    }

    @FXML
    private void onReset() {
        searchField.clear();
        filterStatut.getSelectionModel().clearSelection();
        sortCombo.getSelectionModel().clearSelection();
        sorted.setComparator(null);
        appliquerFiltre();
    }

    private void appliquerFiltre() {
        String txt    = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String statut = filterStatut.getValue();
        filtered.setPredicate(cmd -> {
            boolean matchTxt = txt.isEmpty() || matchLivraison(cmd.getId(), txt);
            boolean matchSt  = statut == null || statut.isBlank() || statutMatch(cmd.getId(), statut);
            return matchTxt && matchSt;
        });
        lblCount.setText(filtered.size() + " commande(s)");
    }

    private boolean matchLivraison(int id, String txt) {
        Livraison l = livraisonsMap.get(id);
        if (l == null) return false;
        return (l.getVille()       != null && l.getVille().toLowerCase().contains(txt))
            || (l.getAdresse()     != null && l.getAdresse().toLowerCase().contains(txt))
            || (l.getPhoneNumber() != null && l.getPhoneNumber().contains(txt));
    }

    private boolean statutMatch(int id, String statut) {
        Livraison l = livraisonsMap.get(id);
        return l != null && statut.equals(l.getStatusLivraison());
    }

    // ── Détail commande ───────────────────────────────────────────────────────

    private void ouvrirDetail(Commande cmd) {
        if (cmd == null) return;
        try {
            List<LigneCommande> lignes = serviceLigneCommande.afficherByCommande(cmd.getId());
            Livraison liv = livraisonsMap.get(cmd.getId());
            String statut = liv != null ? liv.getStatusLivraison() : "en_attente";

            VBox content = new VBox(0);
            content.setPrefWidth(580);

            // ── Bandeau header ──
            VBox header = new VBox(4);
            header.setPadding(new Insets(18, 20, 18, 20));
            header.setStyle("-fx-background-color: linear-gradient(to right, #0f172a, #1e3a5f);");

            HBox headerTop = new HBox(12);
            headerTop.setAlignment(Pos.CENTER_LEFT);

            Label lblTitre = new Label("Commande #" + String.format("%06d", cmd.getId()));
            lblTitre.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-text-fill: white;");
            HBox.setHgrow(lblTitre, Priority.ALWAYS);

            Label chipStatut = buildChipStatutDetail(statut);
            headerTop.getChildren().addAll(lblTitre, chipStatut);

            Label lblDate = new Label("📅  " + (cmd.getDateCommande() != null
                    ? cmd.getDateCommande().format(FMT) : "—"));
            lblDate.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.65);");

            header.getChildren().addAll(headerTop, lblDate);

            // Ligne décorative
            Region ligne = new Region();
            ligne.setPrefHeight(3);
            ligne.setStyle("-fx-background-color: linear-gradient(to right, rgba(0,210,255,0.9), rgba(6,106,201,0.9), rgba(106,17,203,0.9));");

            content.getChildren().addAll(header, ligne);

            // ── Corps scrollable ──
            VBox corps = new VBox(14);
            corps.setPadding(new Insets(16, 20, 20, 20));
            corps.setStyle("-fx-background-color: -edu-surface-2;");

            // KPI rapides
            HBox kpis = new HBox(12);
            kpis.getChildren().addAll(
                buildKpi("💰 Total", String.format("%.2f TND", cmd.getTotal()), "#2563eb"),
                buildKpi("📦 Articles", String.valueOf(lignes.size()), "#7c3aed"),
                buildKpi("🚚 Livraison", statut.replace("_", " "), couleurStatut(statut))
            );
            corps.getChildren().add(kpis);

            // ── Section articles ──
            VBox secArticles = new VBox(0);
            secArticles.getStyleClass().add("card");
            secArticles.setStyle("-fx-background-color: -edu-card; -fx-background-radius: 12px; -fx-border-radius: 12px;");

            HBox titreArticles = new HBox(8);
            titreArticles.setAlignment(Pos.CENTER_LEFT);
            titreArticles.setPadding(new Insets(12, 14, 12, 14));
            titreArticles.setStyle("-fx-background-color: rgba(6,106,201,0.07); -fx-background-radius: 12px 12px 0 0;" +
                    "-fx-border-color: transparent transparent rgba(6,106,201,0.15) transparent; -fx-border-width: 0 0 1 0;");
            Label icArticles = new Label("🛍");
            icArticles.setStyle("-fx-font-size: 13px;");
            Label lblArticles = new Label("Articles commandés");
            lblArticles.setStyle("-fx-font-weight: 800; -fx-font-size: 13px; -fx-text-fill: -edu-primary;");
            titreArticles.getChildren().addAll(icArticles, lblArticles);
            secArticles.getChildren().add(titreArticles);

            VBox listeArticles = new VBox(0);
            listeArticles.setPadding(new Insets(0, 14, 8, 14));
            if (lignes.isEmpty()) {
                Label vide = new Label("Aucun article.");
                vide.getStyleClass().add("page-subtitle");
                vide.setPadding(new Insets(10, 0, 10, 0));
                listeArticles.getChildren().add(vide);
            } else {
                boolean pair = false;
                for (LigneCommande lc : lignes) {
                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(9, 0, 9, 0));
                    if (pair) row.setStyle("-fx-background-color: rgba(6,106,201,0.03);");
                    row.setStyle(row.getStyle() + "-fx-border-color: transparent transparent -edu-border transparent; -fx-border-width: 0 0 1 0;");

                    VBox infos = new VBox(2); HBox.setHgrow(infos, Priority.ALWAYS);
                    Label nomP = new Label(lc.getNomProduit());
                    nomP.setStyle("-fx-font-weight: 700; -fx-font-size: 12px;");
                    Label qte = new Label("× " + lc.getQuantite() + "   (" + String.format("%.2f TND", lc.getPrixUnitaire()) + " / unité)");
                    qte.getStyleClass().add("page-subtitle"); qte.setStyle("-fx-font-size: 10px;");
                    infos.getChildren().addAll(nomP, qte);

                    Label st = new Label(String.format("%.2f TND", lc.getPrixUnitaire() * lc.getQuantite()));
                    st.setStyle("-fx-font-weight: 900; -fx-text-fill: #2563eb; -fx-font-size: 13px;");
                    row.getChildren().addAll(infos, st);
                    listeArticles.getChildren().add(row);
                    pair = !pair;
                }
                // Total
                HBox totalRow = new HBox();
                totalRow.setAlignment(Pos.CENTER_RIGHT);
                totalRow.setPadding(new Insets(10, 0, 4, 0));
                Label lblTotalVal = new Label("Total : " + String.format("%.2f TND", cmd.getTotal()));
                lblTotalVal.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: #2563eb;" +
                        "-fx-background-color: rgba(37,99,235,0.1); -fx-background-radius: 8px; -fx-padding: 5 12 5 12;");
                totalRow.getChildren().add(lblTotalVal);
                listeArticles.getChildren().add(totalRow);
            }
            secArticles.getChildren().add(listeArticles);
            corps.getChildren().add(secArticles);

            // ── Section livraison ──
            VBox secLiv = new VBox(0);
            secLiv.getStyleClass().add("card");
            secLiv.setStyle("-fx-background-color: -edu-card; -fx-background-radius: 12px; -fx-border-radius: 12px;");

            HBox titreLiv = new HBox(8);
            titreLiv.setAlignment(Pos.CENTER_LEFT);
            titreLiv.setPadding(new Insets(12, 14, 12, 14));
            titreLiv.setStyle("-fx-background-color: rgba(6,106,201,0.07); -fx-background-radius: 12px 12px 0 0;" +
                    "-fx-border-color: transparent transparent rgba(6,106,201,0.15) transparent; -fx-border-width: 0 0 1 0;");
            Label icLiv = new Label("🚚"); icLiv.setStyle("-fx-font-size: 13px;");
            Label lblLiv = new Label("Informations de livraison");
            lblLiv.setStyle("-fx-font-weight: 800; -fx-font-size: 13px; -fx-text-fill: -edu-primary;");
            titreLiv.getChildren().addAll(icLiv, lblLiv);
            secLiv.getChildren().add(titreLiv);

            VBox corpsLiv = new VBox(10);
            corpsLiv.setPadding(new Insets(14));

            if (liv == null) {
                Label noLiv = new Label("Aucune livraison associée.");
                noLiv.getStyleClass().add("page-subtitle");
                corpsLiv.getChildren().add(noLiv);
            } else {
                // Infos en grille 2 colonnes
                GridPane grid = new GridPane();
                grid.setHgap(16); grid.setVgap(8);
                addGridRow(grid, 0, "📍 Adresse", liv.getAdresse());
                addGridRow(grid, 1, "🏙 Ville", liv.getVille());
                addGridRow(grid, 2, "📞 Téléphone", liv.getPhoneNumber());
                addGridRow(grid, 3, "📅 Date souhaitée",
                        liv.getDateLivraison() != null ? liv.getDateLivraison().format(FMT) : "Non précisée");
                corpsLiv.getChildren().add(grid);

                // Séparateur
                Region sep = new Region(); sep.setPrefHeight(1);
                sep.setStyle("-fx-background-color: -edu-border;");
                corpsLiv.getChildren().add(sep);

                // Modifier statut
                Label lblModifSt = new Label("Modifier le statut de livraison");
                lblModifSt.setStyle("-fx-font-weight: 700; -fx-font-size: 12px; -fx-text-fill: -edu-text;");

                ComboBox<String> combo = new ComboBox<>();
                combo.getItems().addAll("en_attente", "expediee", "livree", "annulee");
                combo.setValue(liv.getStatusLivraison());
                combo.getStyleClass().add("combo-rgb");
                combo.setMaxWidth(Double.MAX_VALUE);

                Label feedback = new Label(); feedback.setVisible(false);

                Button btnSave = new Button("✔  Enregistrer le statut");
                btnSave.getStyleClass().add("btn-rgb");
                btnSave.setOnAction(e -> {
                    String nv = combo.getValue(); if (nv == null) return;
                    try {
                        liv.setStatusLivraison(nv);
                        liv.setUpdatedAt(java.time.LocalDateTime.now());
                        serviceLivraison.update(liv);
                        livraisonsMap.put(cmd.getId(), liv);
                        listeCommandes.refresh();
                        feedback.setText("✔ Statut mis à jour : " + nv);
                        feedback.setStyle("-fx-text-fill: #10b981; -fx-font-size: 11px; -fx-font-weight: 700;");
                        feedback.setVisible(true);
                    } catch (Exception ex) {
                        feedback.setText("Erreur : " + ex.getMessage());
                        feedback.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
                        feedback.setVisible(true);
                    }
                });

                HBox actionLiv = new HBox(10, combo, btnSave);
                actionLiv.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(combo, Priority.ALWAYS);

                corpsLiv.getChildren().addAll(lblModifSt, actionLiv, feedback);

                // Bouton SMS
                if (liv.getPhoneNumber() != null && !liv.getPhoneNumber().isBlank()) {
                    Button btnSms = new Button("📱  Envoyer SMS au client");
                    btnSms.setStyle("-fx-background-color: #10b981; -fx-text-fill: white;" +
                            "-fx-background-radius: 10px; -fx-font-weight: 700; -fx-padding: 9 14 9 14; -fx-cursor: hand;");
                    btnSms.setMaxWidth(Double.MAX_VALUE);
                    Label smsFeedback = new Label(); smsFeedback.setVisible(false);

                    final Livraison livF = liv; final Commande cmdF = cmd;
                    btnSms.setOnAction(ev -> {
                        btnSms.setDisable(true); btnSms.setText("Envoi…");
                        new Thread(() -> {
                            try {
                                new TwilioSmsService().envoyer(livF.getPhoneNumber(),
                                        construireMessageSms(cmdF, livF, lignes));
                                javafx.application.Platform.runLater(() -> {
                                    btnSms.setText("📱  Envoyer SMS au client"); btnSms.setDisable(false);
                                    smsFeedback.setText("✔ SMS envoyé au " + livF.getPhoneNumber());
                                    smsFeedback.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981; -fx-font-weight: 700;");
                                    smsFeedback.setVisible(true);
                                });
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                javafx.application.Platform.runLater(() -> {
                                    btnSms.setText("📱  Envoyer SMS au client"); btnSms.setDisable(false);
                                    smsFeedback.setText("Erreur : " + ex.getMessage());
                                    smsFeedback.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c;");
                                    smsFeedback.setVisible(true);
                                });
                            }
                        }, "twilio-sms").start();
                    });
                    corpsLiv.getChildren().addAll(btnSms, smsFeedback);
                }
            }
            secLiv.getChildren().add(corpsLiv);
            corps.getChildren().add(secLiv);

            content.getChildren().add(corps);

            // Dialog avec rgb-dialog
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Commande #" + cmd.getId());
            ScrollPane sp = new ScrollPane(content);
            sp.setFitToWidth(true); sp.setPrefViewportHeight(560);
            sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            sp.getStyleClass().add("catalog-scroll");
            dialog.getDialogPane().setContent(sp);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().setPrefWidth(620);
            if (listeCommandes.getScene() != null)
                dialog.getDialogPane().getStylesheets().addAll(listeCommandes.getScene().getStylesheets());
            if (!dialog.getDialogPane().getStyleClass().contains("rgb-dialog"))
                dialog.getDialogPane().getStyleClass().add("rgb-dialog");
            dialog.showAndWait();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    // ── Helpers design ────────────────────────────────────────────────────────

    private HBox buildKpi(String label, String valeur, String couleur) {
        VBox kpi = new VBox(3);
        kpi.setAlignment(Pos.CENTER);
        kpi.setPadding(new Insets(10, 16, 10, 16));
        kpi.setStyle("-fx-background-color: -edu-card; -fx-background-radius: 10px;" +
                "-fx-border-color: " + couleur + "33; -fx-border-radius: 10px; -fx-border-width: 1;");
        HBox.setHgrow(kpi, Priority.ALWAYS);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: -edu-text-muted; -fx-font-weight: 600;");
        Label val = new Label(valeur);
        val.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " + couleur + ";");
        kpi.getChildren().addAll(lbl, val);
        HBox box = new HBox(kpi);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private Label buildChipStatutDetail(String statut) {
        Label chip = new Label(emojiStatut(statut) + "  " + statut.replace("_", " "));
        chip.setStyle("-fx-background-color: " + couleurStatutBg(statut) + ";" +
                "-fx-text-fill: " + couleurStatut(statut) + ";" +
                "-fx-background-radius: 999px; -fx-padding: 4 12 4 12;" +
                "-fx-font-size: 11px; -fx-font-weight: 700;");
        return chip;
    }

    private String emojiStatut(String s) {
        return switch (s) { case "livree" -> "✅"; case "expediee" -> "🚚"; case "annulee" -> "❌"; default -> "⏳"; };
    }

    private String couleurStatut(String s) {
        return switch (s) { case "livree" -> "#10b981"; case "expediee" -> "#3b82f6"; case "annulee" -> "#ef4444"; default -> "#f59e0b"; };
    }

    private String couleurStatutBg(String s) {
        return switch (s) { case "livree" -> "rgba(16,185,129,0.12)"; case "expediee" -> "rgba(59,130,246,0.12)"; case "annulee" -> "rgba(239,68,68,0.12)"; default -> "rgba(245,158,11,0.12)"; };
    }

    private void addGridRow(GridPane grid, int row, String label, String val) {
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: -edu-text-muted; -fx-font-weight: 600;");
        Label v = new Label(val != null ? val : "—");
        v.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: -edu-text;");
        grid.add(l, 0, row);
        grid.add(v, 1, row);
        javafx.scene.layout.ColumnConstraints c0 = new javafx.scene.layout.ColumnConstraints(130);
        javafx.scene.layout.ColumnConstraints c1 = new javafx.scene.layout.ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        if (grid.getColumnConstraints().size() <= row) {
            grid.getColumnConstraints().addAll(c0, c1);
        }
    }

    private String construireMessageSms(Commande cmd, Livraison liv, List<LigneCommande> lignes) {
        StringBuilder sb = new StringBuilder();
        sb.append("EduCampus - Commande #").append(cmd.getId()).append("\n");
        sb.append("Total : ").append(String.format("%.2f TND", cmd.getTotal())).append("\n");
        sb.append("Statut livraison : ").append(liv.getStatusLivraison()).append("\n");
        sb.append("Adresse : ").append(liv.getAdresse()).append(", ").append(liv.getVille()).append("\n");
        if (!lignes.isEmpty()) {
            sb.append("Articles : ");
            for (int i = 0; i < Math.min(lignes.size(), 3); i++) {
                LigneCommande lc = lignes.get(i);
                sb.append(lc.getNomProduit()).append(" x").append(lc.getQuantite());
                if (i < Math.min(lignes.size(), 3) - 1) sb.append(", ");
            }
            if (lignes.size() > 3) sb.append(" +").append(lignes.size() - 3).append(" autre(s)");
            sb.append("\n");
        }
        sb.append("Merci pour votre confiance !");
        return sb.toString();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        if (listeCommandes.getScene() != null)
            a.getDialogPane().getStylesheets().addAll(listeCommandes.getScene().getStylesheets());
        a.showAndWait();
    }
}
