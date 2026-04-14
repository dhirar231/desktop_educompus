package com.educompus.controller.back;

import com.educompus.model.Chapitre;
import com.educompus.model.Cours;
import com.educompus.model.Td;
import com.educompus.model.VideoExplicative;
import com.educompus.repository.CourseManagementRepository;
import com.educompus.service.CoursValidationService;
import com.educompus.service.FormValidator;
import com.educompus.service.ValidationResult;
import com.educompus.util.Dialogs;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TabPane;
import javafx.scene.control.Control;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.control.TableRow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Callback;
import java.awt.Desktop;

import java.io.File;
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
    @FXML private javafx.scene.control.ListView<Cours> coursListView;
    @FXML private javafx.scene.control.ListView<Chapitre> chapitreListView;
    @FXML private javafx.scene.control.ListView<Td> tdListView;
    @FXML private javafx.scene.control.ListView<VideoExplicative> videoListView;
    @FXML private TextField chapitreSearchField;
    @FXML private ComboBox<String> chapitreSortCombo;
    @FXML private TextField tdSearchField;
    @FXML private ComboBox<String> tdSortCombo;
    @FXML private TextField videoSearchField;
    @FXML private ComboBox<String> videoSortCombo;

    @FXML
    private void initialize() {
        setupListViews();
        setupSorts();
        refreshAll();
    }

    @FXML
    private void refreshAll() {
        reloadCours();
        reloadChapitres();
        reloadTds();
        reloadVideos();
    }

    @FXML
    private void createCours() {
        System.out.println("createCours called");
        FormResult<Cours> result = showCoursForm(null);
        if (!result.saved()) {
            System.out.println("Form not saved");
            return;
        }
        try {
            repository.createCours(result.value());
            info("✅ Cours ajouté", "Le cours « " + safe(result.value().getTitre()) + " » a été ajouté avec succès.");
            reloadCours();
        } catch (Exception e) {
            error("Erreur ajout cours", e);
        }
    }

    @FXML
    private void createChapitre() {
        createChapitreForCourse(null);
    }

    @FXML
    private void createChapitreForCourse(Cours cours) {
        FormResult<Chapitre> result = showChapitreForm(null, cours);
        if (!result.saved()) return;
        try {
            repository.createChapitre(result.value());
            info("✅ Chapitre ajouté", "Le chapitre « " + safe(result.value().getTitre()) + " » a été ajouté avec succès.");
            refreshAll();
            if (mainTabPane != null) mainTabPane.getSelectionModel().select(1);
        } catch (Exception e) {
            error("Erreur ajout chapitre", e);
        }
    }

    private void openTdVideoChoice(Chapitre chapitre) {
        if (chapitre == null) return;
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Ajouter du contenu");
        dialog.setHeaderText(null);
        Dialogs.style(dialog);
        Label titre = new Label("Chapitre : " + safe(chapitre.getTitre()));
        titre.getStyleClass().add("project-card-title");
        titre.setWrapText(true);
        Label subtitle = new Label("Que souhaitez-vous ajouter ?");
        subtitle.getStyleClass().add("page-subtitle");
        Button btnTd = new Button("Creer un TD");
        btnTd.getStyleClass().add("btn-rgb");
        btnTd.setMaxWidth(Double.MAX_VALUE);
        btnTd.setPrefHeight(44);
        Button btnVideo = new Button("Creer une video");
        btnVideo.getStyleClass().add("btn-rgb-outline");
        btnVideo.setMaxWidth(Double.MAX_VALUE);
        btnVideo.setPrefHeight(44);
        Button btnCancel = new Button("Annuler");
        btnCancel.getStyleClass().add("btn-ghost");
        btnCancel.setMaxWidth(Double.MAX_VALUE);
        VBox content = new VBox(12, titre, subtitle, new javafx.scene.control.Separator(), btnTd, btnVideo, btnCancel);
        content.setPadding(new javafx.geometry.Insets(18));
        content.setPrefWidth(340);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setManaged(false);
        final String[] res = {null};
        btnTd.setOnAction(e -> { res[0] = "td"; dialog.close(); });
        btnVideo.setOnAction(e -> { res[0] = "video"; dialog.close(); });
        btnCancel.setOnAction(e -> dialog.close());
        dialog.showAndWait();
        if ("td".equals(res[0])) createTdForChapitre(chapitre);
        else if ("video".equals(res[0])) createVideoForChapitre(chapitre);
    }

    private void createTdForChapitre(Chapitre chapitre) {
        FormResult<Td> result = showTdForm(null, chapitre);
        if (!result.saved()) return;
        try {
            repository.createTd(result.value());
            info("✅ TD ajouté", "Le TD « " + safe(result.value().getTitre()) + " » a été ajouté avec succès.");
            refreshAll();
        } catch (Exception e) {
            error("Erreur ajout TD", e);
        }
    }

    private void createVideoForChapitre(Chapitre chapitre) {
        FormResult<VideoExplicative> result = showVideoForm(null, chapitre);
        if (!result.saved()) return;
        try {
            repository.createVideo(result.value());
            info("✅ Vidéo ajoutée", "La vidéo « " + safe(result.value().getTitre()) + " » a été ajoutée avec succès.");
            refreshAll();
        } catch (Exception e) {
            error("Erreur ajout video", e);
        }
    }

    @FXML
    private void createTd() {
        FormResult<Td> result = showTdForm(null);
        if (!result.saved()) return;
        try {
            repository.createTd(result.value());
            info("✅ TD ajouté", "Le TD « " + safe(result.value().getTitre()) + " » a été ajouté avec succès.");
            refreshAll();
        } catch (Exception e) {
            error("Erreur — Ajout TD", e);
        }
    }

    @FXML
    private void createVideo() {
        FormResult<VideoExplicative> result = showVideoForm(null);
        if (!result.saved()) return;
        try {
            repository.createVideo(result.value());
            info("✅ Vidéo ajoutée", "La vidéo « " + safe(result.value().getTitre()) + " » a été ajoutée avec succès.");
            refreshAll();
        } catch (Exception e) {
            error("Erreur — Ajout vidéo", e);
        }
    }

    private void setupSorts() {
        initSearchAndSort(coursSearchField, coursSortCombo, List.of("Titre A-Z", "Formateur"), this::reloadCours, this::applyCoursSort);
        initSearchAndSort(chapitreSearchField, chapitreSortCombo, List.of("Titre A-Z", "Ordre"), this::reloadChapitres, this::applyChapitreSort);
        initSearchAndSort(tdSearchField, tdSortCombo, List.of("Titre A-Z", "Cours"), this::reloadTds, this::applyTdSort);
        initSearchAndSort(videoSearchField, videoSortCombo, List.of("Titre A-Z"), this::reloadVideos, this::applyVideoSort);
    }

    private void initSearchAndSort(TextField search, ComboBox<String> sortCombo, List<String> items, Runnable reload, Runnable sortOnly) {
        if (search != null) {
            search.textProperty().addListener((obs, oldValue, newValue) -> reload.run());
        }
        if (sortCombo != null) {
            sortCombo.getItems().setAll(items);
            sortCombo.setValue(items.get(0));
            sortCombo.valueProperty().addListener((obs, oldValue, newValue) -> sortOnly.run());
        }
    }

    private void reloadCours() {
        coursItems.setAll(repository.listCours(text(coursSearchField)));
        applyCoursSort();
        if (coursListView != null) { coursListView.setItems(coursItems); coursListView.refresh(); }
        statsCoursLabel.setText(String.valueOf(coursItems.size()));
    }

    private void reloadChapitres() {
        chapitreItems.setAll(repository.listChapitres(text(chapitreSearchField)));
        applyChapitreSort();
        if (chapitreListView != null) { chapitreListView.setItems(chapitreItems); chapitreListView.refresh(); }
        statsChapitreLabel.setText(String.valueOf(chapitreItems.size()));
    }

    private void reloadTds() {
        tdItems.setAll(repository.listTds(text(tdSearchField)));
        applyTdSort();
        if (tdListView != null) { tdListView.setItems(tdItems); tdListView.refresh(); }
        statsTdLabel.setText(String.valueOf(tdItems.size()));
    }

    private void reloadVideos() {
        videoItems.setAll(repository.listVideos(text(videoSearchField)));
        applyVideoSort();
        if (videoListView != null) { videoListView.setItems(videoItems); videoListView.refresh(); }
        statsVideoLabel.setText(String.valueOf(videoItems.size()));
    }

    private void setupListViews() {
        // Cours ListView
        if (coursListView != null) {
            coursListView.setCellFactory(lv -> new javafx.scene.control.ListCell<Cours>() {
                @Override protected void updateItem(Cours c, boolean empty) {
                    super.updateItem(c, empty);
                    if (empty || c == null) { setGraphic(null); return; }
                    HBox row = new HBox(12);
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    row.setPadding(new javafx.geometry.Insets(10, 14, 10, 14));
                    VBox info = new VBox(4);
                    HBox.setHgrow(info, Priority.ALWAYS);
                    Label titre = new Label(safe(c.getTitre()));
                    titre.getStyleClass().add("project-card-title");
                    Label meta = new Label(safe(c.getDomaine()) + "  •  " + safe(c.getNiveau()) + "  •  " + safe(c.getNomFormateur()) + "  •  " + c.getDureeTotaleHeures() + "h  •  " + c.getChapitreCount() + " chap.");
                    meta.getStyleClass().add("page-subtitle");
                    info.getChildren().addAll(titre, meta);
                    Button editBtn = new Button("✏️");
                    editBtn.getStyleClass().add("btn-rgb-outline");
                    editBtn.setOnAction(e -> editCours(c));
                    Button delBtn = new Button("🗑️");
                    delBtn.getStyleClass().add("btn-danger");
                    delBtn.setOnAction(e -> deleteCours(c));
                    row.getChildren().addAll(info, editBtn, delBtn);
                    setGraphic(row);
                }
            });
            coursListView.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                    Cours sel = coursListView.getSelectionModel().getSelectedItem();
                    if (sel != null) createChapitreForCourse(sel);
                }
            });
        }
        // Chapitre ListView
        if (chapitreListView != null) {
            chapitreListView.setCellFactory(lv -> new javafx.scene.control.ListCell<Chapitre>() {
                @Override protected void updateItem(Chapitre ch, boolean empty) {
                    super.updateItem(ch, empty);
                    if (empty || ch == null) { setGraphic(null); return; }
                    HBox row = new HBox(12);
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    row.setPadding(new javafx.geometry.Insets(10, 14, 10, 14));
                    Label num = new Label(String.valueOf(ch.getOrdre()));
                    num.getStyleClass().add("chapitre-num");
                    VBox info = new VBox(4);
                    HBox.setHgrow(info, Priority.ALWAYS);
                    Label titre = new Label(safe(ch.getTitre()));
                    titre.getStyleClass().add("project-card-title");
                    Label meta = new Label(safe(ch.getCoursTitre()) + "  •  " + safe(ch.getDomaine()) + "  •  " + ch.getTdCount() + " TD  •  " + ch.getVideoCount() + " vidéos");
                    meta.getStyleClass().add("page-subtitle");
                    info.getChildren().addAll(titre, meta);
                    Button editBtn = new Button("✏️");
                    editBtn.getStyleClass().add("btn-rgb-outline");
                    editBtn.setOnAction(e -> editChapitre(ch));
                    Button delBtn = new Button("🗑️");
                    delBtn.getStyleClass().add("btn-danger");
                    delBtn.setOnAction(e -> deleteChapitre(ch));
                    row.getChildren().addAll(num, info, editBtn, delBtn);
                    setGraphic(row);
                }
            });
            chapitreListView.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                    Chapitre sel = chapitreListView.getSelectionModel().getSelectedItem();
                    if (sel != null) openTdVideoChoice(sel);
                }
            });
        }
        // TD ListView
        if (tdListView != null) {
            tdListView.setCellFactory(lv -> new javafx.scene.control.ListCell<Td>() {
                @Override protected void updateItem(Td td, boolean empty) {
                    super.updateItem(td, empty);
                    if (empty || td == null) { setGraphic(null); return; }
                    HBox row = new HBox(12);
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    row.setPadding(new javafx.geometry.Insets(10, 14, 10, 14));
                    VBox info = new VBox(4);
                    HBox.setHgrow(info, Priority.ALWAYS);
                    Label titre = new Label(safe(td.getTitre()));
                    titre.getStyleClass().add("project-card-title");
                    Label meta = new Label(safe(td.getCoursTitre()) + "  •  " + safe(td.getChapitreTitre()) + "  •  " + safe(td.getDomaine()));
                    meta.getStyleClass().add("page-subtitle");
                    info.getChildren().addAll(titre, meta);
                    Button openBtn = new Button("📄");
                    openBtn.getStyleClass().add("btn-rgb-compact");
                    openBtn.setDisable(td.getFichier() == null || td.getFichier().isBlank());
                    openBtn.setOnAction(e -> { try { Desktop.getDesktop().open(new File(td.getFichier())); } catch (Exception ex) { error("Erreur", ex); } });
                    Button editBtn = new Button("✏️");
                    editBtn.getStyleClass().add("btn-rgb-outline");
                    editBtn.setOnAction(e -> editTd(td));
                    Button delBtn = new Button("🗑️");
                    delBtn.getStyleClass().add("btn-danger");
                    delBtn.setOnAction(e -> deleteTd(td));
                    row.getChildren().addAll(info, openBtn, editBtn, delBtn);
                    setGraphic(row);
                }
            });
        }
        // Video ListView
        if (videoListView != null) {
            videoListView.setCellFactory(lv -> new javafx.scene.control.ListCell<VideoExplicative>() {
                @Override protected void updateItem(VideoExplicative v, boolean empty) {
                    super.updateItem(v, empty);
                    if (empty || v == null) { setGraphic(null); return; }
                    HBox row = new HBox(12);
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    row.setPadding(new javafx.geometry.Insets(10, 14, 10, 14));
                    VBox info = new VBox(4);
                    HBox.setHgrow(info, Priority.ALWAYS);
                    Label titre = new Label(safe(v.getTitre()));
                    titre.getStyleClass().add("project-card-title");
                    Label meta = new Label(safe(v.getCoursTitre()) + "  •  " + safe(v.getChapitreTitre()) + "  •  " + safe(v.getDomaine()));
                    meta.getStyleClass().add("page-subtitle");
                    info.getChildren().addAll(titre, meta);
                    Button playBtn = new Button("▶");
                    playBtn.getStyleClass().add("btn-rgb-compact");
                    playBtn.setDisable(v.getUrlVideo() == null || v.getUrlVideo().isBlank());
                    playBtn.setOnAction(e -> { try { com.educompus.util.UrlOpener.open(v.getUrlVideo()); } catch (Exception ex) { error("Erreur URL", ex); } });
                    Button editBtn = new Button("✏️");
                    editBtn.getStyleClass().add("btn-rgb-outline");
                    editBtn.setOnAction(e -> editVideo(v));
                    Button delBtn = new Button("🗑️");
                    delBtn.getStyleClass().add("btn-danger");
                    delBtn.setOnAction(e -> deleteVideo(v));
                    row.getChildren().addAll(info, playBtn, editBtn, delBtn);
                    setGraphic(row);
                }
            });
        }
    }

    private void editCours(Cours cours) {
        if (!confirm("Modifier le cours",
                "Voulez-vous modifier le cours :\n« " + safe(cours.getTitre()) + " » ?")) return;
        FormResult<Cours> result = showCoursForm(cours);
        if (!result.saved()) return;
        try {
            repository.updateCours(result.value());
            info("✅ Cours modifié", "Le cours « " + safe(result.value().getTitre()) + " » a été modifié avec succès.");
            refreshAll();
        } catch (Exception e) {
            error("Erreur — Modification cours", e);
        }
    }

    private void editChapitre(Chapitre chapitre) {
        if (!confirm("Modifier le chapitre",
                "Voulez-vous modifier le chapitre :\n« " + safe(chapitre.getTitre()) + " » ?")) return;
        FormResult<Chapitre> result = showChapitreForm(chapitre);
        if (!result.saved()) return;
        try {
            repository.updateChapitre(result.value());
            info("✅ Chapitre modifié", "Le chapitre « " + safe(result.value().getTitre()) + " » a été modifié avec succès.");
            refreshAll();
            if (mainTabPane != null) mainTabPane.getSelectionModel().select(1);
        } catch (Exception e) {
            error("Erreur — Modification chapitre", e);
        }
    }

    private void editTd(Td td) {
        if (!confirm("Modifier le TD",
                "Voulez-vous modifier le TD :\n« " + safe(td.getTitre()) + " » ?")) return;
        FormResult<Td> result = showTdForm(td);
        if (!result.saved()) return;
        try {
            repository.updateTd(result.value());
            info("✅ TD modifié", "Le TD « " + safe(result.value().getTitre()) + " » a été modifié avec succès.");
            refreshAll();
        } catch (Exception e) {
            error("Erreur — Modification TD", e);
        }
    }

    private void editVideo(VideoExplicative video) {
        if (!confirm("Modifier la vidéo",
                "Voulez-vous modifier la vidéo :\n« " + safe(video.getTitre()) + " » ?")) return;
        FormResult<VideoExplicative> result = showVideoForm(video);
        if (!result.saved()) return;
        try {
            repository.updateVideo(result.value());
            info("✅ Vidéo modifiée", "La vidéo « " + safe(result.value().getTitre()) + " » a été modifiée avec succès.");
            refreshAll();
        } catch (Exception e) {
            error("Erreur — Modification vidéo", e);
        }
    }

    private void deleteCours(Cours cours) {
        if (!confirm("⚠ Supprimer le cours",
                "Êtes-vous sûr de vouloir supprimer le cours :\n« " + safe(cours.getTitre()) + " » ?\n\nTous ses chapitres, TDs et vidéos seront supprimés.")) return;
        try {
            repository.deleteCours(cours.getId());
            info("🗑 Cours supprimé", "Le cours « " + safe(cours.getTitre()) + " » a été supprimé.");
            refreshAll();
        } catch (Exception e) {
            error("Erreur — Suppression cours", e);
        }
    }

    private void deleteChapitre(Chapitre chapitre) {
        if (!confirm("⚠ Supprimer le chapitre",
                "Êtes-vous sûr de vouloir supprimer le chapitre :\n« " + safe(chapitre.getTitre()) + " » ?\n\nSes TDs et vidéos associés seront supprimés.")) return;
        try {
            repository.deleteChapitre(chapitre.getId());
            info("🗑 Chapitre supprimé", "Le chapitre « " + safe(chapitre.getTitre()) + " » a été supprimé.");
            refreshAll();
        } catch (Exception e) {
            error("Erreur — Suppression chapitre", e);
        }
    }

    private void deleteTd(Td td) {
        if (!confirm("⚠ Supprimer le TD",
                "Êtes-vous sûr de vouloir supprimer le TD :\n« " + safe(td.getTitre()) + " » ?")) return;
        try {
            repository.deleteTd(td.getId());
            info("🗑 TD supprimé", "Le TD « " + safe(td.getTitre()) + " » a été supprimé.");
            refreshAll();
        } catch (Exception e) {
            error("Erreur — Suppression TD", e);
        }
    }

    private void deleteVideo(VideoExplicative video) {
        if (!confirm("⚠ Supprimer la vidéo",
                "Êtes-vous sûr de vouloir supprimer la vidéo :\n« " + safe(video.getTitre()) + " » ?")) return;
        try {
            repository.deleteVideo(video.getId());
            info("🗑 Vidéo supprimée", "La vidéo « " + safe(video.getTitre()) + " » a été supprimée.");
            refreshAll();
        } catch (Exception e) {
            error("Erreur — Suppression vidéo", e);
        }
    }

    private void applyCoursSort() {
        String sort = coursSortCombo.getValue();
        if ("Titre A-Z".equals(sort)) {
            coursItems.sort(Comparator.comparing(c -> safe(c.getTitre()), String.CASE_INSENSITIVE_ORDER));
        } else if ("Formateur".equals(sort)) {
            coursItems.sort(Comparator.comparing(c -> safe(c.getNomFormateur()), String.CASE_INSENSITIVE_ORDER));
        } else {
            coursItems.sort(Comparator.comparingInt(Cours::getId));
        }
    }

    private void applyChapitreSort() {
        String sort = chapitreSortCombo.getValue();
        if ("Titre A-Z".equals(sort)) {
            chapitreItems.sort(Comparator.comparing(c -> safe(c.getTitre()), String.CASE_INSENSITIVE_ORDER));
        } else if ("Ordre".equals(sort)) {
            chapitreItems.sort(Comparator.comparingInt(Chapitre::getOrdre));
        } else {
            chapitreItems.sort(Comparator.comparingInt(Chapitre::getId));
        }
    }

    private void applyTdSort() {
        String sort = tdSortCombo.getValue();
        if ("Titre A-Z".equals(sort)) {
            tdItems.sort(Comparator.comparing(c -> safe(c.getTitre()), String.CASE_INSENSITIVE_ORDER));
        } else if ("Cours".equals(sort)) {
            tdItems.sort(Comparator.comparing(c -> safe(c.getCoursTitre()), String.CASE_INSENSITIVE_ORDER));
        } else {
            tdItems.sort(Comparator.comparingInt(Td::getId));
        }
    }

    private void applyVideoSort() {
        String sort = videoSortCombo.getValue();
        if ("Titre A-Z".equals(sort)) {
            videoItems.sort(Comparator.comparing(c -> safe(c.getTitre()), String.CASE_INSENSITIVE_ORDER));
        } else {
            videoItems.sort(Comparator.comparingInt(VideoExplicative::getId));
        }
    }

    private FormResult<Cours> showCoursForm(Cours source) {
        TextField titreField = field();
        TextArea descriptionArea = area();
        ComboBox<String> niveauCombo = comboStrings(NIVEAUX);
        ComboBox<String> domaineCombo = comboStrings(DOMAINES);
        TextField formateurField = field();
        TextField dureeField = field();
        TextField imageField = field();
        imageField.setPromptText("Chemin ou URL de l image (optionnel)");
        Button browseImageBtn = new Button("Parcourir");
        browseImageBtn.getStyleClass().add("btn-rgb-outline");
        browseImageBtn.setOnAction(ev -> {
            File sel = chooseFile("Choisir une image", List.of("png","jpg","jpeg","gif","bmp"), "Images", imageField);
            if (sel != null) imageField.setText(sel.getAbsolutePath());
        });
        HBox imageBox = new HBox(8, imageField, browseImageBtn);
        imageBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(imageField, Priority.ALWAYS);

        if (source == null) {
            niveauCombo.setValue(NIVEAUX.get(0));
            domaineCombo.setValue(DOMAINES.get(0));
        }

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
        Label errTitre = addRow(grid, 0, "Titre", titreField);
        Label errDesc = addRow(grid, 1, "Description", descriptionArea);
        Label errNiveau = addRow(grid, 2, "Niveau", niveauCombo);
        Label errDomaine = addRow(grid, 3, "Domaine", domaineCombo);
        Label errFormateur = addRow(grid, 4, "Nom du formateur", formateurField);
        Label errDuree = addRow(grid, 5, "Duree totale (heures)", dureeField);
        Label errImage = addRow(grid, 6, "Image (png/jpg)", imageBox);
        niveauCombo.valueProperty().addListener((obs, o, n) -> { if (n == null) { niveauCombo.setStyle("-fx-border-color: #d6293e; -fx-border-width: 2; -fx-border-radius: 8px;"); errNiveau.setText("⚠ Veuillez selectionner un niveau."); } else { niveauCombo.setStyle(""); errNiveau.setText(""); } });
        domaineCombo.valueProperty().addListener((obs, o, n) -> { if (n == null) { domaineCombo.setStyle("-fx-border-color: #d6293e; -fx-border-width: 2; -fx-border-radius: 8px;"); errDomaine.setText("⚠ Veuillez selectionner un domaine."); } else { domaineCombo.setStyle(""); errDomaine.setText(""); } });
        liveValidate(imageField, errImage, () -> { ValidationResult r = new ValidationResult(); String v = imageField.getText().trim(); if (!v.isBlank()) { String vl = v.toLowerCase(); if (!vl.endsWith(".png") && !vl.endsWith(".jpg") && !vl.endsWith(".jpeg")) r.addError("Doit etre .png ou .jpg"); } return r; });

        liveValidate(titreField, errTitre, () -> CoursValidationService.validateChapitreTitre(titreField.getText()));
        liveValidate(descriptionArea, errDesc, () -> { ValidationResult r = new ValidationResult(); String v = descriptionArea.getText().trim(); if (v.isBlank()) r.addError("La description est obligatoire."); else if (v.length() < 10) r.addError("Minimum 10 caracteres."); else if (v.chars().anyMatch(Character::isDigit)) r.addError("La description ne doit pas contenir de chiffres."); return r; });
        liveValidate(formateurField, errFormateur, () -> { ValidationResult r = new ValidationResult(); String v = formateurField.getText().trim(); if (v.isBlank()) r.addError("Le nom du formateur est obligatoire."); else if (v.chars().anyMatch(Character::isDigit)) r.addError("Le nom ne doit pas contenir de chiffres."); return r; });
        liveValidate(dureeField, errDuree, () -> CoursValidationService.validateDureeStr(dureeField.getText()));


        // Déclencher validation visuelle au clic OK
        Dialog<ButtonType> dialog = buildFormDialog(source == null ? "Creer un cours" : "Modifier un cours", grid);
        javafx.scene.control.Button okBtnC = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtnC.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            boolean hasErr = false;
            if (titreField.getText().trim().isBlank()) { FormValidator.markError(titreField, "Obligatoire"); errTitre.setText("⚠ Le titre est obligatoire."); hasErr = true; }
            if (descriptionArea.getText().trim().isBlank()) { FormValidator.markError(descriptionArea, "Obligatoire"); errDesc.setText("⚠ La description est obligatoire."); hasErr = true; } else if (descriptionArea.getText().trim().length() < 10) { FormValidator.markError(descriptionArea, "Min 10 car."); errDesc.setText("⚠ Minimum 10 caracteres."); hasErr = true; } else if (descriptionArea.getText().trim().chars().anyMatch(Character::isDigit)) { FormValidator.markError(descriptionArea, "Pas de chiffres"); errDesc.setText("⚠ La description ne doit pas contenir de chiffres."); hasErr = true; }
            if (formateurField.getText().trim().isBlank()) { FormValidator.markError(formateurField, "Obligatoire"); errFormateur.setText("⚠ Le formateur est obligatoire."); hasErr = true; }
            if (dureeField.getText().trim().isBlank()) { FormValidator.markError(dureeField, "Obligatoire"); errDuree.setText("⚠ La duree est obligatoire."); hasErr = true; }
            if (niveauCombo.getValue() == null) { niveauCombo.setStyle("-fx-border-color: #d6293e; -fx-border-width: 2; -fx-border-radius: 8px;"); errNiveau.setText("⚠ Veuillez selectionner un niveau."); hasErr = true; } else { niveauCombo.setStyle(""); errNiveau.setText(""); }
            if (domaineCombo.getValue() == null) { domaineCombo.setStyle("-fx-border-color: #d6293e; -fx-border-width: 2; -fx-border-radius: 8px;"); errDomaine.setText("⚠ Veuillez selectionner un domaine."); hasErr = true; } else { domaineCombo.setStyle(""); errDomaine.setText(""); }
            String imgV = imageField.getText().trim(); if (!imgV.isBlank()) { String ivl = imgV.toLowerCase(); if (!ivl.endsWith(".png") && !ivl.endsWith(".jpg") && !ivl.endsWith(".jpeg")) { FormValidator.markError(imageField, "Doit etre .png ou .jpg"); errImage.setText("⚠ L image doit etre .png ou .jpg"); hasErr = true; } else if (!new java.io.File(imgV).exists()) { FormValidator.markError(imageField, "Fichier introuvable"); errImage.setText("⚠ Fichier introuvable."); hasErr = true; } }
            if (hasErr) ev.consume();
        });
        Optional<ButtonType> answer = dialog.showAndWait();
        if (answer.isEmpty() || answer.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) return FormResult.cancelled();

        // ── Validation logique via CoursValidationService ──
        Cours draft = new Cours();
        draft.setTitre(text(titreField));
        draft.setDescription(text(descriptionArea));
        draft.setNiveau(safe(niveauCombo.getValue()));
        draft.setDomaine(safe(domaineCombo.getValue()));
        draft.setNomFormateur(text(formateurField));
        draft.setDureeTotaleHeures(parseDuree(text(dureeField)));

        ValidationResult vr = CoursValidationService.validateCours(draft);
        if (!vr.isValid()) {
            // Marquer les champs visuellement
            FormValidator.markError(titreField, CoursValidationService.validateChapitreTitre(draft.getTitre()).firstError());
            FormValidator.markError(dureeField, CoursValidationService.validateDureeStr(text(dureeField)).firstError());
            return FormResult.cancelled();
        }

        int duree = draft.getDureeTotaleHeures();

        Cours cours = source == null ? new Cours() : source;
        cours.setTitre(text(titreField));
        cours.setDescription(text(descriptionArea));
        cours.setNiveau(safe(niveauCombo.getValue()));
        cours.setDomaine(safe(domaineCombo.getValue()));
        cours.setNomFormateur(text(formateurField));
        cours.setDureeTotaleHeures(duree);
        String imgVal = text(imageField);
        cours.setImage(imgVal.isBlank() ? "auto:" + safe(domaineCombo.getValue()).toLowerCase() : imgVal);
        return FormResult.saved(cours);
    }

    private FormResult<Chapitre> showChapitreForm(Chapitre source) {
        return showChapitreForm(source, null);
    }

    private FormResult<Chapitre> showChapitreForm(Chapitre source, Cours selectedCours) {
        ComboBox<Cours> coursCombo = comboCours();
        TextField titreField = field();
        TextField ordreField = field();
        TextArea descriptionArea = area();
        TextField fichierField = field();
        Button browseButton = new Button("Parcourir");
        browseButton.getStyleClass().add("btn-secondary");
        browseButton.setOnAction(event -> {
            File selected = chooseFile("Choisir un fichier PDF", List.of("pdf"), "PDF", fichierField);
            if (selected != null) {
                fichierField.setText(selected.getAbsolutePath());
            }
        });
        HBox fichierBox = new HBox(8, fichierField, browseButton);
        fichierBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(fichierField, Priority.ALWAYS);
        ComboBox<String> niveauCombo = comboStrings(NIVEAUX);
        ComboBox<String> domaineCombo = comboStrings(DOMAINES);

        if (selectedCours != null) {
            selectCours(coursCombo, selectedCours.getId());
            coursCombo.setDisable(true);
            niveauCombo.setValue(safe(selectedCours.getNiveau()));
            domaineCombo.setValue(safe(selectedCours.getDomaine()));
            niveauCombo.setDisable(true);
            domaineCombo.setDisable(true);
        }

        coursCombo.valueProperty().addListener((obs, oldCours, newCours) -> {
            if (newCours != null) {
                niveauCombo.setValue(safe(newCours.getNiveau()));
                domaineCombo.setValue(safe(newCours.getDomaine()));
                niveauCombo.setDisable(true);
                domaineCombo.setDisable(true);
            } else {
                niveauCombo.setValue(null);
                domaineCombo.setValue(null);
                niveauCombo.setDisable(false);
                domaineCombo.setDisable(false);
            }
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
        Label errCours = addRow(grid, 0, "Cours", coursCombo);
        Label errTitre = addRow(grid, 1, "Titre", titreField);
        Label errOrdre = addRow(grid, 2, "Ordre", ordreField);
        Label errDesc = addRow(grid, 3, "Description", descriptionArea);
        Label errFichier = addRow(grid, 4, "Fichier PDF", fichierBox);
        Label errNiveau = addRow(grid, 5, "Niveau", niveauCombo);
        Label errDomaine = addRow(grid, 6, "Domaine", domaineCombo);

        liveValidate(titreField, errTitre, () -> CoursValidationService.validateChapitreTitre(titreField.getText()));
        liveValidate(ordreField, errOrdre, () -> CoursValidationService.validateChapitreOrdre(ordreField.getText()));
        liveValidate(fichierField, errFichier, () -> { ValidationResult r = new ValidationResult(); String v = fichierField.getText().trim(); if (v.isBlank()) r.addError("Le fichier PDF est obligatoire."); else if (!v.toLowerCase().endsWith(".pdf")) r.addError("Doit etre un fichier .pdf"); return r; });
        liveValidate(descriptionArea, errDesc, () -> { ValidationResult r = new ValidationResult(); String v = descriptionArea.getText().trim(); if (v.isBlank()) r.addError("La description est obligatoire."); else if (v.length() < 10) r.addError("Minimum 10 caracteres."); else if (v.chars().anyMatch(Character::isDigit)) r.addError("La description ne doit pas contenir de chiffres."); return r; });


        Dialog<ButtonType> dialog = buildFormDialog(source == null ? "Creer un chapitre" : "Modifier un chapitre", grid);
        javafx.scene.control.Button okBtnCh = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtnCh.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            boolean hasErr = false;
            if (coursCombo.getValue() == null) { FormValidator.markError(coursCombo, "Obligatoire"); errCours.setText("⚠ Cours obligatoire."); hasErr = true; }
            if (titreField.getText().trim().isBlank()) { FormValidator.markError(titreField, "Obligatoire"); errTitre.setText("⚠ Le titre est obligatoire."); hasErr = true; }
            if (ordreField.getText().trim().isBlank()) { FormValidator.markError(ordreField, "Obligatoire"); errOrdre.setText("⚠ L ordre est obligatoire."); hasErr = true; }
            if (fichierField.getText().trim().isBlank()) { FormValidator.markError(fichierField, "Obligatoire"); errFichier.setText("⚠ Le fichier est obligatoire."); hasErr = true; }
            if (descriptionArea.getText().trim().isBlank()) { FormValidator.markError(descriptionArea, "Obligatoire"); errDesc.setText("⚠ La description est obligatoire."); hasErr = true; } else if (descriptionArea.getText().trim().length() < 10) { FormValidator.markError(descriptionArea, "Min 10 car."); errDesc.setText("⚠ Minimum 10 caracteres."); hasErr = true; } else if (descriptionArea.getText().trim().chars().anyMatch(Character::isDigit)) { FormValidator.markError(descriptionArea, "Pas de chiffres"); errDesc.setText("⚠ La description ne doit pas contenir de chiffres."); hasErr = true; }
            if (hasErr) ev.consume();
        });
        Optional<ButtonType> answer = dialog.showAndWait();
        if (answer.isEmpty() || answer.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) return FormResult.cancelled();

        List<Control> invalids = new ArrayList<>();
        requireSelection(coursCombo, invalids);
        requireText(titreField, invalids);
        requireText(descriptionArea, invalids);
        validateTitle(titreField, invalids);
        validateDescription(descriptionArea, invalids);
        requireText(fichierField, invalids);
        if (!text(fichierField).toLowerCase().endsWith(".pdf")) {
            markInvalid(fichierField, true);
            if (!invalids.contains(fichierField)) invalids.add(fichierField);
        }
        validateDuration(ordreField, invalids); // ordre entre 1 et 1000
        requireSelection(niveauCombo, invalids);
        requireSelection(domaineCombo, invalids);
        if (!invalids.isEmpty()) {
            return FormResult.cancelled();
        }

        int ordre = Integer.parseInt(text(ordreField));

        Chapitre chapitre = source == null ? new Chapitre() : source;
        chapitre.setCoursId(coursCombo.getValue().getId());
        chapitre.setTitre(text(titreField));
        chapitre.setOrdre(ordre);
        chapitre.setDescription(text(descriptionArea));
        chapitre.setFichierC(text(fichierField));
        chapitre.setNiveau(safe(niveauCombo.getValue()));
        chapitre.setDomaine(safe(domaineCombo.getValue()));
        return FormResult.saved(chapitre);
    }

    private FormResult<Td> showTdForm(Td source) {
        return showTdForm(source, null);
    }

    private FormResult<Td> showTdForm(Td source, Chapitre selectedChapitre) {
        ComboBox<Cours> coursCombo = comboCours();
        ComboBox<Chapitre> chapitreCombo = comboChapitre();
        bindChapitreComboToCours(coursCombo, chapitreCombo);
        TextField titreField = field();
        TextArea descriptionArea = area();
        TextField fichierField = field();
        Button browseFichier = new Button("Parcourir");
        browseFichier.getStyleClass().add("btn-secondary");
        browseFichier.setOnAction(event -> {
            File selected = chooseFile("Choisir un fichier PDF", List.of("pdf"), "PDF", fichierField);
            if (selected != null) {
                fichierField.setText(selected.getAbsolutePath());
            }
        });
        HBox fichierBox = new HBox(8, fichierField, browseFichier);
        fichierBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(fichierField, Priority.ALWAYS);
        ComboBox<String> niveauCombo = comboStrings(NIVEAUX);
        ComboBox<String> domaineCombo = comboStrings(DOMAINES);

        if (selectedChapitre != null) {
            selectCours(coursCombo, selectedChapitre.getCoursId());
            chapitreCombo.setItems(filteredChapitres(selectedChapitre.getCoursId()));
            selectChapitre(chapitreCombo, selectedChapitre.getId());
            coursCombo.setDisable(true);
            chapitreCombo.setDisable(true);
            niveauCombo.setValue(safe(selectedChapitre.getNiveau()));
            domaineCombo.setValue(safe(selectedChapitre.getDomaine()));
            niveauCombo.setDisable(true);
            domaineCombo.setDisable(true);
        }

        chapitreCombo.valueProperty().addListener((obs, oldChapitre, newChapitre) -> {
            if (newChapitre != null) {
                niveauCombo.setValue(safe(newChapitre.getNiveau()));
                domaineCombo.setValue(safe(newChapitre.getDomaine()));
                niveauCombo.setDisable(true);
                domaineCombo.setDisable(true);
            } else {
                niveauCombo.setValue(null);
                domaineCombo.setValue(null);
                niveauCombo.setDisable(false);
                domaineCombo.setDisable(false);
            }
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
        Label errCours = addRow(grid, 0, "Cours", coursCombo);
        Label errChapitre = addRow(grid, 1, "Chapitre", chapitreCombo);
        Label errTitre = addRow(grid, 2, "Titre", titreField);
        Label errDesc = addRow(grid, 3, "Description", descriptionArea);
        Label errFichier = addRow(grid, 4, "Fichier PDF", fichierBox);
        Label errNiveau = addRow(grid, 5, "Niveau", niveauCombo);
        Label errDomaine = addRow(grid, 6, "Domaine", domaineCombo);

        liveValidate(titreField, errTitre, () -> CoursValidationService.validateTdTitre(titreField.getText()));
        liveValidate(fichierField, errFichier, () -> { ValidationResult r = new ValidationResult(); String v = fichierField.getText().trim(); if (v.isBlank()) r.addError("Le fichier PDF est obligatoire."); else if (!v.toLowerCase().endsWith(".pdf")) r.addError("Doit etre un fichier .pdf"); return r; });
        liveValidate(descriptionArea, errDesc, () -> { ValidationResult r = new ValidationResult(); String v = descriptionArea.getText().trim(); if (v.isBlank()) r.addError("La description est obligatoire."); else if (v.length() < 10) r.addError("Minimum 10 caracteres."); else if (v.chars().anyMatch(Character::isDigit)) r.addError("La description ne doit pas contenir de chiffres."); return r; });


        Dialog<ButtonType> dialog = buildFormDialog(source == null ? "Creer un TD" : "Modifier un TD", grid);
        javafx.scene.control.Button okBtnT = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtnT.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            boolean hasErr = false;
            if (coursCombo.getValue() == null) { FormValidator.markError(coursCombo, "Obligatoire"); errCours.setText("⚠ Cours obligatoire."); hasErr = true; }
            if (chapitreCombo.getValue() == null) { FormValidator.markError(chapitreCombo, "Obligatoire"); errChapitre.setText("⚠ Chapitre obligatoire."); hasErr = true; }
            if (titreField.getText().trim().isBlank()) { FormValidator.markError(titreField, "Obligatoire"); errTitre.setText("⚠ Le titre est obligatoire."); hasErr = true; }
            if (fichierField.getText().trim().isBlank()) { FormValidator.markError(fichierField, "Obligatoire"); errFichier.setText("⚠ Le fichier est obligatoire."); hasErr = true; }
            if (descriptionArea.getText().trim().isBlank()) { FormValidator.markError(descriptionArea, "Obligatoire"); errDesc.setText("⚠ La description est obligatoire."); hasErr = true; } else if (descriptionArea.getText().trim().length() < 10) { FormValidator.markError(descriptionArea, "Min 10 car."); errDesc.setText("⚠ Minimum 10 caracteres."); hasErr = true; } else if (descriptionArea.getText().trim().chars().anyMatch(Character::isDigit)) { FormValidator.markError(descriptionArea, "Pas de chiffres"); errDesc.setText("⚠ La description ne doit pas contenir de chiffres."); hasErr = true; }
            if (hasErr) ev.consume();
        });
        Optional<ButtonType> answer = dialog.showAndWait();
        if (answer.isEmpty() || answer.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) return FormResult.cancelled();

        List<Control> invalids = new ArrayList<>();
        requireSelection(coursCombo, invalids);
        requireSelection(chapitreCombo, invalids);
        validateTitle(titreField, invalids);
        validateDescription(descriptionArea, invalids);
        requireText(fichierField, invalids);
        if (!text(fichierField).toLowerCase().endsWith(".pdf")) {
            markInvalid(fichierField, true);
            if (!invalids.contains(fichierField)) invalids.add(fichierField);
        }
        requireSelection(niveauCombo, invalids);
        requireSelection(domaineCombo, invalids);
        if (!invalids.isEmpty()) {
            return FormResult.cancelled();
        }

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

    private FormResult<VideoExplicative> showVideoForm(VideoExplicative source) {
        return showVideoForm(source, null);
    }

    private FormResult<VideoExplicative> showVideoForm(VideoExplicative source, Chapitre selectedChapitre) {
        ComboBox<Cours> coursCombo = comboCours();
        ComboBox<Chapitre> chapitreCombo = comboChapitre();
        bindChapitreComboToCours(coursCombo, chapitreCombo);
        TextField titreField = field();
        TextField urlField = field();
        TextArea descriptionArea = area();
        ComboBox<String> niveauCombo = comboStrings(NIVEAUX);
        ComboBox<String> domaineCombo = comboStrings(DOMAINES);

        if (selectedChapitre != null) {
            selectCours(coursCombo, selectedChapitre.getCoursId());
            chapitreCombo.setItems(filteredChapitres(selectedChapitre.getCoursId()));
            selectChapitre(chapitreCombo, selectedChapitre.getId());
            coursCombo.setDisable(true);
            chapitreCombo.setDisable(true);
            niveauCombo.setValue(safe(selectedChapitre.getNiveau()));
            domaineCombo.setValue(safe(selectedChapitre.getDomaine()));
            niveauCombo.setDisable(true);
            domaineCombo.setDisable(true);
        }

        chapitreCombo.valueProperty().addListener((obs, oldChapitre, newChapitre) -> {
            if (newChapitre != null) {
                niveauCombo.setValue(safe(newChapitre.getNiveau()));
                domaineCombo.setValue(safe(newChapitre.getDomaine()));
                niveauCombo.setDisable(true);
                domaineCombo.setDisable(true);
            } else {
                niveauCombo.setValue(null);
                domaineCombo.setValue(null);
                niveauCombo.setDisable(false);
                domaineCombo.setDisable(false);
            }
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
        }

        GridPane grid = formGrid();
        Label errCours = addRow(grid, 0, "Cours", coursCombo);
        Label errChapitre = addRow(grid, 1, "Chapitre", chapitreCombo);
        Label errTitre = addRow(grid, 2, "Titre", titreField);
        Label errUrl = addRow(grid, 3, "Url video", urlField);
        Label errDesc = addRow(grid, 4, "Description", descriptionArea);
        Label errNiveau = addRow(grid, 5, "Niveau", niveauCombo);
        Label errDomaine = addRow(grid, 6, "Domaine", domaineCombo);

        liveValidate(titreField, errTitre, () -> CoursValidationService.validateVideoTitre(titreField.getText()));
        liveValidate(urlField, errUrl, () -> CoursValidationService.validateVideoUrl(urlField.getText()));
        liveValidate(descriptionArea, errDesc, () -> { ValidationResult r = new ValidationResult(); String v = descriptionArea.getText().trim(); if (v.isBlank()) r.addError("La description est obligatoire."); else if (v.length() < 10) r.addError("Minimum 10 caracteres."); else if (v.chars().anyMatch(Character::isDigit)) r.addError("La description ne doit pas contenir de chiffres."); return r; });


        Dialog<ButtonType> dialog = buildFormDialog(source == null ? "Creer une video" : "Modifier une video", grid);
        javafx.scene.control.Button okBtnV = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtnV.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            boolean hasErr = false;
            if (coursCombo.getValue() == null) { FormValidator.markError(coursCombo, "Obligatoire"); errCours.setText("⚠ Cours obligatoire."); hasErr = true; }
            if (chapitreCombo.getValue() == null) { FormValidator.markError(chapitreCombo, "Obligatoire"); errChapitre.setText("⚠ Chapitre obligatoire."); hasErr = true; }
            if (titreField.getText().trim().isBlank()) { FormValidator.markError(titreField, "Obligatoire"); errTitre.setText("⚠ Le titre est obligatoire."); hasErr = true; }
            if (urlField.getText().trim().isBlank()) { FormValidator.markError(urlField, "Obligatoire"); errUrl.setText("⚠ L URL est obligatoire."); hasErr = true; }
            if (descriptionArea.getText().trim().isBlank()) { FormValidator.markError(descriptionArea, "Obligatoire"); errDesc.setText("⚠ La description est obligatoire."); hasErr = true; } else if (descriptionArea.getText().trim().length() < 10) { FormValidator.markError(descriptionArea, "Min 10 car."); errDesc.setText("⚠ Minimum 10 caracteres."); hasErr = true; } else if (descriptionArea.getText().trim().chars().anyMatch(Character::isDigit)) { FormValidator.markError(descriptionArea, "Pas de chiffres"); errDesc.setText("⚠ La description ne doit pas contenir de chiffres."); hasErr = true; }
            if (hasErr) ev.consume();
        });
        Optional<ButtonType> answer = dialog.showAndWait();
        if (answer.isEmpty() || answer.get().getButtonData() != ButtonBar.ButtonData.OK_DONE) return FormResult.cancelled();

        List<Control> invalids = new ArrayList<>();
        requireSelection(coursCombo, invalids);
        requireSelection(chapitreCombo, invalids);
        validateTitle(titreField, invalids);
        validateDescription(descriptionArea, invalids);
        requireText(urlField, invalids);
        validateUrl(urlField, invalids);
        requireSelection(niveauCombo, invalids);
        requireSelection(domaineCombo, invalids);
        if (!invalids.isEmpty()) {
            return FormResult.cancelled();
        }

        VideoExplicative video = source == null ? new VideoExplicative() : source;
        video.setCoursId(coursCombo.getValue().getId());
        video.setChapitreId(chapitreCombo.getValue().getId());
        video.setTitre(text(titreField));
        video.setUrlVideo(text(urlField));
        video.setDescription(text(descriptionArea));
        video.setNiveau(safe(niveauCombo.getValue()));
        video.setDomaine(safe(domaineCombo.getValue()));
        return FormResult.saved(video);
    }

    private Dialog<ButtonType> buildFormDialog(String title, Node content) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(760);
        Dialogs.style(dialog);
        // Forcer fond blanc sur tout le dialog
        dialog.getDialogPane().setStyle("-fx-background-color: white; -fx-background-radius: 0;");
        javafx.scene.layout.Region contentRegion = (javafx.scene.layout.Region) dialog.getDialogPane().lookup(".content");
        if (contentRegion != null) contentRegion.setStyle("-fx-background-color: white; -fx-padding: 14 18 14 18;");
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
        if (node instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(region, Priority.ALWAYS);
        }
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

    private <T> void liveValidateCombo(ComboBox<T> combo, Label errLabel, java.util.function.Supplier<ValidationResult> validator) {
        combo.valueProperty().addListener((obs, o, n) -> {
            ValidationResult r = validator.get();
            if (r.isValid()) { errLabel.setText(""); FormValidator.clearError(combo); }
            else { errLabel.setText("⚠ " + r.firstError()); FormValidator.markError(combo, r.firstError()); }
        });
    }

    private TextField field() {
        TextField field = new TextField();
        field.getStyleClass().add("field");
        return field;
    }

    private TextArea area() {
        TextArea area = new TextArea();
        area.getStyleClass().addAll("field", "area");
        area.setPrefRowCount(4);
        area.setWrapText(true);
        return area;
    }

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

    private File chooseFile(String title, List<String> allowedExt, String filterLabel, Node owner) {
        Window window = owner == null || owner.getScene() == null ? null : owner.getScene().getWindow();
        FileChooser fc = new FileChooser();
        fc.setTitle(title == null ? "Choisir un fichier" : title);
        List<String> patterns = new ArrayList<>();
        if (allowedExt != null) {
            for (String ext : allowedExt) {
                String e = safe(ext).toLowerCase();
                if (!e.isBlank()) patterns.add("*." + e);
            }
        }
        if (!patterns.isEmpty()) {
            FileChooser.ExtensionFilter all = new FileChooser.ExtensionFilter("All Files", "*.*");
            FileChooser.ExtensionFilter specific = new FileChooser.ExtensionFilter(safe(filterLabel).isBlank() ? "Fichiers" : filterLabel, patterns);
            fc.getExtensionFilters().setAll(all, specific);
            fc.setSelectedExtensionFilter(specific);
        }
        try {
            return fc.showOpenDialog(window);
        } catch (Exception e) {
            error("Fichier", e);
            return null;
        }
    }

    private void bindChapitreComboToCours(ComboBox<Cours> coursCombo, ComboBox<Chapitre> chapitreCombo) {
        coursCombo.valueProperty().addListener((obs, oldValue, newValue) -> {
            chapitreCombo.setValue(null);
            chapitreCombo.setItems(newValue == null ? FXCollections.observableArrayList() : filteredChapitres(newValue.getId()));
            markInvalid(coursCombo, false);
            markInvalid(chapitreCombo, false);
        });
    }

    private ObservableList<Chapitre> filteredChapitres(int coursId) {
        ObservableList<Chapitre> out = FXCollections.observableArrayList();
        for (Chapitre chapitre : repository.listChapitres("")) {
            if (chapitre.getCoursId() == coursId) {
                out.add(chapitre);
            }
        }
        return out;
    }

    private void selectCours(ComboBox<Cours> combo, int coursId) {
        for (Cours cours : combo.getItems()) {
            if (cours.getId() == coursId) {
                combo.setValue(cours);
                return;
            }
        }
    }

    private void selectChapitre(ComboBox<Chapitre> combo, int chapitreId) {
        for (Chapitre chapitre : combo.getItems()) {
            if (chapitre.getId() == chapitreId) {
                combo.setValue(chapitre);
                return;
            }
        }
    }

    private void requireText(Control control, List<Control> invalids) {
        String value = control instanceof TextArea area ? text(area) : text((TextField) control);
        boolean invalid = value.isBlank();
        markInvalid(control, invalid);
        if (invalid) invalids.add(control);
    }

    private void requireSelection(ComboBox<?> combo, List<Control> invalids) {
        boolean invalid = combo.getValue() == null;
        markInvalid(combo, invalid);
        if (invalid) invalids.add(combo);
    }

    private Integer requirePositiveInt(TextField field, List<Control> invalids) {
        try {
            int value = Integer.parseInt(text(field));
            boolean invalid = value <= 0;
            markInvalid(field, invalid);
            if (invalid) invalids.add(field);
            return invalid ? null : value;
        } catch (Exception e) {
            markInvalid(field, true);
            invalids.add(field);
            warnValidation("La duree doit etre un nombre entier positif.");
            return null;
        }
    }

    private void validateNoDigits(TextField field, String message, List<Control> invalids) {
        boolean invalid = containsDigit(text(field));
        if (invalid) {
            markInvalid(field, true);
            invalids.add(field);
            warnValidation(message);
        }
    }

    private void validateTitle(TextField field, List<Control> invalids) {
        ValidationResult r = CoursValidationService.validateChapitreTitre(text(field));
        if (!r.isValid()) {
            markInvalid(field, true);
            invalids.add(field);
            warnValidation(r.firstError());
        }
    }

    private void validateDescription(TextArea field, List<Control> invalids) {
        String value = text(field);
        boolean invalid = value.isEmpty() || value.length() < 10;
        if (invalid) {
            markInvalid(field, true);
            invalids.add(field);
            warnValidation("La description doit contenir au moins 10 caractères.");
        }
    }

    private void validateDuration(TextField field, List<Control> invalids) {
        ValidationResult r = CoursValidationService.validateDureeStr(text(field));
        if (!r.isValid()) {
            markInvalid(field, true);
            invalids.add(field);
            warnValidation(r.firstError());
        }
    }

    private void validateUrl(TextField field, List<Control> invalids) {
        ValidationResult r = CoursValidationService.validateVideoUrl(text(field));
        if (!r.isValid()) {
            markInvalid(field, true);
            invalids.add(field);
            warnValidation(r.firstError());
        }
    }

    private void validateName(TextField field, List<Control> invalids) {
        String value = text(field);
        boolean invalid = value.isEmpty() || containsDigit(value);
        if (invalid) {
            markInvalid(field, true);
            invalids.add(field);
            warnValidation("Le nom du formateur ne doit pas contenir de chiffres et doit être renseigné.");
        }
    }

    private void markInvalid(Control control, boolean invalid) {
        control.pseudoClassStateChanged(INVALID, invalid);
        if (invalid) {
            if (!control.getStyleClass().contains("field-invalid")) control.getStyleClass().add("field-invalid");
        } else {
            control.getStyleClass().remove("field-invalid");
        }
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        Dialogs.style(alert);
        Optional<ButtonType> answer = alert.showAndWait();
        return answer.isPresent() && answer.get() == ButtonType.OK;
    }

    private void warnValidation(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Controle de saisie");
        alert.setHeaderText("Des champs sont invalides");
        alert.setContentText(message);
        Dialogs.style(alert);
        alert.showAndWait();
    }

    private void info(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        Dialogs.style(alert);
        alert.showAndWait();
    }

    private void error(String title, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(e == null ? "Erreur" : safe(e.getMessage()));
        Dialogs.style(alert);
        alert.showAndWait();
    }

    private static boolean containsDigit(String value) {
        for (char c : safe(value).toCharArray()) {
            if (Character.isDigit(c)) return true;
        }
        return false;
    }

    private static int parseDuree(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String text(TextField field) {
        return field == null ? "" : safe(field.getText());
    }

    private static String text(TextArea area) {
        return area == null ? "" : safe(area.getText());
    }

    private record FormResult<T>(T value, boolean saved) {
        private static <T> FormResult<T> saved(T value) {
            return new FormResult<>(value, true);
        }

        private static <T> FormResult<T> cancelled() {
            return new FormResult<>(null, false);
        }
    }

    @FunctionalInterface
    private interface RowAction<T> {
        void run(T item);
    }
}
