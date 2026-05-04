@echo off
echo ========================================
echo RECOMPILATION COMPLETE DE L'APPLICATION
echo ========================================
echo.

echo [1/4] Suppression du dossier target...
if exist target (
    rmdir /s /q target
    echo OK - Dossier target supprime
) else (
    echo OK - Dossier target n'existe pas
)
echo.

echo [2/4] Nettoyage Maven...
call mvnw.cmd clean
echo.

echo [3/4] Compilation...
call mvnw.cmd compile
echo.

echo [4/4] Lancement de l'application...
echo.
echo ========================================
echo APPLICATION EN COURS DE DEMARRAGE...
echo ========================================
echo.
call mvnw.cmd javafx:run

pause
