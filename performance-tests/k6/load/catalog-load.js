import http from 'k6/http';
import { check, sleep } from 'k6';
import {
  CATALOG_RESTAURANT_ID,
  CATALOG_SEARCH_QUERY,
  endpoint,
} from '../utils/config.js';

export const options = {
  vus: 15,
  duration: '1m',
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const restaurants = http.get(endpoint('/customer/restaurants'), {
    tags: { name: 'catalog_restaurants' },
  });

  check(restaurants, {
    'restaurants status is 200': (res) => res.status === 200,
  });

  const query = encodeURIComponent(CATALOG_SEARCH_QUERY);
  const search = http.get(endpoint(`/customer/restaurants/search?query=${query}`), {
    tags: { name: 'catalog_restaurants_search' },
  });

  check(search, {
    'restaurant search status is 200': (res) => res.status === 200,
  });

  if (CATALOG_RESTAURANT_ID) {
    const products = http.get(endpoint(`/customer/restaurants/${CATALOG_RESTAURANT_ID}/products`), {
      tags: { name: 'catalog_restaurant_products' },
    });

    check(products, {
      'restaurant products status is 200': (res) => res.status === 200,
    });
  }

  sleep(1);
}
