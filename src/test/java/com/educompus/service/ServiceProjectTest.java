package com.educompus.service;

import com.educompus.model.Project;
import com.educompus.repository.ProjectRepository;
import com.educompus.repository.TestMySqlHelper;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceProjectTest {
    static ProjectRepository repo;
    static int idProject = -1;
    static String name = "ServiceProject_JUnit_" + System.currentTimeMillis();
    static String nameUpdated = "ServiceProject_Updated_" + System.currentTimeMillis();

    @BeforeAll
    public static void setupAll() throws Exception {
        TestMySqlHelper.init();
        repo = new ProjectRepository();
    }

    @AfterAll
    public static void teardownAll() {
        if (idProject > 0) {
            try { repo.delete(idProject); } catch (Exception ignored) {}
        }
    }

    @Test
    @Order(1)
    public void createProject() {
        Project p = new Project();
        p.setTitle(name);
        p.setDescription("desc");
        p.setDeadline("2026-12-31");
        p.setDeliverables("zip");
        p.setCreatedById(1);
        p.setPublished(false);

        repo.create(p);
        assertThat(p.getId()).isGreaterThan(0);
        idProject = p.getId();
    }

    @Test
    @Order(2)
    public void findById() {
        Assertions.assertTrue(idProject > 0);
        Project p = repo.getById(idProject);
        assertThat(p).isNotNull();
        assertThat(p.getTitle()).isEqualTo(name);
    }

    @Test
    @Order(3)
    public void updateProject() {
        Assertions.assertTrue(idProject > 0);
        Project p = repo.getById(idProject);
        p.setTitle(nameUpdated);
        repo.update(p);
        Project up = repo.getById(idProject);
        assertThat(up.getTitle()).isEqualTo(nameUpdated);
    }

    @Test
    @Order(4)
    public void deleteProject() {
        Assertions.assertTrue(idProject > 0);
        repo.delete(idProject);
        Project p = repo.getById(idProject);
        assertThat(p).isNull();
        idProject = -1;
    }
}

