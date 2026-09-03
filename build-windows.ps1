param(
    [string]$JdkHome,
    [string]$Version = '4.5.0'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$projectRoot = $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($JdkHome)) {
    $packageTool = Get-Command jpackage.exe -ErrorAction Stop
    $JdkHome = Split-Path (Split-Path $packageTool.Source -Parent) -Parent
}

function Invoke-JdkTool {
    param([string]$Name, [string[]]$Arguments)
    & (Join-Path $JdkHome ('bin\' + $Name + '.exe')) @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Name failed with exit code $LASTEXITCODE" }
}

$dependencyNames = @(
    'flatlaf-3.4.jar',
    'sqlite-jdbc-3.36.0.3.jar',
    'slf4j-api-1.7.36.jar',
    'xchart-3.8.8.jar',
    'VectorGraphics2D-0.13.jar',
    'graphics2d-3.0.1.jar',
    'pdfbox-3.0.1.jar',
    'pdfbox-io-3.0.1.jar',
    'fontbox-3.0.1.jar',
    'commons-logging-1.2.jar',
    'animated-gif-lib-1.4.jar'
)
$dependencies = @($dependencyNames | ForEach-Object {
    (Get-Item -LiteralPath (Join-Path $projectRoot ('lib\' + $_))).FullName
})
$gsonCandidates = @(
    (Join-Path $projectRoot 'lib\gson-2.10.1.jar'),
    (Join-Path $env:USERPROFILE '.m2\repository\com\google\code\gson\gson\2.10.1\gson-2.10.1.jar')
)
$gson = $gsonCandidates | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
if (-not $gson) { throw 'Missing gson-2.10.1.jar. Place it in lib or restore the configured Maven dependency.' }
$dependencies += $gson

$buildId = Get-Date -Format 'yyyyMMdd-HHmmssfff'
$workRoot = Join-Path $projectRoot ('build\windows-' + $buildId)
$classes = Join-Path $workRoot 'classes'
$inputDir = Join-Path $workRoot 'input'
$runtimeDir = Join-Path $workRoot 'runtime'
$releaseName = 'ScoreManager-' + $Version + '-windows'
$releaseRoot = Join-Path $projectRoot ('dist\' + $releaseName)
$zipPath = $releaseRoot + '.zip'
if ((Test-Path -LiteralPath $releaseRoot) -or (Test-Path -LiteralPath $zipPath)) {
    $releaseName += '-' + $buildId
    $releaseRoot = Join-Path $projectRoot ('dist\' + $releaseName)
    $zipPath = $releaseRoot + '.zip'
}
New-Item -ItemType Directory -Path $classes, $inputDir, $releaseRoot -Force | Out-Null

$sourceNames = @(
    'Main.java', 'LoginFrame.java', 'ScoreManagerGUI.java', 'DataManager.java',
    'Student.java', 'LogEntry.java', 'UITools.java', 'AppSplashScreen.java',
    'DashboardPanel.java', 'AccountManagerDialog.java', 'HelpGuideDialog.java',
    'StudentProfileDialog.java'
)
$sources = @($sourceNames | ForEach-Object { Join-Path $projectRoot ('src\' + $_) })

Write-Output '[1/5] Compiling production sources...'
$compilerArgs = @('-encoding', 'UTF-8', '--release', '17', '-classpath', ($dependencies -join ';'), '-d', $classes) + $sources
Invoke-JdkTool -Name 'javac' -Arguments $compilerArgs

Write-Output '[2/5] Preparing application JAR and runtime dependencies...'
foreach ($dependency in $dependencies) { Copy-Item -LiteralPath $dependency -Destination $inputDir }
$applicationJar = Join-Path $inputDir 'ScoreManager.jar'
Invoke-JdkTool -Name 'jar' -Arguments @('--create', '--file', $applicationJar, '--main-class', 'Main', '-C', $classes, '.')

Write-Output '[3/5] Building a bundled Java runtime...'
$modules = 'java.desktop,java.sql,java.logging,java.naming,java.management,jdk.unsupported,jdk.charsets,jdk.localedata,jdk.crypto.ec'
Invoke-JdkTool -Name 'jlink' -Arguments @(
    '--add-modules', $modules, '--strip-debug', '--no-header-files', '--no-man-pages',
    '--compress=2', '--output', $runtimeDir
)

Write-Output '[4/5] Creating the Windows EXE application image...'
Invoke-JdkTool -Name 'jpackage' -Arguments @(
    '--type', 'app-image', '--name', 'ScoreManager', '--app-version', $Version,
    '--vendor', 'ScoreManager', '--description', 'Student score management',
    '--input', $inputDir, '--main-jar', 'ScoreManager.jar', '--main-class', 'Main',
    '--runtime-image', $runtimeDir, '--dest', $releaseRoot,
    '--java-options', '-Dfile.encoding=UTF-8'
)

$readme = @'
ScoreManager for Windows x64 - portable edition

1. Extract the entire ZIP before starting the application.
2. Open the ScoreManager folder and double-click ScoreManager.exe.
3. Keep the EXE together with its app and runtime folders.
   No separate Java installation is required.

The package contains the application only, not student databases or backups.
By default, data is stored under:
%USERPROFILE%\AppData\LocalLow\ScoreManager
The last selected database path is stored in settings.properties there.

To distribute the application, send the complete ZIP, not the EXE alone.
'@
Set-Content -LiteralPath (Join-Path $releaseRoot 'README.txt') -Value $readme -Encoding ASCII

Write-Output '[5/5] Creating the portable ZIP...'
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory(
    $releaseRoot, $zipPath, [System.IO.Compression.CompressionLevel]::Optimal, $false
)

[PSCustomObject]@{
    Executable = (Join-Path $releaseRoot 'ScoreManager\ScoreManager.exe')
    Archive = $zipPath
    ArchiveSizeMB = [math]::Round((Get-Item -LiteralPath $zipPath).Length / 1MB, 1)
    BuildDirectory = $workRoot
} | ConvertTo-Json
