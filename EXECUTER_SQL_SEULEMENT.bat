@echo off
chcp 65001 >nul
echo ================================================================================
echo 📊 EXÉCUTION DES SCRIPTS SQL - Module Engagement
echo ================================================================================
echo.

REM Configuration par défaut
set MYSQL_USER=root
set MYSQL_DB=educompus

echo Configuration :
echo - Utilisateur : %MYSQL_USER%
echo - Base de données : %MYSQL_DB%
echo.
set /p MYSQL_PASS="Entrez votre mot de passe MySQL : "
echo.

echo ================================================================================
echo [1/2] Création des tables...
echo ================================================================================
mysql -u %MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% < "src\main\resources\sql\student_engagement_schema.sql"
if %errorlevel% equ 0 (
    echo ✅ Tables créées
) else (
    echo ❌ Erreur
    pause
    exit /b 1
)
echo.

echo ================================================================================
echo [2/2] Ajout des données de test...
echo ================================================================================
mysql -u %MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% < "TEST_DONNEES_ENGAGEMENT.sql"
if %errorlevel% equ 0 (
    echo ✅ Données ajoutées
) else (
    echo ❌ Erreur
    pause
    exit /b 1
)
echo.

echo ================================================================================
echo ✅ TERMINÉ !
echo ================================================================================
echo.
echo Vous pouvez maintenant lancer l'application.
echo.
pause
