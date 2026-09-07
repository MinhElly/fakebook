# Fakebook Centralized Configuration

## Architecture

Fakebook uses Consul for two separate concerns:

1. **Service discovery**: each backend registers its health endpoint and Gateway discovers it.
2. **Centralized configuration**: `consul-config-loader` loads YAML into Consul KV and Spring Cloud Consul Config reads it during bootstrap.

There is no separate Spring Cloud Config Server in this architecture.

```text
central-server-config/*.yml
          -> consul-config-loader
          -> Consul KV :8500
          -> Gateway and backend services

Frontend :9000 -> Gateway :8080 -> Consul discovery -> backend service
```

## Configuration ownership

- `bootstrap.yml`: only bootstrap values needed to find Consul.
- `infrastructure/config/central-server-config`: non-secret shared and service-specific runtime values.
- `infrastructure/.env`: local secrets and environment overrides; never commit this file.
- `frontend/public/config/app-config.json`: public browser runtime configuration only.
- `docker-compose.yml`: infrastructure images, ports, volumes and health checks.

## Consul key convention

With YAML format and profile separator `-`, files are loaded as:

```text
application.yml       -> config/application/data
application-dev.yml   -> config/application-dev/data
gateway-dev.yml       -> config/gateway-dev/data
authService-dev.yml   -> config/authService-dev/data
```

The service-specific filename uses the exact `spring.application.name`, such as `authService`, while Consul discovery names remain lowercase, such as `authservice`.

## Local startup

```powershell
Copy-Item infrastructure/.env.example infrastructure/.env
. .\infrastructure\load-dev-env.ps1
.\infrastructure\start.ps1
```

Start the backend only after Consul and the config loader are healthy. Backend changes to central config are applied by restarting the affected service; Config Watch is intentionally disabled.

The frontend loads `/config/app-config.json` before rendering and calls the Gateway, never Consul directly.
