package com.educompus.service;

import com.educompus.model.Td;
import com.educompus.repository.CourseManagementRepository;

import java.util.List;

/**
 * Couche service pour les Travaux Dirigés (TD).
 * Centralise la logique métier : validation + appel au repository.
 */
public final class TDService {

    private final CourseManagementRepository repository;

    public TDService() {
        this.repository = new CourseManagementRepository();
    }

    // ── Lecture ──────────────────────────────────────────────────────────────

    /** Retourne tous les TDs, filtrés par terme de recherche. */
    public List<Td> listerTous(String recherche) {
        return repository.listTds(recherche == null ? "" : recherche);
    }

    /** Retourne les TDs d'un cours donné. */
    public List<Td> listerParCours(int coursId) {
        if (coursId <= 0) {
            throw new IllegalArgumentException("ID du cours invalide.");
        }
        return repository.listTdsByCoursId(coursId);
    }

    // ── Création ─────────────────────────────────────────────────────────────

    /**
     * Valide puis crée un TD.
     * @throws IllegalArgumentException si la validation échoue
     */
    public void creer(Td td) {
        valider(td);
        repository.createTd(td);
    }

    // ── Modification ─────────────────────────────────────────────────────────

    /**
     * Valide puis met à jour un TD existant.
     * @throws IllegalArgumentException si la validation échoue
     */
    public void modifier(Td td) {
        if (td.getId() <= 0) {
            throw new IllegalArgumentException("ID du TD invalide pour la modification.");
        }
        valider(td);
        repository.updateTd(td);
    }

    // ── Suppression ──────────────────────────────────────────────────────────

    /**
     * Supprime un TD par son ID.
     * @throws IllegalArgumentException si l'ID est invalide
     */
    public void supprimer(int tdId) {
        if (tdId <= 0) {
            throw new IllegalArgumentException("ID du TD invalide.");
        }
        repository.deleteTd(tdId);
    }

    // ── Validation ───────────────────────────────────────────────────────────

    /**
     * Valide un TD (titre, chapitre, fichier PDF).
     * @throws IllegalArgumentException avec le détail des erreurs si invalide
     */
    public void valider(Td td) {
        ValidationResult result = validerSansException(td);
        if (!result.isValid()) {
            throw new IllegalArgumentException(result.allErrors());
        }
    }

    /** Retourne le résultat de validation sans lever d'exception. */
    public ValidationResult validerSansException(Td td) {
        ValidationResult r = new ValidationResult();
        if (td == null) {
            r.addError("Le TD est null.");
            return r;
        }

        // Titre
        ValidationResult titreResult = CoursValidationService.validateTdTitre(td.getTitre());
        titreResult.getErrors().forEach(r::addError);

        // Chapitre associé
        ValidationResult chapResult = CoursValidationService.validateTdChapitreId(td.getChapitreId());
        chapResult.getErrors().forEach(r::addError);

        // Cours associé
        if (td.getCoursId() <= 0) {
            r.addError("Veuillez sélectionner un cours.");
        }

        // Fichier PDF (obligatoire)
        if (td.getFichier() == null || td.getFichier().isBlank()) {
            r.addError("Le fichier PDF du TD est obligatoire.");
        } else if (!td.getFichier().trim().toLowerCase().endsWith(".pdf")) {
            r.addError("Le fichier du TD doit être un PDF (.pdf).");
        }

        // Description (optionnelle mais si présente, min 10 chars)
        String desc = td.getDescription();
        if (desc != null && !desc.isBlank() && desc.trim().length() < 10) {
            r.addError("La description doit contenir au moins 10 caractères si elle est renseignée.");
        }

        return r;
    }
}
