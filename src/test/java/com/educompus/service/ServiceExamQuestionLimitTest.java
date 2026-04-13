package com.educompus.service;

import com.educompus.model.ExamAnswer;
import com.educompus.model.ExamQuestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceExamQuestionLimitTest {

    private ServiceExamQuestion service;
    private int questionId;

    @BeforeEach
    public void setUp() throws Exception {
        service = new ServiceExamQuestion();
        // create a temporary question in DB
        ExamQuestion q = new ExamQuestion();
        q.setText("Question limit test");
        q.setDurationSeconds(30);
        // insert into DB via service.addQuestion and reuse generated id
        service.addQuestion(q, 0); // examId 0 for test context
        questionId = q.getId();
        // ensure there are exactly 4 answers
        for (int i = 1; i <= 4; i++) {
            ExamAnswer a = new ExamAnswer();
            a.setText("Answer " + i);
            a.setCorrect(i == 1);
            service.addAnswer(a, questionId);
        }
    }

    @Test
    public void addingFifthAnswerShouldFail() {
        ExamAnswer fifth = new ExamAnswer();
        fifth.setText("Answer 5");
        fifth.setCorrect(false);
        Exception ex = assertThrows(IllegalStateException.class, () -> service.addAnswer(fifth, questionId));
        assertTrue(ex.getMessage().contains("4 réponses"));
    }
}
