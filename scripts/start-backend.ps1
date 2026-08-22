# ==============================================================================
# Nexus Backend Dev Server Bootstrap
# ==============================================================================

$rootPath = Resolve-Path "$PSScriptRoot\.."
$envFile = Join-Path $rootPath ".env"

if (Test-Path $envFile) {
    Write-Host "Loading environment configuration from .env..." -ForegroundColor Cyan
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split('=', 2)
            $varName = $parts[0].Trim()
            $varVal = $parts[1].Trim()
            [System.Environment]::SetEnvironmentVariable($varName, $varVal)
        }
    }
}

Set-Location "$rootPath\nexus-app"
Write-Host "Starting Nexus Application (Spring Boot 3.4)..." -ForegroundColor Green
& "..\mvnw.cmd" spring-boot:run "-Dspring-boot.run.profiles=dev"
