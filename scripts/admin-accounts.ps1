param(
    [Parameter(Mandatory=$true)][ValidateSet('backfill','grant-admin','revoke-admin')][string]$Action,
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$Token = $env:FAKEBOOK_ADMIN_TOKEN,
    [Guid]$Subject,
    [long]$Version = -1,
    [string]$Reason,
    [string]$IdempotencyKey = [Guid]::NewGuid().ToString()
)
$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($Token)) { throw 'Set FAKEBOOK_ADMIN_TOKEN to an authenticated Admin access token.' }
$uri = $BaseUrl.TrimEnd('/') + '/services/userservice/api/admin/account-commands'
$headers = @{ Authorization = "Bearer $Token"; 'Idempotency-Key' = $IdempotencyKey }
if ($Action -eq 'backfill') {
    Invoke-RestMethod -Method Post -Uri "$uri/backfill" -Headers $headers
} else {
    if ($Subject -eq [Guid]::Empty -or $Version -lt 0 -or [string]::IsNullOrWhiteSpace($Reason)) {
        throw 'Subject, current Version and Reason are required for role changes.'
    }
    $body = @{ version = $Version; reason = $Reason } | ConvertTo-Json -Compress
    Invoke-RestMethod -Method Post -Uri "$uri/$Subject/$Action" -Headers $headers -ContentType 'application/json' -Body $body
}
