# Script theo doi su kien Circuit Breaker realtime tren Gateway
$ErrorActionPreference = 'Continue'

function Get-AdminToken {
    try {
        return (curl.exe -s -X POST "http://localhost:9080/realms/jhipster/protocol/openid-connect/token" -d "client_id=web_app" -d "username=admin" -d "password=admin" -d "grant_type=password" | ConvertFrom-Json).access_token
    } catch {
        return $null
    }
}

$token = Get-AdminToken
if (-not $token) {
    Write-Host "Khong the lay token tu Keycloak. Hay chac chan container fakebook-keycloak dang chay." -ForegroundColor Red
    exit 1
}

Clear-Host
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "   THEO DOI TRANG THAI CIRCUIT BREAKER (userservice) REALTIME   " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "Dang ket noi va theo doi su kien... Nhan Ctrl+C de dung.`n" -ForegroundColor Gray

# Lay trang thai hien tai
$lastState = ""
try {
    $cbJson = kubectl exec -n fakebook-local deploy/gateway -- curl -s -H "Authorization: Bearer $token" http://localhost:8080/management/circuitbreakers | ConvertFrom-Json
    $lastState = $cbJson.circuitBreakers.userservice.state
} catch {}

$initColor = if ($lastState -eq "OPEN") { "Red" } elseif ($lastState -eq "HALF_OPEN") { "Yellow" } else { "Green" }
Write-Host "[$(Get-Date -Format 'HH:mm:ss')] TRANG THAI KHOI DAU: $lastState`n" -ForegroundColor $initColor

# Lay moc thoi gian su kien cuoi cung (Resilience4j chi luu toi da 100 su kien, nen phai so sanh thoi gian chu khong so sanh Count)
$lastSeenTime = ""
try {
    $evtJson = kubectl exec -n fakebook-local deploy/gateway -- curl -s -H "Authorization: Bearer $token" http://localhost:8080/management/circuitbreakerevents/userservice | ConvertFrom-Json
    if ($evtJson.circuitBreakerEvents -and $evtJson.circuitBreakerEvents.Count -gt 0) {
        $lastSeenTime = $evtJson.circuitBreakerEvents[-1].creationTime
    }
} catch {}

while ($true) {
    try {
        # 1. Kiem tra su kien moi qua Actuator Events
        $rawEvt = kubectl exec -n fakebook-local deploy/gateway -- curl -s -H "Authorization: Bearer $token" http://localhost:8080/management/circuitbreakerevents/userservice
        if ($rawEvt -match "Full authentication is required" -or $rawEvt -match "302 Found" -or -not $rawEvt) {
            $token = Get-AdminToken
            Start-Sleep -Milliseconds 500
            continue
        }

        $evtJson = $rawEvt | ConvertFrom-Json
        $data = $evtJson.circuitBreakerEvents

        if ($data -and $data.Count -gt 0) {
            $newEvents = @()
            if ($lastSeenTime -eq "") {
                $lastSeenTime = $data[-1].creationTime
            } else {
                $foundIdx = -1
                for ($i = $data.Count - 1; $i -ge 0; $i--) {
                    if ($data[$i].creationTime -eq $lastSeenTime) {
                        $foundIdx = $i
                        break
                    }
                }
                if ($foundIdx -ge 0 -and $foundIdx -lt ($data.Count - 1)) {
                    $newEvents = @($data[($foundIdx + 1)..($data.Count - 1)])
                } elseif ($foundIdx -eq -1) {
                    $newEvents = @($data)
                }
            }

            if ($newEvents.Count -gt 0) {
                foreach ($evt in $newEvents) {
                    $time = Get-Date -Format "HH:mm:ss"
                    if ($evt.type -eq "STATE_TRANSITION") {
                        $color = if ($evt.stateTransition -match "to OPEN") { "Red" } elseif ($evt.stateTransition -match "to HALF_OPEN") { "Yellow" } else { "Green" }
                        Write-Host "`n[$time] >>> [STATE_TRANSITION] $($evt.stateTransition) <<<`n" -ForegroundColor $color
                    } elseif ($evt.type -eq "ERROR") {
                        Write-Host "[$time] [ERROR] Request that bai -> Gateway khong goi duoc UserService" -ForegroundColor DarkRed
                    } elseif ($evt.type -eq "NOT_PERMITTED") {
                        Write-Host "[$time] [BLOCKED] Request bi chan (Gateway tra 503 vi Circuit dang OPEN)" -ForegroundColor Magenta
                    } elseif ($evt.type -eq "FAILURE_RATE_EXCEEDED") {
                        Write-Host "[$time] [ALERT] Ty le loi vuot nguong 50%!" -ForegroundColor Yellow
                    } elseif ($evt.type -eq "SUCCESS") {
                        Write-Host "[$time] [OK] Request thanh cong qua Gateway" -ForegroundColor DarkGreen
                    }
                }
                $lastSeenTime = $newEvents[-1].creationTime
            }
        }

        # 2. Dong thoi kiem tra doi trang thai truc tiep
        $rawCb = kubectl exec -n fakebook-local deploy/gateway -- curl -s -H "Authorization: Bearer $token" http://localhost:8080/management/circuitbreakers
        if ($rawCb) {
            $cbJson = $rawCb | ConvertFrom-Json
            $currentState = $cbJson.circuitBreakers.userservice.state
            if ($currentState -and $currentState -ne $lastState) {
                $time = Get-Date -Format "HH:mm:ss"
                $color = if ($currentState -eq "OPEN") { "Red" } elseif ($currentState -eq "HALF_OPEN") { "Yellow" } else { "Green" }
                Write-Host "[$time] ===> TRANG THAI CHUYEN DOI: $lastState ---> $currentState <===" -ForegroundColor $color
                $lastState = $currentState
            }
        }
    } catch {
        # Bo qua loi nhat thoi
    }
    Start-Sleep -Milliseconds 600
}
