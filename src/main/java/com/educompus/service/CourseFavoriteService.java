package com.educompus.service;

import com.educompus.repository.CourseFavoriteRepository;

import java.util.Set;

public final class CourseFavoriteService {

    private final CourseFavoriteRepository repository;

    public CourseFavoriteService() {
        this.repository = new CourseFavoriteRepository();
    }

    public CourseFavoriteService(CourseFavoriteRepository repository) {
        this.repository = repository;
    }

    public Set<Integer> listerIdsFavoris(int studentId) {
        validateStudent(studentId);
        return repository.listFavoriteCourseIds(studentId);
    }

    public boolean estFavori(int studentId, int coursId) {
        validateStudent(studentId);
        validateCours(coursId);
        return repository.isFavorite(studentId, coursId);
    }

    public void ajouter(int studentId, int coursId) {
        validateStudent(studentId);
        validateCours(coursId);
        repository.addFavorite(studentId, coursId);
    }

    public void retirer(int studentId, int coursId) {
        validateStudent(studentId);
        validateCours(coursId);
        repository.removeFavorite(studentId, coursId);
    }

    public boolean basculer(int studentId, int coursId) {
        validateStudent(studentId);
        validateCours(coursId);
        boolean favorite = repository.isFavorite(studentId, coursId);
        if (favorite) {
            repository.removeFavorite(studentId, coursId);
            return false;
        }
        repository.addFavorite(studentId, coursId);
        return true;
    }

    private static void validateStudent(int studentId) {
        if (studentId <= 0) {
            throw new IllegalArgumentException("Etudiant invalide.");
        }
    }

    private static void validateCours(int coursId) {
        if (coursId <= 0) {
            throw new IllegalArgumentException("Cours invalide.");
        }
    }
}
