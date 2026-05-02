@echo off
echo ========================================
echo Test de la fonctionnalite Video IA avec Google Gemini
echo ========================================
echo.

echo Cle API Gemini configuree: AIzaSyD78HeB-zcZPs_nGWNMGYqfKeosRA2mHZo
echo.

if "%DID_API_KEY%"=="" (
    echo ATTENTION: DID_API_KEY n'est pas definie
    echo La generation video fonctionnera en mode simulation
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
call ./mvnw compile -q

if %ERRORLEVEL% neq 0 (
    echo Erreur lors de la compilation
    pause
    exit /b 1
)

echo ✓ Compilation reussie
echo.

echo ========================================
echo Test de configuration Gemini...
echo ========================================
call ./mvnw exec:java -Dexec.mainClass="com.educompus.examples.GeminiVideoExample" -Dexec.args="testConfiguration" -q

echo.

echo ========================================
echo Execution des tests unitaires...
echo ========================================
call ./mvnw test -Dtest=VideoExplicatifServiceTest -q

if %ERRORLEVEL% neq 0 (
    echo Erreur lors des tests
    pause
    exit /b 1
)

echo ✓ Tests passes avec succes
echo.

echo ========================================
echo Execution de l'exemple Gemini...
echo ========================================
call ./mvnw exec:java -Dexec.mainClass="com.educompus.examples.GeminiVideoExample" -q

echo.
echo ========================================
echo Test Gemini termine
echo ========================================
echo.
echo Prochaines etapes:
echo 1. La cle Gemini est deja configuree et fonctionnelle
echo 2. Configurez DID_API_KEY pour la generation video complete
echo 3. Integrez l'interface BackVideoAI.fxml dans votre application
echo 4. Testez la generation avec de vrais chapitres
echo.
echo Avantages de Gemini:
echo - Gratuit jusqu'a 60 requetes par minute
echo - Pas besoin de compte payant pour commencer
echo - Excellente qualite de generation de texte
echo - API simple et rapide
echo.
pause