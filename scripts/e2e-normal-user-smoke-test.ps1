[CmdletBinding()]
param(
    [string]$BaseUrl = $(if ($env:BASE_URL) { $env:BASE_URL } else { 'http://localhost:8080' }),
    [string]$ZipkinUrl = $(if ($env:ZIPKIN_URL) { $env:ZIPKIN_URL } else { 'http://localhost:9411' })
)

$ErrorActionPreference = 'Stop'
$token = $env:FAKEBOOK_USER_ACCESS_TOKEN
if ([string]::IsNullOrWhiteSpace($token)) {
    throw 'FAKEBOOK_USER_ACCESS_TOKEN is required.'
}

$traceId = ([guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')).Substring(0, 32)
$spanId = [guid]::NewGuid().ToString('N').Substring(0, 16)
$headers = @{
    Authorization = "Bearer $token"
    traceparent = "00-$traceId-$spanId-01"
}
$createdPostId = $null

function Invoke-FakebookRequest {
    param(
        [Parameter(Mandatory)] [ValidateSet('GET', 'POST', 'DELETE')] [string]$Method,
        [Parameter(Mandatory)] [string]$Path,
        [object]$Body
    )

    $request = @{
        Method = $Method
        Uri = "$($BaseUrl.TrimEnd('/'))$Path"
        Headers = $headers
        TimeoutSec = 15
    }
    if ($null -ne $Body) {
        $request.ContentType = 'application/json'
        $request.Body = $Body | ConvertTo-Json -Depth 8 -Compress
    }
    Invoke-RestMethod @request
}

try {
    Write-Host "Trace ID: $traceId"
    $account = Invoke-FakebookRequest -Method GET -Path '/api/account'
    if (-not $account.login) {
        throw 'Gateway account response does not contain a login.'
    }

    $profile = Invoke-FakebookRequest -Method GET -Path '/services/userservice/api/user-profiles/me'
    if (-not $profile.id) {
        throw 'User profile response does not contain an id.'
    }

    $post = Invoke-FakebookRequest -Method POST -Path '/services/postservice/api/posts/create' -Body @{
        content = "P1 authenticated smoke $([DateTimeOffset]::UtcNow.ToString('O'))"
        visibility = 'PUBLIC'
        mediaIds = @()
        taggedUserIds = @()
    }
    $createdPostId = $post.id
    if (-not $createdPostId) {
        throw 'Post creation response does not contain an id.'
    }

    $null = Invoke-FakebookRequest -Method GET -Path "/services/postservice/api/posts/$createdPostId"
    $summaries = Invoke-FakebookRequest -Method POST -Path '/services/commentservice/api/comments/summaries' -Body @{
        postIds = @($createdPostId)
    }
    if (@($summaries).Count -ne 1) {
        throw 'Comment summary did not return exactly one post summary.'
    }

    $feedContainsPost = $false
    for ($attempt = 1; $attempt -le 10 -and -not $feedContainsPost; $attempt++) {
        $feed = @(Invoke-FakebookRequest -Method GET -Path '/services/feedservice/api/feed/me?size=50')
        $feedContainsPost = $null -ne ($feed | Where-Object { $_.postId -eq $createdPostId } | Select-Object -First 1)
        if (-not $feedContainsPost) {
            Start-Sleep -Seconds 2
        }
    }
    if (-not $feedContainsPost) {
        throw "Post $createdPostId did not appear in Feed within 20 seconds."
    }

    Start-Sleep -Seconds 5
    $spans = @(Invoke-RestMethod -Uri "$($ZipkinUrl.TrimEnd('/'))/api/v2/trace/$traceId" -TimeoutSec 10)
    $services = @($spans | ForEach-Object { $_.localEndpoint.serviceName } | Where-Object { $_ } | Sort-Object -Unique)
    $expectedServices = @('gateway', 'user', 'post', 'comment', 'feed')
    $missingServices = @($expectedServices | Where-Object { $_ -notin $services })
    if ($missingServices.Count -gt 0) {
        throw "Trace $traceId is missing services: $($missingServices -join ', ')."
    }

    Write-Host "Authenticated smoke passed. Services in trace: $($services -join ', ')"
}
finally {
    if ($createdPostId) {
        try {
            $null = Invoke-FakebookRequest -Method DELETE -Path "/services/postservice/api/posts/$createdPostId"
            Write-Host "Deleted smoke post $createdPostId."
        }
        catch {
            Write-Warning "Could not delete smoke post ${createdPostId}: $($_.Exception.Message)"
        }
    }
}
