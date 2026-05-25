@echo off
cd /d "%~dp0"
set "JAVA_HOME=C:\Program Files\Java\jdk-23"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo JAVA_HOME: %JAVA_HOME%
echo Starting Spring Boot backend...

for /f "delims=" %%i in ('where bash.exe 2^>nul') do set "BASH=%%i"
if defined BASH (
    "%BASH%" -c "export JAVA_HOME='/c/Program Files/Java/jdk-23' && cd '%~dp0' && ./mvnw spring-boot:run"
) else (
    echo bash.exe not found!
    pause
)
