# eduCompus JavaFX (FXML Templates)

Ce dossier contient des **templates UI** en **FXML** + une feuille **CSS**, avec un petit **runner JavaFX** (Maven) pour tester et naviguer entre les écrans.

## Contenu

- `View/front/*.fxml` (Front Office)
- `View/back/*.fxml` (Back Office)
- `styles/educompus.css` (style/couleurs eduCompus)
- `src/main/java/...` (controllers simples pour navigation + thème)

## Tester (Preview avec Maven)

Pré-requis : **Java 17+** et **Maven**.

Depuis `eduCompus-javafx` :

```bash
mvn javafx:run
```

Prévisualiser une vue spécifique :

```bash
mvn javafx:run -Dfxml=View/front/FrontDashboard.fxml
```

Désactiver le splash screen :

```bash
mvn javafx:run -Dsplash=false
```

Utiliser un autre splash FXML :

```bash
mvn javafx:run -DsplashFxml=View/front/Splash.fxml
```

Mode sombre rapide :

```bash
mvn javafx:run -Dfxml=View/back/BackShell.fxml -Dtheme=dark
```

## Intégration (exemple)

- Charger le FXML avec `FXMLLoader`
- Ajouter `styles/educompus.css` à ta `Scene.getStylesheets()`
