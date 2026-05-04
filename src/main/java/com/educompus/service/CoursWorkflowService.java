package com.educompus.service;

import com.educompus.app.AppState;
import com.educompus.model.Cours;
import com.educompus.model.CoursStatut;
import com.educompus.repository.CourseManagementRepository;
import com.educompus.repository.CoursValidationRepository;

public final class CoursWorkflowService {

    private final CoursValidationRepository validationRepo;
    private final CourseManagementRepository coursRepo;

    public CoursWorkflowService() {
        this.validationRepo = new CoursValidationRepository();
        this.coursRepo = new CourseManagementRepository();
    }

    public CoursWorkflowService(CoursValidationRepository validationRepo, CourseManagementRepository coursRepo) {
        this.validationRepo = validationRepo;
        this.coursRepo = coursRepo;
    }

    public void soumettre(Cours cours, int enseignantId) {
        if (!AppState.isTeacher()) {
            throw new SecurityException("Seuls les enseignants peuvent soumettre un cours.");
        }
        ValidationResult result = CoursValidationService.validateCours(cours);
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.allErrors());
        }
        cours.setStatut(CoursStatut.EN_ATTENTE);
        cours.setCreatedById(enseignantId);
        cours.setCommentaireAdmin(null);
        coursRepo.createCours(cours);
    }

    public void approuver(int coursId, int adminId) {
        if (!AppState.isAdmin()) {
            throw new SecurityException("Seuls les administrateurs peuvent approuver un cours.");
        }
        validationRepo.approuver(coursId);
    }

    public void refuser(int coursId, int adminId, String commentaire) {
        if (!AppState.isAdmin()) {
            throw new SecurityException("Seuls les administrateurs peuvent refuser un cours.");
        }
        validationRepo.refuser(coursId, commentaire);
    }

    public void reinitialiserPourModification(int coursId) {
        if (!AppState.isTeacher()) {
            throw new SecurityException("Seuls les enseignants peuvent soumettre à nouveau un cours.");
        }
        Cours cours = validationRepo.findById(coursId);
        if (cours == null) {
            throw new IllegalArgumentException("Cours introuvable.");
        }
        if (cours.getStatut() != CoursStatut.REFUSE) {
            throw new IllegalStateException("Seuls les cours refusés peuvent être remis en attente.");
        }
        validationRepo.reinitialiserStatut(coursId);
    }

    public void modifierEtSoumettre(Cours cours, int enseignantId) {
        if (!AppState.isTeacher()) {
            throw new SecurityException("Seuls les enseignants peuvent modifier un cours.");
        }
        if (cours == null || cours.getId() <= 0) {
            throw new IllegalArgumentException("Cours invalide.");
        }
        Cours existant = validationRepo.findById(cours.getId());
        if (existant == null) {
            throw new IllegalArgumentException("Cours introuvable.");
        }
        if (existant.getCreatedById() > 0 && existant.getCreatedById() != enseignantId) {
            throw new SecurityException("Vous ne pouvez modifier que vos propres cours.");
        }

        ValidationResult result = CoursValidationService.validateCours(cours);
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.allErrors());
        }

        cours.setCreatedById(existant.getCreatedById() > 0 ? existant.getCreatedById() : enseignantId);
        cours.setStatut(CoursStatut.EN_ATTENTE);
        cours.setCommentaireAdmin(null);
        validationRepo.mettreAJourEtRemettreEnAttente(cours);
    }
}
