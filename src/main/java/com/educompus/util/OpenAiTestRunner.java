package com.educompus.util;

public final class OpenAiTestRunner {
    public static void main(String[] args) {
        String subject = (args != null && args.length > 0) ? String.join(" ", args) : "Sujet de test pour génération IA";
        System.out.println("Using subject: " + subject);
        try {
            String generated = OpenAiClient.generateMessageFromSubject(subject);
            java.io.File out = new java.io.File(System.getProperty("user.dir"), "target/openai_result.txt");
            try (java.io.PrintWriter w = new java.io.PrintWriter(java.nio.file.Files.newBufferedWriter(out.toPath(), java.nio.charset.StandardCharsets.UTF_8))) {
                w.println("SUBJECT: " + subject);
                w.println("--- GENERATED START ---");
                w.println(generated == null ? "" : generated);
                w.println("--- GENERATED END ---");
            }
            System.out.println("Wrote generated message to " + out.getAbsolutePath());
        } catch (Exception e) {
            try {
                java.io.File errFile = new java.io.File(System.getProperty("user.dir"), "target/openai_error.txt");
                try (java.io.PrintWriter w = new java.io.PrintWriter(java.nio.file.Files.newBufferedWriter(errFile.toPath(), java.nio.charset.StandardCharsets.UTF_8))) {
                    w.println("OpenAI generation failed: " + e.getMessage());
                    e.printStackTrace(w);
                }
                System.out.println("Wrote error to " + errFile.getAbsolutePath());
            } catch (Exception ignored) {
            }
            System.err.println("OpenAI generation failed: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(2);
        }
    }
}
