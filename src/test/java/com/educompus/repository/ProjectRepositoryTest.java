package com.educompus.repository;

import com.educompus.model.Project;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProjectRepositoryTest {
    private final ProjectRepository repository = new ProjectRepository();

    @Test
    void createRejectsNullProject() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> repository.create(null));
        assertEquals("project is null", ex.getMessage());
    }

    @Test
    void updateRejectsNullProject() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> repository.update(null));
        assertEquals("project is null", ex.getMessage());
    }

    @Test
    void modelStoresProjectFields() {
        Project project = new Project();
        project.setId(7);
        project.setTitle("PFE");
        project.setDescription("Gestion projet");
        project.setDeadline("2026-05-01 10:00:00");
        project.setDeliverables("pdf, zip");
        project.setCreatedById(3);
        project.setPublished(true);
        project.setCreatedAt("2026-04-10 08:00:00");

        assertAll(
                () -> assertEquals(7, project.getId()),
                () -> assertEquals("PFE", project.getTitle()),
                () -> assertEquals("Gestion projet", project.getDescription()),
                () -> assertEquals("2026-05-01 10:00:00", project.getDeadline()),
                () -> assertEquals("pdf, zip", project.getDeliverables()),
                () -> assertEquals(3, project.getCreatedById()),
                () -> assertTrue(project.isPublished()),
                () -> assertEquals("2026-04-10 08:00:00", project.getCreatedAt())
        );
    }
}
