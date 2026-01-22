$ErrorActionPreference = "Stop"

function Check-RunAsAdministrator {
    $currentPrincipal = New-Object Security.Principal.WindowsPrincipal([Security.Principal.WindowsIdentity]::GetCurrent())
    if (-not $currentPrincipal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        Write-Warning "Script is not running as Administrator. Attempting to elevate..."
        Start-Process powershell.exe "-NoProfile -ExecutionPolicy Bypass -File `"$PSCommandPath`"" -Verb RunAs
        exit
    }
}

Check-RunAsAdministrator

$projectRoot = "$PSScriptRoot"
$buildDir = "$projectRoot\out"
$exePath = "$buildDir\Debug\WindowsMetric.exe"
$installDir = "C:\Program Files\WindowsMetric"
$certName = "WindowsMetricCert"

Write-Host ">>> 1. Configuring Build..." -ForegroundColor Cyan
Set-Location $projectRoot
# Ensure clean build for manifest embedding
if (Test-Path $buildDir) { Remove-Item -Recurse -Force $buildDir }
cmake -S . -B out
cmake --build out --config Debug

if (-not (Test-Path $exePath)) {
    Write-Error "Build failed. Could not find $exePath"
}

Write-Host ">>> 2. Setting up Code Signing Certificate..." -ForegroundColor Cyan
$cert = Get-ChildItem Cert:\CurrentUser\My | Where-Object { $_.Subject -eq "CN=$certName" }

if (-not $cert) {
    Write-Host "Creating new self-signed certificate..."
    $cert = New-SelfSignedCertificate -Type Custom `
        -Subject "CN=$certName" `
        -KeyUsage DigitalSignature `
        -FriendlyName "WindowsMetric Development Cert" `
        -CertStoreLocation "Cert:\CurrentUser\My" `
        -TextExtension @("2.5.29.37={text}1.3.6.1.5.5.7.3.3", "2.5.29.19={text}")
}

Write-Host "Ensuring usage of certificate in Trusted Root and Trusted People..."
# Export to temp file to import into other stores
$tempCertPath = "$env:TEMP\$certName.cer"
Export-Certificate -Cert $cert -FilePath $tempCertPath -Type CERT | Out-Null

# Import to LocalMachine\Root and TrustedPeople
Import-Certificate -FilePath $tempCertPath -CertStoreLocation Cert:\LocalMachine\Root | Out-Null
Import-Certificate -FilePath $tempCertPath -CertStoreLocation Cert:\LocalMachine\TrustedPeople | Out-Null
Remove-Item $tempCertPath

Write-Host ">>> 3. Signing the Executable..." -ForegroundColor Cyan
Set-AuthenticodeSignature -Certificate $cert -FilePath $exePath
Write-Host "Signature applied." -ForegroundColor Green

Write-Host ">>> 4. Installing to Trusted Directory ($installDir)..." -ForegroundColor Cyan
if (-not (Test-Path $installDir)) {
    New-Item -ItemType Directory -Path $installDir | Out-Null
}

Copy-Item -Path $exePath -Destination "$installDir\WindowsMetric.exe" -Force
# Copy any dlls if present in the build output which might be needed (ignoring pdbs for now)
Get-ChildItem "$buildDir\Debug\*.dll" | Copy-Item -Destination $installDir -Force -ErrorAction SilentlyContinue

Write-Host ">>> DONE! Application installed successfully." -ForegroundColor Green
Write-Host "You can now run the app from: $installDir\WindowsMetric.exe" -ForegroundColor Yellow
Write-Host "NOTE: You MUST run it from this location for uiAccess to work." -ForegroundColor Yellow
