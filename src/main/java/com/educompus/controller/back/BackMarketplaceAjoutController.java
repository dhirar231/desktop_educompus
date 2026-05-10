package com.educompus.controller.back;

import com.educompus.app.AppState;
import com.educompus.model.Produit;
import com.educompus.service.GroqRecommandationService;
import com.educompus.service.PollinationsImageService;
import com.educompus.service.ServiceProduit;
import com.educompus.service.SymfonyUploadsService;
import com.educompus.util.ProduitValidator;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class BackMarketplaceAjoutController {

    @FXML private TextField        fieldNom;
    @FXML private TextArea         fieldDescription;
    @FXML private TextField        fieldMotsCles;
    @FXML private Button           btnGenererDesc;
    @FXML private TextField        fieldPrix;
    @FXML private TextField        fieldStock;
    @FXML private ComboBox<String> fieldType;
    @FXML private ComboBox<String> fieldCategorie;
    @FXML private TextField        fieldImage;

    // Labels d'erreur sous chaque champ
    @FXML private Label errNom;
    @FXML private Label errDescription;
    @FXML private Label errPrix;
    @FXML private Label errStock;
    @FXML private Label errType;
    @FXML private Label errCategorie;
    @FXML private Label errImage;

    @FXML private Button btnGenererImage;

    private final ServiceProduit service = new ServiceProduit();
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

    @FXML
    private void onGenererDescription(ActionEvent event) {
        String nom      = fieldNom.getText().trim();
        String type     = fieldType.getValue();
        String categorie = fieldCategorie.getValue();

        if (nom.isBlank() || type == null || categorie == null) {
            showAlert("Remplissez d'abord le nom, le type et la catégorie avant de générer.");
            return;
        }

        btnGenererDesc.setDisable(true);
        btnGenererDesc.setText("⏳ Génération…");

        new Thread(() -> {
            try {
                GroqRecommandationService groq = new GroqRecommandationService();
                String desc = groq.genererDescription(nom, type, categorie,
                        fieldMotsCles != null ? fieldMotsCles.getText().trim() : "");
                Platform.runLater(() -> {
                    fieldDescription.setText(desc);
                    btnGenererDesc.setDisable(false);
                    btnGenererDesc.setText("✨  Générer avec IA");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btnGenererDesc.setDisable(false);
                    btnGenererDesc.setText("✨  Générer avec IA");
                    showAlert("Erreur Groq : " + ex.getMessage());
                });
            }
        }, "groq-desc").start();
    }

    @FXML
    private void onParcourir(ActionEvent event) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );
        File file = fc.showOpenDialog(fieldImage.getScene().getWindow());
        if (file != null) {
            String nomFichier = copierImageVersSymfony(file);
            fieldImage.setText(nomFichier);
        }
    }

    @FXML
    private void onGenererImage(ActionEvent event) {
        String nom       = fieldNom.getText().trim();
        String type      = fieldType.getValue();
        String categorie = fieldCategorie.getValue();

        if (nom.isBlank() || type == null || categorie == null) {
            showAlert("Remplissez d'abord le nom, le type et la catégorie avant de générer l'image.");
            return;
        }

        btnGenererImage.setDisable(true);
        btnGenererImage.setText("⏳ Génération…");

        new Thread(() -> {
            try {
                PollinationsImageService pollinations = new PollinationsImageService();
                String prompt = PollinationsImageService.construirePrompt(
                        nom, type, categorie, fieldDescription.getText().trim());
                String fileName = pollinations.genererEtCopierVersSymfony(prompt, 512, 512);
                Platform.runLater(() -> {
                    fieldImage.setText(fileName);
                    btnGenererImage.setDisable(false);
                    btnGenererImage.setText("🎨  Générer image IA");
                    showInfo("Image générée : " + fileName + "\nElle sera visible dans la marketplace Symfony.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btnGenererImage.setDisable(false);
                    btnGenererImage.setText("🎨  Générer image IA");
                    showAlert("Erreur génération image : " + ex.getMessage());
                });
            }
        }, "pollinations-img").start();
    }

    private String copierImageVersSymfony(File source) {
        SymfonyUploadsService symfony = new SymfonyUploadsService();
        if (symfony.estDisponible()) {
            try { return symfony.copierVersSymfony(source); }
            catch (Exception e) { System.err.println("[Ajout] Copie Symfony échouée : " + e.getMessage()); }
        }
        return source.getName();
    }

    @FXML
    private void onEnregistrer(ActionEvent event) {
        // Validation complète avant soumission
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

        try {
            Produit p = new Produit();
            p.setNom(fieldNom.getText().trim());
            p.setDescription(fieldDescription.getText().trim());
            p.setPrix(Double.parseDouble(fieldPrix.getText().trim().replace(",", ".")));
            p.setStock(Integer.parseInt(fieldStock.getText().trim()));
            p.setType(fieldType.getValue());
            p.setCategorie(fieldCategorie.getValue());
            p.setImage(fieldImage.getText().trim());
            p.setUserId(AppState.getUserId());

            service.ajouter(p);
            if (onSuccess != null) onSuccess.run();
            fermer();
        } catch (Exception e) {
            showAlert("Erreur lors de l'enregistrement : " + e.getMessage());
        }
    }

    @FXML
    private void onAnnuler(ActionEvent event) { fermer(); }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur");
        a.setHeaderText(null);
        a.setContentText(msg);
        if (fieldNom.getScene() != null)
            a.getDialogPane().getStylesheets().addAll(fieldNom.getScene().getStylesheets());
        a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Image générée");
        a.setHeaderText(null);
        a.setContentText(msg);
        if (fieldNom.getScene() != null)
            a.getDialogPane().getStylesheets().addAll(fieldNom.getScene().getStylesheets());
        a.showAndWait();
    }

    private void fermer() {
        ((Stage) fieldNom.getScene().getWindow()).close();
    }
}