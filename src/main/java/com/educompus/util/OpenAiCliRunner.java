package com.educompus.util;

import java.io.IOException;

public class OpenAiCliRunner {
    public static void main(String[] args) {
        String subject = "Test sujet génération IA";
        if (args != null && args.length > 0) {
            subject = String.join(" ", args);
        }
        try {
            String result = OpenAiClient.generateMessageFromSubject(subject);
            java.nio.file.Path out = java.nio.file.Path.of("target", "openai_result.txt");
            java.nio.file.Files.createDirectories(out.getParent());
            java.nio.file.Files.writeString(out, result, java.nio.charset.StandardCharsets.UTF_8);
            System.out.println("OPENAI_RESULT_WRITTEN");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.exit(2);
        }
    }
}
