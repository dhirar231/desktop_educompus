package com.educompus.controller.back;

import com.educompus.model.Chapitre;
import com.educompus.model.Cours;
import com.educompus.model.Td;
import com.educompus.model.VideoExplicative;
import com.educompus.model.SessionLive;
import com.educompus.model.SessionStatut;
import com.educompus.repository.CourseManagementRepository;
import com.educompus.repository.SessionLiveRepository;
import com.educompus.service.CoursValidationService;
import com.educompus.service.FormValidator;
import com.educompus.service.SessionLiveMetierService;
import com.educompus.service.SessionLiveValidationService;
import com.educompus.service.GoogleCalendarService;
import com.educompus.service.SessionNotificationService;
import com.educompus.service.ValidationResult;
import com.educompus.service.AIVideoGenerationService;
import com.educompus.util.Dialogs;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TabPane;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import java.awt.Desktop;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class BackCoursesController {
    private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");
    private static final List<String> NIVEAUX = List.of("1er", "2eme", "3eme", "4eme", "5eme");
    private static final List<String> DOMAINES = List.of(
            "Informatique", "Intelligence Artificielle", "Développement Web",
            "Développement Mobile", "Réseaux", "Cybersécurité",
            "Data Science", "Marketing", "Finance", "Comptabilité", "Design Graphique"
    );
    private final CourseManagementRepository repository = new CourseManagementRepository();

    private final ObservableList<Cours> coursItems = FXCollections.observableArrayList();
    private final ObservableList<Chapitre> chapitreItems = FXCollections.observableArrayList();
    private final ObservableList<Td> tdItems = FXCollections.observableArrayList();
    private final ObservableList<VideoExplicative> videoItems = FXCollections.observableArrayList();

    @FXML private Label statsCoursLabel;
    @FXML private Label statsChapitreLabel;
    @FXML private Label statsTdLabel;
    @FXML private Label statsVideoLabel;
    @FXML private TabPane mainTabPane;

    @FXML private TextField coursSearchField;
    @FXML private ComboBox<String> coursSortCombo;
    @FXML private ListView<Cours> coursListView;

    @FXML private TextField chapitreSearchField;
    @FXML private ComboBox<String> chapitreSortCombo;
    @FXML private ListView<Chapitre> chapitreListView;

    @FXML private TextField tdSearchField;
    @FXML private ComboBox<String> tdSortCombo;
    @FXML private ListView<Td> tdListView;

    @FXML private TextField videoSearchField;
    @FXML private ComboBox<String> videoSortCombo;
    @FXML private ListView<VideoExplicative> videoListView;

    @FXML
    private void initialize() {
        setupListViews();
        setupSorts();
        if (chapitreListView != null) {
            chapitreListView.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                    Chapitre sel = chapitreListView.getSelectionModel().getSelectedItem();
                    if (sel != null) openTdVideoChoice(sel);
                }
            });
        }
        if (coursListView != null) {
            coursListView.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                    Cours sel = coursListView.getSelectionModel().getSelectedItem();
                    if (sel != null) createChapitreForCourse(sel);
                }
            });
        }
        refreshAll();
        setupListViews();
        setupSessionFilters();
        setupCalendar();
        setupAutoStatusCallback();
    }

    private void setupSessionFilters() {
    }

    private void setupCalendar() {
    }

    private void setupAutoStatusCallback() {
    }

    @FXML
    private void openGoogleDriveManager() {
    }

    private void setupListViews() {
        // ── Cours ──
        if (coursListView != null) {
            coursListView.setCellFactory(lv -> new ListCell<>() {
                private final Label titleLbl = new Label();
                private final Label metaLbl = new Label();
                private final Label driveLbl = new Label(); // Nouveau : indicateur Google Drive
                private final Button editBtn = new Button("✏️");
                private final Button delBtn = new Button("🗑️");
                private final Button driveBtn = new Button("☁️"); // Nouveau : bouton Drive
                private final HBox row;
                {
                    titleLbl.getStyleClass().add("project-card-title");
                    metaLbl.getStyleClass().add("page-subtitle");
                    metaLbl.setStyle("-fx-font-size: 11px;");
                    driveLbl.getStyleClass().add("chip");
                    driveLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: 700;");
                    editBtn.getStyleClass().add("btn-rgb-outline");
                    delBtn.getStyleClass().add("btn-danger");
                    driveBtn.getStyleClass().add("btn-rgb-compact");
                    driveBtn.setTooltip(new javafx.scene.control.Tooltip("Ouvrir dans Google Drive"));

                    VBox info = new VBox(2, titleLbl, metaLbl);
                    HBox.setHgrow(info, Priority.ALWAYS);
                    row = new HBox(10, info, driveLbl, driveBtn, editBtn, delBtn);
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    row.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
                    editBtn.setOnAction(e -> { if (getItem() != null) editCours(getItem()); });
                    delBtn.setOnAction(e -> { if (getItem() != null) deleteCours(getItem()); });
                    driveBtn.setOnAction(e -> {
                        Cours cours = getItem();
                        if (cours != null) {
                            // Si déjà sur Drive, ouvrir le lien
                            if (cours.getDriveLink() != null && !cours.getDriveLink().isBlank()) {
                                try {
                                    java.awt.Desktop.getDesktop().browse(java.net.URI.create(cours.getDriveLink()));
                                } catch (Exception ex) {
                                    error("Erreur ouverture Drive", ex);
                                }
                            }
                            // Si en attente, afficher le statut
                            else if (cours.getDriveFolderId() != null && cours.getDriveFolderId().equals("EN_ATTENTE")) {
                                info("En attente", "Ce cours est en attente d'ajout dans Google Drive. Veuillez patienter...");
                            }
                            // Sinon, proposer d'ajouter dans Google Drive
                            else {
                                showGoogleDriveMenu(cours);
                            }
                        }
                    });
                }
                @Override protected void updateItem(Cours c, boolean empty) {
                    super.updateItem(c, empty);
                    if (empty || c == null) { setGraphic(null); return; }
                    titleLbl.setText(safe(c.getTitre()));
                    metaLbl.setText(safe(c.getDomaine()) + "  •  " + safe(c.getNiveau()) + "  •  " + safe(c.getNomFormateur()) + "  •  " + c.getDureeTotaleHeures() + "h  •  " + c.getChapitreCount() + " chap.");

                    // Affichage du statut Google Drive
                    if (c.getDriveLink() != null && !c.getDriveLink().isBlank()) {
                        driveLbl.setText("☁️ Sur Drive");
                        driveLbl.getStyleClass().removeAll("chip-warning", "chip-danger");
                        driveLbl.getStyleClass().add("chip-success");
                        driveBtn.setDisable(false);
                        driveBtn.setText("🌐");
                    } else if (c.getDriveFolderId() != null && c.getDriveFolderId().equals("EN_ATTENTE")) {
                        driveLbl.setText("⏳ En attente");
                        driveLbl.getStyleClass().removeAll("chip-success", "chip-danger");
                        driveLbl.getStyleClass().add("chip-warning");
                        driveBtn.setDisable(true);
                        driveBtn.setText("⏳");
                    } else {
                        driveLbl.setText("📱 Local");
                        driveLbl.getStyleClass().removeAll("chip-success", "chip-warning");
                        driveLbl.getStyleClass().add("chip-info");
                        driveBtn.setDisable(true);
                        driveBtn.setText("☁️");
                    }

                    setGraphic(row);
                }
            });
            // Double-click to add chapter
            coursListView.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && coursListView.getSelectionModel().getSelectedItem() != null) {
                    Cours selectedCours = coursListView.getSelectionModel().getSelectedItem();
                    FormResult<Chapitre> result = showChapitreForm(null, selectedCours);
                    if (!result.saved()) return;
                    repository.createChapitre(result.value());
                    refreshAll();
                    info("Succès", "Chapitre créé avec succès");
                }
            });
        }
        // ── Chapitre ──
        if (chapitreListView != null) {
            chapitreListView.setCellFactory(lv -> new ListCell<>() {
                private final Label numLbl = new Label();
                private final Label titleLbl = new Label();
                private final Label metaLbl = new Label();
                private final Button editBtn = new Button("✏️");
                private final Button delBtn = new Button("🗑️");
                private final HBox row;
                {
                    numLbl.getStyleClass().add("chapitre-num");
                    titleLbl.getStyleClass().add("project-card-title");
                    metaLbl.getStyleClass().add("page-subtitle");
                    metaLbl.setStyle("-fx-font-size: 11px;");
                    editBtn.getStyleClass().add("btn-rgb-outline");
                    delBtn.getStyleClass().add("btn-danger");
                    VBox info = new VBox(2, titleLbl, metaLbl);
                    HBox.setHgrow(info, Priority.ALWAYS);
                    row = new HBox(10, numLbl, info, editBtn, delBtn);
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    row.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
                    editBtn.setOnAction(e -> { if (getItem() != null) editChapitre(getItem()); });
                    delBtn.setOnAction(e -> { if (getItem() != null) deleteChapitre(getItem()); });
                }
                @Override protected void updateItem(Chapitre ch, boolean empty) {
                    super.updateItem(ch, empty);
                    if (empty || ch == null) { setGraphic(null); return; }
                    numLbl.setText(String.valueOf(ch.getOrdre()));
                    titleLbl.setText(safe(ch.getTitre()));
                    metaLbl.setText(safe(ch.getCoursTitre()) + "  •  " + safe(ch.getDomaine()) + "  •  " + ch.getTdCount() + " TD  •  " + ch.getVideoCount() + " vidéos");
                    setGraphic(row);
                }
            });
            // Double-click to add TD or Video
            chapitreListView.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && chapitreListView.getSelectionModel().getSelectedItem() != null) {
                    Chapitre selectedChapitre = chapitreListView.getSelectionModel().getSelectedItem();
                    showChapitreActionMenu(selectedChapitre, e);
                }
            });
        }
        // ── TD ──
        if (tdListView != null) {
            tdListView.setCellFactory(lv -> new ListCell<>() {
                private final Label titleLbl = new Label();
                private final Label metaLbl = new Label();
                private final Button openBtn = new Button("📄");
                private final Button editBtn = new Button("✏️");
                private final Button delBtn = new Button("🗑️");
                private final HBox row;
                {
                    titleLbl.getStyleClass().add("project-card-title");
                    metaLbl.getStyleClass().add("page-subtitle");
                    metaLbl.setStyle("-fx-font-size: 11px;");
                    openBtn.getStyleClass().add("btn-rgb-compact");
                    editBtn.getStyleClass().add("btn-rgb-outline");
                    delBtn.getStyleClass().add("btn-danger");
                    VBox info = new VBox(2, titleLbl, metaLbl);
                    HBox.setHgrow(info, Priority.ALWAYS);
                    row = new HBox(10, info, openBtn, editBtn, delBtn);
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    row.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
                    openBtn.setOnAction(e -> {
                        Td td = getItem();
                        if (td == null || td.getFichier() == null || td.getFichier().isBlank()) return;
                        try { Desktop.getDesktop().open(new File(td.getFichier())); } catch (Exception ex) { error("Erreur", ex); }
                    });
                    editBtn.setOnAction(e -> { if (getItem() != null) editTd(getItem()); });
                    delBtn.setOnAction(e -> { if (getItem() != null) deleteTd(getItem()); });
                }
                @Override protected void updateItem(Td td, boolean empty) {
                    super.updateItem(td, empty);
                    if (empty || td == null) { setGraphic(null); return; }
                    titleLbl.setText(safe(td.getTitre()));
                    metaLbl.setText(safe(td.getCoursTitre()) + "  •  " + safe(td.getChapitreTitre()) + "  •  " + safe(td.getDomaine()));
                    openBtn.setDisable(td.getFichier() == null || td.getFichier().isBlank());
                    setGraphic(row);
                }
            });
        }
        // ── Vidéo ──
        if (videoListView != null) {
            videoListView.setCellFactory(lv -> new ListCell<>() {
                private final Label titleLbl = new Label();
                private final Label metaLbl = new Label();
                private final Label statusLbl = new Label(); // Nouveau : statut AI
                private final Button playBtn = new Button("▶");
                private final Button editBtn = new Button("✏️");
                private final Button delBtn = new Button("🗑️");
                private final HBox row;
                {
                    titleLbl.getStyleClass().add("project-card-title");
                    metaLbl.getStyleClass().add("page-subtitle");
                    metaLbl.setStyle("-fx-font-size: 11px;");
                    statusLbl.getStyleClass().add("chip");
                    statusLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: 700;");
                    playBtn.getStyleClass().add("btn-rgb-compact");
                    editBtn.getStyleClass().add("btn-rgb-outline");
                    delBtn.getStyleClass().add("btn-danger");
                    VBox info = new VBox(2, titleLbl, metaLbl);
                    HBox.setHgrow(info, Priority.ALWAYS);
                    row = new HBox(10, info, statusLbl, playBtn, editBtn, delBtn);
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    row.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
                    playBtn.setOnAction(e -> {
                        VideoExplicative v = getItem();
                        if (v == null || v.getUrlVideo() == null || v.getUrlVideo().isBlank()) return;
                        try { com.educompus.util.UrlOpener.open(v.getUrlVideo()); } catch (Exception ex) { error("Erreur URL", ex); }
                    });
                    editBtn.setOnAction(e -> { if (getItem() != null) editVideo(getItem()); });
                    delBtn.setOnAction(e -> { if (getItem() != null) deleteVideo(getItem()); });
                }
                @Override protected void updateItem(VideoExplicative v, boolean empty) {
                    super.updateItem(v, empty);
                    if (empty || v == null) { setGraphic(null); return; }
                    titleLbl.setText(safe(v.getTitre()));
                    metaLbl.setText(safe(v.getCoursTitre()) + "  •  " + safe(v.getChapitreTitre()) + "  •  " + safe(v.getDomaine()));

                    // Affichage du statut selon le type de vidéo
                    if (v.isAIGenerated()) {
                        String status = safe(v.getGenerationStatus());
                        switch (status) {
                            case "PENDING" -> {
                                statusLbl.setText("🤖 En attente");
                                statusLbl.getStyleClass().removeAll("chip-success", "chip-danger", "chip-warning");
                                statusLbl.getStyleClass().add("chip-warning");
                                playBtn.setDisable(true);
                                playBtn.setText("⏳");
                            }
                            case "PROCESSING" -> {
                                statusLbl.setText("🤖 Génération...");
                                statusLbl.getStyleClass().removeAll("chip-success", "chip-danger", "chip-warning");
                                statusLbl.getStyleClass().add("chip-info");
                                playBtn.setDisable(true);
                                playBtn.setText("⚙️");
                            }
                            case "COMPLETED" -> {
                                statusLbl.setText("🤖 AI Générée");
                                statusLbl.getStyleClass().removeAll("chip-success", "chip-danger", "chip-warning", "chip-info");
                                statusLbl.getStyleClass().add("chip-success");
                                playBtn.setDisable(v.getUrlVideo() == null || v.getUrlVideo().isBlank());
                                playBtn.setText("▶");
                            }
                            case "ERROR" -> {
                                statusLbl.setText("❌ Erreur AI");
                                statusLbl.getStyleClass().removeAll("chip-success", "chip-warning", "chip-info");
                                statusLbl.getStyleClass().add("chip-danger");
                                playBtn.setDisable(true);
                                playBtn.setText("❌");
                            }
                            default -> {
                                statusLbl.setText("🤖 AI");
                                statusLbl.getStyleClass().removeAll("chip-success", "chip-danger", "chip-warning", "chip-info");
                                statusLbl.getStyleClass().add("chip-info");
                                playBtn.setDisable(v.getUrlVideo() == null || v.getUrlVideo().isBlank());
                                playBtn.setText("▶");
                            }
                        }
                    } else {
                        statusLbl.setText("🎬 Manuelle");
                        statusLbl.getStyleClass().removeAll("chip-success", "chip-danger", "chip-warning", "chip-info");
                        statusLbl.getStyleClass().add("chip-info");
                        playBtn.setDisable(v.getUrlVideo() == null || v.getUrlVideo().isBlank());
                        playBtn.setText("▶");
                    }

                    setGraphic(row);
                }
            });
        }
    }

    @FXML
    private void refreshAll() {
        reloadCours();
        reloadChapitres();
        reloadTds();
        reloadVideos();
        reloadSessions();
    }

    private void reloadCours() {
        coursItems.setAll(repository.listCours(text(coursSearchField)));
        applyCoursSort();
        if (coursListView != null) { coursListView.setItems(coursItems); coursListView.refresh(); }
        if (statsCoursLabel != null) statsCoursLabel.setText(String.valueOf(coursItems.size()));
    }

    private void reloadChapitres() {
        chapitreItems.setAll(repository.listChapitres(text(chapitreSearchField)));
        applyChapitreSort();
        if (chapitreListView != null) { chapitreListView.setItems(chapitreItems); chapitreListView.refresh(); }
        if (statsChapitreLabel != null) statsChapitreLabel.setText(String.valueOf(chapitreItems.size()));
    }

    private void reloadTds() {
        tdItems.setAll(repository.listTds(text(tdSearchField)));
        applyTdSort();
        if (tdListView != null) { tdListView.setItems(tdItems); tdListView.refresh(); }
        if (statsTdLabel != null) statsTdLabel.setText(String.valueOf(tdItems.size()));
    }

    private void reloadVideos() {
        videoItems.setAll(repository.listVideos(text(videoSearchField)));
        applyVideoSort();
        if (videoListView != null) { videoListView.setItems(videoItems); videoListView.refresh(); }
        if (statsVideoLabel != null) statsVideoLabel.setText(String.valueOf(videoItems.size()));
    }

    private void reloadSessions() {
    }

    private void setupSorts() {
        initSearchAndSort(coursSearchField, coursSortCombo, List.of("Titre A-Z", "Formateur"), this::reloadCours, this::applyCoursSort);
        initSearchAndSort(chapitreSearchField, chapitreSortCombo, List.of("Titre A-Z", "Ordre"), this::reloadChapitres, this::applyChapitreSort);
        initSearchAndSort(tdSearchField, tdSortCombo, List.of("Titre A-Z", "Cours"), this::reloadTds, this::applyTdSort);
        initSearchAndSort(videoSearchField, videoSortCombo, List.of("Titre A-Z"), this::reloadVideos, this::applyVideoSort);
    }

    private void initSearchAndSort(TextField search, ComboBox<String> sortCombo, List<String> items, Runnable reload, Runnable sortOnly) {
        if (search != null) search.textProperty().addListener((obs, o, n) -> reload.run());
        if (sortCombo != null) {
            sortCombo.getItems().setAll(items);
            sortCombo.setValue(items.get(0));
            sortCombo.valueProperty().addListener((obs, o, n) -> sortOnly.run());
        }
    }

    private void applyCoursSort() {
        String sort = coursSortCombo != null ? coursSortCombo.getValue() : null;
        if ("Formateur".equals(sort)) coursItems.sort(Comparator.comparing(c -> safe(c.getNomFormateur()), String.CASE_INSENSITIVE_ORDER));
        else coursItems.sort(Comparator.comparing(c -> safe(c.getTitre()), String.CASE_INSENSITIVE_ORDER));
    }

    private void applyChapitreSort() {
        String sort = chapitreSortCombo != null ? chapitreSortCombo.getValue() : null;
        if ("Ordre".equals(sort)) chapitreItems.sort(Comparator.comparingInt(Chapitre::getOrdre));
        else chapitreItems.sort(Comparator.comparing(c -> safe(c.getTitre()), String.CASE_INSENSITIVE_ORDER));
    }

    private void applyTdSort() {
        String sort = tdSortCombo != null ? tdSortCombo.getValue() : null;
        if ("Cours".equals(sort)) tdItems.sort(Comparator.comparing(c -> safe(c.getCoursTitre()), String.CASE_INSENSITIVE_ORDER));
        else tdItems.sort(Comparator.comparing(c -> safe(c.getTitre()), String.CASE_INSENSITIVE_ORDER));
    }

    private void applyVideoSort() {
        videoItems.sort(Comparator.comparing(c -> safe(c.getTitre()), String.CASE_INSENSITIVE_ORDER));
    }


    // ── CRUD ──────────────────────────────────────────────────────────────────

    @FXML
    private void createCours() {
        FormResult<Cours> result = showCoursForm(null);
        if (!result.saved()) return;
        try {
            repository.createCours(result.value());
            info("✅ Cours ajouté", "Le cours « " + safe(result.value().getTitre()) + " » a été ajouté avec succès.");
            reloadCours();
        } catch (Exception e) { error("Erreur ajout cours", e); }
    }

    @FXML
    private void createChapitre() { createChapitreForCourse(null); }

    @FXML
    private void createChapitreForCourse(Cours cours) {
        FormResult<Chapitre> result = showChapitreForm(null, cours);
        if (!result.saved()) return;
        try {
            repository.createChapitre(result.value());
            info("✅ Chapitre ajouté", "Le chapitre « " + safe(result.value().getTitre()) + " » a été ajouté avec succès.");
            refreshAll();
        } catch (Exception e) {
            error("Erreur ajout chapitre", e);
        }
    }

    private void openTdVideoChoice(Chapitre chapitre) {
        if (chapitre == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Ajouter contenu");
        alert.setHeaderText("Ajouter un TD ou une video pour le chapitre '" + safe(chapitre.getTitre()) + "'? ");
        alert.setContentText("Choisissez l'action souhaitée.");
        ButtonType tdButton = new ButtonType("Creer TD");
        ButtonType videoButton = new ButtonType("Creer video");
        ButtonType cancelButton = ButtonType.CANCEL;
        alert.getButtonTypes().setAll(tdButton, videoButton, cancelButton);
        Optional<ButtonType> choice = alert.showAndWait();
        if (choice.isEmpty() || choice.get() == cancelButton) return;
        if (choice.get() == tdButton) {
            createTdForChapitre(chapitre);
        } else if (choice.get() == videoButton) {
            createVideoForChapitre(chapitre);
        }
    }

    private void createTdForChapitre(Chapitre chapitre) {
        FormResult<Td> result = showTdForm(null, chapitre);
        if (!result.saved()) return;
        try {
            repository.createTd(result.value());
            info("✅ TD ajouté", "Le TD « " + safe(result.value().getTitre()) + " » a été ajouté avec succès.");
            refreshAll();
        } catch (Exception e) { error("Erreur ajout TD", e); }
    }

    private void createVideoForChapitre(Chapitre chapitre) {
        FormResult<VideoExplicative> result = showVideoForm(null, chapitre);
        if (!result.saved()) return;
        try {
            repository.createVideo(result.value());
            info("✅ Vidéo ajoutée", "La vidéo « " + safe(result.value().getTitre()) + " » a été ajoutée avec succès.");
            refreshAll();
        } catch (Exception e) { error("Erreur ajout vidéo", e); }
    }

    @FXML
    private void createTd() {
        FormResult<Td> result = showTdForm(null);
        if (!result.saved()) return;
        try {
            repository.createTd(result.value());
            info("✅ TD ajouté", "Le TD « " + safe(result.value().getTitre()) + " » a été ajouté avec succès.");
            refreshAll();
        } catch (Exception e) { error("Erreur ajout TD", e); }
    }

    @FXML
    private void createVideo() {
        FormResult<VideoExplicative> result = showVideoForm(null);
        if (!result.saved()) return;
        try {
            repository.createVideo(result.value());
            info("✅ Vidéo ajoutée", "La vidéo « " + safe(result.value().getTitre()) + " » a été ajoutée avec succès.");
            refreshAll();
        } catch (Exception e) { error("Erreur ajout vidéo", e); }
    }

    private void editCours(Cours cours) {
        if (!confirm("Modifier le cours", "Voulez-vous modifier le cours :\n« " + safe(cours.getTitre()) + " » ?")) return;
        FormResult<Cours> result = showCoursForm(cours);
        if (!result.saved()) return;
        try {
            repository.updateCours(result.value());

            // Logique Métier Avancé : Google Drive
            if (result.flag() && result.value().getImage() != null && !result.value().getImage().startsWith("auto:")) {
                if (result.value().getDriveFolderId() == null) {
                    result.value().setDriveFolderId("EN_ATTENTE");
                }
            } else {
                result.value().setDriveFolderId(null);
            }

            info("✅ Cours modifié", "Le cours « " + safe(result.value().getTitre()) + " » a été modifié avec succès.");
            refreshAll();
        } catch (Exception e) { error("Erreur modification cours", e); }
    }

    private void editChapitre(Chapitre chapitre) {
        if (!confirm("Modifier le chapitre", "Voulez-vous modifier le chapitre :\n« " + safe(chapitre.getTitre()) + " » ?")) return;
        FormResult<Chapitre> result = showChapitreForm(chapitre);
        if (!result.saved()) return;
        try {
            repository.updateChapitre(result.value());
            info("✅ Chapitre modifié", "Le chapitre « " + safe(result.value().getTitre()) + " » a été modifié avec succès.");
            refreshAll();
        } catch (Exception e) {
            error("Erreur — Modification chapitre", e);
        }
    }

    private void showGoogleDriveMenu(Cours cours) {
        ContextMenu menu = new ContextMenu();

        MenuItem addToDriveItem = new MenuItem("☁️ Ajouter dans Google Drive");
        addToDriveItem.setOnAction(e -> {
            if (!confirm("Ajouter dans Google Drive",
                    "Voulez-vous ajouter le cours « " + safe(cours.getTitre()) + " » dans Google Drive ?\n\n" +
                    "Cela créera un dossier pour ce cours et y ajoutera tous les chapitres, TDs et vidéos.")) {
                return;
            }

            try {
                // Marquer le cours comme en attente d'ajout dans Google Drive
                cours.setDriveFolderId("EN_ATTENTE");
                repository.updateCours(cours);

                info("✅ Ajout en cours", "Le cours « " + safe(cours.getTitre()) + " » a été marqué pour ajout dans Google Drive.\n\n" +
                        "Cela peut prendre quelques minutes. Veuillez rafraîchir la liste pour voir le statut.");

                // Lancer la synchronisation en arrière-plan
                lancerSynchronisationGoogleDrive(cours);

                refreshAll();
            } catch (Exception ex) {
                error("Erreur — Ajout Google Drive", ex);
            }
        });

        MenuItem selectFolderItem = new MenuItem("📁 Sélectionner un dossier Google Drive");
        selectFolderItem.setOnAction(e -> {
            // Ouvrir un dialogue pour sélectionner un dossier
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Sélectionner un dossier Google Drive");
            dialog.setHeaderText("Entrez l'ID ou le lien du dossier Google Drive");
            dialog.setContentText("ID du dossier ou lien complet :");

            var result = dialog.showAndWait();
            if (result.isPresent() && !result.get().isBlank()) {
                String folderId = extractFolderIdFromLink(result.get());

                try {
                    cours.setDriveFolderId(folderId);
                    repository.updateCours(cours);

                    info("✅ Dossier sélectionné", "Le dossier Google Drive a été associé au cours « " + safe(cours.getTitre()) + " ».");
                    refreshAll();
                } catch (Exception ex) {
                    error("Erreur — Sélection dossier", ex);
                }
            }
        });

        MenuItem viewDriveItem = new MenuItem("🌐 Ouvrir Google Drive");
        viewDriveItem.setOnAction(e -> {
            try {
                // Ouvrir la page principale de Google Drive
                java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://drive.google.com/drive/my-drive"));
            } catch (Exception ex) {
                error("Erreur ouverture Drive", ex);
            }
        });

        menu.getItems().addAll(addToDriveItem, new SeparatorMenuItem(), selectFolderItem, viewDriveItem);
        menu.show(coursListView, 100, 100);
    }

    private void lancerSynchronisationGoogleDrive(Cours cours) {
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    System.out.println("📤 Synchronisation Google Drive pour: " + cours.getTitre());
                    updateMessage("Initialisation de Google Drive...");

                    // Créer le service Google Drive
                    com.educompus.service.GoogleDriveService driveService =
                        new com.educompus.service.GoogleDriveService();

                    updateMessage("Création du dossier pour le cours...");

                    // Créer un dossier pour le cours sur Google Drive
                    String coursFolderId = driveService.getOrCreateCoursFolder(cours.getTitre());

                    updateMessage("Récupération des chapitres...");

                    // Récupérer tous les chapitres du cours
                    java.util.List<Chapitre> chapitres = repository.listChapitresByCoursId(cours.getId());

                    int totalItems = chapitres.size();
                    int processedItems = 0;

                    // Pour chaque chapitre, créer un sous-dossier
                    for (Chapitre chapitre : chapitres) {
                        updateMessage("Traitement du chapitre: " + chapitre.getTitre() +
                                    " (" + (processedItems + 1) + "/" + totalItems + ")");

                        // Créer le sous-dossier du chapitre
                        String chapitreFolderId = driveService.getOrCreateSubFolder(
                            coursFolderId,
                            sanitizeFolderName(chapitre.getTitre())
                        );

                        // Uploader le fichier PDF du chapitre si disponible
                        if (chapitre.getFichierC() != null && !chapitre.getFichierC().isBlank()) {
                            java.io.File fichier = new java.io.File(chapitre.getFichierC());
                            if (fichier.exists()) {
                                updateMessage("Upload du chapitre: " + chapitre.getTitre());
                                driveService.uploadChapitreFile(
                                    chapitre.getFichierC(),
                                    fichier.getName(),
                                    cours.getTitre(),
                                    chapitre.getTitre()
                                );
                            }
                        }

                        // Uploader les TDs du chapitre
                        java.util.List<Td> allTds = repository.listTds("");
                        for (Td td : allTds) {
                            if (td.getChapitreId() == chapitre.getId()) {
                                if (td.getFichier() != null && !td.getFichier().isBlank()) {
                                    java.io.File fichier = new java.io.File(td.getFichier());
                                    if (fichier.exists()) {
                                        updateMessage("Upload du TD: " + td.getTitre());
                                        driveService.uploadTdFile(
                                            td.getFichier(),
                                            fichier.getName(),
                                            cours.getTitre(),
                                            chapitre.getTitre()
                                        );
                                    }
                                }
                            }
                        }

                        processedItems++;
                        updateProgress(processedItems, totalItems);
                    }

                    updateMessage("Finalisation...");

                    // Générer le lien vers le dossier du cours
                    String driveLink = "https://drive.google.com/drive/folders/" + coursFolderId;

                    // Mettre à jour le cours avec le lien Google Drive
                    cours.setDriveFolderId(coursFolderId);
                    cours.setDriveLink(driveLink);
                    repository.updateCours(cours);

                    System.out.println("✅ Synchronisation terminée pour: " + cours.getTitre());
                    System.out.println("📁 Lien Drive: " + driveLink);

                    javafx.application.Platform.runLater(() -> {
                        info("✅ Synchronisation terminée",
                             "Le cours « " + safe(cours.getTitre()) + " » a été ajouté à Google Drive avec succès!\n\n" +
                             "Dossier créé avec " + chapitres.size() + " chapitre(s).\n\n" +
                             "Cliquez sur le bouton ☁️ du cours pour ouvrir le dossier Google Drive.");
                        refreshAll();
                    });
                } catch (Exception e) {
                    System.err.println("❌ Erreur synchronisation: " + e.getMessage());
                    e.printStackTrace();

                    // En cas d'erreur, réinitialiser le statut
                    cours.setDriveFolderId(null);
                    try {
                        repository.updateCours(cours);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    javafx.application.Platform.runLater(() -> {
                        String errorMsg = e.getMessage();
                        if (errorMsg != null && (errorMsg.contains("credentials") || errorMsg.contains("FileNotFoundException"))) {
                            // Créer un dialogue personnalisé pour l'erreur de credentials
                            Alert alert = new Alert(Alert.AlertType.ERROR);
                            alert.setTitle("Erreur d'authentification");
                            alert.setHeaderText("Configuration Google Drive requise");

                            VBox content = new VBox(10);
                            content.setPadding(new javafx.geometry.Insets(10));

                            Label msg1 = new Label("Veuillez configurer le fichier credentials.json pour Google Drive.");
                            msg1.setWrapText(true);
                            msg1.setStyle("-fx-font-size: 13px;");

                            Label msg2 = new Label("📁 Emplacement requis : src/main/resources/credentials.json");
                            msg2.setWrapText(true);
                            msg2.setStyle("-fx-font-size: 12px; -fx-text-fill: #666; -fx-font-family: monospace;");

                            Label msg3 = new Label("📚 Guide rapide (5 minutes) :");
                            msg3.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

                            javafx.scene.control.Hyperlink link1 = new javafx.scene.control.Hyperlink("GOOGLE_DRIVE_QUICK_SETUP.md");
                            link1.setOnAction(ev -> {
                                try {
                                    java.awt.Desktop.getDesktop().open(new java.io.File("GOOGLE_DRIVE_QUICK_SETUP.md"));
                                } catch (Exception ex) {
                                    System.err.println("Impossible d'ouvrir le fichier: " + ex.getMessage());
                                }
                            });

                            Label msg4 = new Label("📖 Documentation complète :");
                            msg4.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

                            javafx.scene.control.Hyperlink link2 = new javafx.scene.control.Hyperlink("docs/GOOGLE_DRIVE_SETUP.md");
                            link2.setOnAction(ev -> {
                                try {
                                    java.awt.Desktop.getDesktop().open(new java.io.File("docs/GOOGLE_DRIVE_SETUP.md"));
                                } catch (Exception ex) {
                                    System.err.println("Impossible d'ouvrir le fichier: " + ex.getMessage());
                                }
                            });

                            Label msg5 = new Label("🌐 Console Google Cloud :");
                            msg5.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

                            javafx.scene.control.Hyperlink link3 = new javafx.scene.control.Hyperlink("https://console.cloud.google.com/");
                            link3.setOnAction(ev -> {
                                try {
                                    java.awt.Desktop.getDesktop().browse(java.net.URI.create("https://console.cloud.google.com/"));
                                } catch (Exception ex) {
                                    System.err.println("Impossible d'ouvrir le lien: " + ex.getMessage());
                                }
                            });

                            content.getChildren().addAll(msg1, msg2, new javafx.scene.control.Separator(),
                                                        msg3, link1, msg4, link2, msg5, link3);

                            alert.getDialogPane().setContent(content);
                            alert.getDialogPane().setPrefWidth(500);
                            Dialogs.style(alert);
                            alert.showAndWait();
                        } else {
                            error("Erreur synchronisation", e);
                        }
                        refreshAll();
                    });
                }
                return null;
            }
        };

        new Thread(task).setDaemon(true);
        new Thread(task).start();
    }

    private String sanitizeFolderName(String name) {
        if (name == null) return "Sans_titre";
        // Remplacer les caractères invalides pour les noms de dossiers
        return name.replaceAll("[/\\\\:*?\"<>|]", "_").trim();
    }

    private String extractFolderIdFromLink(String input) {
        // Extraire l'ID du dossier à partir d'un lien Google Drive
        // Format: https://drive.google.com/drive/folders/FOLDER_ID
        if (input.contains("/folders/")) {
            String[] parts = input.split("/folders/");
            if (parts.length > 1) {
                return parts[1].split("[?#]")[0]; // Enlever les paramètres
            }
        }
        // Si c'est déjà un ID, le retourner tel quel
        return input.trim();
    }

    private void showChapitreActionMenu(Chapitre chapitre, javafx.scene.input.MouseEvent event) {
        ContextMenu menu = new ContextMenu();

        MenuItem addTdItem = new MenuItem("➕ Ajouter un TD");
        addTdItem.setOnAction(e -> {
            FormResult<Td> result = showTdForm(null, chapitre);
            if (!result.saved()) return;
            try {
                repository.createTd(result.value());
                info("✅ TD créé", "Le TD a été créé avec succès");
                refreshAll();
            } catch (Exception ex) {
                error("Erreur — Création TD", ex);
            }
        });

        MenuItem addVideoItem = new MenuItem("➕ Ajouter une vidéo explicative");
        addVideoItem.setOnAction(e -> {
            FormResult<VideoExplicative> result = showVideoForm(null, chapitre);
            if (!result.saved()) return;
            try {
                int videoId = repository.createVideoExplicative(result.value());

                // Si c'est une vidéo AI, déclencher la génération
                if (result.flag()) {
                    info("✅ Vidéo créée", "La vidéo explicative a été créée avec succès.\nGénération AI en cours...");

                    // Récupérer la vidéo créée
                    VideoExplicative video = repository.getVideoExplicativeById(videoId);
                    if (video != null) {
                        // Déclencher la génération asynchrone
                        lancerGenerationVideoAI(video);
                    }
                } else {
                    info("✅ Vidéo créée", "La vidéo explicative a été créée avec succès");
                }

                refreshAll();
            } catch (Exception ex) {
                error("Erreur — Création vidéo", ex);
            }
        });

        menu.getItems().addAll(addTdItem, addVideoItem);
        menu.show(chapitreListView, event.getScreenX(), event.getScreenY());
    }

    private void lancerGenerationVideoAI(VideoExplicative video) {
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    System.out.println("🎬 Démarrage génération vidéo AI: " + video.getTitre());

                    // Utiliser le service de génération AI
                    var result = AIVideoGenerationService.generateVideo(
                        video.getDescription(),
                        video.getCoursTitre(),
                        video.getNiveau(),
                        video.getDomaine()
                    ).get(); // Attendre le résultat

                    if (result.isSuccess()) {
                        // Mettre à jour la vidéo avec l'URL générée
                        video.setUrlVideo(result.getVideoUrl());
                        video.setGenerationStatus("COMPLETED");
                        video.setAiScript(result.getScript());
                        repository.updateVideoExplicative(video);

                        System.out.println("✅ Vidéo générée avec succès: " + result.getVideoUrl());

                        javafx.application.Platform.runLater(() -> {
                            info("✅ Génération terminée", "La vidéo AI a été générée avec succès!");
                            refreshAll();
                        });
                    } else {
                        // Erreur de génération
                        video.setGenerationStatus("ERROR");
                        repository.updateVideoExplicative(video);

                        System.err.println("❌ Erreur génération: " + result.getErrorMessage());

                        javafx.application.Platform.runLater(() -> {
                            error("Erreur génération", new Exception(result.getErrorMessage()));
                            refreshAll();
                        });
                    }
                } catch (Exception e) {
                    System.err.println("❌ Exception génération: " + e.getMessage());
                    e.printStackTrace();

                    video.setGenerationStatus("ERROR");
                    try {
                        repository.updateVideoExplicative(video);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    javafx.application.Platform.runLater(() -> {
                        error("Erreur génération vidéo", e);
                        refreshAll();
                    });
                }
                return null;
            }
        };

        // Lancer la tâche en arrière-plan
        new Thread(task).setDaemon(true);
        new Thread(task).start();
    }

    private void editTd(Td td) {
        if (!confirm("Modifier le TD", "Voulez-vous modifier le TD :\n« " + safe(td.getTitre()) + " » ?")) return;
        FormResult<Td> result = showTdForm(td);
        if (!result.saved()) return;
        try {
            repository.updateTd(result.value());
            info("✅ TD modifié", "Le TD « " + safe(result.value().getTitre()) + " » a été modifié avec succès.");
            refreshAll();
        } catch (Exception e) { error("Erreur modification TD", e); }
    }

    private void editVideo(VideoExplicative video) {
        if (!confirm("Modifier la vidéo", "Voulez-vous modifier la vidéo :\n« " + safe(video.getTitre()) + " » ?")) return;
        FormResult<VideoExplicative> result = showVideoForm(video);
        if (!result.saved()) return;
        try {
            repository.updateVideo(result.value());
            info("✅ Vidéo modifiée", "La vidéo « " + safe(result.value().getTitre()) + " » a été modifiée avec succès.");
            refreshAll();
        } catch (Exception e) { error("Erreur modification vidéo", e); }
    }

    private void deleteCours(Cours cours) {
        if (!confirm("⚠ Supprimer le cours", "Êtes-vous sûr de vouloir supprimer le cours :\n« " + safe(cours.getTitre()) + " » ?\n\nTous ses chapitres, TDs et vidéos seront supprimés.")) return;
        try {
            repository.deleteCours(cours.getId());
            info("🗑 Cours supprimé", "Le cours « " + safe(cours.getTitre()) + " » a été supprimé.");
            refreshAll();
        } catch (Exception e) { error("Erreur suppression cours", e); }
    }

    private void deleteChapitre(Chapitre chapitre) {
        if (!confirm("⚠ Supprimer le chapitre", "Êtes-vous sûr de vouloir supprimer le chapitre :\n« " + safe(chapitre.getTitre()) + " » ?\n\nSes TDs et vidéos associés seront supprimés.")) return;
        try {
            repository.deleteChapitre(chapitre.getId());
            info("🗑 Chapitre supprimé", "Le chapitre « " + safe(chapitre.getTitre()) + " » a été supprimé.");
            refreshAll();
        } catch (Exception e) { error("Erreur suppression chapitre", e); }
    }

    private void deleteTd(Td td) {
        if (!confirm("⚠ Supprimer le TD", "Êtes-vous sûr de vouloir supprimer le TD :\n« " + safe(td.getTitre()) + " » ?")) return;
        try {
            repository.deleteTd(td.getId());
            info("🗑 TD supprimé", "Le TD « " + safe(td.getTitre()) + " » a été supprimé.");
            refreshAll();
        } catch (Exception e) { error("Erreur suppression TD", e); }
    }

    private void deleteVideo(VideoExplicative video) {
        if (!confirm("⚠ Supprimer la vidéo", "Êtes-vous sûr de vouloir supprimer la vidéo :\n« " + safe(video.getTitre()) + " » ?")) return;
        try {
            repository.deleteVideo(video.getId());
            info("🗑 Vidéo supprimée", "La vidéo « " + safe(video.getTitre()) + " » a été supprimée.");
            refreshAll();
        } catch (Exception e) { error("Erreur suppression vidéo", e); }
    }


    // ── Formulaires ───────────────────────────────────────────────────────────

    private FormResult<Cours> showCoursForm(Cours source) {
        TextField titreField = field();
        TextArea descriptionArea = area();
        ComboBox<String> niveauCombo = comboStrings(NIVEAUX);
        ComboBox<String> domaineCombo = comboStrings(DOMAINES);
        TextField formateurField = field();
        TextField dureeField = field();
        TextField imageField = field();
        imageField.setPromptText("Chemin image .png ou .jpg (optionnel)");
        Button browseImageBtn = new Button("Parcourir");
        browseImageBtn.getStyleClass().add("btn-rgb-outline");
        browseImageBtn.setOnAction(ev -> {
            File sel = chooseFile("Choisir une image", List.of("png", "jpg", "jpeg"), "Images", imageField);
            if (sel != null) imageField.setText(sel.getAbsolutePath());
        });
        HBox imageBox = new HBox(8, imageField, browseImageBtn);
        imageBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(imageField, Priority.ALWAYS);

        if (source == null) { niveauCombo.setValue(NIVEAUX.get(0)); domaineCombo.setValue(DOMAINES.get(0)); }
        if (source != null) {
            titreField.setText(safe(source.getTitre()));
            descriptionArea.setText(safe(source.getDescription()));
            niveauCombo.setValue(safe(source.getNiveau()));
            domaineCombo.setValue(safe(source.getDomaine()));
            formateurField.setText(safe(source.getNomFormateur()));
            dureeField.setText(source.getDureeTotaleHeures() <= 0 ? "" : String.valueOf(source.getDureeTotaleHeures()));
            imageField.setText(safe(source.getImage()).startsWith("auto:") ? "" : safe(source.getImage()));
        }

        GridPane grid = formGrid();
        Label errTitre = addRow(grid, 0, "Titre *", titreField);
        Label errDesc = addRow(grid, 1, "Description *", descriptionArea);
        Label errNiveau = addRow(grid, 2, "Niveau *", niveauCombo);
        Label errDomaine = addRow(grid, 3, "Domaine *", domaineCombo);
        Label errFormateur = addRow(grid, 4, "Nom du formateur *", formateurField);
        Label errDuree = addRow(grid, 5, "Duree totale (heures) *", dureeField);
        Label errImage = addRow(grid, 6, "Image (png/jpg)", imageBox);

        javafx.scene.control.CheckBox importantCheck = new javafx.scene.control.CheckBox("Cours Important (Sauvegarder dans Google Drive une fois validé)");
        if (source != null && source.getDriveFolderId() != null) importantCheck.setSelected(true);
        importantCheck.setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
        Label errDrive = addRow(grid, 7, "Google Drive", importantCheck);

        niveauCombo.valueProperty().addListener((obs, o, n) -> { if (n == null) { niveauCombo.setStyle("-fx-border-color: #d6293e; -fx-border-width: 2; -fx-border-radius: 8px;"); errNiveau.setText("⚠ Veuillez selectionner un niveau."); } else { niveauCombo.setStyle(""); errNiveau.setText(""); } });
        domaineCombo.valueProperty().addListener((obs, o, n) -> { if (n == null) { domaineCombo.setStyle("-fx-border-color: #d6293e; -fx-border-width: 2; -fx-border-radius: 8px;"); errDomaine.setText("⚠ Veuillez selectionner un domaine."); } else { domaineCombo.setStyle(""); errDomaine.setText(""); } });
        liveValidate(imageField, errImage, () -> { ValidationResult r = new ValidationResult(); String v = imageField.getText().trim(); if (!v.isBlank()) { String vl = v.toLowerCase(); if (!vl.endsWith(".png") && !vl.endsWith(".jpg") && !vl.endsWith(".jpeg")) r.addError("Doit etre .png ou .jpg"); } return r; });

        liveValidate(titreField, errTitre, () -> CoursValidationService.validateChapitreTitre(titreField.getText()));
        liveValidate(descriptionArea, errDesc, () -> { ValidationResult r = new ValidationResult(); String v = descriptionArea.getText().trim(); if (v.isBlank()) r.addError("Obligatoire."); else if (v.length() < 10) r.addError("Min 10 caractères."); else if (v.chars().anyMatch(Character::isDigit)) r.addError("Pas de chiffres."); return r; });
        liveValidate(formateurField, errFormateur, () -> { ValidationResult r = new ValidationResult(); String v = formateurField.getText().trim(); if (v.isBlank()) r.addError("Obligatoire."); else if (v.chars().anyMatch(Character::isDigit)) r.addError("Pas de chiffres."); return r; });
        liveValidate(dureeField, errDuree, () -> CoursValidationService.validateDureeStr(dureeField.getText()));
        liveValidate(imageField, errImage, () -> { ValidationResult r = new ValidationResult(); String v = imageField.getText().trim(); if (!v.isBlank()) { String vl = v.toLowerCase(); if (!vl.endsWith(".png") && !vl.endsWith(".jpg") && !vl.endsWith(".jpeg")) r.addError("Doit être .png ou .jpg"); } return r; });

        Dialog<ButtonType> dialog = buildFormDialog(source == null ? "Créer un cours" : "Modifier un cours", grid);
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            boolean err = false;
            if (titreField.getText().trim().isBlank()) { FormValidator.markError(titreField, "Obligatoire"); errTitre.setText("⚠ Titre obligatoire."); err = true; }
            if (descriptionArea.getText().trim().length() < 10) { FormValidator.markError(descriptionArea, "Min 10 car."); errDesc.setText("⚠ Min 10 caractères."); err = true; }
            if (formateurField.getText().trim().isBlank()) { FormValidator.markError(formateurField, "Obligatoire"); errFormateur.setText("⚠ Formateur obligatoire."); err = true; }
            if (dureeField.getText().trim().isBlank()) { FormValidator.markError(dureeField, "Obligatoire"); errDuree.setText("⚠ Durée obligatoire."); err = true; }
            if (err) ev.consume();
        });

        Optional<ButtonType> answer = dialog.showAndWait();
        if (answer.isEmpty() || answer.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) return FormResult.cancelled();

        Cours cours = source == null ? new Cours() : source;
        cours.setTitre(text(titreField));
        cours.setDescription(text(descriptionArea));
        cours.setNiveau(safe(niveauCombo.getValue()));
        cours.setDomaine(safe(domaineCombo.getValue()));
        cours.setNomFormateur(text(formateurField));
        cours.setDureeTotaleHeures(parseDuree(text(dureeField)));
        String imgVal = text(imageField);
        cours.setImage(imgVal.isBlank() ? "auto:" + safe(domaineCombo.getValue()).toLowerCase() : imgVal);
        return FormResult.saved(cours, importantCheck.isSelected());
    }

    private FormResult<Chapitre> showChapitreForm(Chapitre source) { return showChapitreForm(source, null); }

    private FormResult<Chapitre> showChapitreForm(Chapitre source, Cours selectedCours) {
        ComboBox<Cours> coursCombo = comboCours();
        TextField titreField = field();
        TextField ordreField = field();
        TextArea descriptionArea = area();
        TextField fichierField = field();
        Button browseBtn = new Button("Parcourir");
        browseBtn.getStyleClass().add("btn-rgb-outline");
        browseBtn.setOnAction(ev -> { File f = chooseFile("PDF", List.of("pdf"), "PDF", fichierField); if (f != null) fichierField.setText(f.getAbsolutePath()); });
        HBox fichierBox = new HBox(8, fichierField, browseBtn);
        fichierBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(fichierField, Priority.ALWAYS);
        ComboBox<String> niveauCombo = comboStrings(NIVEAUX);
        ComboBox<String> domaineCombo = comboStrings(DOMAINES);

        if (selectedCours != null) {
            selectCours(coursCombo, selectedCours.getId());
            coursCombo.setDisable(true);
            niveauCombo.setValue(safe(selectedCours.getNiveau()));
            domaineCombo.setValue(safe(selectedCours.getDomaine()));
            niveauCombo.setDisable(true); domaineCombo.setDisable(true);
        }
        coursCombo.valueProperty().addListener((obs, o, n) -> {
            if (n != null) { niveauCombo.setValue(safe(n.getNiveau())); domaineCombo.setValue(safe(n.getDomaine())); niveauCombo.setDisable(true); domaineCombo.setDisable(true); }
            else { niveauCombo.setValue(null); domaineCombo.setValue(null); niveauCombo.setDisable(false); domaineCombo.setDisable(false); }
        });
        if (source != null) {
            selectCours(coursCombo, source.getCoursId());
            titreField.setText(safe(source.getTitre()));
            ordreField.setText(source.getOrdre() <= 0 ? "" : String.valueOf(source.getOrdre()));
            descriptionArea.setText(safe(source.getDescription()));
            fichierField.setText(safe(source.getFichierC()));
            niveauCombo.setValue(safe(source.getNiveau()));
            domaineCombo.setValue(safe(source.getDomaine()));
        }

        GridPane grid = formGrid();
        Label errCours = addRow(grid, 0, "Cours *", coursCombo);
        Label errTitre = addRow(grid, 1, "Titre *", titreField);
        Label errOrdre = addRow(grid, 2, "Ordre *", ordreField);
        Label errDesc = addRow(grid, 3, "Description *", descriptionArea);
        Label errFichier = addRow(grid, 4, "Fichier PDF *", fichierBox);
        Label errNiveau = addRow(grid, 5, "Niveau *", niveauCombo);
        Label errDomaine = addRow(grid, 6, "Domaine *", domaineCombo);

        liveValidate(titreField, errTitre, () -> CoursValidationService.validateChapitreTitre(titreField.getText()));
        liveValidate(ordreField, errOrdre, () -> CoursValidationService.validateChapitreOrdre(ordreField.getText()));
        liveValidate(fichierField, errFichier, () -> { ValidationResult r = new ValidationResult(); String v = fichierField.getText().trim(); if (v.isBlank()) r.addError("Obligatoire."); else if (!v.toLowerCase().endsWith(".pdf")) r.addError("Doit être un .pdf"); return r; });
        liveValidate(descriptionArea, errDesc, () -> { ValidationResult r = new ValidationResult(); String v = descriptionArea.getText().trim(); if (v.isBlank()) r.addError("Obligatoire."); else if (v.length() < 10) r.addError("Min 10 caractères."); else if (v.chars().anyMatch(Character::isDigit)) r.addError("Pas de chiffres."); return r; });

        Dialog<ButtonType> dialog = buildFormDialog(source == null ? "Créer un chapitre" : "Modifier un chapitre", grid);
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            boolean err = false;
            if (coursCombo.getValue() == null) { FormValidator.markError(coursCombo, "Obligatoire"); errCours.setText("⚠ Cours obligatoire."); err = true; }
            if (titreField.getText().trim().isBlank()) { FormValidator.markError(titreField, "Obligatoire"); errTitre.setText("⚠ Titre obligatoire."); err = true; }
            if (ordreField.getText().trim().isBlank()) { FormValidator.markError(ordreField, "Obligatoire"); errOrdre.setText("⚠ Ordre obligatoire."); err = true; }
            if (fichierField.getText().trim().isBlank()) { FormValidator.markError(fichierField, "Obligatoire"); errFichier.setText("⚠ Fichier obligatoire."); err = true; }
            if (descriptionArea.getText().trim().length() < 10) { FormValidator.markError(descriptionArea, "Min 10 car."); errDesc.setText("⚠ Min 10 caractères."); err = true; }
            if (err) ev.consume();
        });

        Optional<ButtonType> answer = dialog.showAndWait();
        if (answer.isEmpty() || answer.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) return FormResult.cancelled();

        Chapitre chapitre = source == null ? new Chapitre() : source;
        chapitre.setCoursId(coursCombo.getValue().getId());
        chapitre.setTitre(text(titreField));
        chapitre.setOrdre(parseDuree(text(ordreField)));
        chapitre.setDescription(text(descriptionArea));
        chapitre.setFichierC(text(fichierField));
        chapitre.setNiveau(safe(niveauCombo.getValue()));
        chapitre.setDomaine(safe(domaineCombo.getValue()));
        return FormResult.saved(chapitre);
    }

    private FormResult<Td> showTdForm(Td source) { return showTdForm(source, null); }

    private FormResult<Td> showTdForm(Td source, Chapitre selectedChapitre) {
        ComboBox<Cours> coursCombo = comboCours();
        ComboBox<Chapitre> chapitreCombo = comboChapitre();
        bindChapitreComboToCours(coursCombo, chapitreCombo);
        TextField titreField = field();
        TextArea descriptionArea = area();
        TextField fichierField = field();
        Button browseBtn = new Button("Parcourir");
        browseBtn.getStyleClass().add("btn-rgb-outline");
        browseBtn.setOnAction(ev -> { File f = chooseFile("PDF", List.of("pdf"), "PDF", fichierField); if (f != null) fichierField.setText(f.getAbsolutePath()); });
        HBox fichierBox = new HBox(8, fichierField, browseBtn);
        fichierBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(fichierField, Priority.ALWAYS);
        ComboBox<String> niveauCombo = comboStrings(NIVEAUX);
        ComboBox<String> domaineCombo = comboStrings(DOMAINES);

        if (selectedChapitre != null) {
            selectCours(coursCombo, selectedChapitre.getCoursId());
            chapitreCombo.setItems(filteredChapitres(selectedChapitre.getCoursId()));
            selectChapitre(chapitreCombo, selectedChapitre.getId());
            coursCombo.setDisable(true); chapitreCombo.setDisable(true);
            niveauCombo.setValue(safe(selectedChapitre.getNiveau()));
            domaineCombo.setValue(safe(selectedChapitre.getDomaine()));
            niveauCombo.setDisable(true); domaineCombo.setDisable(true);
        }
        chapitreCombo.valueProperty().addListener((obs, o, n) -> {
            if (n != null) { niveauCombo.setValue(safe(n.getNiveau())); domaineCombo.setValue(safe(n.getDomaine())); niveauCombo.setDisable(true); domaineCombo.setDisable(true); }
            else { niveauCombo.setValue(null); domaineCombo.setValue(null); niveauCombo.setDisable(false); domaineCombo.setDisable(false); }
        });
        if (source != null) {
            selectCours(coursCombo, source.getCoursId());
            chapitreCombo.setItems(filteredChapitres(source.getCoursId()));
            selectChapitre(chapitreCombo, source.getChapitreId());
            titreField.setText(safe(source.getTitre()));
            descriptionArea.setText(safe(source.getDescription()));
            fichierField.setText(safe(source.getFichier()));
            niveauCombo.setValue(safe(source.getNiveau()));
            domaineCombo.setValue(safe(source.getDomaine()));
        }

        GridPane grid = formGrid();
        Label errCours = addRow(grid, 0, "Cours *", coursCombo);
        Label errChapitre = addRow(grid, 1, "Chapitre *", chapitreCombo);
        Label errTitre = addRow(grid, 2, "Titre *", titreField);
        Label errDesc = addRow(grid, 3, "Description *", descriptionArea);
        Label errFichier = addRow(grid, 4, "Fichier PDF *", fichierBox);
        Label errNiveau = addRow(grid, 5, "Niveau *", niveauCombo);
        Label errDomaine = addRow(grid, 6, "Domaine *", domaineCombo);

        liveValidate(titreField, errTitre, () -> CoursValidationService.validateTdTitre(titreField.getText()));
        liveValidate(fichierField, errFichier, () -> { ValidationResult r = new ValidationResult(); String v = fichierField.getText().trim(); if (v.isBlank()) r.addError("Obligatoire."); else if (!v.toLowerCase().endsWith(".pdf")) r.addError("Doit être un .pdf"); return r; });
        liveValidate(descriptionArea, errDesc, () -> { ValidationResult r = new ValidationResult(); String v = descriptionArea.getText().trim(); if (v.isBlank()) r.addError("Obligatoire."); else if (v.length() < 10) r.addError("Min 10 caractères."); else if (v.chars().anyMatch(Character::isDigit)) r.addError("Pas de chiffres."); return r; });

        Dialog<ButtonType> dialog = buildFormDialog(source == null ? "Créer un TD" : "Modifier un TD", grid);
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            boolean err = false;
            if (coursCombo.getValue() == null) { FormValidator.markError(coursCombo, "Obligatoire"); errCours.setText("⚠ Cours obligatoire."); err = true; }
            if (chapitreCombo.getValue() == null) { FormValidator.markError(chapitreCombo, "Obligatoire"); errChapitre.setText("⚠ Chapitre obligatoire."); err = true; }
            if (titreField.getText().trim().isBlank()) { FormValidator.markError(titreField, "Obligatoire"); errTitre.setText("⚠ Titre obligatoire."); err = true; }
            if (fichierField.getText().trim().isBlank()) { FormValidator.markError(fichierField, "Obligatoire"); errFichier.setText("⚠ Fichier obligatoire."); err = true; }
            if (descriptionArea.getText().trim().length() < 10) { FormValidator.markError(descriptionArea, "Min 10 car."); errDesc.setText("⚠ Min 10 caractères."); err = true; }
            if (err) ev.consume();
        });

        Optional<ButtonType> answer = dialog.showAndWait();
        if (answer.isEmpty() || answer.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) return FormResult.cancelled();

        Td td = source == null ? new Td() : source;
        td.setCoursId(coursCombo.getValue().getId());
        td.setChapitreId(chapitreCombo.getValue().getId());
        td.setTitre(text(titreField));
        td.setDescription(text(descriptionArea));
        td.setFichier(text(fichierField));
        td.setNiveau(safe(niveauCombo.getValue()));
        td.setDomaine(safe(domaineCombo.getValue()));
        return FormResult.saved(td);
    }

    private FormResult<VideoExplicative> showVideoForm(VideoExplicative source) { return showVideoForm(source, null); }

    private FormResult<VideoExplicative> showVideoForm(VideoExplicative source, Chapitre selectedChapitre) {
        ComboBox<Cours> coursCombo = comboCours();
        ComboBox<Chapitre> chapitreCombo = comboChapitre();
        bindChapitreComboToCours(coursCombo, chapitreCombo);
        TextField titreField = field();
        TextField urlField = field();
        TextArea descriptionArea = area();
        ComboBox<String> niveauCombo = comboStrings(NIVEAUX);
        ComboBox<String> domaineCombo = comboStrings(DOMAINES);

        // Nouvelle option : Générer avec AI
        javafx.scene.control.CheckBox aiGenerateCheck = new javafx.scene.control.CheckBox("🤖 Générer vidéo AI depuis la description");
        aiGenerateCheck.setStyle("-fx-text-fill: #9333ea; -fx-font-weight: bold; -fx-font-size: 12px;");

        // Quand AI est activé, désactiver le champ URL
        aiGenerateCheck.selectedProperty().addListener((obs, old, newVal) -> {
            urlField.setDisable(newVal);
            if (newVal) {
                urlField.setText(""); // Vider l'URL
                urlField.setPromptText("URL générée automatiquement par l'AI");
            } else {
                urlField.setPromptText("https://youtube.com/watch?v=...");
            }
        });

        if (selectedChapitre != null) {
            selectCours(coursCombo, selectedChapitre.getCoursId());
            chapitreCombo.setItems(filteredChapitres(selectedChapitre.getCoursId()));
            selectChapitre(chapitreCombo, selectedChapitre.getId());
            coursCombo.setDisable(true); chapitreCombo.setDisable(true);
            niveauCombo.setValue(safe(selectedChapitre.getNiveau()));
            domaineCombo.setValue(safe(selectedChapitre.getDomaine()));
            niveauCombo.setDisable(true); domaineCombo.setDisable(true);
        }
        chapitreCombo.valueProperty().addListener((obs, o, n) -> {
            if (n != null) { niveauCombo.setValue(safe(n.getNiveau())); domaineCombo.setValue(safe(n.getDomaine())); niveauCombo.setDisable(true); domaineCombo.setDisable(true); }
            else { niveauCombo.setValue(null); domaineCombo.setValue(null); niveauCombo.setDisable(false); domaineCombo.setDisable(false); }
        });
        if (source != null) {
            selectCours(coursCombo, source.getCoursId());
            chapitreCombo.setItems(filteredChapitres(source.getCoursId()));
            selectChapitre(chapitreCombo, source.getChapitreId());
            titreField.setText(safe(source.getTitre()));
            urlField.setText(safe(source.getUrlVideo()));
            descriptionArea.setText(safe(source.getDescription()));
            niveauCombo.setValue(safe(source.getNiveau()));
            domaineCombo.setValue(safe(source.getDomaine()));
            // Si c'est une vidéo AI existante, cocher la case
            if (source.isAIGenerated()) {
                aiGenerateCheck.setSelected(true);
                urlField.setDisable(true);
            }
        }

        GridPane grid = formGrid();
        Label errCours = addRow(grid, 0, "Cours *", coursCombo);
        Label errChapitre = addRow(grid, 1, "Chapitre *", chapitreCombo);
        Label errTitre = addRow(grid, 2, "Titre *", titreField);
        Label errUrl = addRow(grid, 3, "Url video *", urlField);
        Label errDesc = addRow(grid, 4, "Description *", descriptionArea);
        Label errNiveau = addRow(grid, 5, "Niveau *", niveauCombo);
        Label errDomaine = addRow(grid, 6, "Domaine *", domaineCombo);

        liveValidate(titreField, errTitre, () -> CoursValidationService.validateVideoTitre(titreField.getText()));
        liveValidate(urlField, errUrl, () -> CoursValidationService.validateVideoUrl(urlField.getText()));
        liveValidate(descriptionArea, errDesc, () -> { ValidationResult r = new ValidationResult(); String v = descriptionArea.getText().trim(); if (v.isBlank()) r.addError("Obligatoire."); else if (v.length() < 10) r.addError("Min 10 caractères."); else if (v.chars().anyMatch(Character::isDigit)) r.addError("Pas de chiffres."); return r; });

        Dialog<ButtonType> dialog = buildFormDialog(source == null ? "Créer une vidéo" : "Modifier une vidéo", grid);
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            boolean err = false;
            if (coursCombo.getValue() == null) { FormValidator.markError(coursCombo, "Obligatoire"); errCours.setText("⚠ Cours obligatoire."); err = true; }
            if (chapitreCombo.getValue() == null) { FormValidator.markError(chapitreCombo, "Obligatoire"); errChapitre.setText("⚠ Chapitre obligatoire."); err = true; }
            if (titreField.getText().trim().isBlank()) { FormValidator.markError(titreField, "Obligatoire"); errTitre.setText("⚠ Titre obligatoire."); err = true; }
            if (urlField.getText().trim().isBlank()) { FormValidator.markError(urlField, "Obligatoire"); errUrl.setText("⚠ URL obligatoire."); err = true; }
            if (descriptionArea.getText().trim().length() < 10) { FormValidator.markError(descriptionArea, "Min 10 car."); errDesc.setText("⚠ Min 10 caractères."); err = true; }
            if (err) ev.consume();
        });

        Optional<ButtonType> answer = dialog.showAndWait();
        if (answer.isEmpty() || answer.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) return FormResult.cancelled();

        VideoExplicative video = source == null ? new VideoExplicative() : source;
        video.setCoursId(coursCombo.getValue().getId());
        video.setChapitreId(chapitreCombo.getValue().getId());
        video.setTitre(text(titreField));
        video.setDescription(text(descriptionArea));
        video.setNiveau(safe(niveauCombo.getValue()));
        video.setDomaine(safe(domaineCombo.getValue()));

        // Configuration selon le mode choisi
        if (aiGenerateCheck.isSelected()) {
            video.setAIGenerated(true);
            video.setGenerationStatus("PENDING");
            video.setUrlVideo(""); // Sera rempli après génération
        } else {
            video.setUrlVideo(text(urlField));
            video.setAIGenerated(false);
            video.setGenerationStatus("COMPLETED");
        }

        return FormResult.saved(video, aiGenerateCheck.isSelected()); // Le flag indique si c'est une génération AI
    }


    // ── Helpers UI ────────────────────────────────────────────────────────────

    private Dialog<ButtonType> buildFormDialog(String title, Node content) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(760);
        Dialogs.style(dialog);
        dialog.getDialogPane().setStyle("-fx-background-color: white;");
        javafx.scene.layout.Region cr = (javafx.scene.layout.Region) dialog.getDialogPane().lookup(".content");
        if (cr != null) cr.setStyle("-fx-background-color: white; -fx-padding: 14 18 14 18;");
        return dialog;
    }

    private GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(4);
        return grid;
    }

    private Label addRow(GridPane grid, int row, String label, Node node) {
        Label formLabel = new Label(label);
        formLabel.getStyleClass().add("form-label");
        grid.add(formLabel, 0, row * 2);
        grid.add(node, 1, row * 2);
        if (node instanceof Region region) { region.setMaxWidth(Double.MAX_VALUE); GridPane.setHgrow(region, Priority.ALWAYS); }
        Label errLabel = new Label("");
        errLabel.setStyle("-fx-text-fill: #d6293e; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 0 0 4 2;");
        errLabel.setWrapText(true);
        errLabel.setMaxWidth(400);
        grid.add(errLabel, 1, row * 2 + 1);
        return errLabel;
    }

    private void liveValidate(TextInputControl field, Label errLabel, java.util.function.Supplier<ValidationResult> validator) {
        field.textProperty().addListener((obs, o, n) -> {
            ValidationResult r = validator.get();
            if (r.isValid()) { errLabel.setText(""); FormValidator.clearError(field); }
            else { errLabel.setText("⚠ " + r.firstError()); FormValidator.markError(field, r.firstError()); }
        });
    }

    private TextField field() { TextField f = new TextField(); f.getStyleClass().add("field"); return f; }

    private TextArea area() { TextArea a = new TextArea(); a.getStyleClass().addAll("field", "area"); a.setPrefRowCount(4); a.setWrapText(true); return a; }

    private ComboBox<String> comboStrings(List<String> items) {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().setAll(items);
        combo.getStyleClass().addAll("field", "combo-box");
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    private ComboBox<Cours> comboCours() {
        ComboBox<Cours> combo = new ComboBox<>();
        combo.getStyleClass().addAll("field", "combo-box");
        combo.setItems(FXCollections.observableArrayList(repository.listCours("")));
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    private ComboBox<Chapitre> comboChapitre() {
        ComboBox<Chapitre> combo = new ComboBox<>();
        combo.getStyleClass().addAll("field", "combo-box");
        combo.setMaxWidth(Double.MAX_VALUE);
        return combo;
    }

    private File chooseFile(String title, List<String> exts, String filterLabel, Node owner) {
        Window window = owner == null || owner.getScene() == null ? null : owner.getScene().getWindow();
        FileChooser fc = new FileChooser();
        fc.setTitle(title == null ? "Choisir" : title);
        List<String> patterns = new ArrayList<>();
        if (exts != null) for (String e : exts) { String ext = safe(e).toLowerCase(); if (!ext.isBlank()) patterns.add("*." + ext); }
        if (!patterns.isEmpty()) {
            fc.getExtensionFilters().setAll(new FileChooser.ExtensionFilter("All Files", "*.*"), new FileChooser.ExtensionFilter(safe(filterLabel).isBlank() ? "Fichiers" : filterLabel, patterns));
            fc.setSelectedExtensionFilter(fc.getExtensionFilters().get(1));
        }
        try { return fc.showOpenDialog(window); } catch (Exception e) { error("Fichier", e); return null; }
    }

    private void bindChapitreComboToCours(ComboBox<Cours> coursCombo, ComboBox<Chapitre> chapitreCombo) {
        coursCombo.valueProperty().addListener((obs, o, n) -> {
            chapitreCombo.setValue(null);
            chapitreCombo.setItems(n == null ? FXCollections.observableArrayList() : filteredChapitres(n.getId()));
        });
    }

    private ObservableList<Chapitre> filteredChapitres(int coursId) {
        ObservableList<Chapitre> out = FXCollections.observableArrayList();
        for (Chapitre ch : repository.listChapitres("")) if (ch.getCoursId() == coursId) out.add(ch);
        return out;
    }

    private void selectCours(ComboBox<Cours> combo, int id) { for (Cours c : combo.getItems()) if (c.getId() == id) { combo.setValue(c); return; } }
    private void selectChapitre(ComboBox<Chapitre> combo, int id) { for (Chapitre c : combo.getItems()) if (c.getId() == id) { combo.setValue(c); return; } }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title); alert.setHeaderText(title); alert.setContentText(message);
        Dialogs.style(alert);
        return alert.showAndWait().map(b -> b == ButtonType.OK).orElse(false);
    }

    private void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(title); alert.setContentText(message);
        Dialogs.style(alert); alert.showAndWait();
    }

    private void error(String title, Exception e) {
        if (e != null) e.printStackTrace();
        // Construire le message complet en remontant la chaîne des causes
        String msg;
        if (e == null) {
            msg = "Erreur inconnue";
        } else {
            StringBuilder sb = new StringBuilder();
            Throwable t = e;
            while (t != null) {
                String m = t.getMessage();
                if (m != null && !m.isBlank()) {
                    if (sb.length() > 0) sb.append("\nCause : ");
                    sb.append(m);
                }
                t = t.getCause();
            }
            msg = sb.length() > 0 ? sb.toString() : e.getClass().getSimpleName();
        }
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title); alert.setHeaderText(title);
        alert.setContentText(e == null ? "Erreur" : safe(e.getMessage()));
        Dialogs.style(alert); alert.showAndWait();
    }

    private static int parseDuree(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; } }
    private static String safe(String v) { return v == null ? "" : v.trim(); }
    private static String text(TextField f) { return f == null ? "" : safe(f.getText()); }
    private static String text(TextArea a) { return a == null ? "" : safe(a.getText()); }

    private record FormResult<T>(T value, boolean saved, boolean flag) {
        static <T> FormResult<T> saved(T v) { return new FormResult<>(v, true, false); }
        static <T> FormResult<T> saved(T v, boolean flag) { return new FormResult<>(v, true, flag); }
        static <T> FormResult<T> cancelled() { return new FormResult<>(null, false, false); }
    }
}
