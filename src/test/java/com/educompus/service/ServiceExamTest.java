package com.educompus.service;

import com.educompus.model.ExamCatalogueItem;
import com.educompus.repository.ExamRepository;
import com.educompus.repository.TestMySqlHelper;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceExamTest {
    static ExamRepository repo;
    static int idExam = -1;
    static String title = "ServiceExam_JUnit_" + System.currentTimeMillis();
    static String titleUpdated = "ServiceExam_Updated_" + System.currentTimeMillis();

    @BeforeAll
    public static void setupAll() throws Exception {
        TestMySqlHelper.init();
        repo = new ExamRepository();
    }

    @AfterAll
    public static void teardownAll() {
        if (idExam > 0) {
            try { repo.deleteExam(idExam); } catch (Exception ignored) {}
        }
    }

    @Test
    @Order(1)
    public void createExam() throws Exception {
        ExamCatalogueItem item = new ExamCatalogueItem();
        item.setExamTitle(title);
        item.setExamDescription("desc");
        item.setCourseId(1);
        item.setLevelLabel("Beginner");
        item.setDomainLabel("Test");
        item.setPublished(false);

        repo.addExam(item);
        assertThat(item.getExamId()).isGreaterThan(0);
        idExam = item.getExamId();
    }

    @Test
    @Order(2)
    public void getExamById() throws Exception {
        Assumptions.assumeTrue(idExam > 0);
        ExamCatalogueItem fetched = repo.getExamById(idExam);
        assertThat(fetched).isNotNull();
        assertThat(fetched.getExamTitle()).isEqualTo(title);
    }

    @Test
    @Order(3)
    public void updateExam() throws Exception {
        Assumptions.assumeTrue(idExam > 0);
        ExamCatalogueItem item = repo.getExamById(idExam);
        item.setExamTitle(titleUpdated);
        repo.updateExam(item);
        ExamCatalogueItem up = repo.getExamById(idExam);
        assertThat(up.getExamTitle()).isEqualTo(titleUpdated);
    }

    @Test
    @Order(4)
    public void deleteExam() throws Exception {
        Assumptions.assumeTrue(idExam > 0);
        repo.deleteExam(idExam);
        ExamCatalogueItem after = repo.getExamById(idExam);
        assertThat(after).isNull();
        idExam = -1;
    }
}

