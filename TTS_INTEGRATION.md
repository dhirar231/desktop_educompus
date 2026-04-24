# 🎤 Intégration Google TTS - Conversion Texte vers Parole

## 🎯 Fonctionnalité Ajoutée

Votre système EduCompus dispose maintenant d'une **conversion texte-vers-parole (TTS)** complète avec Google Cloud Text-to-Speech !

### ✅ Votre clé API
La même clé Gemini fonctionne pour le TTS :
```
AIzaSyD78HeB-zcZPs_nGWNMGYqfKeosRA2mHZo
```

## 🚀 Fonctionnalités TTS

### 🎙️ Service GoogleTTSService
- **Conversion texte → audio** (MP3, WAV, OGG)
- **7 voix françaises** disponibles (masculines, féminines, neurales)
- **Paramètres ajustables** : vitesse, tonalité, volume
- **Génération asynchrone** pour l'interface utilisateur
- **Intégration automatique** avec la génération de vidéos

### 🎛️ Interface utilisateur complète
- **BackTTSController** + **BackTTS.fxml**
- Sélection de chapitres et scripts existants
- Paramètres de synthèse en temps réel
- Lecture audio intégrée
- Historique des fichiers générés

### 🔧 Paramètres disponibles

#### Voix françaises :
- **Féminine Standard** (fr-FR-Standard-A)
- **Féminine Claire** (fr-FR-Standard-C)  
- **Féminine Douce** (fr-FR-Standard-E)
- **Masculine Standard** (fr-FR-Standard-B)
- **Masculine Grave** (fr-FR-Standard-D)
- **Féminine Neurale** (fr-FR-Neural2-A) - Premium
- **Masculine Neurale** (fr-FR-Neural2-B) - Premium

#### Réglages :
- **Vitesse** : 0.25x à 4.0x
- **Tonalité** : -20 à +20 (grave ↔ aigu)
- **Volume** : -96 à +16 dB
- **Formats** : MP3, WAV, OGG

## 📁 Fichiers créés

### Services
- `GoogleTTSService.java` - Service principal TTS
- `BackTTSController.java` - Contrôleur interface

### Interface
- `BackTTS.fxml` - Interface utilisateur complète

### Tests et exemples
- `TTSExample.java` - Exemples d'utilisation

### Configuration
- Dépendance `javafx-media` ajoutée au pom.xml

## 🎯 Utilisation

### 1. Interface graphique
```java
// Ouvrir BackTTS.fxml dans votre application JavaFX
// L'interface permet de :
// - Sélectionner un chapitre/script existant
// - Ajuster les paramètres de voix
// - Convertir et écouter le résultat
// - Gérer l'historique des fichiers
```

### 2. API programmatique
```java
// Conversion simple
ParametresTTS parametres = new ParametresTTS();
parametres.setVoix(VoixFrancaise.FEMALE_A);
parametres.setVitesse(0.9); // Plus lent pour l'éducation

ResultatTTS resultat = GoogleTTSService.convertirTexte(texte, parametres);

if (resultat.isSucces()) {
    System.out.println("Audio généré: " + resultat.getCheminFichier());
}
```

### 3. Intégration automatique
Le TTS est **automatiquement intégré** dans la génération de vidéos :
- Quand vous générez une vidéo IA avec Gemini
- Un fichier audio est aussi créé en parallèle
- Disponible dans le dossier `audio/generated/`

## 💰 Coûts Google TTS

### Tarification
- **Gratuit** : 1 million de caractères/mois
- **Standard** : $4.00 par million de caractères
- **Neural (Premium)** : $16.00 par million de caractères

### Estimation pour l'éducation
- **Script de 500 mots** ≈ 3000 caractères ≈ $0.012
- **Très économique** pour un usage éducatif normal

## 🎵 Formats de sortie

### MP3 (Recommandé)
- Taille optimisée
- Compatible partout
- Qualité excellente

### WAV
- Qualité maximale
- Fichiers plus volumineux
- Idéal pour l'édition

### OGG
- Open source
- Bonne compression
- Support variable selon les plateformes

## 📊 Fonctionnalités avancées

### 🧹 Nettoyage automatique du texte
- Suppression des balises markdown
- Remplacement des abréviations
- Optimisation de la ponctuation pour la voix

### ⏱️ Estimation de durée
- Calcul automatique basé sur le nombre de mots
- Ajustement selon la vitesse de lecture
- Affichage en format MM:SS

### 🎛️ Paramètres éducatifs optimisés
- Vitesse légèrement réduite (0.9x)
- Volume augmenté (+2dB)
- Voix claire et pédagogique

## 🔧 Intégration dans votre app

### 1. Ajouter l'interface TTS
```java
// Dans votre menu principal, ajouter :
// "Outils" → "Conversion TTS" → BackTTS.fxml
```

### 2. Utiliser avec les chapitres existants
```java
// Le TTS peut convertir :
// - Descriptions de chapitres
// - Scripts générés par Gemini
// - Texte libre saisi par l'utilisateur
```

### 3. Lecture intégrée
```java
// L'interface inclut :
// - Lecteur audio JavaFX intégré
// - Contrôles play/pause/stop
// - Gestion de l'historique des fichiers
```

## ✨ Résultat final

Votre système EduCompus peut maintenant :
- ✅ **Générer des scripts** avec Gemini
- ✅ **Convertir en audio** avec Google TTS  
- ✅ **Créer des vidéos** avec D-ID
- ✅ **Interface complète** pour tout gérer
- ✅ **Intégration transparente** entre tous les services

**Une solution complète de génération de contenu éducatif multimédia !** 🎓🎬🎵

---

### 🎯 Prochaines étapes

1. **Testez l'interface TTS** : Ouvrez `BackTTS.fxml`
2. **Convertissez vos premiers textes** : Utilisez vos descriptions de chapitres
3. **Explorez les voix** : Testez les différentes options disponibles
4. **Intégrez dans votre workflow** : TTS + Gemini + D-ID = contenu complet !

**Votre plateforme éducative est maintenant équipée d'IA multimodale !** 🚀