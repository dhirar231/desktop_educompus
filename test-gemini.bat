@echo off
echo Test de Gemini...
./mvnw compile exec:java -Dexec.mainClass=com.educompus.examples.TestGeminiSimple
pause