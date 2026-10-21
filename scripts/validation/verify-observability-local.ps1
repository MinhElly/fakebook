param(
    [ValidateSet('kind-fakebook-local', 'docker-desktop')]
    [string]$Context = 'kind-fakebook-local'
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$evidenceDirectory = Join-Path $repositoryRoot '.artifacts/observability-local'
New-Item -ItemType Directory -Force -Path $evidenceDirectory | Out-Null

function Read-KubernetesJson([string[]]$Arguments) {
    $result = & kubectl --context $Context @Arguments --request-timeout=20s -o json
    if ($LASTEXITCODE -ne 0) { throw "kubectl failed: $($Arguments -join ' ')" }
    return $result | ConvertFrom-Json
}

$applications = Read-KubernetesJson @('-n', 'argocd', 'get', 'applications')
$deployments = Read-KubernetesJson @('-n', 'fakebook-local', 'get', 'deployments')
$claims = Read-KubernetesJson @('-n', 'monitoring', 'get', 'pvc')
$monitoringPods = Read-KubernetesJson @('-n', 'monitoring', 'get', 'pods')
$backendPods = Read-KubernetesJson @('-n', 'fakebook-local', 'get', 'pods')
$metricQuery = 'up{namespace="fakebook-local",app=~"gateway|userservice|postservice|mediaservice|commentservice|feedservice"}'
$queryResult = Invoke-RestMethod -Uri ('http://127.0.0.1:9090/api/v1/query?query=' + [uri]::EscapeDataString($metricQuery)) -TimeoutSec 20
$grafanaHealth = Invoke-RestMethod -Uri 'http://127.0.0.1:3000/api/health' -TimeoutSec 20
$argoCode = & curl.exe --insecure --silent --show-error --max-time 20 --output NUL --write-out '%{http_code}' 'https://127.0.0.1:8443/healthz'
if ($LASTEXITCODE -ne 0) { throw 'Argo CD HTTP health check failed.' }

$applicationSummary = @($applications.items | ForEach-Object {
    [ordered]@{ name = $_.metadata.name; sync = $_.status.sync.status; health = $_.status.health.status; revision = $_.status.sync.revision; phase = $_.status.operationState.phase }
})
$deploymentSummary = @($deployments.items | ForEach-Object {
    [ordered]@{ name = $_.metadata.name; desired = $_.spec.replicas; ready = $_.status.readyReplicas; updated = $_.status.updatedReplicas; images = @($_.spec.template.spec.containers.image) }
})
$targetSummary = @($queryResult.data.result | ForEach-Object {
    [ordered]@{ app = $_.metric.app; pod = $_.metric.pod; up = [int]$_.value[1] }
})
$claimSummary = @($claims.items | ForEach-Object {
    [ordered]@{ name = $_.metadata.name; phase = $_.status.phase; capacity = $_.status.capacity.storage }
})
$podSummary = @(@($monitoringPods.items) + @($backendPods.items) | Where-Object { -not $_.metadata.deletionTimestamp -and $_.status.phase -ne 'Succeeded' } | ForEach-Object {
    [ordered]@{ namespace = $_.metadata.namespace; name = $_.metadata.name; phase = $_.status.phase; containersReady = @($_.status.containerStatuses | Where-Object ready -ne $true).Count -eq 0; restarts = ($_.status.containerStatuses.restartCount | Measure-Object -Sum).Sum }
})
$expectedApplications = @('fakebook-local', 'monitoring-local', 'traefik-local')
$expectedDeployments = @('gateway', 'userservice', 'postservice', 'mediaservice', 'commentservice', 'feedservice', 'frontend')
$checks = [ordered]@{
    applications = @($applicationSummary | Where-Object { $_.name -in $expectedApplications -and $_.sync -eq 'Synced' -and $_.health -eq 'Healthy' -and $_.phase -eq 'Succeeded' }).Count -eq 3
    deployments = @($deploymentSummary | Where-Object { $_.name -in $expectedDeployments -and $_.ready -eq $_.desired -and $_.updated -eq $_.desired }).Count -eq 7
    storage = $claimSummary.Count -eq 3 -and @($claimSummary | Where-Object phase -ne 'Bound').Count -eq 0
    podReadiness = @($podSummary | Where-Object { $_.phase -ne 'Running' -or -not $_.containersReady }).Count -eq 0
    sixBackendTargets = $targetSummary.Count -eq 6 -and @($targetSummary | Where-Object up -ne 1).Count -eq 0 -and @($targetSummary.app | Sort-Object -Unique).Count -eq 6
    grafana = $grafanaHealth.database -eq 'ok'
    argoHttp = $argoCode -eq '200'
}
$evidence = [ordered]@{
    observedAt = [DateTimeOffset]::UtcNow.ToString('o')
    context = $Context
    sourceHead = (& git -C $repositoryRoot rev-parse HEAD)
    checks = $checks
    applications = $applicationSummary
    deployments = $deploymentSummary
    storage = $claimSummary
    pods = $podSummary
    backendMetrics = $targetSummary
    grafana = @{ database = $grafanaHealth.database; version = $grafanaHealth.version }
    argoHttpStatus = $argoCode
}
$evidencePath = Join-Path $evidenceDirectory 'verification.json'
$evidence | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $evidencePath -Encoding utf8
$checks | ConvertTo-Json
Write-Output "Evidence: $evidencePath"
if (@($checks.Values | Where-Object { -not $_ }).Count -gt 0) { throw 'One or more runtime checks failed; inspect the saved evidence.' }
