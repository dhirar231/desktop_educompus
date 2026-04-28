package com.educompus.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class WindowsHelloAuthService {
    private WindowsHelloAuthService() {
    }

    public static VerificationResult verify(String reason) {
        if (!isWindows()) {
            return new VerificationResult(false, "Windows Hello est disponible uniquement sous Windows.");
        }

        Path cliProject = resolveHelloCliProject();
        if (cliProject == null) {
            return new VerificationResult(false, "Projet Windows Hello introuvable: windows-hello-auth-cli.");
        }

        Process process = null;
        try {
            List<String> command = new ArrayList<>();
            command.add("dotnet");
            command.add("run");
            command.add("--project");
            command.add(cliProject.toString());

            process = new ProcessBuilder(command)
                    .directory(cliProject.getParent().toFile())
                    .redirectErrorStream(true)
                    .start();

            boolean finished = process.waitFor(90, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new VerificationResult(false, "Le delai Windows Hello est depasse.");
            }

            String output = readAll(process.getInputStream()).trim();
            return parseResult(output, process.exitValue());
        } catch (Exception e) {
            return new VerificationResult(false, "Windows Hello indisponible: " + safeMessage(e));
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static VerificationResult parseResult(String output, int exitCode) {
        if (output == null || output.isBlank()) {
            return new VerificationResult(false, "Aucune reponse de Windows Hello.");
        }

        String[] lines = output.split("\\R");
        for (String line : lines) {
            String value = line == null ? "" : line.trim();
            if ("Authentication success.".equalsIgnoreCase(value)) {
                return new VerificationResult(true, "");
            }
            if (value.startsWith("Authentication failed:")) {
                String detail = value.substring("Authentication failed:".length()).trim();
                return new VerificationResult(false, "Verification Windows Hello echouee: " + detail + ".");
            }
            if (value.startsWith("Windows Hello unavailable:")) {
                String detail = value.substring("Windows Hello unavailable:".length()).trim();
                return new VerificationResult(false, "Windows Hello indisponible: " + detail + ".");
            }
            if (value.startsWith("Windows Hello error:")) {
                String detail = value.substring("Windows Hello error:".length()).trim();
                return new VerificationResult(false, "Windows Hello indisponible: " + detail + ".");
            }
        }

        if (exitCode == 0) {
            return new VerificationResult(false, "Verification Windows Hello echouee.");
        }
        return new VerificationResult(false, "Commande Windows Hello en echec (code: " + exitCode + ").");
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

    private static Path resolveHelloCliProject() {
        String projectRelative = "windows-hello-auth-cli/windows-hello-auth-cli.csproj";
        Path cwd = Path.of("").toAbsolutePath().normalize();
        Path current = cwd;
        for (int i = 0; i < 7 && current != null; i++) {
            Path candidate = current.resolve(projectRelative);
            if (candidate.toFile().isFile()) {
                return candidate;
            }
            current = current.getParent();
        }
        return null;
    }

    private static String readAll(InputStream inputStream) throws Exception {
        try (InputStream in = inputStream; ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] tmp = new byte[1024];
            int read;
            while ((read = in.read(tmp)) >= 0) {
                if (read == 0) {
                    continue;
                }
                buffer.write(tmp, 0, read);
            }
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }

    private static String safeMessage(Exception e) {
        if (e == null || e.getMessage() == null || e.getMessage().isBlank()) {
            return "erreur inconnue";
        }
        String msg = e.getMessage().replace('\n', ' ').replace('\r', ' ').trim();
        return msg.length() > 140 ? msg.substring(0, 140) + "..." : msg;
    }

    public record VerificationResult(boolean success, String message) {
    }
}
