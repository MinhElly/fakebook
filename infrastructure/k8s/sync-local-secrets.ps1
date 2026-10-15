param(
    [string]$Context = 'docker-desktop',
    [string]$Namespace = 'fakebook-local',
    [switch]$FilesOnly
)

$ErrorActionPreference = 'Stop'
$envPath = Join-Path $PSScriptRoot '../.env'
$outputDirectory = Join-Path $PSScriptRoot '.local'
$values = @{}

foreach ($line in Get-Content -LiteralPath $envPath) {
    $entry = $line.Trim()
    if (-not $entry -or $entry.StartsWith('#')) { continue }
    if ($entry -notmatch '^([A-Za-z_][A-Za-z0-9_]*)\s*=(.*)$') {
        throw 'Unsupported .env entry; no secret values are printed.'
    }
    $key = $matches[1]
    $value = $matches[2].Trim()
    if ($value.Length -ge 2 -and (
        ($value.StartsWith('"') -and $value.EndsWith('"')) -or
        ($value.StartsWith("'") -and $value.EndsWith("'"))
    )) { $value = $value.Substring(1, $value.Length - 2) }
    $values[$key] = $value
}

function Get-RequiredValue([string]$Key, [switch]$AllowEmpty) {
    if (-not $values.ContainsKey($Key)) { throw "Missing .env key: $Key" }
    if (-not $AllowEmpty -and [string]::IsNullOrWhiteSpace($values[$Key])) {
        throw "Empty .env key: $Key"
    }
    if ($values[$Key] -match '[\r\n]' -or $values[$Key].Contains('${')) {
        throw "Unsupported multiline/interpolated .env key: $Key"
    }
    return [string]$values[$Key]
}

# The local .env uses shared database credentials. Do not import staging credentials.
$database = [ordered]@{
    FAKEBOOK_DB_USERNAME = Get-RequiredValue 'FAKEBOOK_DB_USERNAME'
    FAKEBOOK_DB_PASSWORD = Get-RequiredValue 'FAKEBOOK_DB_PASSWORD' -AllowEmpty
}
$backendClient = [ordered]@{
    FAKEBOOK_OIDC_CLIENT_ID = Get-RequiredValue 'FAKEBOOK_OIDC_INTERNAL_CLIENT_ID'
    FAKEBOOK_OIDC_CLIENT_SECRET = Get-RequiredValue 'FAKEBOOK_OIDC_INTERNAL_CLIENT_SECRET'
}
$gatewayClient = [ordered]@{
    FAKEBOOK_OIDC_GATEWAY_CLIENT_ID = Get-RequiredValue 'FAKEBOOK_OIDC_GATEWAY_CLIENT_ID'
    FAKEBOOK_OIDC_GATEWAY_CLIENT_SECRET = Get-RequiredValue 'FAKEBOOK_OIDC_GATEWAY_CLIENT_SECRET'
}
$mediaCredentials = [ordered]@{
    CLOUDINARY_CLOUD_NAME = Get-RequiredValue 'CLOUDINARY_CLOUD_NAME'
    CLOUDINARY_API_KEY = Get-RequiredValue 'CLOUDINARY_API_KEY'
    CLOUDINARY_API_SECRET = Get-RequiredValue 'CLOUDINARY_API_SECRET'
}

$feedCursorSecret = Get-RequiredValue 'FEED_CURSOR_SECRET'
if ($feedCursorSecret.Length -lt 32 -or $feedCursorSecret.StartsWith('REPLACE_ME')) { throw 'FEED_CURSOR_SECRET must contain at least 32 characters.' }

# Validate every required input before writing files or changing the cluster.
$payloads = [ordered]@{}
foreach ($service in @('gateway', 'userservice', 'postservice', 'mediaservice', 'commentservice', 'feedservice')) {
    $payload = [ordered]@{}
    foreach ($key in $database.Keys) { $payload[$key] = $database[$key] }
    $client = if ($service -eq 'gateway') { $gatewayClient } else { $backendClient }
    foreach ($key in $client.Keys) { $payload[$key] = $client[$key] }
    if ($service -eq 'mediaservice') {
        foreach ($key in $mediaCredentials.Keys) { $payload[$key] = $mediaCredentials[$key] }
    }
    if ($service -eq 'feedservice') { $payload['FEED_CURSOR_SECRET'] = $feedCursorSecret }
    $payloads[$service] = $payload
}

if (-not $FilesOnly) {
    & kubectl --context $Context get namespace $Namespace -o name
    if ($LASTEXITCODE -ne 0) { throw "Namespace $Namespace is unavailable in context $Context." }
}

New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
$encoding = [System.Text.UTF8Encoding]::new($false)
foreach ($service in $payloads.Keys) {
    $filePath = Join-Path $outputDirectory "$service.env"
    $lines = foreach ($key in $payloads[$service].Keys) { "$key=$($payloads[$service][$key])" }
    [System.IO.File]::WriteAllLines($filePath, [string[]]$lines, $encoding)
    if (-not $FilesOnly) {
        $manifest = & kubectl --context $Context -n $Namespace create secret generic "$service-secrets" `
            "--from-env-file=$filePath" --dry-run=client -o json
        if ($LASTEXITCODE -ne 0) { throw "Could not prepare Secret for $service." }
        $manifest | & kubectl --context $Context apply -f -
        if ($LASTEXITCODE -ne 0) { throw "Could not apply Secret for $service." }
    }
    Write-Output "Prepared $service-secrets ($($payloads[$service].Count) keys)."
}
