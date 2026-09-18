param(
    [string]$JdkHome,
    [string]$Version = '4.6.0',
    [switch]$Test,
    [switch]$Smoke,
    [switch]$CompileOnly
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
if ([Environment]::OSVersion.Platform -ne 'Win32NT') {
    throw 'Use python scripts/build.py on macOS or Linux.'
}
$buildArgs = @((Join-Path $PSScriptRoot 'scripts/build.py'), '--version', $Version)
if ($JdkHome) { $buildArgs += @('--jdk-home', $JdkHome) }
if ($Test) { $buildArgs += '--test' }
if ($Smoke) { $buildArgs += '--smoke' }
if ($CompileOnly) { $buildArgs += '--compile-only' }
& python @buildArgs
if ($LASTEXITCODE -ne 0) { throw "Native build failed with exit code $LASTEXITCODE" }
