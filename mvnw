#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper startup script, version 3.2.0
# ----------------------------------------------------------------------------

MAVEN_PROJECTBASEDIR="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.properties"

if [ ! -f "$WRAPPER_JAR" ]; then
  WRAPPER_URL="$(grep -E '^wrapperUrl=' "$WRAPPER_PROPERTIES" | sed 's/wrapperUrl=//')"
  if [ -z "$WRAPPER_URL" ]; then
    echo "[ERROR] wrapperUrl not set in $WRAPPER_PROPERTIES" >&2
    exit 1
  fi
  echo "Downloading Maven Wrapper from $WRAPPER_URL"
  mkdir -p "$MAVEN_PROJECTBASEDIR/.mvn/wrapper" || exit 1
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$WRAPPER_URL" -o "$WRAPPER_JAR" || exit 1
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$WRAPPER_URL" -O "$WRAPPER_JAR" || exit 1
  else
    echo "[ERROR] Neither curl nor wget found; cannot download Maven Wrapper JAR." >&2
    exit 1
  fi
fi

exec java ${MAVEN_OPTS:-} -classpath "$WRAPPER_JAR" "-Dmaven.multiModuleProjectDirectory=$MAVEN_PROJECTBASEDIR" org.apache.maven.wrapper.MavenWrapperMain "$@"
