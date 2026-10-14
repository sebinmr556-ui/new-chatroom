#!/bin/sh
# Lightweight Gradle launcher for this project.
# Downloads Gradle 8.9 on first run, then executes it.
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION="8.9"
CACHE_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/vibe-bootstrap"
GRADLE_HOME="$CACHE_DIR/gradle-$GRADLE_VERSION"
GRADLE_BIN="$GRADLE_HOME/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$CACHE_DIR"
  ARCHIVE="$CACHE_DIR/gradle-$GRADLE_VERSION-bin.zip"
  URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  echo "Gradle $GRADLE_VERSION is not installed; downloading it now..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL "$URL" -o "$ARCHIVE"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ARCHIVE" "$URL"
  else
    echo "Please install Gradle $GRADLE_VERSION or install curl/wget and retry." >&2
    exit 1
  fi
  if command -v unzip >/dev/null 2>&1; then
    unzip -q -o "$ARCHIVE" -d "$CACHE_DIR"
  else
    echo "Please install unzip and retry." >&2
    exit 1
  fi
fi
exec "$GRADLE_BIN" -p "$APP_HOME" "$@"
