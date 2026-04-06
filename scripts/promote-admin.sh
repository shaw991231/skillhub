#!/usr/bin/env bash
set -euo pipefail

usage() {
    echo "Usage: $0 <user-id>"
    echo ""
    echo "Promotes an existing user to SUPER_ADMIN and adds them to the global namespace."
    echo "Example: $0 usr_cb7b80bc-6ad8-4327-a381-b2275cf6187e"
    exit 1
}

if [ $# -ne 1 ]; then
    usage
fi

TARGET_USER_ID="$1"

if [ -z "${TARGET_USER_ID}" ]; then
    echo "Error: user-id must not be empty"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SERVER_DIR="$(cd "$SCRIPT_DIR/../server" && pwd)"

# Resolve Java 21 (same logic as run-dev-app.sh)
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
    echo "Java 21 is required. Install JDK 21 or set JAVA_HOME." >&2
    exit 1
fi

export JAVA_HOME="$JAVA21_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

echo "Building skillhub-app..."
cd "$SERVER_DIR"
./mvnw -pl skillhub-app -am clean package -DskipTests >/dev/null

APP_JAR="$(find skillhub-app/target -maxdepth 1 -type f -name 'skillhub-app-*.jar' ! -name '*.original' | head -n 1)"
if [[ -z "$APP_JAR" ]]; then
    echo "Could not locate packaged skillhub-app jar under skillhub-app/target" >&2
    exit 1
fi

echo "Promoting user ${TARGET_USER_ID} to SUPER_ADMIN..."
exec "$JAVA_HOME/bin/java" -jar "$APP_JAR" \
    --spring.profiles.active=local \
    --skillhub.admin.promote.target-user-id="${TARGET_USER_ID}"
