package com.educompus.nav;

import com.educompus.util.Theme;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.awt.Taskbar;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public final class Navigator {
    private static Stage stage;
    private static String cssPath = "styles/educompus.css";
    private static File projectRoot;
    private static String appName = System.getProperty("appName", "EduCampus").trim();

    private Navigator() {
    }

	    public static void init(Stage primaryStage, String cssFilePath) {
	        stage = primaryStage;
	        cssPath = cssFilePath == null ? cssPath : cssFilePath;
	        projectRoot = guessProjectRoot();

	        if (stage != null && stage.getIcons().isEmpty()) {
	            try {
	                // Provide multiple PNG sizes so Windows can pick the best for title bar / taskbar.
	                List<Image> icons = new ArrayList<>();
	                URL png = Navigator.class.getResource("/assets/images/app-icon.png");
	                if (png != null) {
	                    icons.add(new Image(png.toExternalForm(), 16, 16, true, true));
	                    icons.add(new Image(png.toExternalForm(), 24, 24, true, true));
	                    icons.add(new Image(png.toExternalForm(), 32, 32, true, true));
	                    icons.add(new Image(png.toExternalForm(), 48, 48, true, true));
	                    icons.add(new Image(png.toExternalForm(), 64, 64, true, true));
	                    icons.add(new Image(png.toExternalForm(), 128, 128, true, true));
	                    icons.add(new Image(png.toExternalForm(), 256, 256, true, true));
	                }

	                // Also attempt to decode the .ico (some platforms may prefer it).
	                URL ico = Navigator.class.getResource("/assets/images/app-icon.ico");
	                if (ico != null) {
	                    icons.addAll(loadIcoAsFxImages(ico));
	                }

	                if (icons.isEmpty()) {
	                    URL url = Navigator.class.getResource("/assets/images/logo-light.png");
	                    if (url != null) {
	                        icons.add(new Image(url.toExternalForm()));
	                    }
	                }
	                stage.getIcons().addAll(icons);
	            } catch (Exception ignored) {
	            }
	        }

	        // Best effort: set Windows taskbar icon (some environments ignore Stage icons).
	        try {
	            if (Taskbar.isTaskbarSupported()) {
	                URL icon = Navigator.class.getResource("/assets/images/app-icon.png");
	                if (icon == null) {
	                    icon = Navigator.class.getResource("/assets/images/app-icon.ico");
	                }
	                if (icon != null) {
	                    BufferedImage img = javax.imageio.ImageIO.read(icon);
	                    if (img != null) {
	                        Taskbar.getTaskbar().setIconImage(img);
	                    }
	                }
	            }
	        } catch (Exception ignored) {
	        }
	    }

    private static List<Image> loadIcoAsFxImages(URL icoUrl) {
        if (icoUrl == null) {
            return List.of();
        }
        try {
            BufferedImage img = javax.imageio.ImageIO.read(icoUrl);
            if (img == null) {
                return List.of();
            }
            Image fx = bufferedToFx(img);
            return fx == null ? List.of() : List.of(fx);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    public static Stage getStage() {
        return stage;
    }

    private static Image bufferedToFx(BufferedImage img) {
        if (img == null) {
            return null;
        }
        int w = img.getWidth();
        int h = img.getHeight();
        if (w <= 0 || h <= 0) {
            return null;
        }

        WritableImage out = new WritableImage(w, h);
        PixelWriter writer = out.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                writer.setArgb(x, y, img.getRGB(x, y));
            }
        }
        return out;
    }

    public static Parent load(String fxmlPath) {
        return loadFromFile(fxmlPath);
    }

    public static FXMLLoader loader(String fxmlPath) {
        try {
            File file = resolveFile(fxmlPath);
            URL url;
            if (file != null && file.exists()) {
                url = file.toURI().toURL();
            } else {
                String cp = fxmlPath == null ? "" : fxmlPath.trim();
                if (cp.startsWith("/")) {
                    cp = cp.substring(1);
                }
                url = Navigator.class.getResource("/" + cp);
                if (url == null) {
                    File direct = new File(fxmlPath);
                    throw new IllegalArgumentException("FXML not found: " + direct.getAbsolutePath());
                }
            }
            return new FXMLLoader(url);
        } catch (Exception e) {
            File resolved = resolveFile(fxmlPath);
            String where = resolved == null ? "" : resolved.getAbsolutePath();
            String cause = summarizeThrowable(e);
            if (where.isBlank()) {
                throw new IllegalStateException("Failed to create FXML loader: " + fxmlPath + " (" + cause + ")", e);
            }
            throw new IllegalStateException("Failed to create FXML loader: " + fxmlPath + " -> " + where + " (" + cause + ")", e);
        }
    }

    public static File resolvePath(String path) {
        return resolveFile(path);
    }

    public static void goRoot(String fxmlPath) {
        if (stage == null) {
            throw new IllegalStateException("Navigator is not initialized");
        }

        Parent root = loadFromFile(fxmlPath);
        Scene scene = stage.getScene();
        if (scene == null) {
            scene = new Scene(root, 1200, 760);
            stage.setScene(scene);
        } else {
            scene.setRoot(root);
        }

        applyCss(scene);
        Theme.apply(root);

        stage.setTitle(appName.isBlank() ? "EduCampus" : appName);
    }

    private static Parent loadFromFile(String relativePath) {
        try {
            File file = resolveFile(relativePath);
            URL url;
            if (file != null && file.exists()) {
                url = file.toURI().toURL();
            } else {
                String cp = relativePath == null ? "" : relativePath.trim();
                if (cp.startsWith("/")) {
                    cp = cp.substring(1);
                }
                url = Navigator.class.getResource("/" + cp);
                if (url == null) {
                    File direct = new File(relativePath);
                    throw new IllegalArgumentException("FXML not found: " + direct.getAbsolutePath());
                }
            }
            FXMLLoader loader = new FXMLLoader(url);
            return loader.load();
        } catch (Exception e) {
            File resolved = resolveFile(relativePath);
            String where = resolved == null ? "" : resolved.getAbsolutePath();
            String cause = summarizeThrowable(e);
            if (where.isBlank()) {
                throw new IllegalStateException("Failed to load FXML: " + relativePath + " (" + cause + ")", e);
            }
            throw new IllegalStateException("Failed to load FXML: " + relativePath + " -> " + where + " (" + cause + ")", e);
        }
    }

    private static void applyCss(Scene scene) {
        if (scene == null) {
            return;
        }
        File cssFile = resolveFile(cssPath);
        if (cssFile.exists()) {
            String css = cssFile.toURI().toString();
            if (!scene.getStylesheets().contains(css)) {
                scene.getStylesheets().add(css);
            }
        }
    }

    private static File resolveFile(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        File direct = new File(path);
        if (direct.exists()) {
            return direct;
        }

        if (projectRoot != null) {
            File rooted = new File(projectRoot, path);
            if (rooted.exists()) {
                return rooted;
            }
        }

        File prefixed = new File("eduCompus-javafx", path);
        if (prefixed.exists()) {
            return prefixed;
        }

        File parentPrefixed = new File(new File("..", "eduCompus-javafx"), path);
        if (parentPrefixed.exists()) {
            return parentPrefixed;
        }

        return direct;
    }

    private static File guessProjectRoot() {
        // 1) From -Dfxml (if user passes absolute/long relative path)
        String fxml = System.getProperty("fxml", "").trim();
        if (!fxml.isBlank()) {
            File f = new File(fxml);
            if (f.exists()) {
                File root = parentOfDirNamed(f, "View");
                if (root == null) {
                    root = parentOfDirNamed(f, "fxml");
                }
                if (root != null) {
                    return root;
                }
            }
        }

        // 2) From cssPath if it points to an existing file.
        File css = new File(cssPath);
        if (css.exists()) {
            File root = parentOfDirNamed(css, "styles");
            if (root != null) {
                return root;
            }
        }

        // 3) Conventional folder name under current working directory.
        File conventional = new File("eduCompus-javafx");
        if (conventional.exists() && conventional.isDirectory()) {
            return conventional;
        }

        return new File(".");
    }

    private static File parentOfDirNamed(File file, String dirName) {
        if (file == null || dirName == null || dirName.isBlank()) {
            return null;
        }
        File cur = file.getAbsoluteFile();
        File parent = cur.isDirectory() ? cur : cur.getParentFile();
        while (parent != null) {
            if (dirName.equalsIgnoreCase(parent.getName())) {
                return parent.getParentFile();
            }
            parent = parent.getParentFile();
        }
        return null;
    }

    private static String summarizeThrowable(Throwable t) {
        if (t == null) {
            return "unknown error";
        }
        Throwable cur = t;
        int hops = 0;
        while (cur.getCause() != null && cur.getCause() != cur && hops < 6) {
            cur = cur.getCause();
            hops++;
        }
        String msg = cur.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = t.getMessage();
        }
        msg = msg == null ? "" : msg.replace('\n', ' ').replace('\r', ' ').trim();
        if (msg.length() > 200) {
            msg = msg.substring(0, 200) + "...";
        }
        String name = cur.getClass().getSimpleName();
        return msg.isBlank() ? name : (name + ": " + msg);
    }
}
