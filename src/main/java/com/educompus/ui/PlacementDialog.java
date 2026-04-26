package com.educompus.ui;

import com.educompus.model.PlacementQuestion;
import com.educompus.nav.Navigator;
import com.educompus.repository.UserRepository;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.util.List;

public class PlacementDialog {
    private final Stage stage;
    private final List<PlacementQuestion> questions;
    private final int[] answers;
    private int current = 0;
    private final String userEmail;
    private boolean completed = false;
    private boolean abandoning = false;

    public PlacementDialog(Window owner, List<PlacementQuestion> questions, String userEmail) {
        this.questions = questions;
        this.answers = new int[questions.size()];
        for (int i = 0; i < answers.length; i++) answers[i] = -1;
        this.userEmail = userEmail == null ? "" : userEmail;

        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        // Ensure dialog has an owner when possible (prevents closing the dialog from closing the whole app)
        javafx.stage.Window actualOwner = owner == null ? Navigator.getStage() : owner;
        if (actualOwner != null) {
            stage.initOwner(actualOwner);
        }

        // Prevent accidental app exit: ask confirmation when user tries to close the dialog without completing the test
        stage.setOnCloseRequest(evt -> {
            if (completed || abandoning) return;
            evt.consume();
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Le test n'est pas terminé. Voulez-vous abandonner le test ?", ButtonType.YES, ButtonType.NO);
            if (stage.getOwner() != null) confirm.initOwner(stage.getOwner());
            Optional<ButtonType> res = confirm.showAndWait();
            if (res.isPresent() && res.get() == ButtonType.YES) {
                abandoning = true;
                stage.close();
            }
        });
        stage.setTitle("Placement Test — 20 questions");

        VBox root = new VBox(12);
        root.setPadding(new Insets(14));
        root.getStyleClass().add("placement-dialog-root");

        Label title = new Label("Test de niveau — 10 questions — Anglais / Français");
        title.getStyleClass().addAll("dialog-title", "placement-title");

        Label progressLabel = new Label("1/" + questions.size());
        progressLabel.getStyleClass().add("placement-progress");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(header, Priority.ALWAYS);
        Region spacerHeader = new Region();
        HBox.setHgrow(spacerHeader, Priority.ALWAYS);
        header.getChildren().addAll(title, spacerHeader, progressLabel);

        VBox body = new VBox(10);
        body.setPrefWidth(640);
        body.getStyleClass().add("placement-dialog-body");

        // question area
        Label qLabel = new Label();
        qLabel.setWrapText(true);
        ToggleGroup tg = new ToggleGroup();

        VBox choicesBox = new VBox(6);
        choicesBox.getStyleClass().add("placement-choices");

        HBox nav = new HBox(8);
        Button prev = new Button("Précédent");
        Button next = new Button("Suivant");
        Button submit = new Button("Soumettre et enregistrer");
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        nav.getChildren().addAll(prev, next, spacer, submit);
        nav.getStyleClass().add("placement-nav");

        // style buttons to match app
        // Use gradient-outline style for Prev/Next to show blue RGB border
        prev.getStyleClass().add("btn-rgb-outline");
        next.getStyleClass().add("btn-rgb-outline");
        submit.getStyleClass().add("btn-rgb");

        body.getChildren().addAll(qLabel, choicesBox, nav);

        root.getChildren().addAll(header, body);

        // scene will be created after handlers so we can attach stylesheet

        // handlers
        Runnable refresh = () -> {
            PlacementQuestion pq = questions.get(current);
            qLabel.setText((current + 1) + ". " + pq.getText());
            choicesBox.getChildren().clear();
            tg.getToggles().clear();
            progressLabel.setText((current + 1) + "/" + questions.size() + " — " + ("en".equals(pq.getLanguage()) ? "EN" : "FR"));
            for (int i = 0; i < pq.getChoices().size(); i++) {
                RadioButton rb = new RadioButton(pq.getChoices().get(i));
                rb.setToggleGroup(tg);
                final int idx = i;
                rb.setOnAction(e -> answers[current] = idx);
                choicesBox.getChildren().add(rb);
                if (answers[current] == i) rb.setSelected(true);
            }
        };

        prev.setOnAction(e -> {
            if (current > 0) { current--; refresh.run(); }
        });
        next.setOnAction(e -> {
            if (current < questions.size() - 1) { current++; refresh.run(); }
        });

        submit.setOnAction(e -> {
            // ensure all answered
            for (int a : answers) if (a == -1) {
                // move to the first unanswered
                for (int i = 0; i < answers.length; i++) if (answers[i] == -1) { current = i; refresh.run(); break; }
                return;
            }
            // calculate results
            int enCorrect = 0, enTotal = 0, frCorrect = 0, frTotal = 0;
            for (int i = 0; i < questions.size(); i++) {
                PlacementQuestion pq = questions.get(i);
                if ("en".equals(pq.getLanguage())) { enTotal++; if (answers[i] == pq.getCorrectIndex()) enCorrect++; }
                if ("fr".equals(pq.getLanguage())) { frTotal++; if (answers[i] == pq.getCorrectIndex()) frCorrect++; }
            }
            int enPct = enTotal == 0 ? 0 : (int) Math.round(100.0 * enCorrect / enTotal);
            int frPct = frTotal == 0 ? 0 : (int) Math.round(100.0 * frCorrect / frTotal);
            // show styled results dialog to the user before persisting
            try {
                showResultsDialog(enPct, frPct);
            } catch (Exception ignored) {}

            // persist
            try { new UserRepository().setPlacementResult(userEmail, enPct, frPct); } catch (Exception ignored) {}
            completed = true;
            stage.close();
        });
        // set scene and load global css (same as app)
        Scene scene = new Scene(root);
        File cssFile = Navigator.resolvePath("styles/educompus.css");
        if (cssFile != null && cssFile.exists()) {
            scene.getStylesheets().add(cssFile.toURI().toString());
        }
        stage.setScene(scene);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.centerOnScreen();

        refresh.run();
    }

    public boolean showAndWait() {
        stage.showAndWait();
        return completed;
    }

    public boolean wasAbandoned() {
        return abandoning;
    }

    private String levelForPercent(int pct) {
        int p = Math.max(0, Math.min(100, pct));
        if (p < 30) return "A1";
        if (p < 50) return "A2";
        if (p < 70) return "B1";
        if (p < 85) return "B2";
        if (p < 95) return "C1";
        return "C2";
    }

    private void showResultsDialog(int enPct, int frPct) {
        String enLevel = levelForPercent(enPct);
        String frLevel = levelForPercent(frPct);

        Stage resultsStage = new Stage();
        resultsStage.initModality(Modality.APPLICATION_MODAL);
        resultsStage.initOwner(stage.getOwner() == null ? stage : stage.getOwner());
        resultsStage.setTitle("Résultats du test de placement");

        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.getStyleClass().add("placement-results-dialog");

        Label title = new Label("Résultats du test de placement");
        title.getStyleClass().addAll("placement-results-title");

        HBox scores = new HBox(32);
        scores.setAlignment(Pos.CENTER);

        VBox enBox = new VBox(6);
        enBox.setAlignment(Pos.CENTER);
        Label enLabel = new Label("Anglais");
        enLabel.getStyleClass().add("placement-results-lang");
        Label enPctLabel = new Label(enPct + "%");
        enPctLabel.getStyleClass().add("placement-results-percent");
        Label enLevelLabel = new Label(enLevel);
        enLevelLabel.getStyleClass().addAll("placement-results-level", "level-" + enLevel);
        enBox.getChildren().addAll(enLabel, enPctLabel, enLevelLabel);

        VBox frBox = new VBox(6);
        frBox.setAlignment(Pos.CENTER);
        Label frLabel = new Label("Français");
        frLabel.getStyleClass().add("placement-results-lang");
        Label frPctLabel = new Label(frPct + "%");
        frPctLabel.getStyleClass().add("placement-results-percent");
        Label frLevelLabel = new Label(frLevel);
        frLevelLabel.getStyleClass().addAll("placement-results-level", "level-" + frLevel);
        frBox.getChildren().addAll(frLabel, frPctLabel, frLevelLabel);

        scores.getChildren().addAll(enBox, frBox);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button closeBtn = new Button("Fermer");
        closeBtn.getStyleClass().addAll("btn-rgb");
        closeBtn.setOnAction(ev -> resultsStage.close());
        actions.getChildren().add(closeBtn);

        root.getChildren().addAll(title, scores, actions);

        Scene scene = new Scene(root);
        File cssFile = Navigator.resolvePath("styles/educompus.css");
        if (cssFile != null && cssFile.exists()) {
            scene.getStylesheets().add(cssFile.toURI().toString());
        }
        resultsStage.setScene(scene);
        resultsStage.setResizable(false);
        resultsStage.sizeToScene();
        resultsStage.centerOnScreen();
        resultsStage.showAndWait();
    }
}
