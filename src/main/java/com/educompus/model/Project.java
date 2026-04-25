package com.educompus.model;

public class Project {
    private int id;
    private String title;
    private String description;
    private String deadline;
    private String deliverables;
    private int createdById;
    private boolean published;
    private String createdAt;
    private String meetingRoom;
    private String meetingUrl;
    private boolean meetingActive;
    private int meetingStartedById;
    private String meetingStartedAt;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    public String getDeliverables() {
        return deliverables;
    }

    public void setDeliverables(String deliverables) {
        this.deliverables = deliverables;
    }

    public int getCreatedById() {
        return createdById;
    }

    public void setCreatedById(int createdById) {
        this.createdById = createdById;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getMeetingRoom() {
        return meetingRoom;
    }

    public void setMeetingRoom(String meetingRoom) {
        this.meetingRoom = meetingRoom;
    }

    public String getMeetingUrl() {
        return meetingUrl;
    }

    public void setMeetingUrl(String meetingUrl) {
        this.meetingUrl = meetingUrl;
    }

    public boolean isMeetingActive() {
        return meetingActive;
    }

    public void setMeetingActive(boolean meetingActive) {
        this.meetingActive = meetingActive;
    }

    public int getMeetingStartedById() {
        return meetingStartedById;
    }

    public void setMeetingStartedById(int meetingStartedById) {
        this.meetingStartedById = meetingStartedById;
    }

    public String getMeetingStartedAt() {
        return meetingStartedAt;
    }

    public void setMeetingStartedAt(String meetingStartedAt) {
        this.meetingStartedAt = meetingStartedAt;
    }
}

