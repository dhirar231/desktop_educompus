package com.educompus.service;

import com.educompus.model.AuthUser;
import com.educompus.model.Project;
import com.educompus.model.ProjectSubmissionView;
import com.educompus.repository.AuthUserRepository;
import com.educompus.repository.ProjectRepository;
import com.educompus.repository.ProjectSubmissionRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProjectMailingService {
    public enum Scope {
        ALL,
        SELECTED,
        PROJECT_SUBMITTERS
    }

    public record MailingResult(int sent, List<String> failedEmails, Map<String, String> failedReasons) {
    }

    private final AuthUserRepository userRepository = new AuthUserRepository();
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final ProjectSubmissionRepository submissionRepository = new ProjectSubmissionRepository();
    private final SmtpMailer smtpMailer = new SmtpMailer();

    public List<AuthUser> listStudents() throws Exception {
        List<AuthUser> students = new ArrayList<>();
        for (AuthUser user : userRepository.findAll()) {
            if (user != null && !user.admin() && !user.teacher() && !safe(user.email()).isBlank()) {
                students.add(user);
            }
        }
        return students;
    }

    public List<Project> listProjects() {
        return projectRepository.listAll("");
    }

    public SmtpMailer.MailConfig getMailConfig() {
        return smtpMailer.loadConfig();
    }

    public MailingResult sendMail(Scope scope, Integer projectId, Collection<Integer> selectedIds, String subject, String message) throws Exception {
        String rawSubject = safe(subject);
        String rawMessage = safe(message);
        if (rawSubject.isBlank() || rawMessage.isBlank()) {
            throw new IllegalArgumentException("Sujet et message sont obligatoires.");
        }

        SmtpMailer.MailConfig config = smtpMailer.loadConfig();
        if (!config.isConfigured()) {
            throw new IllegalStateException(config.summary());
        }

        Project project = resolveProject(projectId);
        List<AuthUser> recipients = resolveRecipients(scope, projectId, selectedIds);
        if (recipients.isEmpty()) {
            throw new IllegalArgumentException("Aucun etudiant cible pour cet envoi.");
        }

        int sent = 0;
        List<String> failed = new ArrayList<>();
        Map<String, String> failedReasons = new LinkedHashMap<>();
        for (AuthUser student : recipients) {
            String personalizedSubject = applyTemplate(rawSubject, student, project);
            String personalizedMessage = applyTemplate(rawMessage, student, project);
            try {
                smtpMailer.send(config, student.email(), personalizedSubject, personalizedMessage);
                sent++;
            } catch (Exception ex) {
                String email = safe(student.email());
                failed.add(email);
                failedReasons.put(email, safeFailureReason(ex));
            }
        }
        return new MailingResult(sent, failed, failedReasons);
    }

    public List<AuthUser> resolveRecipients(Scope scope, Integer projectId, Collection<Integer> selectedIds) throws Exception {
        Scope effectiveScope = scope == null ? Scope.ALL : scope;
        List<AuthUser> students = listStudents();
        return switch (effectiveScope) {
            case SELECTED -> resolveSelectedStudents(students, selectedIds);
            case PROJECT_SUBMITTERS -> resolveProjectSubmitters(projectId, students);
            case ALL -> students;
        };
    }

    public String applyTemplate(String template, AuthUser student, Project project) {
        String studentName = safe(student == null ? null : student.displayName());
        if (studentName.isBlank()) {
            studentName = "Etudiant";
        }
        String studentEmail = safe(student == null ? null : student.email());
        String projectTitle = safe(project == null ? null : project.getTitle());
        String projectDeadline = safe(project == null ? null : project.getDeadline());

        return safe(template)
                .replace("{{name}}", studentName)
                .replace("{{email}}", studentEmail)
                .replace("{{project_title}}", projectTitle)
                .replace("{{project_deadline}}", projectDeadline);
    }

    private List<AuthUser> resolveSelectedStudents(List<AuthUser> students, Collection<Integer> selectedIds) {
        Map<Integer, Boolean> wanted = new LinkedHashMap<>();
        if (selectedIds != null) {
            for (Integer id : selectedIds) {
                if (id != null && id > 0) {
                    wanted.put(id, Boolean.TRUE);
                }
            }
        }
        List<AuthUser> out = new ArrayList<>();
        for (AuthUser student : students) {
            if (wanted.containsKey(student.id())) {
                out.add(student);
            }
        }
        return out;
    }

    private List<AuthUser> resolveProjectSubmitters(Integer projectId, List<AuthUser> students) {
        int id = projectId == null ? 0 : projectId;
        if (id <= 0) {
            return List.of();
        }

        Map<Integer, AuthUser> studentsById = new LinkedHashMap<>();
        for (AuthUser student : students) {
            studentsById.put(student.id(), student);
        }

        Map<Integer, AuthUser> unique = new LinkedHashMap<>();
        for (ProjectSubmissionView submission : submissionRepository.listAll()) {
            if (submission != null && submission.getProjectId() == id) {
                AuthUser student = studentsById.get(submission.getStudentId());
                if (student != null) {
                    unique.put(student.id(), student);
                }
            }
        }
        return new ArrayList<>(unique.values());
    }

    private Project resolveProject(Integer projectId) {
        int id = projectId == null ? 0 : projectId;
        return id > 0 ? projectRepository.getById(id) : null;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safeFailureReason(Exception ex) {
        String message = ex == null ? "" : safe(ex.getMessage());
        if (!message.isBlank()) {
            return message;
        }
        return ex == null ? "Echec inconnu." : ex.getClass().getSimpleName();
    }
}
