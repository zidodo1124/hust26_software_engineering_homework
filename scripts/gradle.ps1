# Run the checked-in Gradle wrapper using a local JDK when available.
# Usage: powershell -ExecutionPolicy Bypass -File scripts/gradle.ps1 :app:assembleDebug
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleTasks
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$localJdk = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.local-tools') -Directory -Filter 'jdk-*' -ErrorAction SilentlyContinue |
    Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin/java.exe') } |
    Select-Object -First 1

if ($localJdk) {
    $env:JAVA_HOME = $localJdk.FullName
}
if (-not $env:JAVA_HOME -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/java.exe'))) {
    throw 'Set JAVA_HOME to a JDK 17 installation, or install JDK 17 under .local-tools/jdk-*/.'
}

$localSdk = Join-Path $projectRoot '.local-tools/android-sdk'
if (Test-Path -LiteralPath $localSdk) {
    $env:ANDROID_HOME = $localSdk
}
$env:PATH = (Join-Path $env:JAVA_HOME 'bin') + [IO.Path]::PathSeparator + $env:PATH

Push-Location $projectRoot
try {
    & (Join-Path $projectRoot 'gradlew.bat') @GradleTasks
    $gradleExitCode = $LASTEXITCODE
} finally {
    Pop-Location
}
exit $gradleExitCode
