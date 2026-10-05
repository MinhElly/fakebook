# Load & Performance Testing (Kiểm thử Tải & Hiệu năng)

Tài liệu này hướng dẫn cách thực thi và phân tích kết quả kiểm thử tải cho Fakebook bằng công cụ **k6** dựa trên kịch bản thực tế tại `performance/k6/http-baseline.js`.

---

## 1. Kịch bản Baseline k6 (`http-baseline.js`)

Kịch bản baseline cho phép đo lường độ trễ và khả năng chịu tải của các endpoint thông qua Gateway:

```javascript
import http from 'k6/http';
import { check } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://test.127.0.0.1.nip.io';
const targetPath = __ENV.TARGET_PATH || '/healthz';
const expectedStatus = Number(__ENV.EXPECTED_STATUS || '200');

export const options = {
  vus: Number(__ENV.VUS || '10'),
  duration: __ENV.DURATION || '20s',
  discardResponseBodies: true,
  thresholds: {
    http_req_failed: ['rate<0.01'], // Tỷ lệ lỗi phải dưới 1%
    http_req_duration: ['p(95)<500', 'p(99)<1000'], // 95% request dưới 500ms, 99% dưới 1s
  },
};

export default function () {
  const headers = {};
  if (__ENV.AUTH_TOKEN) {
    headers.Authorization = `Bearer ${__ENV.AUTH_TOKEN}`;
  }

  const response = http.get(`${baseUrl}${targetPath}`, {
    headers,
    tags: { endpoint: targetPath },
  });

  check(response, {
    [`status is ${expectedStatus}`]: res => res.status === expectedStatus,
  });
}
```

---

## 2. Các tham số cấu hình khi chạy

| Biến môi trường | Mặc định | Mô tả |
| :--- | :--- | :--- |
| `BASE_URL` | `http://test.127.0.0.1.nip.io` | URL gốc của Gateway cần test |
| `TARGET_PATH`| `/healthz` | Đường dẫn endpoint kiểm thử (ví dụ: `/services/feedservice/api/feeds`) |
| `VUS` | `10` | Số lượng Virtual Users chạy đồng thời |
| `DURATION` | `20s` | Thời gian duy trì tải (ví dụ: `30s`, `1m`) |
| `AUTH_TOKEN` | *(để trống)* | Bearer Token JWT nếu endpoint yêu cầu xác thực |
| `EXPECTED_STATUS`| `200` | Mã HTTP mong đợi |

---

## 3. Câu lệnh thực thi k6 mẫu

### Cài đặt k6 (nếu chưa có):
- Windows (Chocolatey / Winget): `winget install k6`
- Linux (Ubuntu): `sudo apt-get install k6`

### Chạy kiểm thử tải Endpoint Gateway Health:
```bash
k6 run -e BASE_URL="http://localhost:8080" -e TARGET_PATH="/management/health" -e VUS=20 -e DURATION=30s performance/k6/http-baseline.js
```

### Chạy kiểm thử tải Feed Service có xác thực Bearer Token:
```bash
k6 run \
  -e BASE_URL="http://localhost:8080" \
  -e TARGET_PATH="/services/feedservice/api/feeds" \
  -e AUTH_TOKEN="<YOUR_ACCESS_TOKEN>" \
  -e VUS=50 \
  -e DURATION=1m \
  performance/k6/http-baseline.js
```

---

## 4. Diễn giải các chỉ số kết quả (Metrics Interpretation)

- **`http_req_failed`**: Tỷ lệ request thất bại (HTTP status không khớp hoặc connection timeout). Tiêu chuẩn: `< 1%`.
- **`http_req_duration (p50)`**: Thời gian phản hồi trung vị (50% người dùng nhận kết quả nhanh hơn mức này).
- **`http_req_duration (p95)`**: 95% số lượng request có thời gian phản hồi thấp hơn ngưỡng này. Tiêu chuẩn: `< 500ms`.
- **`http_req_duration (p99)`**: 99% số lượng request có thời gian phản hồi thấp hơn ngưỡng này (đo lường đuôi độ trễ - tail latency). Tiêu chuẩn: `< 1000ms`.
- **`http_reqs (RPS)`**: Số lượng request hoàn thành trên mỗi giây (Throughput).
