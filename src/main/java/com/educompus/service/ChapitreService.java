package com.educompus.service;

import com.educompus.model.Chapitre;
import com.educompus.repository.CourseManagementRepository;

import java.util.List;


public final class ChapitreService {

    private final CourseManagementRepository repository;

    public ChapitreService() {
        this.repository = new CourseManagementRepository();
    }

    //  Lecture


    public List<Chapitre> listerTous(String recherche) {
        return repository.listChapitres(recherche == null ? "" : recherche);
    }


    public List<Chapitre> listerParCours(int coursId) {
        if (coursId <= 0) {
            throw new IllegalArgumentException("ID du cours invalide.");
        }
        return repository.listChapitresByCoursId(coursId);
    }

    //  Création


    public void creer(Chapitre chapitre) {
        valider(chapitre);
        repository.createChapitre(chapitre);
    }

    //  Modification


    public void modifier(Chapitre chapitre) {
        if (chapitre.getId() <= 0) {
            throw new IllegalArgumentException("ID du chapitre invalide pour la modification.");
        }
        valider(chapitre);
        repository.updateChapitre(chapitre);
    }

    // Suppression


    public void supprimer(int chapitreId) {
        if (chapitreId <= 0) {
            throw new IllegalArgumentException("ID du chapitre invalide.");
        }
        repository.deleteChapitre(chapitreId);
    }

    // Validation


    public void valider(Chapitre chapitre) {
        ValidationResult result = validerSansException(chapitre);
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.allErrors());
        }
    }


    public ValidationResult validerSansException(Chapitre chapitre) {
        ValidationResult r = new ValidationResult();
        if (chapitre == null) {
            r.addError("Le chapitre est null.");
            return r;
        }

        // Titre
        ValidationResult titreResult = CoursValidationService.validateChapitreTitre(chapitre.getTitre());
        titreResult.getErrors().forEach(r::addError);

        // Ordre
        ValidationResult ordreResult = CoursValidationService.validateChapitreOrdre(
                chapitre.getOrdre() <= 0 ? "" : String.valueOf(chapitre.getOrdre())
        );
        ordreResult.getErrors().forEach(r::addError);

        // Cours associé
        ValidationResult coursResult = CoursValidationService.validateChapitreCoursId(chapitre.getCoursId());
        coursResult.getErrors().forEach(r::addError);

        // Fichier PDF
        if (chapitre.getFichierC() == null || chapitre.getFichierC().isBlank()) {
            r.addError("Le fichier PDF du chapitre est obligatoire.");
        } else if (!chapitre.getFichierC().trim().toLowerCase().endsWith(".pdf")) {
            r.addError("Le fichier du chapitre doit être un PDF (.pdf).");
        }

        return r;
    }
}
