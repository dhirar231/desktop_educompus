package com.educompus.controller.back;

import com.educompus.model.Produit;
import com.educompus.nav.Navigator;
import com.educompus.service.GroqRecommandationService;
import com.educompus.service.ServiceProduit;
import com.educompus.util.ProduitValidator;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Optional;

public class BackMarketplaceController {

    @FXML private ListView<Produit>  listeProduits;
    @FXML private TextField          searchField;
    @FXML private ComboBox<String>   filterCategorie;
    @FXML private ComboBox<String>   sortCombo;
    @FXML private Label              lblCount;

    private final ServiceProduit service = new ServiceProduit();
    private final ObservableList<Produit> data    = FXCollections.observableArrayList();
    private FilteredList<Produit>         filtered;
    private SortedList<Produit>           sorted;

    @FXML
    private void initialize() {
        sortCombo.getItems().addAll(
                "Nom A → Z", "Nom Z → A",
                "Prix croissant", "Prix décroissant",
                "Stock croissant", "Stock décroissant");
        filtered = new FilteredList<>(data, p -> true);
        sorted   = new SortedList<>(filtered);
        listeProduits.setItems(sorted);
        listeProduits.setCellFactory(lv -> new ProduitCell());
        chargerDonnees();
    }

    // ── CellFactory ───────────────────────────────────────────────────────────

    private class ProduitCell extends ListCell<Produit> {
        private final HBox   root    = new HBox(14);
        private final VBox   infos   = new VBox(4);
        private final Label  lblNom  = new Label();
        private final Label  lblMeta = new Label();
        private final Label  lblPrix = new Label();
        private final Label  lblStock= new Label();
        private final Button btnModif = new Button("✏  Modifier");
        private final Button btnSuppr = new Button("🗑");

        ProduitCell() {
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(10, 16, 10, 16));
            root.setStyle("-fx-border-color: transparent transparent -edu-border transparent; -fx-border-width: 0 0 1 0;");
            lblNom.setStyle("-fx-font-weight: 800; -fx-font-size: 13px; -fx-text-fill: -edu-text;");
            lblMeta.getStyleClass().add("page-subtitle");
            lblMeta.setStyle("-fx-font-size: 11px;");
            infos.getChildren().addAll(lblNom, lblMeta);
            HBox.setHgrow(infos, Priority.ALWAYS);
            lblPrix.setStyle("-fx-font-weight: 900; -fx-font-size: 14px; -fx-text-fill: -edu-primary; -fx-min-width: 100px; -fx-alignment: CENTER-RIGHT;");
            lblStock.setStyle("-fx-font-size: 11px; -fx-min-width: 80px; -fx-alignment: CENTER;");
            btnModif.getStyleClass().add("btn-rgb-outline");
            btnSuppr.getStyleClass().add("btn-rgb-outline");
            btnSuppr.setStyle("-fx-text-fill: #e74c3c;");
            btnModif.setOnAction(e -> ouvrirFormulaireModification(getItem()));
            btnSuppr.setOnAction(e -> confirmerSuppression(getItem()));
            HBox actions = new HBox(6, btnModif, btnSuppr);
            actions.setAlignment(Pos.CENTER_RIGHT);
            root.getChildren().addAll(infos, lblPrix, lblStock, actions);
        }

        @Override
        protected void updateItem(Produit p, boolean empty) {
            super.updateItem(p, empty);
            if (empty || p == null) { setGraphic(null); return; }
            lblNom.setText(p.getNom());
            lblMeta.setText(p.getCategorie() + "  ·  " + p.getType());
            lblPrix.setText(String.format("%.2f TND", p.getPrix()));
            if (p.getStock() == 0) {
                lblStock.setText("Rupture");
                lblStock.setStyle("-fx-font-size: 11px; -fx-text-fill: #e74c3c; -fx-font-weight: 700; -fx-min-width: 80px; -fx-alignment: CENTER;");
            } else if (p.getStock() <= 5) {
                lblStock.setText("Stock : " + p.getStock());
                lblStock.setStyle("-fx-font-size: 11px; -fx-text-fill: #b8860b; -fx-font-weight: 700; -fx-min-width: 80px; -fx-alignment: CENTER;");
            } else {
                lblStock.setText("Stock : " + p.getStock());
                lblStock.setStyle("-fx-font-size: 11px; -fx-text-fill: -edu-text-muted; -fx-min-width: 80px; -fx-alignment: CENTER;");
            }
            setGraphic(root);
        }
    }

    // ── Chargement ────────────────────────────────────────────────────────────

    private void chargerDonnees() {
        try {
            data.setAll(service.afficherAll());
            filterCategorie.getItems().clear();
            data.stream().map(Produit::getCategorie).distinct().sorted()
                    .forEach(filterCategorie.getItems()::add);
            appliquerFiltre();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les produits :\n" + e.getMessage());
        }
    }

    @FXML private void onSearch() { appliquerFiltre(); }

    @FXML
    private void onSort() {
        String choix = sortCombo.getValue();
        if (choix == null) { sorted.setComparator(null); return; }
        sorted.setComparator(switch (choix) {
            case "Nom A → Z"         -> java.util.Comparator.comparing(p -> p.getNom().toLowerCase());
            case "Nom Z → A"         -> java.util.Comparator.<Produit,String>comparing(p -> p.getNom().toLowerCase()).reversed();
            case "Prix croissant"    -> java.util.Comparator.comparingDouble(Produit::getPrix);
            case "Prix décroissant"  -> java.util.Comparator.comparingDouble(Produit::getPrix).reversed();
            case "Stock croissant"   -> java.util.Comparator.comparingInt(Produit::getStock);
            case "Stock décroissant" -> java.util.Comparator.comparingInt(Produit::getStock).reversed();
            default -> null;
        });
    }

    @FXML
    private void onReset() {
        searchField.clear();
        filterCategorie.getSelectionModel().clearSelection();
        sortCombo.getSelectionModel().clearSelection();
        sorted.setComparator(null);
        appliquerFiltre();
    }

    private void appliquerFiltre() {
        String txt = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();
        String cat = filterCategorie.getValue();
        filtered.setPredicate(p -> {
            boolean matchTxt = txt.isEmpty()
                    || p.getNom().toLowerCase().contains(txt)
                    || p.getType().toLowerCase().contains(txt)
                    || p.getCategorie().toLowerCase().contains(txt);
            boolean matchCat = cat == null || cat.isBlank() || p.getCategorie().equals(cat);
            return matchTxt && matchCat;
        });
        lblCount.setText(filtered.size() + " produit(s)");
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML private void onVoirStatistiques(ActionEvent e) { naviguerVers("View/back/BackStatistiques.fxml"); }
    @FXML private void onVoirCalendrier(ActionEvent e)   { naviguerVers("View/back/BackCalendrierCommandes.fxml"); }
    @FXML private void onVoirCommandes(ActionEvent e)    { naviguerVers("View/back/BackCommandes.fxml"); }

    private void naviguerVers(String fxmlPath) {
        try {
            FXMLLoader loader = Navigator.loader(fxmlPath);
            Node vue = loader.load();
            StackPane container = getContentWrap();
            if (container == null) return;
            Node marketplaceView = container.getChildren().get(0);
            VBox wrapper = new VBox();
            VBox.setVgrow(vue, Priority.ALWAYS);
            Button btnRetour = new Button("← Retour au catalogue");
            btnRetour.getStyleClass().add("btn-ghost");
            btnRetour.setOnAction(ev -> container.getChildren().setAll(marketplaceView));
            btnRetour.setStyle("-fx-padding: 10 16 10 16;");
            HBox topBar = new HBox(btnRetour);
            topBar.setStyle("-fx-background-color: -edu-card;" +
                    "-fx-border-color: transparent transparent -edu-border transparent;" +
                    "-fx-border-width: 0 0 1 0; -fx-padding: 8 18 8 18;");
            wrapper.getChildren().addAll(topBar, vue);
            wrapper.setStyle("-fx-background-color: -edu-surface-2;");
            container.getChildren().setAll(wrapper);
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
        }
    }

    private StackPane getContentWrap() {
        try {
            Node node = listeProduits;
            while (node.getParent() != null) {
                node = node.getParent();
                if (node instanceof StackPane sp && "contentWrap".equals(sp.getId())) return sp;
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ── Formulaires produit (style rgb-dialog comme les projets) ─────────────

    @FXML
    private void onAjouter(ActionEvent event) {
        afficherFormulaireAjout();
    }

    private void afficherFormulaireAjout() {
        // Champs
        TextField fieldNom  = champ("Ex: Cours de mathematiques avancees");
        TextArea  fieldDesc = zone("Decrivez le produit (min. 10 caracteres)...");
        TextField fieldPrix = champ("0.00");
        TextField fieldStock= champ("0");
        ComboBox<String> fieldType = combo(
                "Livre", "Cours en ligne", "Logiciel", "Materiel", "Autre");
        ComboBox<String> fieldCat  = combo(
                "Mathematiques", "Sciences", "Langues", "Informatique",
                "Histoire", "Arts", "Sport", "Autre");
        TextField fieldImage = champ("https://... ou chemin local");
        TextField fieldMotsCles = champ("Mots-cles pour la generation IA...");

        // Labels erreur
        Label errNom  = errLabel(); Label errDesc = errLabel();
        Label errPrix = errLabel(); Label errStock= errLabel();
        Label errType = errLabel(); Label errCat  = errLabel();
        Label errImg  = errLabel();

        // Bouton IA
        Button btnIA = new Button("✨ Generer avec IA");
        btnIA.getStyleClass().add("btn-rgb-compact");
        btnIA.setOnAction(e -> genererDescriptionIA(
                fieldNom, fieldType, fieldCat, fieldMotsCles, fieldDesc, btnIA));

        GridPane grid = grille();
        int r = 0;
        ajouterLigne(grid, r++, "Nom du produit *", fieldNom, errNom);
        ajouterLigne(grid, r++, "Description *", fieldDesc, errDesc);
        // Ligne IA
        HBox ligneIA = new HBox(8, fieldMotsCles, btnIA);
        HBox.setHgrow(fieldMotsCles, Priority.ALWAYS);
        ligneIA.setAlignment(Pos.CENTER_LEFT);
        grid.add(ligneIA, 1, r++ * 2);
        ajouterLigne2Col(grid, r++, "Prix (TND) *", fieldPrix, errPrix, "Stock *", fieldStock, errStock);
        ajouterLigne2Col(grid, r++, "Type *", fieldType, errType, "Categorie *", fieldCat, errCat);
        ajouterLigne(grid, r++, "Image (optionnel)", creerLigneImage(fieldImage), errImg);

        // Attacher validation
        ProduitValidator.attacher(fieldNom, errNom, fieldDesc, errDesc, fieldPrix, errPrix,
                fieldStock, errStock, fieldType, errType, fieldCat, errCat, fieldImage, errImg);

        Dialog<ButtonType> dialog = construireDialog("Ajouter un produit", grid);
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Enregistrer");
        okBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            boolean ok = ProduitValidator.validerTout(fieldNom, errNom, fieldDesc, errDesc,
                    fieldPrix, errPrix, fieldStock, errStock, fieldType, errType,
                    fieldCat, errCat, fieldImage, errImg);
            if (!ok) ev.consume();
        });

        Optional<ButtonType> rep = dialog.showAndWait();
        if (rep.isEmpty() || rep.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) return;

        try {
            Produit p = new Produit();
            p.setNom(fieldNom.getText().trim());
            p.setDescription(fieldDesc.getText().trim());
            p.setPrix(Double.parseDouble(fieldPrix.getText().trim().replace(",", ".")));
            p.setStock(Integer.parseInt(fieldStock.getText().trim()));
            p.setType(fieldType.getValue());
            p.setCategorie(fieldCat.getValue());
            p.setImage(fieldImage.getText().trim());
            p.setUserId(com.educompus.app.AppState.getUserId());
            service.ajouter(p);
            chargerDonnees();
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
        }
    }

    private void ouvrirFormulaireModification(Produit produit) {
        if (produit == null) return;

        TextField fieldNom  = champ(produit.getNom());
        TextArea  fieldDesc = zone(produit.getDescription());
        TextField fieldPrix = champ(String.valueOf(produit.getPrix()));
        TextField fieldStock= champ(String.valueOf(produit.getStock()));
        ComboBox<String> fieldType = combo(
                "Livre", "Cours en ligne", "Logiciel", "Materiel", "Autre");
        ComboBox<String> fieldCat  = combo(
                "Mathematiques", "Sciences", "Langues", "Informatique",
                "Histoire", "Arts", "Sport", "Autre");
        TextField fieldImage = champ(produit.getImage() != null ? produit.getImage() : "");
        TextField fieldMotsCles = champ("Mots-cles pour ameliorer la description...");

        fieldType.setValue(produit.getType());
        fieldCat.setValue(produit.getCategorie());

        Label errNom  = errLabel(); Label errDesc = errLabel();
        Label errPrix = errLabel(); Label errStock= errLabel();
        Label errType = errLabel(); Label errCat  = errLabel();
        Label errImg  = errLabel();

        Button btnIA = new Button("✨ Ameliorer avec IA");
        btnIA.getStyleClass().add("btn-rgb-compact");
        btnIA.setOnAction(e -> genererDescriptionIA(
                fieldNom, fieldType, fieldCat, fieldMotsCles, fieldDesc, btnIA));

        GridPane grid = grille();
        int r = 0;
        ajouterLigne(grid, r++, "Nom du produit *", fieldNom, errNom);
        ajouterLigne(grid, r++, "Description *", fieldDesc, errDesc);
        HBox ligneIA = new HBox(8, fieldMotsCles, btnIA);
        HBox.setHgrow(fieldMotsCles, Priority.ALWAYS);
        ligneIA.setAlignment(Pos.CENTER_LEFT);
        grid.add(ligneIA, 1, r++ * 2);
        ajouterLigne2Col(grid, r++, "Prix (TND) *", fieldPrix, errPrix, "Stock *", fieldStock, errStock);
        ajouterLigne2Col(grid, r++, "Type *", fieldType, errType, "Categorie *", fieldCat, errCat);
        ajouterLigne(grid, r++, "Image (optionnel)", creerLigneImage(fieldImage), errImg);

        ProduitValidator.attacher(fieldNom, errNom, fieldDesc, errDesc, fieldPrix, errPrix,
                fieldStock, errStock, fieldType, errType, fieldCat, errCat, fieldImage, errImg);

        Dialog<ButtonType> dialog = construireDialog("Modifier le produit — " + produit.getNom(), grid);
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setText("Valider les modifications");
        okBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            boolean ok = ProduitValidator.validerTout(fieldNom, errNom, fieldDesc, errDesc,
                    fieldPrix, errPrix, fieldStock, errStock, fieldType, errType,
                    fieldCat, errCat, fieldImage, errImg);
            if (!ok) { ev.consume(); return; }
            // Vérifier qu'au moins un champ a changé
            if (fieldNom.getText().trim().equals(produit.getNom())
                    && fieldDesc.getText().trim().equals(produit.getDescription())
                    && fieldPrix.getText().trim().equals(String.valueOf(produit.getPrix()))
                    && fieldStock.getText().trim().equals(String.valueOf(produit.getStock()))
                    && fieldType.getValue().equals(produit.getType())
                    && fieldCat.getValue().equals(produit.getCategorie())) {
                showAlert(Alert.AlertType.INFORMATION, "Aucune modification",
                        "Aucun champ n'a été modifié.");
                ev.consume();
            }
        });

        Optional<ButtonType> rep = dialog.showAndWait();
        if (rep.isEmpty() || rep.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) return;

        try {
            produit.setNom(fieldNom.getText().trim());
            produit.setDescription(fieldDesc.getText().trim());
            produit.setPrix(Double.parseDouble(fieldPrix.getText().trim().replace(",", ".")));
            produit.setStock(Integer.parseInt(fieldStock.getText().trim()));
            produit.setType(fieldType.getValue());
            produit.setCategorie(fieldCat.getValue());
            produit.setImage(fieldImage.getText().trim());
            service.update(produit);
            chargerDonnees();
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur", ex.getMessage());
        }
    }

    // ── Génération IA description ─────────────────────────────────────────────

    private void genererDescriptionIA(TextField fieldNom, ComboBox<String> fieldType,
                                       ComboBox<String> fieldCat, TextField fieldMotsCles,
                                       TextArea fieldDesc, Button btnIA) {
        String nom = fieldNom.getText().trim();
        String type = fieldType.getValue();
        String cat  = fieldCat.getValue();
        if (nom.isBlank() || type == null || cat == null) {
            showAlert(Alert.AlertType.WARNING, "Champs manquants",
                    "Remplissez d'abord le nom, le type et la catégorie.");
            return;
        }
        btnIA.setDisable(true);
        btnIA.setText("⏳ Génération...");
        new Thread(() -> {
            try {
                GroqRecommandationService groq = new GroqRecommandationService();
                String desc = groq.genererDescription(nom, type, cat, fieldMotsCles.getText().trim());
                Platform.runLater(() -> {
                    fieldDesc.setText(desc);
                    btnIA.setDisable(false);
                    btnIA.setText("✨ Generer avec IA");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btnIA.setDisable(false);
                    btnIA.setText("✨ Generer avec IA");
                    showAlert(Alert.AlertType.ERROR, "Erreur Groq", ex.getMessage());
                });
            }
        }, "groq-desc").start();
    }

    // ── Suppression ───────────────────────────────────────────────────────────

    private void confirmerSuppression(Produit produit) {
        if (produit == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer « " + produit.getNom() + " » ?");
        confirm.setContentText("Cette action est irréversible.");
        styleAlert(confirm);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                service.delete(produit.getId());
                chargerDonnees();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression échouée :\n" + e.getMessage());
            }
        }
    }

    // ── Helpers formulaire (style projet) ─────────────────────────────────────

    private GridPane grille() {
        GridPane g = new GridPane();
        g.setHgap(14); g.setVgap(4);
        ColumnConstraints c0 = new ColumnConstraints(130);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c0, c1);
        return g;
    }

    private void ajouterLigne(GridPane g, int row, String label, Node node, Label err) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("form-label");
        g.add(lbl, 0, row * 2);
        g.add(node, 1, row * 2);
        if (node instanceof Region r) { r.setMaxWidth(Double.MAX_VALUE); GridPane.setHgrow(r, Priority.ALWAYS); }
        if (err != null) g.add(err, 1, row * 2 + 1);
    }

    private void ajouterLigne2Col(GridPane g, int row, String lbl1, Node n1, Label e1,
                                   String lbl2, Node n2, Label e2) {
        HBox pair = new HBox(12);
        VBox col1 = new VBox(3); col1.getChildren().addAll(n1, e1); HBox.setHgrow(col1, Priority.ALWAYS);
        VBox col2 = new VBox(3); col2.getChildren().addAll(n2, e2); HBox.setHgrow(col2, Priority.ALWAYS);
        if (n1 instanceof Region r1) r1.setMaxWidth(Double.MAX_VALUE);
        if (n2 instanceof Region r2) r2.setMaxWidth(Double.MAX_VALUE);
        pair.getChildren().addAll(col1, col2);
        pair.setMaxWidth(Double.MAX_VALUE);
        Label lbl = new Label(lbl1 + " / " + lbl2);
        lbl.getStyleClass().add("form-label");
        g.add(lbl, 0, row * 2);
        g.add(pair, 1, row * 2);
        GridPane.setHgrow(pair, Priority.ALWAYS);
    }

    private HBox creerLigneImage(TextField fieldImage) {
        Button btnParcourir = new Button("📁 Parcourir");
        btnParcourir.getStyleClass().add("btn-ghost");
        btnParcourir.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choisir une image");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png","*.jpg","*.jpeg","*.gif","*.webp"));
            File f = fc.showOpenDialog(listeProduits.getScene().getWindow());
            if (f != null) fieldImage.setText(f.toURI().toString());
        });
        HBox row = new HBox(8, fieldImage, btnParcourir);
        HBox.setHgrow(fieldImage, Priority.ALWAYS);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private Dialog<ButtonType> construireDialog(String titre, Node contenu) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle(titre);
        d.setHeaderText(titre);
        d.getDialogPane().setContent(contenu);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.getDialogPane().setPrefWidth(760);
        if (listeProduits.getScene() != null)
            d.getDialogPane().getStylesheets().addAll(listeProduits.getScene().getStylesheets());
        if (!d.getDialogPane().getStyleClass().contains("rgb-dialog"))
            d.getDialogPane().getStyleClass().add("rgb-dialog");
        // Style bouton OK
        Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        if (ok != null) ok.getStyleClass().add("btn-rgb");
        return d;
    }

    private TextField champ(String valeur) {
        TextField tf = new TextField(valeur);
        tf.getStyleClass().add("field");
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private TextArea zone(String valeur) {
        TextArea ta = new TextArea(valeur);
        ta.getStyleClass().add("field");
        ta.setPrefRowCount(3);
        ta.setWrapText(true);
        ta.setMaxWidth(Double.MAX_VALUE);
        return ta;
    }

    private ComboBox<String> combo(String... items) {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll(items);
        cb.getStyleClass().add("field");
        cb.setMaxWidth(Double.MAX_VALUE);
        return cb;
    }

    private Label errLabel() {
        Label l = new Label();
        l.getStyleClass().add("field-error");
        l.setWrapText(true);
        l.setVisible(false);
        l.setManaged(false);
        return l;
    }

    // ── Helpers alert ─────────────────────────────────────────────────────────

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg);
        styleAlert(a); a.showAndWait();
    }

    private void styleAlert(Alert a) {
        if (listeProduits.getScene() != null)
            a.getDialogPane().getStylesheets().addAll(listeProduits.getScene().getStylesheets());
        if (!a.getDialogPane().getStyleClass().contains("rgb-dialog"))
            a.getDialogPane().getStyleClass().add("rgb-dialog");
    }
}
