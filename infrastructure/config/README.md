# Central Configuration Directory

Thư mục này chứa các tệp cấu hình tập trung dùng chung cho toàn bộ các microservices của Fakebook.

- `central-server-config/application.yml`: Được container `consul-config-loader` tự động đọc và nạp vào HashiCorp Consul Key/Value (KV) store khi hạ tầng khởi động.
