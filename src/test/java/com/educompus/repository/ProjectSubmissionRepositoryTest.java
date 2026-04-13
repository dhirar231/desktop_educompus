package com.educompus.repository;

import com.educompus.model.ProjectSubmission;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProjectSubmissionRepositoryTest {
    private final ProjectSubmissionRepository repository = new ProjectSubmissionRepository();

    @Test
    void createRejectsNullSubmission() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> repository.create(null));
        assertEquals("submission is null", ex.getMessage());
    }

    @Test
    void updateMineRejectsMissingId() {
        ProjectSubmission submission = new ProjectSubmission();
        submission.setStudentId(4);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> repository.updateMine(submission));
        assertEquals("submission.id is required", ex.getMessage());
    }

    @Test
    void updateMineRejectsMissingStudentId() {
        ProjectSubmission submission = new ProjectSubmission();
        submission.setId(5);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> repository.updateMine(submission));
        assertEquals("submission.studentId is required", ex.getMessage());
    }

    @Test
    void deleteMineRejectsMissingIdentifiers() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> repository.deleteMine(0, 0));
        assertEquals("submissionId and studentId are required", ex.getMessage());
    }

    @Test
    void listMineReturnsEmptyWhenNoIdentityProvided() {
        assertTrue(repository.listMine(0, "").isEmpty());
    }

    @Test
    void hasSubmissionMineReturnsFalseForInvalidProjectId() {
        assertFalse(repository.hasSubmissionMine(0, 4, "student@educompus.tn"));
    }
}
 


 //////////////////////////////////