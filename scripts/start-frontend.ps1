# ==============================================================================
# Nexus Frontend Dev Server Bootstrap
# ==============================================================================

$rootPath = Resolve-Path "$PSScriptRoot\.."
$env:NEXT_PUBLIC_API_URL = if ($env:NEXT_PUBLIC_API_URL) { $env:NEXT_PUBLIC_API_URL } else { "http://localhost:8080" }

Set-Location "$rootPath\nexus-frontend"
Write-Host "Starting Nexus Frontend (Next.js 15)..." -ForegroundColor Green
npm run dev
