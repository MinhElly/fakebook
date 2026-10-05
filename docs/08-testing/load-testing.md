# Load & Performance Testing (Kiểm thử Tải & Hiệu năng)

Công cụ kiểm thử tải đã được loại bỏ khỏi repository ngày 2026-10-05. Hiện chưa có runner thay thế hoặc lệnh kiểm thử tải có thể chạy trực tiếp từ checkout.

## Yêu cầu kiểm thử

- Đo các endpoint nghiệp vụ qua Gateway, đặc biệt feed cá nhân, với token normal-user.
- Cố định dataset, số người dùng đồng thời, thời gian chạy và môi trường/phần cứng; chạy warm-up riêng.
- Kiểm tra HTTP status và nội dung nghiệp vụ, không chỉ độ trễ hoặc health endpoint.
- Thu thập tỷ lệ lỗi, p50/p95/p99, throughput và mức sử dụng CPU/RAM/database pool.
- Lưu raw summary, cấu hình chạy và commit SHA; mask token và dữ liệu nhạy cảm.

## Tiêu chí dự kiến

Tỷ lệ lỗi dưới 1%, p95 dưới 500 ms và p99 dưới 1000 ms là ngưỡng mục tiêu cần xác nhận theo workload. Chưa có run artifact để kết luận đạt SLO. Cần chuẩn bị và review công cụ kiểm thử tải trước khi đưa gate này vào CI.
