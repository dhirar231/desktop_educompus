package com.educompus.controller.back;

import com.educompus.app.AppState;
import com.educompus.model.Produit;
import com.educompus.service.GroqRecommandationService;
import com.educompus.service.PollinationsImageService;
import com.educompus.service.ServiceProduit;
import com.educompus.util.ProduitValidator;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

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

    private static final String SYMFONY_UPLOADS =
        "C:/Users/rania/Desktop/projetweb2026/eduCompus/public/uploads";

    public void setOnSuccess(Runnable callback) { this.onSuccess = callback; }

    @FXML
    private void initialize() {
        fieldType.getItems().addAll("Livre", "Cours en ligne", "Logiciel", "Materiel", "Autre");
        fieldCategorie.getItems().addAll("Mathematiques", "Sciences", "Langues", "Informatique", "Histoire", "Arts", "Sport", "Autre");
        ProduitValidator.attacher(
                fieldNom, errNom, fieldDescription, errDescription,
                fieldPrix, errPrix, fieldStock, errStock,
                fieldType, errType, fieldCategorie, errCategorie,
                fieldImage, errImage
        );
    }

    @FXML
    private void onGenererDescription(ActionEvent event) {
        String nom       = fieldNom.getText().trim();
        String type      = fieldType.getValue();
        String categorie = fieldCategorie.getValue();
        if (nom.isBlank() || type == null || categorie == null) {
            showAlert("Remplissez d'abord le nom, le type et la categorie avant de generer.");
            return;
        }
        btnGenererDesc.setDisable(true);
        btnGenererDesc.setText("Generation...");
        new Thread(() -> {
            try {
                GroqRecommandationService groq = new GroqRecommandationService();
                String desc = groq.genererDescription(nom, type, categorie,
                        fieldMotsCles != null ? fieldMotsCles.getText().trim() : "");
                Platform.runLater(() -> {
                    fieldDescription.setText(desc);
                    btnGenererDesc.setDisable(false);
                    btnGenererDesc.setText("Generer avec IA");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btnGenererDesc.setDisable(false);
                    btnGenererDesc.setText("Generer avec IA");
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
            fieldImage.setText(copierVersUploads(file));
        }
    }

    @FXML
    private void onGenererImage(ActionEvent event) {
        String nom       = fieldNom.getText().trim();
        String type      = fieldType.getValue();
        String categorie = fieldCategorie.getValue();
        if (nom.isBlank() || type == null || categorie == null) {
            showAlert("Remplissez d'abord le nom, le type et la categorie avant de generer l'image.");
            return;
        }
        btnGenererImage.setDisable(true);
        btnGenererImage.setText("Generation...");
        new Thread(() -> {
            try {
                PollinationsImageService pollinations = new PollinationsImageService();
                String prompt = PollinationsImageService.construirePrompt(
                        nom, type, categorie, fieldDescription.getText().trim());
                File imageLocale = pollinations.genererImage(prompt, 512, 512);
                String fileName = copierVersUploads(imageLocale);
                Platform.runLater(() -> {
                    fieldImage.setText(fileName);
                    btnGenererImage.setDisable(false);
                    btnGenererImage.setText("Generer image IA");
                    showInfo("Image generee : " + fileName + "\nVisible dans la marketplace Symfony.");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    btnGenererImage.setDisable(false);
                    btnGenererImage.setText("Generer image IA");
                    showAlert("Erreur generation image : " + ex.getMessage());
                });
            }
        }, "pollinations-img").start();
    }

    private String copierVersUploads(File source) {
        String fileName = source.getName();
        try {
            Path dest = Paths.get(SYMFONY_UPLOADS);
            if (!Files.exists(dest)) Files.createDirectories(dest);
            Files.copy(source.toPath(), dest.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[Uploads] Copie OK : " + fileName);
        } catch (Exception e) {
            System.err.println("[Uploads] Erreur copie : " + e.getMessage());
        }
        return fileName;
    }

    @FXML
    private void onEnregistrer(ActionEvent event) {
        boolean ok = ProduitValidator.validerTout(
                fieldNom, errNom, fieldDescription, errDescription,
                fieldPrix, errPrix, fieldStock, errStock,
                fieldType, errType, fieldCategorie, errCategorie,
                fieldImage, errImage
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
        a.setTitle("Erreur"); a.setHeaderText(null); a.setContentText(msg);
        if (fieldNom.getScene() != null)
            a.getDialogPane().getStylesheets().addAll(fieldNom.getScene().getStylesheets());
        a.showAndWait();
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Image generee"); a.setHeaderText(null); a.setContentText(msg);
        if (fieldNom.getScene() != null)
            a.getDialogPane().getStylesheets().addAll(fieldNom.getScene().getStylesheets());
        a.showAndWait();
    }

    private void fermer() {
        ((Stage) fieldNom.getScene().getWindow()).close();
    }
}