package com.educompus.service;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

/**
 * Génère de VRAIES vidéos MP4 éducatives :
 *  - Images réelles téléchargées selon le contexte
 *  - Narration vocale Windows TTS
 *  - Slideshow Ken Burns + sous-titres via FFmpeg
 */
public final class RealVideoGeneratorService {

    private static final String BASE_DIR;
    private static final String VIDEO_DIR;
    private static final String TEMP_DIR;
    private static final String FONT_PATH = "C\\\\:/Windows/Fonts/arial.ttf";

    static {
        String base = System.getProperty("user.dir", System.getProperty("user.home", "."));
        base = base.replace("\\", "/");
        BASE_DIR  = base;
        VIDEO_DIR = base + "/videos/generated/";
        TEMP_DIR  = base + "/videos/temp/";
    }

    private RealVideoGeneratorService() {}

    // ─────────────────────────────────────────────────────────────────────────
    // Point d'entrée
    // ─────────────────────────────────────────────────────────────────────────

    public static String genererVideoReelle(String videoId, String titreCours,
            String descriptionCours, String titreChapitre, String descriptionChapitre,
            String niveau, String domaine, String scriptNarration) {
        try {
            Files.createDirectories(Paths.get(VIDEO_DIR));
            Files.createDirectories(Paths.get(TEMP_DIR));

            System.out.println("🎬 [RealVideo] Démarrage...");
            System.out.println("📁 [RealVideo] Dossier: " + BASE_DIR);

            // Script de narration
            String script = (scriptNarration != null && !scriptNarration.isBlank())
                    ? scriptNarration
                    : buildDefaultScript(titreCours, titreChapitre, descriptionChapitre, domaine, niveau);

            // Mots-clés pour les images
            List<String> keywords = extractKeywords(descriptionChapitre != null ? descriptionChapitre : titreCours, domaine);
            System.out.println("🔍 [RealVideo] Mots-clés: " + keywords);

            // 1. Télécharger les images
            List<String> images = downloadImages(keywords, videoId);
            System.out.println("🖼️ [RealVideo] Images téléchargées: " + images.size());

            // 2. Générer l'audio TTS
            String audioPath = TEMP_DIR + "narration_" + videoId + ".wav";
            boolean audioOk = generateTTS(script, audioPath);
            System.out.println("🔊 [RealVideo] TTS: " + (audioOk ? "OK" : "ECHEC"));

            // 3. Créer le slideshow vidéo
            String slidesPath = TEMP_DIR + "slides_" + videoId + ".mp4";
            int duree = calcDuration(script);

            boolean slidesOk;
            if (!images.isEmpty()) {
                slidesOk = createSlideshowFromImages(images, slidesPath, titreCours,
                        titreChapitre, descriptionChapitre, duree, script);
            } else {
                slidesOk = createColorSlideshow(slidesPath, titreCours, titreChapitre, duree);
            }

            if (!slidesOk) {
                System.err.println("❌ [RealVideo] Echec slideshow");
                cleanup(images);
                return null;
            }

            // 4. Fusionner audio + vidéo
            String outputPath = VIDEO_DIR + "video_" + videoId + ".mp4";
            boolean ok = mergeAudioVideo(slidesPath, audioOk ? audioPath : null, outputPath);

            // Nettoyage
            cleanup(images);
            deleteIfExists(audioPath);
            deleteIfExists(slidesPath);

            if (ok) {
                long sz = Files.size(Paths.get(outputPath));
                System.out.println("✅ [RealVideo] Vidéo créée: " + outputPath + " (" + sz/1024 + " KB)");
                return Paths.get(outputPath).toAbsolutePath().toString();
            }
            return null;

        } catch (Exception e) {
            System.err.println("❌ [RealVideo] Exception: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Téléchargement d'images
    // ─────────────────────────────────────────────────────────────────────────

    private static List<String> downloadImages(List<String> keywords, String videoId) {
        List<String> paths = new ArrayList<>();
        for (int i = 0; i < Math.min(keywords.size(), 5); i++) {
            String kw = keywords.get(i);
            String dest = TEMP_DIR + "img_" + videoId + "_" + i + ".jpg";
            if (downloadImage(kw, dest)) {
                paths.add(dest);
                System.out.println("  ✅ Image " + i + ": " + kw);
            } else {
                System.out.println("  ⚠️ Echec image: " + kw);
            }
        }
        return paths;
    }

    private static boolean downloadImage(String keyword, String destPath) {
        // Essayer plusieurs sources d'images gratuites
        String[] sources = {
            "https://loremflickr.com/1280/720/" + URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8),
            "https://picsum.photos/seed/" + keyword.hashCode() + "/1280/720"
        };
        for (String src : sources) {
            try {
                URL url = new URL(src);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent", "EduCompus/1.0");

                // Suivre les redirections manuellement
                int status = conn.getResponseCode();
                if (status == 301 || status == 302 || status == 303) {
                    String loc = conn.getHeaderField("Location");
                    conn = (HttpURLConnection) new URL(loc).openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("User-Agent", "EduCompus/1.0");
                    status = conn.getResponseCode();
                }

                if (status == 200) {
                    try (InputStream in = conn.getInputStream()) {
                        Files.copy(in, Paths.get(destPath), StandardCopyOption.REPLACE_EXISTING);
                    }
                    long sz = Files.size(Paths.get(destPath));
                    if (sz > 5000) return true;
                }
            } catch (Exception e) {
                // Essayer la source suivante
            }
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Génération audio TTS (Windows SAPI)
    // ─────────────────────────────────────────────────────────────────────────

    private static boolean generateTTS(String script, String audioPath) {
        String psFile = null;
        try {
            // Nettoyer le script pour PowerShell
            String clean = script
                .replace("\"", " ").replace("'", " ").replace("`", " ")
                .replace("\r\n", " ").replace("\n", " ").replace("\\", " ");
            if (clean.length() > 1500) clean = clean.substring(0, 1500) + ". Fin.";

            // Chemin absolu du WAV
            String audioAbs = Paths.get(audioPath).toAbsolutePath().toString();

            // Écrire le script PS1 dans un fichier temp (évite les problèmes de guillemets/espaces inline)
            psFile = TEMP_DIR + "tts_" + System.currentTimeMillis() + ".ps1";
            String psContent =
                "Add-Type -AssemblyName System.Speech\r\n" +
                "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer\r\n" +
                "$synth.Rate = -1\r\n" +
                "$synth.Volume = 100\r\n" +
                "$synth.SetOutputToWaveFile(\"" + audioAbs.replace("\\", "\\\\") + "\")\r\n" +
                "$synth.Speak(\"" + clean + "\")\r\n" +
                "$synth.Dispose()\r\n";

            Files.write(Paths.get(psFile), psContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            String psFileAbs = Paths.get(psFile).toAbsolutePath().toString();

            ProcessBuilder pb = new ProcessBuilder(
                "powershell", "-NonInteractive", "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-File", psFileAbs
            );
            pb.redirectErrorStream(true);
            pb.directory(new File(BASE_DIR));
            Process proc = pb.start();

            StringBuilder out = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
                String line; while ((line = r.readLine()) != null) out.append(line).append("\n");
            }

            int exit = proc.waitFor();
            boolean ok = exit == 0
                    && Files.exists(Paths.get(audioPath))
                    && Files.size(Paths.get(audioPath)) > 1000;

            if (ok) {
                System.out.println("✅ [TTS] Audio généré: " + Files.size(Paths.get(audioPath)) / 1024 + " KB");
            } else {
                System.out.println("⚠️ [TTS] exit=" + exit + " | " + out.toString().trim());
            }
            return ok;

        } catch (Exception e) {
            System.out.println("⚠️ [TTS] Erreur: " + e.getMessage());
            return false;
        } finally {
            if (psFile != null) deleteIfExists(psFile);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3a. Slideshow depuis images réelles (Ken Burns + sous-titres)
    // ─────────────────────────────────────────────────────────────────────────

    private static boolean createSlideshowFromImages(List<String> images, String outputPath,
            String titre, String chapitre, String description, int totalDuration, String script) {
        try {
            int durPerImage = Math.max(totalDuration / images.size(), 5);

            List<String> slideFiles = new ArrayList<>();

            // Découper le script en segments pour les sous-titres
            String[] segments = splitScript(script, images.size());

            for (int i = 0; i < images.size(); i++) {
                String imgPath = Paths.get(images.get(i)).toAbsolutePath().toString();
                String slidePath = TEMP_DIR + "slide_" + System.currentTimeMillis() + "_" + i + ".mp4";
                String subtitle = i < segments.length ? escFFmpeg(segments[i]) : "";
                String headerText = i == 0 ? escFFmpeg(titre != null ? titre : "EduCompus") : "";

                boolean ok = createImageSlide(imgPath, slidePath, headerText, subtitle, durPerImage, i == 0);
                if (ok) slideFiles.add(slidePath);
            }

            if (slideFiles.isEmpty()) return false;

            boolean ok = concatSlides(slideFiles, outputPath);
            for (String f : slideFiles) deleteIfExists(f);
            return ok;

        } catch (Exception e) {
            System.err.println("❌ [Slideshow] " + e.getMessage());
            return false;
        }
    }

    private static boolean createImageSlide(String imgPath, String outputPath,
            String headerText, String subtitle, int duration, boolean isFirst) {
        try {
            // Ken Burns: zoom progressif sur l'image
            String zoompan = "zoompan=z='min(zoom+0.0008,1.3)':d=" + (duration * 25) +
                             ":x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':s=1280x720:fps=25";

            // Overlay sombre en bas pour la lisibilité des sous-titres
            String overlay = "drawbox=x=0:y=590:w=1280:h=130:color=black@0.65:t=fill";

            // Construire les filtres drawtext
            StringBuilder vf = new StringBuilder();
            vf.append(zoompan).append(",").append(overlay);

            if (!headerText.isEmpty()) {
                // En-tête avec fond bleu en haut
                vf.append(",drawbox=x=0:y=0:w=1280:h=80:color=0x1e40af@0.85:t=fill");
                vf.append(",drawtext=fontfile='").append(FONT_PATH).append("'")
                  .append(":text='").append(headerText).append("'")
                  .append(":fontcolor=white:fontsize=32:x=(w-text_w)/2:y=22")
                  .append(":shadowcolor=black:shadowx=1:shadowy=1");
            }

            if (!subtitle.isEmpty()) {
                vf.append(",drawtext=fontfile='").append(FONT_PATH).append("'")
                  .append(":text='").append(subtitle).append("'")
                  .append(":fontcolor=white:fontsize=22:x=(w-text_w)/2:y=610")
                  .append(":shadowcolor=black:shadowx=2:shadowy=2")
                  .append(":line_spacing=5");
            }

            // Filigrane EduCompus
            vf.append(",drawtext=fontfile='").append(FONT_PATH).append("'")
              .append(":text='EduCompus IA'")
              .append(":fontcolor=white@0.5:fontsize=14:x=w-130:y=h-30");

            String outAbs = Paths.get(outputPath).toAbsolutePath().toString();

            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y",
                "-loop", "1",
                "-i", imgPath,
                "-vf", vf.toString(),
                "-c:v", "libx264",
                "-t", String.valueOf(duration),
                "-pix_fmt", "yuv420p",
                "-preset", "fast",
                "-tune", "stillimage",
                outAbs
            );
            pb.redirectErrorStream(true);
            pb.directory(new File(BASE_DIR));
            Process proc = pb.start();
            new Thread(() -> { try { proc.getInputStream().transferTo(OutputStream.nullOutputStream()); } catch (Exception ignored) {} }).start();
            int exit = proc.waitFor();

            if (exit == 0 && Files.exists(Paths.get(outputPath)) && Files.size(Paths.get(outputPath)) > 500) {
                return true;
            }
            // Fallback sans Ken Burns (image simple)
            return createSimpleImageSlide(imgPath, outputPath, duration);

        } catch (Exception e) {
            System.out.println("⚠️ [Slide] " + e.getMessage());
            return createSimpleImageSlide(imgPath, outputPath, duration);
        }
    }

    private static boolean createSimpleImageSlide(String imgPath, String outputPath, int duration) {
        try {
            String outAbs = Paths.get(outputPath).toAbsolutePath().toString();
            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y", "-loop", "1", "-i", imgPath,
                "-vf", "scale=1280:720:force_original_aspect_ratio=increase,crop=1280:720",
                "-c:v", "libx264", "-t", String.valueOf(duration),
                "-pix_fmt", "yuv420p", "-preset", "ultrafast", outAbs
            );
            pb.redirectErrorStream(true);
            pb.directory(new File(BASE_DIR));
            Process proc = pb.start();
            new Thread(() -> { try { proc.getInputStream().transferTo(OutputStream.nullOutputStream()); } catch (Exception ignored) {} }).start();
            int exit = proc.waitFor();
            return exit == 0 && Files.exists(Paths.get(outputPath)) && Files.size(Paths.get(outputPath)) > 500;
        } catch (Exception e) {
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3b. Slideshow couleur (si pas d'images disponibles)
    // ─────────────────────────────────────────────────────────────────────────

    private static boolean createColorSlideshow(String outputPath, String titre, String chapitre, int duration) {
        try {
            String outAbs = Paths.get(outputPath).toAbsolutePath().toString();
            String titreEsc = escFFmpeg(titre != null ? titre : "EduCompus");

            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y", "-f", "lavfi",
                "-i", "color=c=0x1e40af:size=1280x720:duration=" + duration + ":rate=25",
                "-vf", "drawtext=fontfile='" + FONT_PATH + "':text='" + titreEsc + "':fontcolor=white:fontsize=40:x=(w-text_w)/2:y=(h-text_h)/2",
                "-c:v", "libx264", "-t", String.valueOf(duration),
                "-pix_fmt", "yuv420p", "-preset", "fast", outAbs
            );
            pb.redirectErrorStream(true);
            pb.directory(new File(BASE_DIR));
            Process proc = pb.start();
            new Thread(() -> { try { proc.getInputStream().transferTo(OutputStream.nullOutputStream()); } catch (Exception ignored) {} }).start();
            int exit = proc.waitFor();
            if (exit != 0) {
                // Ultra-fallback sans texte
                ProcessBuilder pb2 = new ProcessBuilder(
                    "ffmpeg", "-y", "-f", "lavfi",
                    "-i", "color=c=0x1e40af:size=1280x720:duration=" + duration + ":rate=25",
                    "-c:v", "libx264", "-t", String.valueOf(duration),
                    "-pix_fmt", "yuv420p", "-preset", "ultrafast", outAbs
                );
                pb2.redirectErrorStream(true);
                pb2.directory(new File(BASE_DIR));
                Process p2 = pb2.start();
                new Thread(() -> { try { p2.getInputStream().transferTo(OutputStream.nullOutputStream()); } catch (Exception ignored) {} }).start();
                exit = p2.waitFor();
            }
            return exit == 0 && Files.exists(Paths.get(outputPath)) && Files.size(Paths.get(outputPath)) > 500;
        } catch (Exception e) {
            System.err.println("❌ [ColorSlide] " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Concaténation + Fusion audio
    // ─────────────────────────────────────────────────────────────────────────

    private static boolean concatSlides(List<String> files, String outputPath) {
        try {
            String listFile = TEMP_DIR + "concat_" + System.currentTimeMillis() + ".txt";
            StringBuilder sb = new StringBuilder();
            for (String f : files) {
                sb.append("file '").append(Paths.get(f).toAbsolutePath().toString().replace("\\", "/")).append("'\n");
            }
            Files.write(Paths.get(listFile), sb.toString().getBytes());

            String listAbs = Paths.get(listFile).toAbsolutePath().toString();
            String outAbs  = Paths.get(outputPath).toAbsolutePath().toString();

            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-y", "-f", "concat", "-safe", "0",
                "-i", listAbs, "-c:v", "libx264", "-pix_fmt", "yuv420p", "-preset", "fast", outAbs
            );
            pb.redirectErrorStream(true);
            pb.directory(new File(BASE_DIR));
            Process proc = pb.start();
            new Thread(() -> { try { proc.getInputStream().transferTo(OutputStream.nullOutputStream()); } catch (Exception ignored) {} }).start();
            int exit = proc.waitFor();
            deleteIfExists(listFile);
            return exit == 0 && Files.exists(Paths.get(outputPath)) && Files.size(Paths.get(outputPath)) > 1000;
        } catch (Exception e) {
            System.err.println("❌ [Concat] " + e.getMessage());
            return false;
        }
    }

    private static boolean mergeAudioVideo(String videoPath, String audioPath, String outputPath) {
        try {
            String vAbs = Paths.get(videoPath).toAbsolutePath().toString();
            String oAbs = Paths.get(outputPath).toAbsolutePath().toString();

            List<String> cmd = new ArrayList<>(Arrays.asList("ffmpeg", "-y", "-i", vAbs));

            if (audioPath != null && Files.exists(Paths.get(audioPath))) {
                String aAbs = Paths.get(audioPath).toAbsolutePath().toString();
                cmd.addAll(Arrays.asList("-i", aAbs, "-c:v", "copy", "-c:a", "aac",
                        "-b:a", "128k", "-shortest", "-map", "0:v:0", "-map", "1:a:0"));
            } else {
                cmd.addAll(Arrays.asList("-c:v", "copy"));
            }
            cmd.add(oAbs);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.directory(new File(BASE_DIR));
            Process proc = pb.start();
            new Thread(() -> { try { proc.getInputStream().transferTo(OutputStream.nullOutputStream()); } catch (Exception ignored) {} }).start();
            int exit = proc.waitFor();
            return exit == 0 && Files.exists(Paths.get(outputPath)) && Files.size(Paths.get(outputPath)) > 1000;
        } catch (Exception e) {
            System.err.println("❌ [Merge] " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilitaires
    // ─────────────────────────────────────────────────────────────────────────

    private static List<String> extractKeywords(String description, String domaine) {
        List<String> kw = new ArrayList<>();
        if (domaine != null && !domaine.isBlank()) kw.add(domaine.split("[,;]")[0].trim());

        if (description != null) {
            String[] words = description.split("[\\s,;.!?]+");
            Set<String> stop = new HashSet<>(Arrays.asList(
                "le","la","les","de","du","des","un","une","en","et","est","pour",
                "que","qui","par","sur","dans","avec","ce","se","il","elle","nous","vous","ils"
            ));
            for (String w : words) {
                if (w.length() > 4 && !stop.contains(w.toLowerCase()) && kw.size() < 5) {
                    kw.add(w.replaceAll("[^a-zA-Z0-9\\u00C0-\\u024F]", ""));
                }
            }
        }
        if (kw.isEmpty()) kw.add("education");
        return kw;
    }

    private static String[] splitScript(String script, int parts) {
        if (script == null || script.isBlank()) return new String[0];
        String[] sentences = script.split("(?<=[.!?])\\s+");
        if (sentences.length <= parts) return sentences;
        // Regrouper les phrases en `parts` segments
        String[] result = new String[parts];
        int perPart = Math.max(sentences.length / parts, 1);
        for (int i = 0; i < parts; i++) {
            int start = i * perPart;
            int end = Math.min(start + perPart, sentences.length);
            StringBuilder sb = new StringBuilder();
            for (int j = start; j < end; j++) {
                String s = sentences[j].trim();
                if (sb.length() + s.length() < 80) { if (sb.length() > 0) sb.append(" "); sb.append(s); }
            }
            result[i] = sb.toString();
        }
        return result;
    }

    private static String buildDefaultScript(String titre, String chapitre, String desc, String domaine, String niveau) {
        StringBuilder s = new StringBuilder();
        s.append("Bienvenue dans cette video educative EduCompus. ");
        if (domaine != null && !domaine.isBlank()) s.append("Domaine ").append(domaine).append(". ");
        if (niveau != null && !niveau.isBlank()) s.append("Niveau ").append(niveau).append(". ");
        if (chapitre != null && !chapitre.isBlank()) s.append("Chapitre ").append(chapitre).append(". ");
        if (desc != null && !desc.isBlank()) s.append(desc).append(". ");
        s.append("Bon apprentissage.");
        return s.toString();
    }

    private static int calcDuration(String script) {
        if (script == null) return 30;
        long words = script.split("\\s+").length;
        return (int) Math.max(20, Math.min(words * 60 / 130 + 10, 180));
    }

    private static String escFFmpeg(String s) {
        if (s == null) return "";
        s = s.replaceAll("[^\\x20-\\x7E\\u00C0-\\u024F]", ""); // Enlever émojis
        return s.replace("\\", "").replace("'", "").replace(":", " -").replace("%", "pct").trim();
    }

    private static void deleteIfExists(String path) {
        try { Files.deleteIfExists(Paths.get(path)); } catch (Exception ignored) {}
    }

    private static void cleanup(List<String> files) {
        for (String f : files) deleteIfExists(f);
    }
}
