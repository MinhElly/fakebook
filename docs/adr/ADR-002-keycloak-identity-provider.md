# ADR-002: Keycloak as Centralized Identity Provider (IAM)

## Trạng thái
**Accepted**

## Bối cảnh (Context)
Hệ thống mạng xã hội yêu cầu quản lý tài khoản người dùng, băm mật khẩu an toàn, cấp phát và thu hồi token, hỗ trợ SSO với mạng xã hội (Google) và phân quyền chặt chẽ.
Nếu tự viết service Authentication (custom auth):
- Tiềm ẩn rủi ro bảo mật (lưu trữ mật khẩu, refresh token rotation, PKCE).
- Tốn nhiều công sức bảo trì tính năng đăng nhập mạng xã hội (OAuth2 Social Logins) và chuẩn hóa Token.

## Quyết định (Decision)
1. Sử dụng **Keycloak 26 (Quay.io)** làm Identity Provider (IdP) duy nhất cho toàn bộ hệ thống.
2. Thiết lập Realm `jhipster` với:
   - Client `web_app`: Dành cho Frontend và Gateway theo luồng **Authorization Code Flow với PKCE** (Public Client).
   - Client `internal`: Dành cho giao tiếp ngầm Service-to-Service theo luồng **Client Credentials Grant** (Confidential Client).
3. Toàn bộ các backend microservices đóng vai trò **OAuth2 Resource Server**, chỉ cần xác thực chữ ký số của JWT Token bằng Public Key của Keycloak mà không cần lưu trữ mật khẩu người dùng.

## Các giải pháp thay thế (Alternatives Considered)
- **Tự phát triển Auth Service với Spring Security JWT**: Dễ tùy biến nhưng phải tự triển khai chuẩn OIDC, tự viết logic Google SSO, quên mật khẩu, email verification.
- **SaaS Auth (Auth0 / Firebase Auth / Okta)**: Tiện lợi nhưng phát sinh chi phí hàng tháng lớn và khó chạy môi trường offline cục bộ.

## Hệ quả (Consequences)
- **Tích cực**:
  - Bảo mật đạt tiêu chuẩn quốc tế (OIDC, OAuth2, PKCE).
  - Tích hợp sẵn Google Identity Provider, luồng reset password qua SMTP, và giao diện quản trị Admin Console mạnh mẽ.
  - Phân tách rõ ràng trách nhiệm: Backend chỉ tập trung vào nghiệp vụ mạng xã hội, không quản lý bảng mật khẩu.
- **Thách thức**:
  - Keycloak tiêu tốn tài nguyên (khuyến nghị tối thiểu 1GB RAM cho container Keycloak).
