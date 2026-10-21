param(
    [ValidateSet('kind-fakebook-local', 'docker-desktop')]
    [string]$Context = 'kind-fakebook-local'
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$logDirectory = Join-Path $repositoryRoot '.artifacts/observability-local'
New-Item -ItemType Directory -Force -Path $logDirectory | Out-Null
$kubectlPath = (Get-Command kubectl -ErrorAction Stop).Source

& $kubectlPath --context $Context get namespace argocd monitoring -o name
if ($LASTEXITCODE -ne 0) { throw 'Argo CD and monitoring namespaces must exist.' }

$targets = @(
    @{ Name = 'argocd'; Namespace = 'argocd'; Service = 'argocd-server'; LocalPort = 8443; RemotePort = 443; Url = 'https://localhost:8443' },
    @{ Name = 'grafana'; Namespace = 'monitoring'; Service = 'monitoring-grafana'; LocalPort = 3000; RemotePort = 80; Url = 'http://localhost:3000' },
    @{ Name = 'prometheus'; Namespace = 'monitoring'; Service = 'monitoring-prometheus'; LocalPort = 9090; RemotePort = 9090; Url = 'http://localhost:9090' }
)

foreach ($target in $targets) {
    $listener = Get-NetTCPConnection -State Listen -LocalPort $target.LocalPort -ErrorAction SilentlyContinue
    if ($listener) {
        Write-Output "$($target.Name): port $($target.LocalPort) already has a listener; no new process started."
        continue
    }
    $endpointSlices = & $kubectlPath --context $Context -n $target.Namespace get endpointslices `
        -l "kubernetes.io/service-name=$($target.Service)" -o json | ConvertFrom-Json
    if ($LASTEXITCODE -ne 0) { throw "Could not inspect endpoints for $($target.Name)." }
    $readyEndpoints = @($endpointSlices.items.endpoints | Where-Object { $_.conditions.ready -eq $true })
    if ($readyEndpoints.Count -eq 0) {
        Write-Output "$($target.Name): no ready endpoints yet; run this script again after the workload is Ready."
        continue
    }
    $portMapping = "$($target.LocalPort):$($target.RemotePort)"
    $process = Start-Process -FilePath $kubectlPath -ArgumentList @(
        '--context', $Context, '-n', $target.Namespace,
        'port-forward', "svc/$($target.Service)", $portMapping, '--address=127.0.0.1'
    ) -WindowStyle Hidden -PassThru `
      -RedirectStandardOutput (Join-Path $logDirectory "$($target.Name).out.log") `
      -RedirectStandardError (Join-Path $logDirectory "$($target.Name).err.log")
    Write-Output "$($target.Name): $($target.Url) (port-forward PID $($process.Id))."
}

Write-Output "Port-forwards end on process exit or cluster interruption. Run this script again to open missing listeners. Logs: $logDirectory"
