# Fonctionnalité Vidéos IA - EduCompus

## 🎯 Objectif
Générer automatiquement des vidéos explicatives à partir des descriptions de chapitres en utilisant l'intelligence artificielle.

## 🏗️ Architecture implémentée

### 1. Modèle de données
- **VideoExplicative** (existant, étendu) : Modèle pour les vidéos avec support IA
  - Nouveaux champs : `isAIGenerated`, `aiScript`, `generationStatus`, `didVideoId`
  - Statuts : PENDING, PROCESSING, COMPLETED, ERROR, CANCELLED

### 2. Service principal
- **VideoExplicatifService** : Service complet pour la gestion des vidéos IA
  - Génération asynchrone de vidéos
  - Intégration OpenAI GPT-4 pour les scripts
  - Intégration D-ID pour la génération vidéo
  - Validation complète des données
  - Gestion des erreurs et timeouts

### 3. Repository étendu
- **CourseManagementRepository** : Méthodes ajoutées pour les vidéos IA
  - Nouvelles colonnes dans la table `video_explicative`
  - Méthodes CRUD complètes pour les vidéos IA
  - Migration automatique du schéma

### 4. Interface utilisateur
- **BackVideoAIController** : Contrôleur JavaFX pour l'interface de génération
- **BackVideoAI.fxml** : Interface graphique complète
  - Formulaire de configuration des paramètres
  - Suivi en temps réel de la génération
  - Liste et gestion des vidéos existantes

### 5. Tests et exemples
- **VideoExplicatifServiceTest** : Tests unitaires complets
- **VideoAIExample** : Exemple d'utilisation programmatique
- Scripts de test automatisés

## 🔧 Configuration requise

### Variables d'environnement
```bash
OPENAI_API_KEY=votre_cle_openai
DID_API_KEY=votre_cle_did
```

### Dépendances ajoutées
- Jackson pour le parsing JSON
- HTTP Client Java (intégré)

## 🚀 Utilisation

### Interface graphique
1. Ouvrir `BackVideoAI.fxml` dans votre application JavaFX
2. Sélectionner un chapitre existant
3. Configurer les paramètres (durée, langue, qualité, etc.)
4. Cliquer sur "Générer Vidéo IA"
5. Suivre le progrès et voir le résultat

### API programmatique
```java
VideoExplicatifService service = new VideoExplicatifService();

ParametresGeneration parametres = new ParametresGeneration();
parametres.setDureeMinutes(5);
parametres.setLangue("fr");
parametres.setQualite("HD");

CompletableFuture<VideoExplicative> future = 
    service.genererVideoAsync(chapitreId, parametres);

future.whenComplete((video, error) -> {
    if (error == null) {
        System.out.println("Vidéo générée : " + video.getUrlVideo());
    } else {
        System.err.println("Erreur : " + error.getMessage());
    }
});
```

## 📊 Fonctionnalités

### ✅ Implémenté
- [x] Génération de scripts avec OpenAI GPT-4
- [x] Génération de vidéos avec D-ID
- [x] Interface utilisateur complète
- [x] Validation des données
- [x] Gestion asynchrone
- [x] Mode simulation (sans clés API)
- [x] Tests unitaires complets
- [x] Documentation complète
- [x] Gestion des erreurs
- [x] Suivi du statut de génération

### 🔄 Paramètres configurables
- Durée de la vidéo (1-30 minutes)
- Langue (français, anglais, espagnol, etc.)
- Qualité (Standard, HD, 4K)
- Type de voix (neutre, masculine, féminine)
- Style de narration (pédagogique, professionnel, etc.)

### 📈 Métriques et suivi
- Statut de génération en temps réel
- Historique des vidéos générées
- Scripts générés consultables
- Gestion des erreurs détaillée

## 💰 Coûts estimés

### OpenAI GPT-4
- ~$0.10-0.30 par script généré
- Dépend de la longueur de la description du chapitre

### D-ID
- ~$0.20-0.30 par minute de vidéo
- Une vidéo de 5 minutes coûte ~$1.00-1.50

### Recommandations
- Commencer avec des vidéos courtes (2-5 minutes)
- Tester en mode simulation d'abord
- Surveiller l'usage via les dashboards des APIs

## 🔒 Sécurité

- Clés API stockées dans des variables d'environnement
- Validation stricte des entrées utilisateur
- Gestion sécurisée des timeouts
- Pas de stockage des clés dans le code source

## 📁 Fichiers créés/modifiés

### Nouveaux fichiers
- `src/main/java/com/educompus/service/VideoExplicatifService.java`
- `src/main/java/com/educompus/controller/back/BackVideoAIController.java`
- `src/main/resources/View/back/BackVideoAI.fxml`
- `src/main/java/com/educompus/examples/VideoAIExample.java`
- `src/main/resources/config/ai-config.properties`
- `docs/VIDEO_AI_SETUP.md`
- `scripts/test-video-ai.bat`

### Fichiers modifiés
- `src/main/java/com/educompus/model/VideoExplicative.java` (champs IA ajoutés)
- `src/main/java/com/educompus/repository/CourseManagementRepository.java` (méthodes IA)
- `src/test/java/com/educompus/service/VideoExplicatifServiceTest.java` (tests IA)
- `pom.xml` (dépendance Jackson)

## 🎯 Prochaines étapes

1. **Configuration** : Définir les clés API dans les variables d'environnement
2. **Test** : Exécuter les tests unitaires et l'exemple
3. **Intégration** : Ajouter l'interface dans votre application principale
4. **Production** : Tester avec de vrais chapitres et surveiller les coûts

## 🆘 Support

- Consultez `docs/VIDEO_AI_SETUP.md` pour la configuration détaillée
- Exécutez `VideoAIExample.java` pour tester la fonctionnalité
- Les tests unitaires valident toutes les fonctionnalités
- Mode simulation disponible sans clés API

---

**La fonctionnalité est prête à être utilisée !** 🚀

Vous pouvez maintenant générer des vidéos explicatives automatiquement à partir de vos descriptions de chapitres.