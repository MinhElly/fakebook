import http from 'k6/http';
import { check } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const targetPath = __ENV.TARGET_PATH || '/management/health';
const expectedStatus = Number(__ENV.EXPECTED_STATUS || '200');

export const options = {
  vus: Number(__ENV.VUS || '10'),
  duration: __ENV.DURATION || '20s',
  discardResponseBodies: true,
  thresholds: {
    http_req_failed: ['rate<0.01'], // Error rate under 1%
    http_req_duration: ['p(95)<500', 'p(99)<1000'], // 95% < 500ms, 99% < 1000ms
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
