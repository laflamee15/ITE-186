@echo off
setlocal
setlocal EnableDelayedExpansion

if not exist build mkdir build
if not exist lib mkdir lib

echo Compiling LendWise...
break > build\sources.txt
for /r src %%F in (*.java) do (
    set "p=%%F"
    set "p=!p:\=/!"
    echo "!p!" >> build\sources.txt
)
javac -cp "lib/*" -d build @build\sources.txt

if errorlevel 1 (
    echo.
    echo Build failed.
    exit /b 1
)

echo.
echo Build complete.
exit /b 0
