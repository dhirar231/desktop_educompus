@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script, version 3.2.0
@REM ----------------------------------------------------------------------------
@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
if "%MAVEN_PROJECTBASEDIR%"=="" set MAVEN_PROJECTBASEDIR=.
set MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%

set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_PROPERTIES="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties"

if exist %WRAPPER_JAR% goto run

for /f "usebackq tokens=1,2 delims==" %%A in (%WRAPPER_PROPERTIES%) do (
  if /I "%%A"=="wrapperUrl" set WRAPPER_URL=%%B
)

if "%WRAPPER_URL%"=="" (
  echo [ERROR] wrapperUrl not set in %WRAPPER_PROPERTIES%
  exit /b 1
)

echo Downloading Maven Wrapper from %WRAPPER_URL%
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop';" ^
  "New-Item -ItemType Directory -Force -Path '%MAVEN_PROJECTBASEDIR%\\.mvn\\wrapper' | Out-Null;" ^
  "Invoke-WebRequest -UseBasicParsing -Uri '%WRAPPER_URL%' -OutFile '%MAVEN_PROJECTBASEDIR%\\.mvn\\wrapper\\maven-wrapper.jar'"
if errorlevel 1 (
  echo [ERROR] Failed to download Maven Wrapper JAR.
  exit /b 1
)

:run
set MAVEN_OPTS=%MAVEN_OPTS%
java %MAVEN_OPTS% -classpath %WRAPPER_JAR% "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" org.apache.maven.wrapper.MavenWrapperMain %*
exit /b %ERRORLEVEL%
