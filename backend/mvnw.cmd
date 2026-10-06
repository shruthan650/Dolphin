@REM ----------------------------------------------------------------------------
@REM Minimal Maven wrapper for Windows: downloads Maven (once) into
@REM %USERPROFILE%\.m2\wrapper\dists and runs it. Requires JAVA_HOME or java on PATH.
@REM ----------------------------------------------------------------------------
@echo off
setlocal
set "WRAPPER_DIR=%~dp0.mvn\wrapper"
for /f "usebackq tokens=1,* delims==" %%A in ("%WRAPPER_DIR%\maven-wrapper.properties") do (
  if "%%A"=="distributionUrl" set "DIST_URL=%%B"
)
for %%F in ("%DIST_URL%") do set "DIST_ZIP=%%~nxF"
set "DIST_NAME=%DIST_ZIP:-bin.zip=%"
set "DIST_HOME=%USERPROFILE%\.m2\wrapper\dists\%DIST_NAME%"
set "MVN_CMD=%DIST_HOME%\%DIST_NAME%\bin\mvn.cmd"

if not exist "%MVN_CMD%" (
  echo Downloading %DIST_URL%
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12;" ^
    "New-Item -ItemType Directory -Force '%DIST_HOME%' | Out-Null;" ^
    "$zip = Join-Path '%DIST_HOME%' '%DIST_ZIP%';" ^
    "Invoke-WebRequest -UseBasicParsing -Uri '%DIST_URL%' -OutFile $zip;" ^
    "Expand-Archive -Force $zip '%DIST_HOME%'; Remove-Item $zip"
  if errorlevel 1 (
    echo Failed to download Maven distribution 1>&2
    exit /b 1
  )
)

"%MVN_CMD%" %*
exit /b %ERRORLEVEL%
