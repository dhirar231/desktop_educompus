@echo off
chcp 65001 >nul
echo ================================================================================
echo 🚀 INSTALLATION AUTOMATIQUE - Module Engagement Étudiants
echo ================================================================================
echo.

REM Vérifier si MySQL est accessible
echo [1/3] Vérification de MySQL...
mysql --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ MySQL n'est pas dans le PATH
    echo.
    echo 💡 Solution : Ajoutez MySQL au PATH ou utilisez le chemin complet
    echo    Exemple : "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    echo.
    pause
    exit /b 1
)
echo ✅ MySQL trouvé
echo.

REM Demander les informations de connexion
set /p MYSQL_USER="Entrez votre nom d'utilisateur MySQL (par défaut: root): "
if "%MYSQL_USER%"=="" set MYSQL_USER=root

set /p MYSQL_PASS="Entrez votre mot de passe MySQL: "

set /p MYSQL_DB="Entrez le nom de la base de données (par défaut: educompus): "
if "%MYSQL_DB%"=="" set MYSQL_DB=educompus

echo.
echo ================================================================================
echo [2/3] Création des tables SQL...
echo ================================================================================
echo.

REM Exécuter le script de création des tables
mysql -u %MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% < "src\main\resources\sql\student_engagement_schema.sql"
if %errorlevel% neq 0 (
    echo ❌ Erreur lors de la création des tables
    echo.
    pause
    exit /b 1
)
echo ✅ Tables créées avec succès
echo.

echo ================================================================================
echo [3/3] Ajout des données de test...
echo ================================================================================
echo.

REM Exécuter le script de données de test
mysql -u %MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% < "TEST_DONNEES_ENGAGEMENT.sql"
if %errorlevel% neq 0 (
    echo ❌ Erreur lors de l'ajout des données de test
    echo.
    pause
    exit /b 1
)
echo ✅ Données de test ajoutées avec succès
echo.

echo ================================================================================
echo ✅ INSTALLATION TERMINÉE !
echo ================================================================================
echo.
echo 🎉 Le module Engagement est prêt à être utilisé !
echo.
echo 📋 Prochaines étapes :
echo    1. Lancer votre application JavaFX
echo    2. Se connecter avec un compte Admin ou Enseignant
echo    3. Menu → OPÉRATIONS → Engagement
echo    4. Sélectionner un cours
echo.
echo 📊 Vous devriez voir :
echo    - Dashboard avec statistiques
echo    - Liste des étudiants avec cartes colorées
echo    - Scores de risque (vert/orange/rouge)
echo.
pause
