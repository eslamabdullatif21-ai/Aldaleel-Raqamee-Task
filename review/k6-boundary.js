import http from 'k6/http';
import { check, fail, sleep } from 'k6';

const baseUrl = __ENV.BASE_URL || 'http://127.0.0.1:8080';
const virtualUsers = Number(__ENV.VUS || 30);
const rampDuration = __ENV.RAMP_DURATION || '15s';
const holdDuration = __ENV.HOLD_DURATION || '15s';
const rampDownDuration = __ENV.RAMP_DOWN_DURATION || '5s';
const thinkSeconds = Number(__ENV.THINK_SECONDS || 10);

export const options = {
  discardResponseBodies: true,
  scenarios: {
    ticketListBoundary: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: rampDuration, target: virtualUsers },
        { duration: holdDuration, target: virtualUsers },
        { duration: rampDownDuration, target: 0 },
      ],
      gracefulRampDown: '15s',
      gracefulStop: '15s',
    },
  },
  thresholds: {
    checks: ['rate>0.99'],
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

export function setup() {
  const response = http.post(`${baseUrl}/api/auth/login`, JSON.stringify({
    email: __ENV.CUSTOMER_EMAIL || 'customer@example.com',
    password: __ENV.CUSTOMER_PASSWORD || 'Customer123!',
  }), {
    headers: { 'Content-Type': 'application/json' },
    responseType: 'text',
    tags: { flow: 'setup' },
  });
  if (!check(response, { 'setup login succeeds': (result) => result.status === 200 })) {
    fail(`Login failed with HTTP ${response.status}`);
  }
  return { token: response.json('token') };
}

export default function boundary(data) {
  const response = http.get(`${baseUrl}/api/tickets?page=0&size=20`, {
    headers: { Authorization: `Bearer ${data.token}` },
    responseType: 'none',
    timeout: '10s',
    tags: { flow: 'ticket-list' },
  });
  check(response, { 'ticket list is 200': (result) => result.status === 200 });
  sleep(thinkSeconds);
}
