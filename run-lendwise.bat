@echo off
setlocal

if not exist build (
    echo Build folder not found. Running build first...
    call build-lendwise.bat
    if errorlevel 1 exit /b 1
)

if not exist lib (
    mkdir lib
)

echo Starting LendWise...
java -cp "build;lib/*" lendwise.Main
