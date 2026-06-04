import http from 'k6/http';
import { check } from 'k6';
import { endpoint } from '../utils/config.js';

export const options = {
  vus: 1,
  iterations: 3,
  thresholds: {
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const response = http.get(endpoint('/customer/restaurants'), {
    tags: { name: 'gateway_public_catalog_smoke' },
  });

  check(response, {
    'gateway routes public catalog request': (res) => res.status === 200,
  });
}
