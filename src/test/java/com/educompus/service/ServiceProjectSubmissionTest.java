package com.educompus.service;

import com.educompus.model.Project;
import com.educompus.model.ProjectSubmission;
import com.educompus.repository.ProjectRepository;
import com.educompus.repository.TestMySqlHelper;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceProjectSubmissionTest {
    static ProjectRepository projectRepo;
    static ServiceProjectSubmission service;
    static int idProject = -1;
    static int idSubmission = -1;

    @BeforeAll
    public static void setupAll() throws Exception {
        TestMySqlHelper.init();
        projectRepo = new ProjectRepository();
        service = new ServiceProjectSubmission();

        Project p = new Project();
        p.setTitle("JUnit_Project_Sub");
        p.setDescription("desc");
        p.setDeadline("2026-12-31");
        p.setDeliverables("zip");
        p.setCreatedById(1);
        p.setPublished(false);
        projectRepo.create(p);
        idProject = p.getId();
    }

    @AfterAll
    public static void teardownAll() {
        if (idSubmission > 0) {
            try { service.deleteSubmission(idSubmission); } catch (Exception ignored) {}
        }
        if (idProject > 0) {
            try { projectRepo.delete(idProject); } catch (Exception ignored) {}
        }
    }

    @Test
    @Order(1)
    public void addSubmission() throws Exception {
        ProjectSubmission s = new ProjectSubmission();
        s.setProjectId(idProject);
        s.setStudentId(1);
        s.setTextResponse("Here is my work");
        s.setCahierPath("/tmp/cahier.pdf");
        s.setDossierPath("/tmp/dossier.zip");
        service.addSubmission(s);
        assertThat(s.getId()).isGreaterThan(0);
        idSubmission = s.getId();
    }

    @Test
    @Order(2)
    public void listByProject() throws Exception {
        List<ProjectSubmission> list = service.listByProject(idProject);
        assertThat(list).isNotEmpty();
        ProjectSubmission s = list.stream().filter(x -> x.getId() == idSubmission).findFirst().orElse(null);
        assertThat(s).isNotNull();
        assertThat(s.getTextResponse()).isEqualTo("Here is my work");
    }

    @Test
    @Order(3)
    public void updateSubmission() throws Exception {
        ProjectSubmission s = new ProjectSubmission();
        s.setId(idSubmission);
        s.setTextResponse("Updated response");
        s.setCahierPath("/tmp/newcahier.pdf");
        s.setDossierPath("/tmp/newdossier.zip");
        service.updateSubmission(s);

        List<ProjectSubmission> list = service.listByProject(idProject);
        ProjectSubmission fetched = list.stream().filter(x -> x.getId() == idSubmission).findFirst().orElse(null);
        assertThat(fetched).isNotNull();
        assertThat(fetched.getTextResponse()).isEqualTo("Updated response");
    }

    @Test
    @Order(4)
    public void deleteSubmission() throws Exception {
        service.deleteSubmission(idSubmission);
        List<ProjectSubmission> list = service.listByProject(idProject);
        boolean exists = list.stream().anyMatch(x -> x.getId() == idSubmission);
        assertThat(exists).isFalse();
        idSubmission = -1;
    }
}
