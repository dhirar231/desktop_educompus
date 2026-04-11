package com.educompus.service;

import com.educompus.model.Chapitre;
import com.educompus.model.Cours;
import com.educompus.model.Td;
import com.educompus.model.VideoExplicative;
import com.educompus.repository.CourseManagementRepository;

import java.util.List;

/**
 * Couche service pour les Cours.
 * Centralise la logique métier : validation + appel au repository.
 */
public final class CoursService {

    private final CourseManagementRepository repository;

    public CoursService() {
        this.repository = new CourseManagementRepository();
    }

    // ── Lecture ──────────────────────────────────────────────────────────────

    /** Retourne tous les cours, filtrés par le terme de recherche (vide = tous). */
    public List<Cours> listerTous(String recherche) {
        return repository.listCours(recherche == null ? "" : recherche);
    }

    /** Retourne les chapitres d'un cours. */
    public List<Chapitre> listerChapitres(int coursId) {
        return repository.listChapitresByCoursId(coursId);
    }

    /** Retourne les TDs d'un cours. */
    public List<Td> listerTds(int coursId) {
        return repository.listTdsByCoursId(coursId);
    }

    /** Retourne les vidéos d'un cours. */
    public List<VideoExplicative> listerVideos(int coursId) {
        return repository.listVideosByCoursId(coursId);
    }

    // ── Création ─────────────────────────────────────────────────────────────

    /**
     * Valide puis crée un cours.
     * @throws IllegalArgumentException si la validation échoue
     */
    public void creer(Cours cours) {
        valider(cours);
        repository.createCours(cours);
    }

    // ── Modification ─────────────────────────────────────────────────────────

    /**
     * Valide puis met à jour un cours existant.
     * @throws IllegalArgumentException si la validation échoue
     */
    public void modifier(Cours cours) {
        if (cours.getId() <= 0) {
            throw new IllegalArgumentException("ID du cours invalide pour la modification.");
        }
        valider(cours);
        repository.updateCours(cours);
    }

    // ── Suppression ──────────────────────────────────────────────────────────

    /**
     * Supprime un cours par son ID.
     * @throws IllegalArgumentException si l'ID est invalide
     */
    public void supprimer(int coursId) {
        if (coursId <= 0) {
            throw new IllegalArgumentException("ID du cours invalide.");
        }
        repository.deleteCours(coursId);
    }

    // ── Validation ───────────────────────────────────────────────────────────

    /**
     * Valide un cours via {@link CoursValidationService}.
     * @throws IllegalArgumentException avec le détail des erreurs si invalide
     */
    public void valider(Cours cours) {
        ValidationResult result = CoursValidationService.validateCours(cours);
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.allErrors());
        }
    }

    /** Retourne le résultat de validation sans lever d'exception. */
    public ValidationResult validerSansException(Cours cours) {
        return CoursValidationService.validateCours(cours);
    }
}
