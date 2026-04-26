#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

java_major_version() {
  local version
  version="$(
    java -XshowSettings:properties -version 2>&1 \
      | sed -n 's/.*java.specification.version = //p' \
      | head -n 1
  )"
  if [[ "$version" == 1.* ]]; then
    echo "${version#1.}"
  else
    echo "${version%%.*}"
  fi
}

if ! command -v java >/dev/null 2>&1 || [[ "$(java_major_version)" -lt 17 ]]; then
  if [[ "$(uname -s)" != "Linux" ]]; then
    echo "Java 17+ is required. Install a local JDK 17 before running this script on $(uname -s)." >&2
    exit 1
  fi
  JDK_DIR="$ROOT/.vercel/jdk-17"
  if [[ ! -x "$JDK_DIR/bin/java" ]]; then
    echo "Installing JDK 17 for the Kotlin Wasm build..."
    rm -rf "$JDK_DIR"
    mkdir -p "$JDK_DIR"
    curl -fsSL \
      "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse?project=jdk" \
      -o /tmp/sova-jdk-17.tar.gz
    tar -xzf /tmp/sova-jdk-17.tar.gz -C "$JDK_DIR" --strip-components=1
  fi
  export JAVA_HOME="$JDK_DIR"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

echo "Using Java: $(java -version 2>&1 | head -n 1)"
./gradlew :composeApp:wasmJsBrowserDistribution --no-daemon
