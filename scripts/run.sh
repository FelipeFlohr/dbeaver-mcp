#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JRE_DIR="$SCRIPT_DIR/jre-25"
JAR_FILE="$SCRIPT_DIR/dbeaver-mcp.jar"
JRE_URL="https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.2%2B10/OpenJDK25U-jre_x64_linux_hotspot_25.0.2_10.tar.gz"
JRE_ARCHIVE="$SCRIPT_DIR/jre.tar.gz"
JRE_EXTRACTED_NAME="jdk-25.0.2+10-jre"

if [ ! -d "$JRE_DIR" ]; then
    echo "Downloading JRE 25..."
    curl -fSL -o "$JRE_ARCHIVE" "$JRE_URL"
    tar -xzf "$JRE_ARCHIVE" -C "$SCRIPT_DIR"
    mv "$SCRIPT_DIR/$JRE_EXTRACTED_NAME" "$JRE_DIR"
    rm -f "$JRE_ARCHIVE"
    echo "JRE 25 installed."
fi

export JAVA_HOME="$JRE_DIR"
exec "$JAVA_HOME/bin/java" -jar "$JAR_FILE" "$@"
