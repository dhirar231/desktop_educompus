package com.educompus.service;

import com.educompus.model.VideoExplicative;
import com.educompus.repository.CourseManagementRepository;

import java.util.List;


public final class VideoExplicatifService {

    private final CourseManagementRepository repository;

    public VideoExplicatifService() {
        this.repository = new CourseManagementRepository();
    }

    //  Lecture


    public List<VideoExplicative> listerToutes(String recherche) {
        return repository.listVideos(recherche == null ? "" : recherche);
    }


    public List<VideoExplicative> listerParCours(int coursId) {
        if (coursId <= 0) {
            throw new IllegalArgumentException("ID du cours invalide.");
        }
        return repository.listVideosByCoursId(coursId);
    }

    //  Création


    public void creer(VideoExplicative video) {
        valider(video);
        repository.createVideo(video);
    }

    //  Modification


    public void modifier(VideoExplicative video) {
        if (video.getId() <= 0) {
            throw new IllegalArgumentException("ID de la vidéo invalide pour la modification.");
        }
        valider(video);
        repository.updateVideo(video);
    }

    // Suppression


    public void supprimer(int videoId) {
        if (videoId <= 0) {
            throw new IllegalArgumentException("ID de la vidéo invalide.");
        }
        repository.deleteVideo(videoId);
    }

    // ── Validation ───────────────────────────────────────────────────────────


    public void valider(VideoExplicative video) {
        ValidationResult result = validerSansException(video);
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.allErrors());
        }
    }


    public ValidationResult validerSansException(VideoExplicative video) {
        ValidationResult r = new ValidationResult();
        if (video == null) {
            r.addError("La vidéo est null.");
            return r;
        }

        // Titre
        ValidationResult titreResult = CoursValidationService.validateVideoTitre(video.getTitre());
        titreResult.getErrors().forEach(r::addError);

        // URL
        ValidationResult urlResult = CoursValidationService.validateVideoUrl(video.getUrlVideo());
        urlResult.getErrors().forEach(r::addError);

        // Chapitre associé
        ValidationResult chapResult = CoursValidationService.validateVideoChapitreId(video.getChapitreId());
        chapResult.getErrors().forEach(r::addError);

        // Cours associé
        if (video.getCoursId() <= 0) {
            r.addError("Veuillez sélectionner un cours.");
        }

        // Description
        String desc = video.getDescription();
        if (desc != null && !desc.isBlank() && desc.trim().length() < 10) {
            r.addError("La description doit contenir au moins 10 caractères si elle est renseignée.");
        }

        return r;
    }
}
