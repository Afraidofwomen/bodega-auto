#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
MVN_VERSION="3.9.9"
DIST="$ROOT/.mvn-dist"
BIN="$DIST/apache-maven-$MVN_VERSION/bin/mvn"

if [[ ! -x "$BIN" ]]; then
  mkdir -p "$DIST"
  ARCHIVE="$DIST/maven.tgz"
  echo "Descargando Maven $MVN_VERSION…"
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MVN_VERSION/apache-maven-$MVN_VERSION-bin.tar.gz" -o "$ARCHIVE"
  else
    wget -q -O "$ARCHIVE" "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MVN_VERSION/apache-maven-$MVN_VERSION-bin.tar.gz"
  fi
  tar -xzf "$ARCHIVE" -C "$DIST"
fi

exec "$BIN" -f "$ROOT/pom.xml" javafx:run
