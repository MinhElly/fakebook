# Central Configuration Directory

This directory is the single source of non-secret backend configuration for Fakebook.
The `consul-config-loader` container synchronizes the YAML files into Consul KV.
Consul provides both centralized configuration and service discovery.

The loader follows the Spring Cloud Consul naming convention:

- `application.yml` -> `config/application/data`
- `application-dev.yml` -> `config/application-dev/data`
- `gateway-dev.yml` -> `config/gateway-dev/data`
- `authService-dev.yml` -> `config/authService-dev/data`

Secrets are referenced through environment variables and are not stored here.
Backend services use `bootstrap.yml` to locate Consul before loading these files.

Thư mục này chứa các tệp cấu hình tập trung dùng chung cho toàn bộ các microservices của Fakebook.

- `central-server-config/application.yml`: Được container `consul-config-loader` tự động đọc và nạp vào HashiCorp Consul Key/Value (KV) store khi hạ tầng khởi động.
