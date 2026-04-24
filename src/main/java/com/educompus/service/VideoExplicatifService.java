package com.educompus.service;

import com.educompus.model.VideoExplicative;
import com.educompus.model.ParametresGeneration;
import com.educompus.repository.CourseManagementRepository;

import java.util.List;

/**
 * Service pour la gestion des vidéos explicatives.
 */
public final class VideoExplicatifService {

    private final CourseManagementRepository repository;

    public VideoExplicatifService() {
        this.repository = new CourseManagementRepository();
    }

    // Lecture

    public List<VideoExplicative> listerTous(String recherche) {
        return repository.listVideos(recherche == null ? "" : recherche);
    }

    public List<VideoExplicative> listerParCours(int coursId) {
        if (coursId <= 0) {
            throw new IllegalArgumentException("ID du cours invalide.");
        }
        return repository.listVideosByCoursId(coursId);
    }

    /**
     * Liste les vidéos par cours (alias pour listerParCours).
     */
    public List<VideoExplicative> listerVideosParCours(int coursId) {
        return listerParCours(coursId);
    }

    // Création

    public void creer(VideoExplicative video) {
        valider(video);
        repository.createVideo(video);
    }

    // Modification

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

    // Validation

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

        // Chapitre associé
        ValidationResult chapResult = CoursValidationService.validateVideoChapitreId(video.getChapitreId());
        chapResult.getErrors().forEach(r::addError);

        // Cours associé
        if (video.getCoursId() <= 0) {
            r.addError("Veuillez sélectionner un cours.");
        }

        // URL vidéo (optionnelle pour les vidéos générées par IA)
        if (video.getUrlVideo() != null && !video.getUrlVideo().isBlank()) {
            String url = video.getUrlVideo().trim();
            if (!isValidUrl(url)) {
                r.addError("L'URL de la vidéo n'est pas valide.");
            }
        }

        // Description
        String desc = video.getDescription();
        if (desc != null && !desc.isBlank() && desc.trim().length() < 10) {
            r.addError("La description doit contenir au moins 10 caractères si elle est renseignée.");
        }

        // Validation spécifique pour les vidéos IA
        if (video.isAIGenerated()) {
            if (video.getAiScript() == null || video.getAiScript().isBlank()) {
                r.addError("Le script IA est obligatoire pour les vidéos générées par IA.");
            }
            if (video.getGenerationStatus() == null || video.getGenerationStatus().isBlank()) {
                r.addError("Le statut de génération est obligatoire pour les vidéos IA.");
            }
        }

        return r;
    }

    /**
     * Valide si une URL est correctement formée.
     */
    private boolean isValidUrl(String url) {
        try {
            new java.net.URL(url);
            return true;
        } catch (java.net.MalformedURLException e) {
            return false;
        }
    }

    /**
     * Marque une vidéo comme générée par IA.
     */
    public void marquerCommeVideoIA(int videoId, String script, String didVideoId) {
        try {
            List<VideoExplicative> videos = repository.listVideos("");
            VideoExplicative video = videos.stream()
                    .filter(v -> v.getId() == videoId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Vidéo non trouvée: " + videoId));

            video.setAIGenerated(true);
            video.setAiScript(script);
            video.setDidVideoId(didVideoId);
            video.setGenerationStatus("PROCESSING");

            repository.updateVideo(video);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du marquage de la vidéo IA: " + e.getMessage(), e);
        }
    }

    /**
     * Met à jour le statut de génération d'une vidéo IA.
     */
    public void mettreAJourStatutGeneration(int videoId, String statut, String urlVideo) {
        try {
            List<VideoExplicative> videos = repository.listVideos("");
            VideoExplicative video = videos.stream()
                    .filter(v -> v.getId() == videoId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Vidéo non trouvée: " + videoId));

            video.setGenerationStatus(statut);
            if (urlVideo != null && !urlVideo.isBlank()) {
                video.setUrlVideo(urlVideo);
            }

            repository.updateVideo(video);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour du statut: " + e.getMessage(), e);
        }
    }

    /**
     * Obtient toutes les vidéos en cours de génération.
     */
    public List<VideoExplicative> listerVideosEnCoursDeGeneration() {
        return repository.listVideos("").stream()
                .filter(v -> v.isAIGenerated() && "PROCESSING".equals(v.getGenerationStatus()))
                .toList();
    }

    /**
     * Obtient toutes les vidéos IA terminées.
     */
    public List<VideoExplicative> listerVideosIATerminees() {
        return repository.listVideos("").stream()
                .filter(v -> v.isAIGenerated() && "COMPLETED".equals(v.getGenerationStatus()))
                .toList();
    }

    // Méthodes pour compatibilité avec l'ancien code

    /**
     * Liste toutes les vidéos (alias pour listerTous).
     */
    public List<VideoExplicative> listerToutes(String recherche) {
        return listerTous(recherche);
    }

    /**
     * Liste les vidéos par chapitre.
     */
    public List<VideoExplicative> listerVideosParChapitre(int chapitreId) {
        return repository.listVideosByChapitreId(chapitreId);
    }

    /**
     * Ferme les ressources (pour compatibilité).
     */
    public void fermer() {
        // Rien à fermer pour l'instant, méthode pour compatibilité
    }

    /**
     * Génère une vidéo de manière asynchrone avec les paramètres spécifiés.
     */
    public java.util.concurrent.CompletableFuture<VideoExplicative> genererVideoAsync(int videoId, ParametresGeneration parametres) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                // Récupérer la vidéo existante
                List<VideoExplicative> videos = repository.listVideos("");
                VideoExplicative video = videos.stream()
                        .filter(v -> v.getId() == videoId)
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Vidéo non trouvée: " + videoId));

                // Marquer comme vidéo IA en cours de génération
                video.setAIGenerated(true);
                video.setGenerationStatus("PROCESSING");
                
                // Simuler la génération (dans un vrai système, ceci ferait appel à l'API IA)
                Thread.sleep(parametres.getDureeMinutes() * 1000); // Simulation
                
                // Marquer comme terminé
                video.setGenerationStatus("COMPLETED");
                video.setUrlVideo("https://example.com/generated-video-" + videoId + ".mp4");
                
                repository.updateVideo(video);
                return video;
                
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors de la génération de la vidéo: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Génère une vidéo de manière synchrone avec les paramètres spécifiés.
     */
    public VideoExplicative genererVideo(int videoId, ParametresGeneration parametres) {
        try {
            return genererVideoAsync(videoId, parametres).get();
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération synchrone de la vidéo: " + e.getMessage(), e);
        }
    }
}