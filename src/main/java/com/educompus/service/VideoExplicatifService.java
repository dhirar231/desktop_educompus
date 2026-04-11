package com.educompus.service;

import com.educompus.model.VideoExplicative;
import com.educompus.repository.CourseManagementRepository;

import java.util.List;

/**
 * Couche service pour les Vidéos Explicatives.
 * Centralise la logique métier : validation + appel au repository.
 */
public final class VideoExplicatifService {

    private final CourseManagementRepository repository;

    public VideoExplicatifService() {
        this.repository = new CourseManagementRepository();
    }

    // ── Lecture ──────────────────────────────────────────────────────────────

    /** Retourne toutes les vidéos, filtrées par terme de recherche. */
    public List<VideoExplicative> listerToutes(String recherche) {
        return repository.listVideos(recherche == null ? "" : recherche);
    }

    /** Retourne les vidéos d'un cours donné. */
    public List<VideoExplicative> listerParCours(int coursId) {
        if (coursId <= 0) {
            throw new IllegalArgumentException("ID du cours invalide.");
        }
        return repository.listVideosByCoursId(coursId);
    }

    // ── Création ─────────────────────────────────────────────────────────────

    /**
     * Valide puis crée une vidéo.
     * @throws IllegalArgumentException si la validation échoue
     */
    public void creer(VideoExplicative video) {
        valider(video);
        repository.createVideo(video);
    }

    // ── Modification ─────────────────────────────────────────────────────────

    /**
     * Valide puis met à jour une vidéo existante.
     * @throws IllegalArgumentException si la validation échoue
     */
    public void modifier(VideoExplicative video) {
        if (video.getId() <= 0) {
            throw new IllegalArgumentException("ID de la vidéo invalide pour la modification.");
        }
        valider(video);
        repository.updateVideo(video);
    }

    // ── Suppression ──────────────────────────────────────────────────────────

    /**
     * Supprime une vidéo par son ID.
     * @throws IllegalArgumentException si l'ID est invalide
     */
    public void supprimer(int videoId) {
        if (videoId <= 0) {
            throw new IllegalArgumentException("ID de la vidéo invalide.");
        }
        repository.deleteVideo(videoId);
    }

    // ── Validation ───────────────────────────────────────────────────────────

    /**
     * Valide une vidéo (titre, URL, chapitre).
     * @throws IllegalArgumentException avec le détail des erreurs si invalide
     */
    public void valider(VideoExplicative video) {
        ValidationResult result = validerSansException(video);
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.allErrors());
        }
    }

    /** Retourne le résultat de validation sans lever d'exception. */
    public ValidationResult validerSansException(VideoExplicative video) {
        ValidationResult r = new ValidationResult();
        if (video == null) {
            r.addError("La vidéo est null.");
            return r;
        }

        // Titre
        ValidationResult titreResult = CoursValidationService.validateVideoTitre(video.getTitre());
        titreResult.getErrors().forEach(r::addError);

        // URL (obligatoire)
        ValidationResult urlResult = CoursValidationService.validateVideoUrl(video.getUrlVideo());
        urlResult.getErrors().forEach(r::addError);

        // Chapitre associé
        ValidationResult chapResult = CoursValidationService.validateVideoChapitreId(video.getChapitreId());
        chapResult.getErrors().forEach(r::addError);

        // Cours associé
        if (video.getCoursId() <= 0) {
            r.addError("Veuillez sélectionner un cours.");
        }

        // Description (optionnelle mais si présente, min 10 chars)
        String desc = video.getDescription();
        if (desc != null && !desc.isBlank() && desc.trim().length() < 10) {
            r.addError("La description doit contenir au moins 10 caractères si elle est renseignée.");
        }

        return r;
    }
}
