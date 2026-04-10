# eduCompus JavaFX (FXML Templates)

Ce dossier contient des **templates UI** en **FXML** + une feuille **CSS**, avec un petit **runner JavaFX** (Maven) pour tester et naviguer entre les écrans.

## Contenu

- `src/main/resources/View/front/*.fxml` (Front Office)
- `src/main/resources/View/back/*.fxml` (Back Office)
- `styles/educompus.css` (style/couleurs eduCompus)
- `src/main/java/...` (controllers + navigation + thème)

## Tester (Preview avec Maven)

Pré-requis : **Java 17+** et **Maven** (ou Maven Wrapper).

Depuis `eduCompus-javafx` :

```bash
mvn javafx:run
```

Ou (sans Maven installé) :

```bash
./mvnw.cmd javafx:run
```

Prévisualiser une vue spécifique :

```bash
mvn javafx:run -Dfxml=View/front/FrontDashboard.fxml
```

Mode sombre rapide :

```bash
mvn javafx:run -Dfxml=View/back/BackShell.fxml -Dtheme=dark
```

## Schéma DB (Projets)

Le schéma minimal pour **Project / ProjectSubmission / KanbanTask** est dans :

- `src/main/resources/sql/projects_schema.sql`

