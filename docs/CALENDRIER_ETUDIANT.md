# 📅 Calendrier Étudiant - Guide Utilisateur

## Vue d'ensemble

Le **Calendrier Étudiant** permet aux étudiants de consulter toutes leurs sessions live et événements à venir dans une interface visuelle intuitive, similaire à Google Calendar.

## Fonctionnalités

### 🗓️ Vue Calendrier Mensuelle

- **Navigation mensuelle** : Naviguez entre les mois avec les boutons ◀ et ▶
- **Bouton "Aujourd'hui"** : Retournez rapidement au mois actuel
- **Jour actuel surligné** : Le jour d'aujourd'hui est mis en évidence avec une bordure bleue
- **Sessions affichées** : Chaque jour affiche jusqu'à 2 sessions, avec un compteur pour les sessions supplémentaires

### 📌 Sessions à Venir

Une section dédiée affiche toutes les sessions prévues dans les **7 prochains jours**, triées par date et heure.

### 🎨 Codes Couleur par Statut

Les sessions sont colorées selon leur statut :

- **🔴 En cours** (Cyan) : Session actuellement active
- **📅 Planifiée** (Bleu) : Session à venir
- **✅ Terminée** (Gris) : Session passée
- **❌ Annulée** (Rouge) : Session annulée

### 🔗 Actions Disponibles

#### Pour les sessions EN COURS :
- **Bouton "🔴 Rejoindre la session"** : Ouvre directement le lien de la session dans le navigateur
- Clic sur la carte de session : Rejoint automatiquement la session

#### Pour les sessions PLANIFIÉES :
- **Bouton "📋 Détails"** : Affiche les informations complètes de la session
- Clic sur la carte : Affiche les détails

### 📅 Synchronisation Google Calendar

Les sessions synchronisées avec Google Calendar affichent un badge **"📅 Dans Google Calendar"** pour indiquer qu'elles sont également disponibles dans votre calendrier Google.

## Comment Accéder au Calendrier

1. Connectez-vous à votre compte étudiant
2. Dans la barre latérale gauche, cliquez sur **"📅 Calendrier"**
3. Le calendrier s'affiche avec le mois actuel

## Interface

### En-tête du Calendrier
```
◀  ▶  [Mois Année]                    [Aujourd'hui]
```

### Grille du Calendrier
```
Lun  Mar  Mer  Jeu  Ven  Sam  Dim
─────────────────────────────────────
 1    2    3    4    5    6    7
     [Session 1]
     [Session 2]
     +2 autres
```

### Carte de Session
```
┌─────────────────────────────────────┐
│ 🔴  Nom du Cours          [EN COURS]│
│     🕐 27/04/2026 14:00             │
│                                     │
│     [🔴 Rejoindre la session]       │
│     [📅 Dans Google Calendar]       │
└─────────────────────────────────────┘
```

## Détails Techniques

### Fichiers Créés

1. **Contrôleur** : `src/main/java/com/educompus/controller/front/FrontCalendarController.java`
   - Gère la logique du calendrier
   - Charge les sessions depuis la base de données
   - Gère les interactions utilisateur

2. **Vue FXML** : `src/main/resources/View/front/FrontCalendar.fxml`
   - Interface utilisateur du calendrier
   - Layout responsive avec ScrollPane

### Intégration

Le calendrier est intégré dans le **FrontShellController** via le bouton `navCalendarBtn` qui est visible uniquement pour les étudiants (non-admins).

### Dépendances

- **SessionLiveRepository** : Récupère les sessions depuis la base de données
- **SessionLive** : Modèle de données pour les sessions
- **SessionStatut** : Énumération des statuts de session

## Améliorations Futures Possibles

- ✨ Filtrage par cours
- 🔔 Notifications push avant les sessions
- 📥 Export des sessions vers fichier ICS
- 🔄 Synchronisation bidirectionnelle avec Google Calendar
- 📱 Vue responsive pour mobile
- 🎯 Marquage des sessions comme "Intéressé"
- 💬 Chat intégré pour les sessions en cours

## Support

Pour toute question ou problème, contactez l'équipe de support EduCompus.

---

**Version** : 1.0.0  
**Date** : 27 avril 2026  
**Auteur** : EduCompus Development Team
