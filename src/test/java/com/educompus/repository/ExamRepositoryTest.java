package com.educompus.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class ExamRepositoryTest {

    private final ExamRepository repo = new ExamRepository();

    @BeforeEach
    void cleanup() {
        // remove attempts file to ensure test isolation
        File attempts = new File("var/exam_attempts.properties");
        if (attempts.exists()) attempts.delete();

        // remove any test certificates
        File certDir = new File("var/certificates");
        if (certDir.exists() && certDir.isDirectory()) {
            File[] files = certDir.listFiles((d, name) -> name.startsWith("certificate_test"));
            if (files != null) for (File f : files) f.delete();
        }
    }

    @Test
    void listCatalogueFallbackReturnsExpectedItems() {
        var items = repo.listCatalogue("Java");
        assertNotNull(items, "Catalogue should not be null");
        assertFalse(items.isEmpty(), "Fallback catalogue should contain items");
        boolean foundJava = items.stream()
                .anyMatch(i -> i.getCourseTitle() != null && i.getCourseTitle().toLowerCase().contains("java"));
        assertTrue(foundJava, "Fallback catalogue should contain a Java course");
    }

    @Test
    void recordAttemptIncrementsAndStoresCertificate() throws Exception {
        String email = "test.user@example.com";
        int examId = 99999;

        assertEquals(0, repo.getAttemptCount(email, examId));

        // create certificate file and record attempt
        String certPath = repo.createCertificatePdf("Test User", email, "Unit Test Exam", 88, examId);
        assertNotNull(certPath);
        File certFile = new File(certPath);
        assertTrue(certFile.exists());

        repo.recordAttempt(email, examId, 88, true, certPath);
        assertEquals(1, repo.getAttemptCount(email, examId));
        assertEquals(certFile.getAbsolutePath(), repo.getCertificatePath(email, examId));
        assertTrue(repo.hasPassed(email, examId));
    }

    @Test
    void createCertificatePdfProducesReadableFile() throws Exception {
        String email = "test.cert@example.com";
        int examId = 88888;
        String path = repo.createCertificatePdf("Cert User", email, "Another Exam", 95, examId);
        assertNotNull(path, "createCertificatePdf should return a file path");
        File f = new File(path);
        assertTrue(f.exists(), "Certificate file should exist");
        assertTrue(f.length() > 64, "Certificate file should have non-trivial size");
    }
}
