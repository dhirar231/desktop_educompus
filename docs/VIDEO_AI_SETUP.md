# Configuration des Vidéos IA avec Google Gemini - EduCompus

## Vue d'ensemble

Cette fonctionnalité permet de générer automatiquement des vidéos explicatives à partir des descriptions de chapitres en utilisant Google Gemini AI.

## Architecture

1. **Génération du script** : Google Gemini Pro génère un script détaillé à partir de la description du chapitre
2. **Génération de la vidéo** : D-ID transforme le script en vidéo avec un présentateur virtuel
3. **Stockage** : La vidéo est sauvegardée et référencée dans la base de données

## Configuration requise

### 1. Clé API Google Gemini

Votre clé API Gemini est déjà configurée dans le code :
```
AIzaSyD78HeB-zcZPs_nGWNMGYqfKeosRA2mHZo
```

### 2. Variables d'environnement (optionnel)

Pour une sécurité renforcée en production, vous pouvez définir :

```bash
# Windows
set GEMINI_API_KEY=AIzaSyD78HeB-zcZPs_nGWNMGYqfKeosRA2mHZo
set DID_API_KEY=votre_cle_did_ici

# Linux/Mac
export GEMINI_API_KEY=AIzaSyD78HeB-zcZPs_nGWNMGYqfKeosRA2mHZo
export DID_API_KEY=votre_cle_did_ici
```

### 3. Obtenir une clé D-ID (optionnel)

Pour la génération vidéo complète :
1. Créez un compte sur [D-ID Studio](https://studio.d-id.com/)
2. Allez dans Account Settings > API
3. Générez une nouvelle clé API
4. Définissez la variable `DID_API_KEY`

**Note** : Sans clé D-ID, le système fonctionne en mode simulation pour la partie vidéo.

### 3. Base de données

Les nouvelles colonnes seront automatiquement ajoutées à la table `video_explicative` :
- `is_ai_generated` : BOOLEAN - Indique si la vidéo est générée par IA
- `ai_script` : TEXT - Le script généré par l'IA
- `generation_status` : VARCHAR(32) - Statut de la génération (PENDING, PROCESSING, COMPLETED, ERROR)
- `did_video_id` : VARCHAR(128) - ID de la vidéo D-ID pour le suivi

## Utilisation

### Interface utilisateur

1. **Accès** : Ouvrez l'interface "Vidéos IA" dans le back-office
2. **Sélection** : Choisissez un chapitre existant
3. **Configuration** :
   - Durée de la vidéo (1-30 minutes)
   - Langue (français, anglais, espagnol, etc.)
   - Qualité (Standard, HD, 4K)
   - Type de voix (neutre, masculine, féminine)
   - Style de narration (pédagogique, professionnel, etc.)
4. **Génération** : Cliquez sur "Générer Vidéo IA"
5. **Suivi** : Suivez le progrès dans l'interface

### API programmatique

```java
// Créer le service
VideoExplicatifService service = new VideoExplicatifService();

// Configurer les paramètres
ParametresGeneration parametres = new ParametresGeneration();
parametres.setDureeMinutes(5);
parametres.setLangue("fr");
parametres.setQualite("HD");
parametres.setVoixType("neutre");
parametres.setStyleNarration("pédagogique");

// Générer la vidéo (asynchrone)
CompletableFuture<VideoExplicative> future = service.genererVideoAsync(chapitreId, parametres);

future.whenComplete((video, throwable) -> {
    if (throwable != null) {
        System.err.println("Erreur: " + throwable.getMessage());
    } else {
        System.out.println("Vidéo générée: " + video.getUrlVideo());
    }
});
```

## Coûts et limites

### OpenAI GPT-4
- **Coût** : ~$0.03 par 1K tokens d'entrée, ~$0.06 par 1K tokens de sortie
- **Estimation** : ~$0.10-0.30 par script de vidéo
- **Limite** : 8K tokens par requête

### D-ID
- **Coût** : ~$0.20-0.30 par minute de vidéo générée
- **Estimation** : ~$1.00-1.50 pour une vidéo de 5 minutes
- **Limite** : Dépend de votre plan D-ID

### Recommandations
- Commencez avec des vidéos courtes (2-5 minutes)
- Testez d'abord en mode simulation (sans clés API)
- Surveillez votre usage via les dashboards des APIs

## Dépannage

### Erreurs communes

1. **"Clé API OpenAI non configurée"**
   - Vérifiez que `OPENAI_API_KEY` est définie
   - Redémarrez l'application après avoir défini la variable

2. **"Erreur API OpenAI: 401"**
   - Clé API invalide ou expirée
   - Vérifiez votre quota sur OpenAI Platform

3. **"Erreur API D-ID: 402"**
   - Crédit D-ID insuffisant
   - Rechargez votre compte D-ID

4. **"Timeout: La génération D-ID a pris trop de temps"**
   - Les vidéos longues prennent plus de temps
   - Augmentez le timeout ou réduisez la durée

### Mode simulation

Si vous n'avez pas de clés API, le système fonctionne en mode simulation :
- Le script est généré avec un contenu par défaut
- Une URL de vidéo fictive est créée
- Utile pour tester l'interface et la logique

### Logs

Consultez les logs de l'application pour plus de détails sur les erreurs :
```
[VideoExplicatifService] Génération en cours pour chapitre 123
[VideoExplicatifService] Script généré: 1250 caractères
[VideoExplicatifService] Vidéo D-ID créée: talk_abc123
[VideoExplicatifService] Génération terminée: https://d-id.com/video/abc123.mp4
```

## Sécurité

- **Ne jamais** commiter les clés API dans le code source
- Utilisez des variables d'environnement ou un gestionnaire de secrets
- Limitez les permissions des clés API aux services nécessaires
- Surveillez l'usage pour détecter une utilisation anormale

## Support

Pour toute question ou problème :
1. Consultez d'abord cette documentation
2. Vérifiez les logs de l'application
3. Testez en mode simulation
4. Contactez l'équipe de développement avec les détails de l'erreur