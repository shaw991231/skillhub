#!/usr/bin/env bash

set -euo pipefail

SERVER_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PROFILE="${SPRING_PROFILES_ACTIVE:-local}"

resolve_java21_home() {
  if [[ -n "${JAVA_HOME:-}" ]] && [[ -x "${JAVA_HOME}/bin/java" ]]; then
    local current_version
    current_version="$("${JAVA_HOME}/bin/java" -version 2>&1 | head -n 1)"
    if [[ "$current_version" == *'"21.'* ]]; then
      printf '%s\n' "$JAVA_HOME"
      return 0
    fi
  fi

  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    local detected_home
    detected_home="$(/usr/libexec/java_home -v 21 2>/dev/null || true)"
    if [[ -n "$detected_home" ]]; then
      printf '%s\n' "$detected_home"
      return 0
    fi
  fi

  return 1
}

JAVA21_HOME="$(resolve_java21_home || true)"
if [[ -z "$JAVA21_HOME" ]]; then
  echo "Java 21 is required to run the local backend. Install JDK 21 or set JAVA_HOME to a JDK 21 home." >&2
  exit 1
fi

export JAVA_HOME="$JAVA21_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

cd "$SERVER_DIR"

./mvnw -pl skillhub-app -am clean package -DskipTests >/dev/null

APP_JAR="$(find skillhub-app/target -maxdepth 1 -type f -name 'skillhub-app-*.jar' ! -name '*.original' | head -n 1)"
if [[ -z "$APP_JAR" ]]; then
  echo "Could not locate packaged skillhub-app jar under skillhub-app/target" >&2
  exit 1
fi

exec "${JAVA_BIN:-$JAVA_HOME/bin/java}" -jar "$APP_JAR" --spring.profiles.active="$PROFILE" "$@"
