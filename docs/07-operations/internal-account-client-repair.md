# Sửa lỗi xác thực account directory nội bộ — 21/10/2026

## Bằng chứng và nguyên nhân

- Bật DEBUG tạm riêng ExceptionTranslator của Feed và Media trong `kind-fakebook-local`, namespace `fakebook-local`.
- Feed ghi `NoFallbackAvailableException`, nguyên nhân là HTTP 503 từ `POST /api/internal/feed-posts/candidates` của Post, với `ACCOUNT_STATE_UNAVAILABLE`.
- Post và Media có đăng ký OAuth `internal` từ application.yml, dùng `FAKEBOOK_OIDC_INTERNAL_CLIENT_ID` và `FAKEBOOK_OIDC_INTERNAL_CLIENT_SECRET`. Hai biến này thiếu trong pod Post; Secret hiện có dùng tên `FAKEBOOK_OIDC_CLIENT_ID` và `FAKEBOOK_OIDC_CLIENT_SECRET`. Feed đã override registration internal bằng hai tên hiện có.
- Bản sửa bổ sung hai alias bằng secretKeyRef trong deployment Post/Media, tham chiếu credential internal sẵn có của từng service. Không thay giá trị Secret, không bỏ kiểm tra quyền, không sửa Cloudinary.

## Kiểm tra runtime

- Post lấy token internal và gọi User `/api/internal/accounts/excluded`: HTTP 200.
- Media lấy token internal và gọi cùng endpoint: HTTP 200.
- Kiểm tra tiếp Comment sau log `/api/comments/summaries` và danh sách bình luận trả 500/503: pod cũng thiếu hai biến internal. Bổ sung secretKeyRef tương tự vào deployment và manifest base Comment. Sau rollout, Comment lấy token internal và gọi account directory trả HTTP 200; pod ready 1/1. Chưa xác minh summaries/list bằng JWT người dùng.
- Gọi Post `/api/internal/feed-posts/candidates` bằng token internal, viewer UUID synthetic, source DISCOVERY, limit 1: HTTP 200. Chỉ kiểm tra status, không xuất nội dung bài viết/token/secret.
- Kiểm tra bảng tin và tải ảnh bằng JWT người dùng trên trình duyệt: chưa xác minh.
- Đã cập nhật hai manifest base Post/Media để lưu cách tham chiếu credential; chưa commit/push GitOps. Image ứng dụng không thay trong lần sửa runtime này.

## Source hỗ trợ chẩn đoán

- ExceptionTranslator của Gateway, Feed, Media ghi lỗi 5xx ở ERROR thay vì chỉ DEBUG, gồm method/path và exception; không ghi request header/body.
- PostMediaAccess giữ cause khi bọc lỗi kiểm tra quyền.
- Cả ba service compile thành công. Những thay đổi source này chưa được build image/deploy lên cluster.
