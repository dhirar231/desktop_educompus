package com.educompus.service;

import com.educompus.model.Chapitre;
import com.educompus.repository.CourseManagementRepository;

import java.util.List;

/**
 * Couche service pour les Chapitres.
 * Centralise la logique métier : validation + appel au repository.
 */
public final class ChapitreService {

    private final CourseManagementRepository repository;

    public ChapitreService() {
        this.repository = new CourseManagementRepository();
    }

    // ── Lecture ──────────────────────────────────────────────────────────────

    /** Retourne tous les chapitres, filtrés par terme de recherche. */
    public List<Chapitre> listerTous(String recherche) {
        return repository.listChapitres(recherche == null ? "" : recherche);
    }

    /** Retourne les chapitres d'un cours donné. */
    public List<Chapitre> listerParCours(int coursId) {
        if (coursId <= 0) {
            throw new IllegalArgumentException("ID du cours invalide.");
        }
        return repository.listChapitresByCoursId(coursId);
    }

    // ── Création ─────────────────────────────────────────────────────────────

    /**
     * Valide puis crée un chapitre.
     * @throws IllegalArgumentException si la validation échoue
     */
    public void creer(Chapitre chapitre) {
        valider(chapitre);
        repository.createChapitre(chapitre);
    }

    // ── Modification ─────────────────────────────────────────────────────────

    /**
     * Valide puis met à jour un chapitre existant.
     * @throws IllegalArgumentException si la validation échoue
     */
    public void modifier(Chapitre chapitre) {
        if (chapitre.getId() <= 0) {
            throw new IllegalArgumentException("ID du chapitre invalide pour la modification.");
        }
        valider(chapitre);
        repository.updateChapitre(chapitre);
    }

    // ── Suppression ──────────────────────────────────────────────────────────

    /**
     * Supprime un chapitre par son ID.
     * @throws IllegalArgumentException si l'ID est invalide
     */
    public void supprimer(int chapitreId) {
        if (chapitreId <= 0) {
            throw new IllegalArgumentException("ID du chapitre invalide.");
        }
        repository.deleteChapitre(chapitreId);
    }

    // ── Validation ───────────────────────────────────────────────────────────

    /**
     * Valide un chapitre (titre, ordre, coursId).
     * @throws IllegalArgumentException avec le détail des erreurs si invalide
     */
    public void valider(Chapitre chapitre) {
        ValidationResult result = validerSansException(chapitre);
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.allErrors());
        }
    }

    /** Retourne le résultat de validation sans lever d'exception. */
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

        // Fichier PDF (obligatoire)
        if (chapitre.getFichierC() == null || chapitre.getFichierC().isBlank()) {
            r.addError("Le fichier PDF du chapitre est obligatoire.");
        } else if (!chapitre.getFichierC().trim().toLowerCase().endsWith(".pdf")) {
            r.addError("Le fichier du chapitre doit être un PDF (.pdf).");
        }

        return r;
    }
}
