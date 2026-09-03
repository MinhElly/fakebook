$ErrorActionPreference = 'Stop'

$infrastructureDirectory = $PSScriptRoot

docker compose --project-directory $infrastructureDirectory -f "$infrastructureDirectory\docker-compose.yml" down

if ($LASTEXITCODE -ne 0) {
    throw 'Khong the dung ha tang Fakebook.'
}

