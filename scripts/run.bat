@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "JRE_DIR=%SCRIPT_DIR%jre-25"
set "JAR_FILE=%SCRIPT_DIR%dbeaver-mcp.jar"
set "JRE_URL=https://github.com/adoptium/temurin25-binaries/releases/download/jdk-25.0.2%%2B10/OpenJDK25U-jre_x64_windows_hotspot_25.0.2_10.zip"
set "JRE_ARCHIVE=%SCRIPT_DIR%jre.zip"
set "JRE_EXTRACTED_NAME=jdk-25.0.2+10-jre"

if not exist "%JRE_DIR%" (
    echo Downloading JRE 25...
    powershell -Command "Invoke-WebRequest -Uri '%JRE_URL%' -OutFile '%JRE_ARCHIVE%'"
    powershell -Command "Expand-Archive -Path '%JRE_ARCHIVE%' -DestinationPath '%SCRIPT_DIR%'"
    move "%SCRIPT_DIR%%JRE_EXTRACTED_NAME%" "%JRE_DIR%"
    del "%JRE_ARCHIVE%"
    echo JRE 25 installed.
)

set "JAVA_HOME=%JRE_DIR%"
"%JAVA_HOME%\bin\java" -jar "%JAR_FILE%" %*
