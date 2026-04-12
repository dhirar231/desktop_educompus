package com.educompus.service;

import com.educompus.model.ExamAnswer;
import com.educompus.model.ExamQuestion;
import com.educompus.repository.ExamRepository;
import com.educompus.repository.TestMySqlHelper;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceExamQuestionTest {
    static ExamRepository examRepo;
    static ServiceExamQuestion service;
    static int idExam = -1;
    static int idQuestion = -1;

    @BeforeAll
    public static void setupAll() throws Exception {
        TestMySqlHelper.init();
        examRepo = new ExamRepository();
        service = new ServiceExamQuestion();
        // create a base exam
        com.educompus.model.ExamCatalogueItem item = new com.educompus.model.ExamCatalogueItem();
        item.setExamTitle("JUnit_Eq_Test");
        item.setExamDescription("desc");
        item.setCourseId(1);
        item.setLevelLabel("L1");
        item.setDomainLabel("D");
        item.setPublished(false);
        examRepo.addExam(item);
        idExam = item.getExamId();
    }

    @AfterAll
    public static void teardownAll() {
        if (idQuestion > 0) {
            try { service.deleteQuestion(idQuestion); } catch (Exception ignored) {}
        }
        if (idExam > 0) {
            try { examRepo.deleteExam(idExam); } catch (Exception ignored) {}
        }
    }

    @Test
    @Order(1)
    public void createQuestionAndAnswers() throws Exception {
        ExamQuestion q = new ExamQuestion();
        q.setText("Quelle est la capitale ?");
        q.setDurationSeconds(60);
        service.addQuestion(q, idExam);
        assertThat(q.getId()).isGreaterThan(0);
        idQuestion = q.getId();

        ExamAnswer a1 = new ExamAnswer();
        a1.setText("Paris");
        a1.setCorrect(true);
        service.addAnswer(a1, idQuestion);
        assertThat(a1.getId()).isGreaterThan(0);

        ExamAnswer a2 = new ExamAnswer();
        a2.setText("Lyon");
        a2.setCorrect(false);
        service.addAnswer(a2, idQuestion);
        assertThat(a2.getId()).isGreaterThan(0);
    }

    @Test
    @Order(2)
    public void listQuestionsAndVerifyAnswers() throws Exception {
        List<ExamQuestion> list = service.listQuestionsByExamId(idExam);
        assertThat(list).isNotEmpty();
        ExamQuestion q = list.stream().filter(x -> x.getId() == idQuestion).findFirst().orElse(null);
        assertThat(q).isNotNull();
        assertThat(q.getAnswers()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @Order(3)
    public void updateQuestionAndAnswer() throws Exception {
        ExamQuestion q = new ExamQuestion();
        q.setId(idQuestion);
        q.setText("Updated question text");
        q.setDurationSeconds(90);
        service.updateQuestion(q);

        List<ExamQuestion> list = service.listQuestionsByExamId(idExam);
        ExamQuestion fetched = list.stream().filter(x -> x.getId() == idQuestion).findFirst().orElse(null);
        assertThat(fetched).isNotNull();
        assertThat(fetched.getText()).isEqualTo("Updated question text");
    }

    @Test
    @Order(4)
    public void deleteQuestion() throws Exception {
        service.deleteQuestion(idQuestion);
        List<ExamQuestion> list = service.listQuestionsByExamId(idExam);
        boolean exists = list.stream().anyMatch(x -> x.getId() == idQuestion);
        assertThat(exists).isFalse();
        idQuestion = -1;
    }
}
