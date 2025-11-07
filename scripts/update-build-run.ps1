[CmdletBinding()]
param(
    [string]$Branch = "master",
    [string]$GradleTask = "assembleDebug",
    [string]$Variant = "debug",
    [switch]$Clean,
    [string]$PackageName = "vn.haui.heartlink",
    [string]$LaunchActivity = "vn.haui.heartlink.activities.SplashActivity"
)

$ErrorActionPreference = "Stop"

function Invoke-Process {
    param(
        [string]$FilePath,
        [string[]]$Arguments,
        [string]$ErrorMessage
    )

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw $ErrorMessage
    }
}

function Ensure-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Command '$Name' was not found in PATH."
    }
}

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $repoRoot

Ensure-Command git
Ensure-Command adb

$gitStatus = git status --porcelain
if ($LASTEXITCODE -ne 0) {
    throw "Unable to read git status."
}
if ($gitStatus) {
    throw "Repository has uncommitted changes. Commit or stash them before running this script."
}

$currentBranch = (git rev-parse --abbrev-ref HEAD).Trim()
if ($LASTEXITCODE -ne 0) {
    throw "Unable to determine current branch."
}
if ($currentBranch -ne $Branch) {
    Invoke-Process git ("checkout", $Branch) "Failed to checkout branch '$Branch'."
}

Invoke-Process git ("pull", "--ff-only", "origin", $Branch) "Failed to pull latest changes from origin/$Branch."

$gradleArguments = @("--no-daemon")
if ($Clean) {
    $gradleArguments += "clean"
}
$gradleArguments += $GradleTask
Invoke-Process (Join-Path $repoRoot "gradlew.bat") $gradleArguments "Gradle task '$GradleTask' failed."

$apkFileName = "app-$Variant.apk"
$apkPath = Join-Path $repoRoot "app\build\outputs\apk\$Variant\$apkFileName"
if (-not (Test-Path $apkPath)) {
    throw "APK not found at $apkPath."
}

$devices = (& adb devices)
if ($LASTEXITCODE -ne 0) {
    throw "Unable to list connected devices via adb."
}
$activeDevices = $devices | Where-Object { $_ -match "\tdevice$" }
if (-not $activeDevices) {
    throw "No connected Android device or emulator detected."
}

& adb shell pm clear $PackageName | Out-Null

Invoke-Process adb ("install", "-r", $apkPath) "Failed to install APK on device."

Invoke-Process adb ("shell", "am", "start", "-n", "$PackageName/$LaunchActivity") "Failed to launch activity $LaunchActivity."

Write-Host "App updated, installed, and launched successfully." -ForegroundColor Green
