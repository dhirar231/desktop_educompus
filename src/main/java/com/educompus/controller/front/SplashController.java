package com.educompus.controller.front;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;

public final class SplashController {
    @FXML
    private VBox content;

    @FXML
    private ImageView logoImage;

    @FXML
    private ProgressBar progressBar;

    private Runnable onDone;

    public void setOnDone(Runnable action) {
        this.onDone = action;
    }

    @FXML
    private void initialize() {
        if (logoImage != null) {
            URL splashImg = getClass().getResource("/assets/images/ss.png");
            if (splashImg == null) {
                splashImg = getClass().getResource("/assets/images/logo-light.png");
            }
            if (splashImg != null) {
                Image src = new Image(splashImg.toExternalForm(), true);
                logoImage.setImage(toWhite(src));
            }
        }
        if (progressBar != null) {
            progressBar.setProgress(0);
        }
    }

    private static Image toWhite(Image src) {
        if (src == null || src.isError() || src.getWidth() <= 0 || src.getHeight() <= 0) {
            return src;
        }

        PixelReader reader = src.getPixelReader();
        if (reader == null) {
            return src;
        }

        int w = (int) Math.ceil(src.getWidth());
        int h = (int) Math.ceil(src.getHeight());
        WritableImage out = new WritableImage(w, h);
        PixelWriter writer = out.getPixelWriter();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = reader.getArgb(x, y);
                int a = (argb >>> 24) & 0xFF;
                writer.setArgb(x, y, (a << 24) | 0x00FFFFFF);
            }
        }
        return out;
    }

    public void play() {
        if (content == null) {
            return;
        }

        // User requested: start larger then shrink to the initial state.
        content.setScaleX(1.25);
        content.setScaleY(1.25);
        content.setOpacity(0);

        ScaleTransition zoom = new ScaleTransition(Duration.millis(750), content);
        zoom.setFromX(1.25);
        zoom.setFromY(1.25);
        zoom.setToX(1.0);
        zoom.setToY(1.0);
        zoom.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(Duration.millis(520), content);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_OUT);

        Timeline loading = null;
        if (progressBar != null) {
            loading = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(progressBar.progressProperty(), 0)),
                    new KeyFrame(Duration.seconds(1.8), new KeyValue(progressBar.progressProperty(), 1, Interpolator.EASE_BOTH))
            );
        }

        ParallelTransition all = loading == null
                ? new ParallelTransition(zoom, fade)
                : new ParallelTransition(zoom, fade, loading);
        all.setOnFinished(e -> {
            if (onDone != null) {
                onDone.run();
            }
        });
        all.play();
    }
}
