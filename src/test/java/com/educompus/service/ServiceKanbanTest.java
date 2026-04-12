package com.educompus.service;

import com.educompus.model.KanbanTask;
import com.educompus.model.KanbanStatus;
import com.educompus.model.Project;
import com.educompus.repository.ProjectRepository;
import com.educompus.repository.TestMySqlHelper;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ServiceKanbanTest {
    static ProjectRepository projectRepo;
    static ServiceKanban service;
    static int idProject = -1;
    static int idTask = -1;

    @BeforeAll
    public static void setupAll() throws Exception {
        TestMySqlHelper.init();
        projectRepo = new ProjectRepository();
        service = new ServiceKanban();

        Project p = new Project();
        p.setTitle("JUnit_Kanban_Project");
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
        if (idTask > 0) {
            try { service.deleteTask(idTask); } catch (Exception ignored) {}
        }
        if (idProject > 0) {
            try { projectRepo.delete(idProject); } catch (Exception ignored) {}
        }
    }

    @Test
    @Order(1)
    public void addTask() throws Exception {
        KanbanTask t = new KanbanTask();
        t.setTitle("Task 1");
        t.setDescription("Do stuff");
        t.setStatus(KanbanStatus.TODO);
        t.setPosition(1);
        t.setProjectId(idProject);
        t.setStudentId(1);
        service.addTask(t);
        assertThat(t.getId()).isGreaterThan(0);
        idTask = t.getId();
    }

    @Test
    @Order(2)
    public void listTasks() throws Exception {
        List<KanbanTask> list = service.listByProjectAndStudent(idProject, 1);
        assertThat(list).isNotEmpty();
        KanbanTask t = list.stream().filter(x -> x.getId() == idTask).findFirst().orElse(null);
        assertThat(t).isNotNull();
        assertThat(t.getTitle()).isEqualTo("Task 1");
    }

    @Test
    @Order(3)
    public void updateAndSetStatus() throws Exception {
        KanbanTask t = new KanbanTask();
        t.setId(idTask);
        t.setTitle("Task 1 Updated");
        t.setDescription("Updated desc");
        t.setStatus(KanbanStatus.IN_PROGRESS);
        t.setPosition(2);
        service.updateTask(t);

        List<KanbanTask> list = service.listByProjectAndStudent(idProject, 1);
        KanbanTask fetched = list.stream().filter(x -> x.getId() == idTask).findFirst().orElse(null);
        assertThat(fetched).isNotNull();
        assertThat(fetched.getTitle()).isEqualTo("Task 1 Updated");

        service.setStatus(idTask, KanbanStatus.DONE);
        list = service.listByProjectAndStudent(idProject, 1);
        fetched = list.stream().filter(x -> x.getId() == idTask).findFirst().orElse(null);
        assertThat(fetched.getStatus()).isEqualTo(KanbanStatus.DONE);
    }

    @Test
    @Order(4)
    public void deleteTask() throws Exception {
        service.deleteTask(idTask);
        List<KanbanTask> list = service.listByProjectAndStudent(idProject, 1);
        boolean exists = list.stream().anyMatch(x -> x.getId() == idTask);
        assertThat(exists).isFalse();
        idTask = -1;
    }
}
