@echo off
chcp 65001 >nul
echo ================================================================================
echo 🔍 VÉRIFICATION DE L'INSTALLATION - Module Engagement
echo ================================================================================
echo.

set MYSQL_USER=root
set MYSQL_DB=educompus

set /p MYSQL_PASS="Entrez votre mot de passe MySQL : "
echo.

echo ================================================================================
echo Vérification des tables créées...
echo ================================================================================
echo.

mysql -u %MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% -e "SHOW TABLES LIKE '%%attendance%%'; SHOW TABLES LIKE '%%activity%%'; SHOW TABLES LIKE '%%download%%';"

echo.
echo ================================================================================
echo Vérification des données de test...
echo ================================================================================
echo.

mysql -u %MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% -e "SELECT COUNT(*) as nb_logs FROM user_activity_log; SELECT COUNT(*) as nb_downloads FROM pdf_download_log;"

echo.
echo ================================================================================
echo Vérification des étudiants...
echo ================================================================================
echo.

mysql -u %MYSQL_USER% -p%MYSQL_PASS% %MYSQL_DB% -e "SELECT id, nom, prenom, email FROM utilisateur WHERE role = 'STUDENT' LIMIT 5;"

echo.
echo ================================================================================
echo ✅ VÉRIFICATION TERMINÉE
echo ================================================================================
echo.
pause
