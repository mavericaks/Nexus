$env:GEMINI_API_KEY = (Get-Content "a:\Nexus\.env" | Select-String '^GEMINI_API_KEY=(.*)$').Matches.Groups[1].Value.Trim()
$dbUser = 'nexus'
$dbPass = 'nexus_local'
$dbName = 'nexus'

$query = "SELECT id, title, content FROM knowledge_articles WHERE embedding IS NULL;"
$result = docker exec nexus-postgres psql -U $dbUser -d $dbName -t -c $query

foreach ($line in $result) {
    if ([string]::IsNullOrWhiteSpace($line)) { continue }
    $parts = $line.Split('|')
    if ($parts.Length -lt 3) { continue }
    $id = $parts[0].Trim()
    $title = $parts[1].Trim()
    $content = $parts[2].Trim()
    $text = $title + " " + $content

    $body = @{
        model = "models/gemini-embedding-2"
        content = @{
            parts = @( @{ text = $text } )
        }
    } | ConvertTo-Json -Depth 5 -Compress
    
    $apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-2:embedContent?key=$($env:GEMINI_API_KEY)"
    
    $response = Invoke-RestMethod -Uri $apiUrl -Method Post -ContentType "application/json" -Body $body
    if ($response.embedding) {
        $vector = "[" + ($response.embedding.values -join ",") + "]"
        $updateQuery = "UPDATE knowledge_articles SET embedding = cast('' AS vector) WHERE id = '';"
        docker exec nexus-postgres psql -U $dbUser -d $dbName -c $updateQuery
        Write-Host "Updated $id"
    } else {
        Write-Host "Failed to get embedding for $id. Response: $($response | ConvertTo-Json -Depth 5)"
    }
}
