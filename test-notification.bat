@echo off
echo 🚨 LANCEMENT DU TEST DE NOTIFICATION URGENTE
echo ============================================

echo 📋 Compilation...
call mvnw.cmd compile -q

if %ERRORLEVEL% NEQ 0 (
    echo ❌ Erreur de compilation
    pause
    exit /b 1
)

echo ✅ Compilation réussie

echo 🚨 Lancement du test...
java -cp "target/classes;target/dependency/*" com.educompus.debug.SimpleNotificationTest

pause