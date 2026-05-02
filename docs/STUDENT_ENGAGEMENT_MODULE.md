# 🎯 Module de Détection des Étudiants Désengagés

## Vue d'ensemble

Le module de **Détection des Étudiants Désengagés** est un système d'analyse automatique qui identifie les étudiants à risque de décrochage en calculant un score de risque basé sur plusieurs critères d'engagement.

## 🎨 Fonctionnalités

### 1. Analyse Automatique
- Calcul automatique du score de risque pour chaque étudiant
- Analyse basée sur 4 critères principaux :
  - Absences aux sessions live
  - Inactivité (jours depuis dernière connexion)
  - Chapitres non consultés
  - Absence de téléchargements PDF

### 2. Dashboard Statistiques
- **Total Étudiants** : Nombre total d'étudiants dans le cours
- **Étudiants Actifs** (0-30 points) : Étudiants engagés ✅
- **À Surveiller** (31-60 points) : Étudiants à risque modéré ⚠️
- **Désengagés** (61+ points) : Étudiants à risque élevé ❌

### 3. Liste Détaillée des Étudiants
- Cartes colorées selon le niveau de risque (vert/orange/rouge)
- Affichage des métriques d'engagement
- Raisons détaillées du risque
- Actions disponibles : Envoyer rappel, Voir détails

### 4. Envoi d'Emails de Rappel
- Email HTML personnalisé
- Contenu adapté au niveau de risque
- Statistiques personnalisées pour chaque étudiant

### 5. Export CSV
- Export complet des données d'engagement
- Format compatible Excel
- Horodatage automatique du fichier

## 📊 Système de Scoring

### Critères et Poids

| Critère | Poids | Description |
|---------|-------|-------------|
| Absence session live | +10 par absence | Chaque session live manquée |
| Pas de connexion 7 jours | +25 | Aucune connexion depuis 7 jours |
| Pas de connexion 14 jours | +40 | Aucune connexion depuis 14 jours |
| Chapitre non consulté | +5 par chapitre | Chaque chapitre non ouvert |
| Aucun téléchargement PDF | +15 | Aucun support téléchargé |

### Niveaux de Risque

| Niveau | Score | Couleur | Action recommandée |
|--------|-------|---------|-------------------|
| **Actif** | 0-30 | Vert (#27ae60) | Aucune action nécessaire |
| **À Surveiller** | 31-60 | Orange (#f39c12) | Surveillance régulière |
| **Désengagé** | 61+ | Rouge (#e74c3c) | Intervention immédiate |

## 🗄️ Structure de la Base de Données

### Tables Créées

#### 1. `session_attendance`
Enregistre les présences aux sessions live.

```sql
CREATE TABLE session_attendance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    session_id INT NOT NULL,
    student_id INT NOT NULL,
    attended_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES session_live(id),
    FOREIGN KEY (student_id) REFERENCES utilisateur(id),
    UNIQUE KEY unique_attendance (session_id, student_id)
);
```

#### 2. `user_activity_log`
Enregistre les connexions des utilisateurs.

```sql
CREATE TABLE user_activity_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    date_connexion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT,
    FOREIGN KEY (user_id) REFERENCES utilisateur(id),
    INDEX idx_user_date (user_id, date_connexion)
);
```

#### 3. `pdf_download_log`
Enregistre les téléchargements de PDF.

```sql
CREATE TABLE pdf_download_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    chapter_id INT,
    pdf_type VARCHAR(50) NOT NULL,
    downloaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES utilisateur(id),
    FOREIGN KEY (course_id) REFERENCES cours(id),
    INDEX idx_student_course (student_id, course_id)
);
```

## 📁 Architecture du Code

### Modèle
- **`StudentEngagementRisk.java`** : Modèle représentant le risque d'un étudiant
  - Métriques d'engagement
  - Score et niveau de risque
  - Raisons du risque
  - Enum `RiskLevel` (ACTIF, A_SURVEILLER, DESENGAGÉ)

### Service
- **`StudentEngagementService.java`** : Logique métier
  - `analyzeStudentEngagement(courseId)` : Analyse tous les étudiants
  - `calculateRiskForStudent()` : Calcule le score d'un étudiant
  - `getEngagementStats()` : Statistiques globales
  - Méthodes privées pour chaque critère

### Contrôleur
- **`BackStudentEngagementController.java`** : Interface utilisateur
  - Sélection du cours
  - Affichage du dashboard
  - Gestion des actions (rappel, détails, export)
  - Envoi d'emails
  - Export CSV

### Vue
- **`BackStudentEngagement.fxml`** : Interface FXML
  - ComboBox de sélection de cours
  - Dashboard avec 4 cartes statistiques
  - Liste scrollable des étudiants
  - Bouton d'export CSV

## 🚀 Installation et Configuration

### 1. Exécuter le Script SQL

```bash
mysql -u root -p educompus < src/main/resources/sql/student_engagement_schema.sql
```

### 2. Configuration Email (Optionnel)

Pour activer l'envoi d'emails, modifiez dans `BackStudentEngagementController.java` :

```java
props.put("mail.smtp.host", "votre-serveur-smtp");
props.put("mail.smtp.port", "587");

// Dans getPasswordAuthentication()
return new javax.mail.PasswordAuthentication(
    "votre-email@educompus.tn",
    "votre-mot-de-passe"
);
```

**Recommandation** : Utilisez des variables d'environnement pour les credentials.

### 3. Dépendances Maven

Ajoutez dans `pom.xml` si nécessaire :

```xml
<dependency>
    <groupId>com.sun.mail</groupId>
    <artifactId>javax.mail</artifactId>
    <version>1.6.2</version>
</dependency>
```

## 📖 Guide d'Utilisation

### Pour les Enseignants

1. **Accéder au module**
   - Menu latéral → Section "OPÉRATIONS" → "Engagement"

2. **Analyser un cours**
   - Sélectionner un cours dans le menu déroulant
   - L'analyse se lance automatiquement
   - Consulter le dashboard pour une vue d'ensemble

3. **Identifier les étudiants à risque**
   - Les cartes sont triées par score décroissant
   - Couleur de la bordure gauche indique le niveau
   - Lire les raisons détaillées du risque

4. **Prendre des mesures**
   - **Envoyer un rappel** : Email automatique à l'étudiant
   - **Voir détails** : Popup avec toutes les informations
   - **Exporter CSV** : Sauvegarde pour analyse externe

### Interprétation des Résultats

#### Étudiant Actif (Vert)
- Score : 0-30
- Action : Aucune intervention nécessaire
- Continuer à surveiller normalement

#### Étudiant À Surveiller (Orange)
- Score : 31-60
- Action : Surveillance accrue
- Envisager un contact préventif

#### Étudiant Désengagé (Rouge)
- Score : 61+
- Action : Intervention immédiate
- Envoyer un rappel et contacter personnellement

## 🔧 Maintenance et Évolution

### Ajuster les Poids

Pour modifier les critères de scoring, éditer dans `StudentEngagementService.java` :

```java
private static final int WEIGHT_ABSENCE_PER_SESSION = 10;
private static final int WEIGHT_NO_CONNECTION_7_DAYS = 25;
private static final int WEIGHT_NO_CONNECTION_14_DAYS = 40;
private static final int WEIGHT_UNOPENED_CHAPTER = 5;
private static final int WEIGHT_NO_PDF_DOWNLOAD = 15;
```

### Ajuster les Seuils

Pour modifier les niveaux de risque, éditer dans `StudentEngagementRisk.java` :

```java
public enum RiskLevel {
    ACTIF("Actif", "#27ae60", 0, 30),
    A_SURVEILLER("À surveiller", "#f39c12", 31, 60),
    DESENGAGÉ("Désengagé", "#e74c3c", 61, Integer.MAX_VALUE);
}
```

## 📈 Améliorations Futures

### Court Terme
- [ ] Historique des scores par étudiant
- [ ] Graphiques d'évolution temporelle
- [ ] Filtres avancés (par niveau de risque)
- [ ] Notifications automatiques hebdomadaires

### Moyen Terme
- [ ] Prédiction ML du risque de décrochage
- [ ] Recommandations personnalisées d'intervention
- [ ] Intégration avec système de messagerie interne
- [ ] Rapports PDF automatiques

### Long Terme
- [ ] Dashboard temps réel avec WebSocket
- [ ] Analyse comparative entre cours
- [ ] Système de gamification pour l'engagement
- [ ] API REST pour intégrations externes

## 🐛 Dépannage

### Problème : Aucun étudiant affiché
- Vérifier que le cours contient des étudiants inscrits
- Vérifier les tables de la base de données
- Consulter les logs d'erreur

### Problème : Scores tous à 0
- Les tables de tracking sont peut-être vides
- Vérifier que les logs d'activité sont enregistrés
- Exécuter le script SQL de création des tables

### Problème : Envoi d'email échoue
- Vérifier la configuration SMTP
- Vérifier les credentials email
- Vérifier la connexion réseau
- Consulter les logs d'exception

## 📝 Notes Techniques

### Performance
- L'analyse est exécutée dans un thread séparé (JavaFX Task)
- Les requêtes SQL sont optimisées avec des index
- Le chargement est asynchrone pour ne pas bloquer l'UI

### Sécurité
- Les emails sont envoyés via SMTP sécurisé (STARTTLS)
- Les données sensibles doivent être en variables d'environnement
- Les requêtes SQL utilisent PreparedStatement (protection SQL injection)

### Compatibilité
- JavaFX 17+
- MySQL 8.0+
- Java Mail API 1.6+

## 📞 Support

Pour toute question ou problème :
- Consulter la documentation technique dans le code
- Vérifier les logs d'application
- Contacter l'équipe de développement

---

**Version** : 1.0.0  
**Date** : Avril 2026  
**Auteur** : EduCampus Development Team
