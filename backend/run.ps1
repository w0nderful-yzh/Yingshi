$env:JAVA_HOME = "C:\Program Files\Java\jdk-23"

$addOpens = @(
    "--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
    "--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED",
    "--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
    "--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
    "--add-opens=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED",
    "--add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
    "--add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED",
    "--add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
    "--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
    "--add-opens=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED"
)
$env:MAVEN_OPTS = $addOpens -join " "

Write-Host "JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Green
Write-Host "Starting Spring Boot..." -ForegroundColor Green

bash mvnw spring-boot:run
