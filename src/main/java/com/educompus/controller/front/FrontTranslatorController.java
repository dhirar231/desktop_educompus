package com.educompus.controller.front;

import com.educompus.service.MyMemoryTranslationService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 * Contrôleur pour la page de traduction instantanée.
 * Permet de traduire n'importe quel texte via MyMemory API.
 */
public final class FrontTranslatorController {

    @FXML private ComboBox<MyMemoryTranslationService.Language> sourceLangCombo;
    @FXML private ComboBox<MyMemoryTranslationService.Language> targetLangCombo;
    @FXML private TextArea sourceText;
    @FXML private TextArea resultText;
    @FXML private Button btnTranslate;
    @FXML private Button btnSwapLangs;
    @FXML private Button btnClear;
    @FXML private ProgressIndicator translateSpinner;
    @FXML private Label statusLabel;
    @FXML private Label charCountLabel;
    @FXML private VBox emptyStateBox;

    private Task<String> currentTask;
    private static final int MAX_CHARS = 5000;

    @FXML
    private void initialize() {
        // Initialiser les ComboBox
        if (sourceLangCombo != null) {
            sourceLangCombo.setItems(FXCollections.observableArrayList(MyMemoryTranslationService.Language.values()));
            sourceLangCombo.setValue(MyMemoryTranslationService.Language.FR);
        }
        if (targetLangCombo != null) {
            targetLangCombo.setItems(FXCollections.observableArrayList(MyMemoryTranslationService.Language.values()));
            targetLangCombo.setValue(MyMemoryTranslationService.Language.EN);
        }

        // Masquer le spinner et le statut
        if (translateSpinner != null) {
            translateSpinner.setVisible(false);
            translateSpinner.setManaged(false);
        }
        if (statusLabel != null) {
            statusLabel.setVisible(false);
            statusLabel.setManaged(false);
        }

        // Compteur de caractères avec limite
        if (sourceText != null && charCountLabel != null) {
            sourceText.textProperty().addListener((obs, old, newVal) -> {
                int count = newVal == null ? 0 : newVal.length();
                charCountLabel.setText(count + " / " + MAX_CHARS);
                
                // Limiter à MAX_CHARS
                if (count > MAX_CHARS) {
                    sourceText.setText(newVal.substring(0, MAX_CHARS));
                }
                
                // Afficher/masquer l'état vide
                if (emptyStateBox != null) {
                    boolean isEmpty = resultText.getText() == null || resultText.getText().isBlank();
                    emptyStateBox.setVisible(isEmpty);
                    emptyStateBox.setManaged(isEmpty);
                }
            });
        }

        // Traduction automatique (debounce 1 seconde)
        if (sourceText != null) {
            sourceText.textProperty().addListener((obs, old, newVal) -> {
                if (newVal != null && !newVal.isBlank()) {
                    scheduleAutoTranslate();
                } else {
                    // Effacer la traduction si le texte source est vide
                    if (resultText != null) resultText.clear();
                    if (emptyStateBox != null) {
                        emptyStateBox.setVisible(true);
                        emptyStateBox.setManaged(true);
                    }
                }
            });
        }

        // Afficher l'état vide au démarrage
        if (emptyStateBox != null) {
            emptyStateBox.setVisible(true);
            emptyStateBox.setManaged(true);
        }
    }

    @FXML
    private void onCopyResult() {
        if (resultText == null || resultText.getText() == null || resultText.getText().isBlank()) return;
        
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(resultText.getText());
        clipboard.setContent(content);
        
        showStatus("✓ Copié dans le presse-papiers");
    }

    @FXML
    private void onTranslate() {
        translateNow();
    }

    @FXML
    private void onSwapLangs() {
        if (sourceLangCombo == null || targetLangCombo == null) return;
        MyMemoryTranslationService.Language temp = sourceLangCombo.getValue();
        sourceLangCombo.setValue(targetLangCombo.getValue());
        targetLangCombo.setValue(temp);
        
        // Échanger aussi les textes
        if (sourceText != null && resultText != null) {
            String tempText = sourceText.getText();
            sourceText.setText(resultText.getText());
            resultText.setText(tempText);
        }
    }

    @FXML
    private void onClear() {
        if (sourceText != null) sourceText.clear();
        if (resultText != null) resultText.clear();
        if (statusLabel != null) {
            statusLabel.setVisible(false);
            statusLabel.setManaged(false);
        }
    }

    private javafx.animation.PauseTransition autoTranslateDebounce;

    private void scheduleAutoTranslate() {
        if (autoTranslateDebounce != null) {
            autoTranslateDebounce.stop();
        }
        autoTranslateDebounce = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1.2));
        autoTranslateDebounce.setOnFinished(e -> translateNow());
        autoTranslateDebounce.play();
    }

    private void translateNow() {
        if (sourceText == null || resultText == null) return;
        String text = sourceText.getText();
        if (text == null || text.isBlank()) {
            resultText.clear();
            return;
        }

        MyMemoryTranslationService.Language sourceLang = sourceLangCombo.getValue();
        MyMemoryTranslationService.Language targetLang = targetLangCombo.getValue();
        if (sourceLang == null || targetLang == null) return;

        // Si même langue, copier le texte
        if (sourceLang == targetLang) {
            resultText.setText(text);
            showStatus("✓ Même langue");
            return;
        }

        // Annuler la tâche précédente si elle existe
        if (currentTask != null && currentTask.isRunning()) {
            currentTask.cancel();
        }

        setTranslating(true);

        currentTask = new Task<>() {
            @Override
            protected String call() {
                return MyMemoryTranslationService.translate(text, sourceLang.code, targetLang.code);
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    resultText.setText(getValue());
                    if (emptyStateBox != null) {
                        emptyStateBox.setVisible(false);
                        emptyStateBox.setManaged(false);
                    }
                    setTranslating(false);
                    showStatus("✓ Traduit");
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    setTranslating(false);
                    showStatus("⚠ Erreur de traduction");
                });
            }

            @Override
            protected void cancelled() {
                Platform.runLater(() -> {
                    setTranslating(false);
                });
            }
        };

        Thread thread = new Thread(currentTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void setTranslating(boolean loading) {
        if (translateSpinner != null) {
            translateSpinner.setVisible(loading);
            translateSpinner.setManaged(loading);
        }
        if (btnTranslate != null) btnTranslate.setDisable(loading);
        if (sourceLangCombo != null) sourceLangCombo.setDisable(loading);
        if (targetLangCombo != null) targetLangCombo.setDisable(loading);
    }

    private void showStatus(String msg) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        
        // Masquer après 3 secondes
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3));
        pause.setOnFinished(e -> {
            statusLabel.setVisible(false);
            statusLabel.setManaged(false);
        });
        pause.play();
    }
}
