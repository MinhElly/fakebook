# ADR-006: Hybrid Staging Architecture (Vercel + Azure VM + AWS RDS)

## Trạng thái
**Accepted**

## Bối cảnh (Context)
Khi triển khai môi trường Staging tích hợp để kiểm thử toàn diện:
- Nếu triển khai một cụm Kubernetes hoàn chỉnh (EKS/AKS): Chi phí duy trì hạ tầng rất tốn kém (khoảng vài trăm USD/tháng) và đòi hỏi đội ngũ DevOps chuyên trách vận hành cluster.
- Nếu gom toàn bộ Frontend, Backend và Database vào chung một máy chủ VPS duy nhất: Dễ bị nghẽn RAM, rủi ro mất dữ liệu nếu VM gặp sự cố đĩa, và không tận dụng được CDN cho frontend.

## Quyết định (Decision)
Áp dụng mô hình **Kiến trúc Lai (Hybrid Staging Architecture)**:
1. **Frontend**: Triển khai trên **Vercel** (`fakebook-zeta.vercel.app`), tận dụng hạ tầng Edge Network miễn phí, build siêu nhanh và tích hợp tự động với Git.
2. **Backend Microservices & Hạ tầng phụ trợ**: Triển khai bằng **Docker Compose trên một Azure VM duy nhất** (`20-189-114-210.nip.io`).
   - Sử dụng **Nginx Reverse Proxy** quản lý TLS/SSL Let's Encrypt tại cổng 443.
   - Định tuyến `/realms/` về Keycloak, `/services/` và `/api/` về API Gateway, và redirect `/` về Vercel.
3. **Cơ sở dữ liệu**: Sử dụng **AWS RDS MariaDB** (gói db.t3/t4g tiết kiệm chi phí) để đảm bảo an toàn dữ liệu, hỗ trợ backup tự động và bắt buộc mã hóa đường truyền SSL.

## Các giải pháp thay thế (Alternatives Considered)
- **Tất cả trên 1 VPS (All-in-one Compose bao gồm cả MariaDB container)**: Tiết kiệm chi phí nhất nhưng rủi ro cao: mất dữ liệu nếu container bị xóa volume, khó giả lập môi trường cloud-like latency và cấu hình SSL cho DB.
- **Full Kubernetes on Cloud**: Quá phức tạp và đắt đỏ cho giai đoạn thử nghiệm staging hiện tại của dự án.

## Hệ quả (Consequences)
- **Tích cực**:
  - Tiết kiệm tối đa chi phí hạ tầng trong khi vẫn đạt được tính bảo mật, hiệu năng và tính thực tế cao.
  - Tách biệt rõ ràng ranh giới giữa Client Static Assets (Vercel) và Business Processing (Azure VM + AWS RDS).
- **Thách thức**:
  - Cần quản lý CORS giữa domain Vercel (`https://fakebook-zeta.vercel.app`) và domain Gateway/Keycloak (`https://20-189-114-210.nip.io`).
