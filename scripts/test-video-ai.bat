@echo off
echo ========================================
echo Test de la fonctionnalite Video AI
echo ========================================
echo.

REM Vérifier si les variables d'environnement sont définies
if "%OPENAI_API_KEY%"=="" (
    echo ATTENTION: OPENAI_API_KEY n'est pas definie
    echo La generation fonctionnera en mode simulation
    echo.
    echo Pour configurer:
    echo set OPENAI_API_KEY=votre_cle_ici
    echo.
) else (
    echo ✓ OPENAI_API_KEY configuree
)

if "%DID_API_KEY%"=="" (
    echo ATTENTION: DID_API_KEY n'est pas definie
    echo La generation fonctionnera en mode simulation
    echo.
    echo Pour configurer:
    echo set DID_API_KEY=votre_cle_ici
    echo.
) else (
    echo ✓ DID_API_KEY configuree
)

echo ========================================
echo Compilation du projet...
echo ========================================
call mvn compile -q

if %ERRORLEVEL% neq 0 (
    echo Erreur lors de la compilation
    pause
    exit /b 1
)

echo ✓ Compilation reussie
echo.

echo ========================================
echo Execution des tests...
echo ========================================
call mvn test -Dtest=VideoExplicatifServiceTest -q

if %ERRORLEVEL% neq 0 (
    echo Erreur lors des tests
    pause
    exit /b 1
)

echo ✓ Tests passes avec succes
echo.

echo ========================================
echo Execution de l'exemple...
echo ========================================
call mvn exec:java -Dexec.mainClass="com.educompus.examples.VideoAIExample" -q

echo.
echo ========================================
echo Test termine
echo ========================================
echo.
echo Prochaines etapes:
echo 1. Configurez vos cles API pour utiliser les vraies APIs
echo 2. Integrez l'interface BackVideoAI.fxml dans votre application
echo 3. Testez la generation avec de vrais chapitres
echo.
pause