# Launches Glacier Client in a dev Minecraft 1.21.11 (uses JDK 21; the system default JDK 26 cannot run Gradle/Loom).
$ErrorActionPreference = 'Stop'
$env:JAVA_HOME = "$env:USERPROFILE\jdks\jdk-21.0.11+10"
Set-Location $PSScriptRoot
& .\gradlew.bat runClient
