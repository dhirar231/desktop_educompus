package com.educompus.service;

import com.educompus.app.AppState;
import com.educompus.model.Project;
import com.educompus.repository.ProjectRepository;

public final class ProjectMeetingService {
    private final ProjectRepository projectRepository = new ProjectRepository();
    private final MeetingService meetingService = new MeetingService();

    public Project openMeetingForProject(Project project) {
        Project source = refresh(project);
        if (source == null || source.getId() <= 0) {
            throw new IllegalArgumentException("Projet introuvable.");
        }
        String room = meetingService.createProjectRoom(source);
        String url = meetingService.buildMeetingUrl(room);
        return projectRepository.activateMeeting(source.getId(), room, url, AppState.getUserId());
    }

    public Project closeMeetingForProject(Project project) {
        Project source = refresh(project);
        if (source == null || source.getId() <= 0) {
            throw new IllegalArgumentException("Projet introuvable.");
        }
        return projectRepository.deactivateMeeting(source.getId());
    }

    public Project refresh(Project project) {
        if (project == null || project.getId() <= 0) {
            return null;
        }
        return projectRepository.getById(project.getId());
    }

    public String joinUrl(Project project, boolean startMuted) {
        Project source = refresh(project);
        if (source == null || !source.isMeetingActive() || safe(source.getMeetingRoom()).isBlank()) {
            throw new IllegalStateException("Aucun meeting actif pour ce projet.");
        }
        return meetingService.buildMeetingUrl(
                source.getMeetingRoom(),
                AppState.getUserDisplayName(),
                startMuted,
                startMuted
        );
    }

    public String statusText(Project project) {
        Project source = refresh(project);
        if (source == null) {
            return "Meeting status: unavailable";
        }
        if (source.isMeetingActive() && !safe(source.getMeetingUrl()).isBlank()) {
            return "Meeting status: Ready";
        }
        return "Meeting status: Waiting for teacher";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
