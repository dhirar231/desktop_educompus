package com.educompus.controller.back;

import com.educompus.model.Commande;
import com.educompus.model.LigneCommande;
import com.educompus.model.Livraison;
import com.educompus.service.ServiceCommande;
import com.educompus.service.ServiceLigneCommande;
import com.educompus.service.ServiceLivraison;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackCommandesController {

    @FXML private TableView<Commande>            tableCommandes;
    @FXML private TableColumn<Commande, String>  colId, colDate, colTotal, colUserId,
                                                  colAdresse, colVille, colPhone,
                                                  colStatut, colActions;
    @FXML private TextField                      searchField;
    @FXML private ComboBox<String>               filterStatut;
    @FXML private Label                          lblCount;

    private final ServiceCommande      serviceCommande      = new ServiceCommande();
    private final ServiceLivraison     serviceLivraison     = new ServiceLivraison();
    private final ServiceLigneCommande serviceLigneCommande = new ServiceLigneCommande();

    private final ObservableList<Commande> data     = FXCollections.observableArrayList();
    private FilteredList<Commande>         filtered;

    // Cache livraisons par commande_id
    private final Map<Integer, Livraison> livraisonsMap = new HashMap<>();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Init ─────────────────────────────────────────────────────────────────

    @FXML
    private void initialize() {
        filterStatut.getItems().addAll(
                "en_attente", "expediee", "livree", "annulee"
        );

        configurerColonnes();

        filtered = new FilteredList<>(data, c -> true);
        tableCommandes.setItems(filtered);

        chargerDonnees();
    }

    private void configurerColonnes() {
        colId    .setCellValueFactory(c -> new SimpleStringProperty(
                String.valueOf(c.getValue().getId())));
        colDate  .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDateCommande() != null
                        ? c.getValue().getDateCommande().format(FMT) : "—"));
        colTotal .setCellValueFactory(c -> new SimpleStringProperty(
                String.format("%.2f", c.getValue().getTotal())));
        colUserId.setCellValueFactory(c -> new SimpleStringProperty(
                String.valueOf(c.getValue().getUserId())));

        // Colonnes issues de la livraison associée
        colAdresse.setCellValueFactory(c -> {
            Livraison l = livraisonsMap.get(c.getValue().getId());
            return new SimpleStringProperty(l != null ? l.getAdresse() : "—");
        });
        colVille.setCellValueFactory(c -> {
            Livraison l = livraisonsMap.get(c.getValue().getId());
            return new SimpleStringProperty(l != null ? l.getVille() : "—");
        });
        colPhone.setCellValueFactory(c -> {
            Livraison l = livraisonsMap.get(c.getValue().getId());
            return new SimpleStringProperty(l != null && l.getPhoneNumber() != null
                    ? l.getPhoneNumber() : "—");
        });
        colStatut.setCellValueFactory(c -> {
            Livraison l = livraisonsMap.get(c.getValue().getId());
            return new SimpleStringProperty(l != null ? l.getStatusLivraison() : "—");
        });

        // Chip coloré pour le statut
        colStatut.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label chip = new Label(item);
                chip.getStyleClass().add("chip");
                switch (item) {
                    case "livree"    -> chip.getStyleClass().add("chip-success");
                    case "expediee"  -> chip.getStyleClass().add("chip-info");
                    case "annulee"   -> chip.setStyle("-fx-background-color: rgba(214,41,62,0.12); -fx-text-fill: #d6293e; -fx-background-radius: 999px; -fx-padding: 4 10 4 10;");
                    default          -> chip.getStyleClass().add("chip-warning");
                }
                setGraphic(chip);
                setText(null);
            }
        });

        // Colonne actions — bouton Détail uniquement
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button btnDetail = new Button("👁  Détail");

            {
                btnDetail.getStyleClass().add("btn-ghost");
                btnDetail.setOnAction(e -> {
                    Commande cmd = getTableView().getItems().get(getIndex());
                    ouvrirDetail(cmd);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDetail);
            }
        });
    }

    // ── Chargement ───────────────────────────────────────────────────────────

    private void chargerDonnees() {
        try {
            livraisonsMap.clear();
            List<Livraison> livraisons = serviceLivraison.afficherAll();
            for (Livraison l : livraisons) {
                if (l.getCommandeId() != null) livraisonsMap.put(l.getCommandeId(), l);
            }

            data.setAll(serviceCommande.afficherAll());
            appliquerFiltre();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Chargement échoué : " + e.getMessage());
        }
    }

    // ── Filtres ──────────────────────────────────────────────────────────────

    @FXML private void onSearch()     { appliquerFiltre(); }
    @FXML private void onActualiser() { chargerDonnees(); }

    @FXML
    private void onReset() {
        searchField.clear();
        filterStatut.getSelectionModel().clearSelection();
        appliquerFiltre();
    }

    private void appliquerFiltre() {
        String txt    = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String statut = filterStatut.getValue();

        filtered.setPredicate(cmd -> {
            boolean matchTxt = txt.isEmpty()
                    || String.valueOf(cmd.getId()).contains(txt)
                    || matchLivraison(cmd.getId(), txt);
            boolean matchStatut = statut == null || statut.isBlank()
                    || statutMatch(cmd.getId(), statut);
            return matchTxt && matchStatut;
        });
        lblCount.setText(filtered.size() + " commande(s)");
    }

    private boolean matchLivraison(int cmdId, String txt) {
        Livraison l = livraisonsMap.get(cmdId);
        if (l == null) return false;
        return (l.getVille()    != null && l.getVille().toLowerCase().contains(txt))
            || (l.getAdresse()  != null && l.getAdresse().toLowerCase().contains(txt))
            || (l.getPhoneNumber() != null && l.getPhoneNumber().contains(txt));
    }

    private boolean statutMatch(int cmdId, String statut) {
        Livraison l = livraisonsMap.get(cmdId);
        return l != null && statut.equals(l.getStatusLivraison());
    }

    // ── Détail commande ───────────────────────────────────────────────────────

    private void ouvrirDetail(Commande cmd) {
        try {
            List<LigneCommande> lignes = serviceLigneCommande.afficherByCommande(cmd.getId());
            Livraison liv = livraisonsMap.get(cmd.getId());

            // Construire le contenu de la boîte de dialogue
            VBox content = new VBox(14);
            content.setPadding(new Insets(4));
            content.setPrefWidth(520);

            // ── Infos commande ──
            VBox secCmd = new VBox(6);
            secCmd.getStyleClass().add("card");
            secCmd.setPadding(new Insets(14));

            Label titreCmd = new Label("Commande #" + cmd.getId());
            titreCmd.getStyleClass().add("stat-title");

            Label dateCmd = new Label("Date : " + (cmd.getDateCommande() != null
                    ? cmd.getDateCommande().format(FMT) : "—"));
            dateCmd.getStyleClass().add("page-subtitle");

            Label totalCmd = new Label("Total : " + String.format("%.2f TND", cmd.getTotal()));
            totalCmd.setStyle("-fx-font-weight: 800; -fx-text-fill: -edu-primary;");

            Label clientCmd = new Label("Client ID : " + cmd.getUserId());
            clientCmd.getStyleClass().add("page-subtitle");

            secCmd.getChildren().addAll(titreCmd, dateCmd, totalCmd, clientCmd);

            // ── Lignes de commande ──
            VBox secLignes = new VBox(6);
            secLignes.getStyleClass().add("card");
            secLignes.setPadding(new Insets(14));

            Label titreLignes = new Label("Articles commandés");
            titreLignes.getStyleClass().add("stat-title");
            secLignes.getChildren().add(titreLignes);

            if (lignes.isEmpty()) {
                Label vide = new Label("Aucune ligne de commande.");
                vide.getStyleClass().add("page-subtitle");
                secLignes.getChildren().add(vide);
            } else {
                for (LigneCommande lc : lignes) {
                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(6, 0, 6, 0));
                    row.setStyle("-fx-border-color: transparent transparent -edu-border transparent; -fx-border-width: 0 0 1 0;");

                    VBox infos = new VBox(2);
                    HBox.setHgrow(infos, Priority.ALWAYS);
                    Label nomProd = new Label(lc.getNomProduit());
                    nomProd.setStyle("-fx-font-weight: 700;");
                    Label qte = new Label("× " + lc.getQuantite()
                            + "  (" + String.format("%.2f TND", lc.getPrixUnitaire()) + " / unité)");
                    qte.getStyleClass().add("page-subtitle");
                    qte.setStyle("-fx-font-size: 11px;");
                    infos.getChildren().addAll(nomProd, qte);

                    Label st = new Label(String.format("%.2f TND",
                            lc.getPrixUnitaire() * lc.getQuantite()));
                    st.setStyle("-fx-font-weight: 800; -fx-text-fill: -edu-primary;");

                    row.getChildren().addAll(infos, st);
                    secLignes.getChildren().add(row);
                }
            }

            // ── Infos livraison + modification statut ──
            VBox secLiv = new VBox(10);
            secLiv.getStyleClass().add("card");
            secLiv.setPadding(new Insets(14));

            Label titreLiv = new Label("Livraison");
            titreLiv.getStyleClass().add("stat-title");
            secLiv.getChildren().add(titreLiv);

            if (liv == null) {
                Label noLiv = new Label("Aucune livraison associée.");
                noLiv.getStyleClass().add("page-subtitle");
                secLiv.getChildren().add(noLiv);
            } else {
                addLivRow(secLiv, "Adresse",         liv.getAdresse());
                addLivRow(secLiv, "Ville",           liv.getVille());
                addLivRow(secLiv, "Téléphone",       liv.getPhoneNumber());
                addLivRow(secLiv, "Date souhaitée",
                        liv.getDateLivraison() != null ? liv.getDateLivraison().format(FMT) : "—");
                addLivRow(secLiv, "Tracking",
                        liv.getTrackingNumber() != null ? liv.getTrackingNumber() : "—");

                // Séparateur
                Region sep = new Region();
                sep.setPrefHeight(1);
                sep.setStyle("-fx-background-color: -edu-border;");
                secLiv.getChildren().add(sep);

                // Modifier le statut
                Label lblStatutTitre = new Label("Modifier le statut de livraison");
                lblStatutTitre.getStyleClass().add("stat-title");
                lblStatutTitre.setStyle("-fx-font-size: 12px;");

                ComboBox<String> comboStatut = new ComboBox<>();
                comboStatut.getItems().addAll("en_attente", "expediee", "livree", "annulee");
                comboStatut.setValue(liv.getStatusLivraison());
                comboStatut.getStyleClass().add("field");
                comboStatut.setMaxWidth(Double.MAX_VALUE);

                Label lblFeedback = new Label();
                lblFeedback.setStyle("-fx-font-size: 11px;");
                lblFeedback.setVisible(false);

                Button btnSauvegarder = new Button("✔  Enregistrer le statut");
                btnSauvegarder.getStyleClass().add("btn-primary");
                btnSauvegarder.setOnAction(e -> {
                    String nouveauStatut = comboStatut.getValue();
                    if (nouveauStatut == null) return;
                    try {
                        liv.setStatusLivraison(nouveauStatut);
                        liv.setUpdatedAt(java.time.LocalDateTime.now());
                        serviceLivraison.update(liv);
                        // Mettre à jour le cache
                        livraisonsMap.put(cmd.getId(), liv);
                        // Rafraîchir le tableau
                        tableCommandes.refresh();
                        lblFeedback.setText("✔ Statut mis à jour : " + nouveauStatut);
                        lblFeedback.setStyle("-fx-text-fill: -edu-success; -fx-font-size: 11px; -fx-font-weight: 700;");
                        lblFeedback.setVisible(true);
                    } catch (Exception ex) {
                        lblFeedback.setText("Erreur : " + ex.getMessage());
                        lblFeedback.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
                        lblFeedback.setVisible(true);
                    }
                });

                secLiv.getChildren().addAll(lblStatutTitre, comboStatut, btnSauvegarder, lblFeedback);
            }

            content.getChildren().addAll(secCmd, secLignes, secLiv);

            // Afficher dans un Dialog scrollable
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Détail commande #" + cmd.getId());
            dialog.getDialogPane().setContent(new ScrollPane(content) {{
                setFitToWidth(true);
                setPrefViewportHeight(500);
                setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                getStyleClass().add("catalog-scroll");
            }});
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            if (tableCommandes.getScene() != null)
                dialog.getDialogPane().getStylesheets().addAll(tableCommandes.getScene().getStylesheets());
            dialog.showAndWait();

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void addLivRow(VBox parent, String label, String valeur) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(label + " :");
        lbl.getStyleClass().add("page-subtitle");
        lbl.setMinWidth(120);
        Label val = new Label(valeur != null ? valeur : "—");
        val.setStyle("-fx-font-weight: 600;");
        row.getChildren().addAll(lbl, val);
        parent.getChildren().add(row);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        styleAlert(a);
        a.showAndWait();
    }

    private void styleAlert(Alert a) {
        if (tableCommandes.getScene() != null)
            a.getDialogPane().getStylesheets().addAll(tableCommandes.getScene().getStylesheets());
    }
}
