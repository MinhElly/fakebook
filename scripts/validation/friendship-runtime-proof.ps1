# Run after User and Feed clean verify. Uses only new disposable Testcontainers.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$proofDir = Join-Path ([IO.Path]::GetTempPath()) ('fakebook-friendship-proof-' + [Guid]::NewGuid())
New-Item -ItemType Directory -Path $proofDir | Out-Null
Push-Location $repo
try {
    foreach ($service in @('userService', 'feedService')) {
        foreach ($output in @('classes', 'test-classes')) {
            if (!(Test-Path "$service/target/$output")) {
                throw "Run $service/mvnw.cmd -ntp clean verify first."
            }
        }
    }
    $dependencyPath = Join-Path $proofDir 'dependencies.txt'
    Push-Location userService
    try {
        & ./mvnw.cmd -ntp dependency:build-classpath "-Dmdep.outputFile=$dependencyPath" '-Dmdep.includeScope=test'
        if ($LASTEXITCODE -ne 0) { throw 'Could not resolve test dependencies.' }
    } finally { Pop-Location }
    $outputs = @('userService/target/classes', 'userService/target/test-classes',
        'feedService/target/classes', 'feedService/target/test-classes') | ForEach-Object { Join-Path $repo $_ }
    $classpath = (Get-Content $dependencyPath -Raw).Trim() + ';' + ($outputs -join ';') + ';' + $proofDir
    $classpath = $classpath.Replace('\', '/')
    $compilerArgs = Join-Path $proofDir 'javac.args'
    $source = (Join-Path $PSScriptRoot 'FriendshipRuntimeProof.java').Replace('\', '/')
    @('-proc:none', '-cp', ('"{0}"' -f $classpath), '-d', ('"{0}"' -f $proofDir.Replace('\', '/')), ('"{0}"' -f $source)) |
        Set-Content $compilerArgs -Encoding utf8NoBOM
    & javac "@$compilerArgs"
    if ($LASTEXITCODE -ne 0) { throw 'Could not compile friendship proof.' }
    $runtimeArgs = Join-Path $proofDir 'java.args'
    @('-Dspring.devtools.restart.enabled=false', '-cp', ('"{0}"' -f $classpath), 'FriendshipRuntimeProof') |
        Set-Content $runtimeArgs -Encoding utf8NoBOM
    & java "@$runtimeArgs"
    if ($LASTEXITCODE -ne 0) { throw 'Friendship runtime proof failed.' }
} finally { Pop-Location }
