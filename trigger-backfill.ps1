$ErrorActionPreference = 'Stop'
while (1) {
    try {
        Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get -ErrorAction Stop | Out-Null
        break
    } catch {
        Start-Sleep -Seconds 2
    }
}
Write-Host "App is UP"
$body = @{
    email = "owner@acme.com"
    password = "password123"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method Post -ContentType "application/json" -Body $body
$token = $response.token
Write-Host "Token obtained"

$tenantId = "aaaa0000-0000-0000-0000-000000000001"
$backfillUrl = "http://localhost:8080/api/v1/tenants/$tenantId/tickets/backfill-kb"
$headers = @{
    Authorization = "Bearer $token"
}

try {
    $backfillResp = Invoke-RestMethod -Uri $backfillUrl -Method Post -Headers $headers
    Write-Host "Backfilled count: $backfillResp"
} catch {
    Write-Host "Error backfilling: $($_.Exception.Message)"
}
