import http from 'k6/http';
import { check, fail } from 'k6';
import { endpoint, TEST_EMAIL, TEST_PASSWORD } from '../utils/config.js';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  if (!TEST_EMAIL || !TEST_PASSWORD) {
    fail('TEST_EMAIL and TEST_PASSWORD must be provided for identity login smoke tests.');
  }

  const response = http.post(
    endpoint('/auth/login'),
    JSON.stringify({
      email: TEST_EMAIL,
      password: TEST_PASSWORD,
    }),
    {
      headers: {
        'Content-Type': 'application/json',
      },
      tags: { name: 'identity_login' },
    }
  );

  check(response, {
    'login status is 200': (res) => res.status === 200,
  });
}
