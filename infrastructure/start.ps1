$ErrorActionPreference = 'Stop'

$infrastructureDirectory = $PSScriptRoot

docker compose --project-directory $infrastructureDirectory -f "$infrastructureDirectory\docker-compose.yml" up -d --wait

if ($LASTEXITCODE -ne 0) {
    throw 'Khong the khoi dong ha tang Fakebook.'
}

docker compose --project-directory $infrastructureDirectory -f "$infrastructureDirectory\docker-compose.yml" ps

