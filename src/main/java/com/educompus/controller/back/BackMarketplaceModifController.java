package com.educompus.controller.back;

import com.educompus.model.Produit;
import com.educompus.service.GroqRecommandationService;
import com.educompus.service.ServiceProduit;
import com.educompus.util.ProduitValidator;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Optional;

public class BackMarketplaceModifController {

    @FXML private TextField        fieldNom;
    @FXML private TextArea         fieldDescription;
    @FXML private TextField        fieldMotsCles;
    @FXML private Button           btnGenererDesc;
    @FXML private TextField        fieldPrix;
    @FXML private TextField        fieldStock;
    @FXML private ComboBox<String> fieldType;
    @FXML private ComboBox<String> fieldCategorie;
    @FXML private TextField        fieldImage;
    @FXML private Label            lblSousTitre;

    // Labels d'erreur sous chaque champ
    @FXML private Label errNom;
    @FXML private Label errDescription;
    @FXML private Label errPrix;
    @FXML private Label errStock;
    @FXML private Label errType;
    @FXML private Label errCategorie;
    @FXML private Label errImage;

    private final ServiceProduit service = new ServiceProduit();
    private Produit produit;
    private Runnable onSuccess;

    public void setOnSuccess(Runnable callback) { this.onSuccess = callback; }

    @FXML
    private void initialize() {
        fieldType.getItems().addAll(
                "Livre", "Cours en ligne", "Logiciel", "Matériel", "Autre"
        );
        fieldCategorie.getItems().addAll(
                "Mathématiques", "Sciences", "Langues", "Informatique",
                "Histoire", "Arts", "Sport", "Autre"
        );

        // Attacher la validation en temps réel
        ProduitValidator.attacher(
                fieldNom,         errNom,
                fieldDescription, errDescription,
                fieldPrix,        errPrix,
                fieldStock,       errStock,
                fieldType,        errType,
                fieldCategorie,   errCategorie,
                fieldImage,       errImage
        );
    }

    public void setProduit(Produit p) {
        this.produit = p;
        lblSousTitre.setText("Modification du produit #" + p.getId() + " — " + p.getNom());
        fieldNom.setText(p.getNom());
        fieldDescription.setText(p.getDescription());
        fieldPrix.setText(String.valueOf(p.getPrix()));
        fieldStock.setText(String.valueOf(p.getStock()));
        fieldType.setValue(p.getType());
        fieldCategorie.setValue(p.getCategorie());
        fieldImage.setText(p.getImage() == null ? "" : p.getImage());
    }

    @FXML
    private void onGenererDescription(ActionEvent event) {
        String nom       = fieldNom.getText().trim();
        String type      = fieldType.getValue();
        String categorie = fieldCategorie.getValue();

        if (nom.isBlank() || type == null || categorie == null) {
            showAlert("Remplissez d'abord le nom, le type et la catégorie.");
            return;
        }

        btnGenererDesc.setDisable(true);
        btnGenererDesc.setText("⏳ Génération…");

        new Thread(() -> {
            try {
                GroqRecommandationService groq = new GroqRecommandationService();
                String desc = groq.genererDescription(nom, type, categorie,
                        fieldMotsCles != null ? fieldMotsCles.getText().trim() : "");
                javafx.application.Platform.runLater(() -> {
                    fieldDescription.setText(desc);
                    btnGenererDesc.setDisable(false);
                    btnGenererDesc.setText("✨  Améliorer avec IA");
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    btnGenererDesc.setDisable(false);
                    btnGenererDesc.setText("✨  Améliorer avec IA");
                    showAlert("Erreur Groq : " + ex.getMessage());
                });
            }
        }, "groq-desc-modif").start();
    }

    @FXML
    private void onParcourir(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );
        File file = fc.showOpenDialog(fieldImage.getScene().getWindow());
        if (file != null) fieldImage.setText(file.toURI().toString());
    }

    @FXML
    private void onValider(ActionEvent event) {
        boolean ok = ProduitValidator.validerTout(
                fieldNom,         errNom,
                fieldDescription, errDescription,
                fieldPrix,        errPrix,
                fieldStock,       errStock,
                fieldType,        errType,
                fieldCategorie,   errCategorie,
                fieldImage,       errImage
        );
        if (!ok) return;

        // Vérifier qu'au moins un champ a été modifié
        if (!aEteModifie()) {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Aucune modification");
            info.setHeaderText(null);
            info.setContentText("Aucune modification détectée. Veuillez modifier au moins un champ avant de valider.");
            styleAlert(info);
            info.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer les modifications");
        confirm.setHeaderText("Êtes-vous sûr de ces modifications ?");
        confirm.setContentText("Les changements apportés au produit « "
                + fieldNom.getText().trim() + " » seront enregistrés.");
        styleAlert(confirm);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        try {
            produit.setNom(fieldNom.getText().trim());
            produit.setDescription(fieldDescription.getText().trim());
            produit.setPrix(Double.parseDouble(fieldPrix.getText().trim().replace(",", ".")));
            produit.setStock(Integer.parseInt(fieldStock.getText().trim()));
            produit.setType(fieldType.getValue());
            produit.setCategorie(fieldCategorie.getValue());
            produit.setImage(fieldImage.getText().trim());

            service.update(produit);
            if (onSuccess != null) onSuccess.run();
            fermer();
        } catch (Exception e) {
            showAlert("Erreur lors de la modification : " + e.getMessage());
        }
    }

    /** Retourne true si au moins un champ diffère de la valeur originale du produit. */
    private boolean aEteModifie() {
        String imageOrigine = produit.getImage() == null ? "" : produit.getImage();
        try {
            double prixSaisi  = Double.parseDouble(fieldPrix.getText().trim().replace(",", "."));
            int    stockSaisi = Integer.parseInt(fieldStock.getText().trim());

            return !fieldNom.getText().trim().equals(produit.getNom())
                || !fieldDescription.getText().trim().equals(produit.getDescription())
                || prixSaisi  != produit.getPrix()
                || stockSaisi != produit.getStock()
                || !fieldType.getValue().equals(produit.getType())
                || !fieldCategorie.getValue().equals(produit.getCategorie())
                || !fieldImage.getText().trim().equals(imageOrigine);
        } catch (NumberFormatException e) {
            // Si le format est invalide, la validation aura déjà bloqué — on laisse passer
            return true;
        }
    }

    @FXML
    private void onAnnuler(ActionEvent event) { fermer(); }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur");
        a.setHeaderText(null);
        a.setContentText(msg);
        styleAlert(a);
        a.showAndWait();
    }

    private void styleAlert(Alert a) {
        if (fieldNom.getScene() != null)
            a.getDialogPane().getStylesheets().addAll(fieldNom.getScene().getStylesheets());
    }

    private void fermer() {
        ((Stage) fieldNom.getScene().getWindow()).close();
    }
}
