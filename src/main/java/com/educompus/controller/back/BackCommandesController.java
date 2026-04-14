package com.educompus.controller.back;

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

            VBox content = new VBox(14);
            content.setPadding(new Insets(4));
            content.setPrefWidth(520);

            // Infos commande
            VBox secCmd = new VBox(6);
            secCmd.getStyleClass().add("card");
            secCmd.setPadding(new Insets(14));
            Label titreCmd = new Label("Commande #" + cmd.getId());
            titreCmd.getStyleClass().add("stat-title");
            Label dateCmd = new Label("Date : " + (cmd.getDateCommande() != null ? cmd.getDateCommande().format(FMT) : "—"));
            dateCmd.getStyleClass().add("page-subtitle");
            Label totalCmd = new Label("Total : " + String.format("%.2f TND", cmd.getTotal()));
            totalCmd.setStyle("-fx-font-weight: 800; -fx-text-fill: -edu-primary;");
            secCmd.getChildren().addAll(titreCmd, dateCmd, totalCmd);

            // Lignes commande
            VBox secLignes = new VBox(6);
            secLignes.getStyleClass().add("card");
            secLignes.setPadding(new Insets(14));
            Label titreLignes = new Label("Articles commandés");
            titreLignes.getStyleClass().add("stat-title");
            secLignes.getChildren().add(titreLignes);
            if (lignes.isEmpty()) {
                secLignes.getChildren().add(new Label("Aucune ligne."));
            } else {
                for (LigneCommande lc : lignes) {
                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(6, 0, 6, 0));
                    row.setStyle("-fx-border-color: transparent transparent -edu-border transparent; -fx-border-width: 0 0 1 0;");
                    VBox infos = new VBox(2); HBox.setHgrow(infos, Priority.ALWAYS);
                    Label nomP = new Label(lc.getNomProduit()); nomP.setStyle("-fx-font-weight: 700;");
                    Label qte  = new Label("× " + lc.getQuantite() + "  (" + String.format("%.2f TND", lc.getPrixUnitaire()) + " / unité)");
                    qte.getStyleClass().add("page-subtitle"); qte.setStyle("-fx-font-size: 11px;");
                    infos.getChildren().addAll(nomP, qte);
                    Label st = new Label(String.format("%.2f TND", lc.getPrixUnitaire() * lc.getQuantite()));
                    st.setStyle("-fx-font-weight: 800; -fx-text-fill: -edu-primary;");
                    row.getChildren().addAll(infos, st);
                    secLignes.getChildren().add(row);
                }
            }

            // Livraison + statut
            VBox secLiv = new VBox(10);
            secLiv.getStyleClass().add("card");
            secLiv.setPadding(new Insets(14));
            Label titreLiv = new Label("Livraison"); titreLiv.getStyleClass().add("stat-title");
            secLiv.getChildren().add(titreLiv);
            if (liv == null) {
                secLiv.getChildren().add(new Label("Aucune livraison associée."));
            } else {
                addRow(secLiv, "Adresse",   liv.getAdresse());
                addRow(secLiv, "Ville",     liv.getVille());
                addRow(secLiv, "Téléphone", liv.getPhoneNumber());
                addRow(secLiv, "Date souhaitée", liv.getDateLivraison() != null ? liv.getDateLivraison().format(FMT) : "—");

                Region sep = new Region(); sep.setPrefHeight(1);
                sep.setStyle("-fx-background-color: -edu-border;");
                secLiv.getChildren().add(sep);

                Label lblSt = new Label("Modifier le statut"); lblSt.getStyleClass().add("stat-title"); lblSt.setStyle("-fx-font-size: 12px;");
                ComboBox<String> combo = new ComboBox<>();
                combo.getItems().addAll("en_attente", "expediee", "livree", "annulee");
                combo.setValue(liv.getStatusLivraison());
                combo.getStyleClass().add("field"); combo.setMaxWidth(Double.MAX_VALUE);
                Label feedback = new Label(); feedback.setVisible(false);
                Button btnSave = new Button("✔  Enregistrer");
                btnSave.getStyleClass().add("btn-primary");
                btnSave.setOnAction(e -> {
                    String nv = combo.getValue(); if (nv == null) return;
                    try {
                        liv.setStatusLivraison(nv);
                        liv.setUpdatedAt(java.time.LocalDateTime.now());
                        serviceLivraison.update(liv);
                        livraisonsMap.put(cmd.getId(), liv);
                        listeCommandes.refresh();
                        feedback.setText("✔ Statut mis à jour : " + nv);
                        feedback.setStyle("-fx-text-fill: -edu-success; -fx-font-size: 11px; -fx-font-weight: 700;");
                        feedback.setVisible(true);
                    } catch (Exception ex) {
                        feedback.setText("Erreur : " + ex.getMessage());
                        feedback.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 11px;");
                        feedback.setVisible(true);
                    }
                });
                secLiv.getChildren().addAll(lblSt, combo, btnSave, feedback);
            }

            content.getChildren().addAll(secCmd, secLignes, secLiv);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Détail commande #" + cmd.getId());
            ScrollPane sp = new ScrollPane(content);
            sp.setFitToWidth(true); sp.setPrefViewportHeight(500);
            sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            sp.getStyleClass().add("catalog-scroll");
            dialog.getDialogPane().setContent(sp);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            if (listeCommandes.getScene() != null)
                dialog.getDialogPane().getStylesheets().addAll(listeCommandes.getScene().getStylesheets());
            dialog.showAndWait();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    private void addRow(VBox parent, String label, String val) {
        HBox row = new HBox(8); row.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(label + " :"); l.getStyleClass().add("page-subtitle"); l.setMinWidth(120);
        Label v = new Label(val != null ? val : "—"); v.setStyle("-fx-font-weight: 600;");
        row.getChildren().addAll(l, v); parent.getChildren().add(row);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        if (listeCommandes.getScene() != null)
            a.getDialogPane().getStylesheets().addAll(listeCommandes.getScene().getStylesheets());
        a.showAndWait();
    }
}
