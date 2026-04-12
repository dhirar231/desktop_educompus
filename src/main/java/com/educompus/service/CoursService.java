package com.educompus.service;

import com.educompus.model.Chapitre;
import com.educompus.model.Cours;
import com.educompus.model.Td;
import com.educompus.model.VideoExplicative;
import com.educompus.repository.CourseManagementRepository;

import java.util.List;


public final class CoursService {

    private final CourseManagementRepository repository;

    public CoursService() {
        this.repository = new CourseManagementRepository();
    }

    // Lecture


    public List<Cours> listerTous(String recherche) {
        return repository.listCours(recherche == null ? "" : recherche);
    }


    public List<Chapitre> listerChapitres(int coursId) {
        return repository.listChapitresByCoursId(coursId);
    }


    public List<Td> listerTds(int coursId) {
        return repository.listTdsByCoursId(coursId);
    }


    public List<VideoExplicative> listerVideos(int coursId) {
        return repository.listVideosByCoursId(coursId);
    }

    // Création


    public void creer(Cours cours) {
        valider(cours);
        repository.createCours(cours);
    }

    //  Modification


    public void modifier(Cours cours) {
        if (cours.getId() <= 0) {
            throw new IllegalArgumentException("ID du cours invalide pour la modification.");
        }
        valider(cours);
        repository.updateCours(cours);
    }

    //  Suppression


    public void supprimer(int coursId) {
        if (coursId <= 0) {
            throw new IllegalArgumentException("ID du cours invalide.");
        }
        repository.deleteCours(coursId);
    }

    // Validation


    public void valider(Cours cours) {
        ValidationResult result = CoursValidationService.validateCours(cours);
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.allErrors());
        }
    }


    public ValidationResult validerSansException(Cours cours) {
        return CoursValidationService.validateCours(cours);
    }
}
