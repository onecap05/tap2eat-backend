import http from 'k6/http';
import { check, group } from 'k6';
import {
  CATALOG_PRODUCT_ID,
  CATALOG_RESTAURANT_ID,
  CATALOG_SEARCH_QUERY,
  endpoint,
} from '../utils/config.js';

export const options = {
  vus: 2,
  iterations: 4,
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  group('public catalog listing', () => {
    const restaurants = http.get(endpoint('/customer/restaurants'), {
      tags: { name: 'catalog_restaurants' },
    });

    check(restaurants, {
      'restaurants status is 200': (res) => res.status === 200,
    });
  });

  group('public catalog search', () => {
    const query = encodeURIComponent(CATALOG_SEARCH_QUERY);
    const search = http.get(endpoint(`/customer/restaurants/search?query=${query}`), {
      tags: { name: 'catalog_restaurants_search' },
    });

    check(search, {
      'restaurant search status is 200': (res) => res.status === 200,
    });
  });

  if (CATALOG_RESTAURANT_ID) {
    group('public restaurant details', () => {
      const restaurant = http.get(endpoint(`/customer/restaurants/${CATALOG_RESTAURANT_ID}`), {
        tags: { name: 'catalog_restaurant_detail' },
      });
      const branches = http.get(endpoint(`/customer/restaurants/${CATALOG_RESTAURANT_ID}/branches`), {
        tags: { name: 'catalog_restaurant_branches' },
      });
      const categories = http.get(endpoint(`/customer/restaurants/${CATALOG_RESTAURANT_ID}/categories`), {
        tags: { name: 'catalog_restaurant_categories' },
      });
      const products = http.get(endpoint(`/customer/restaurants/${CATALOG_RESTAURANT_ID}/products`), {
        tags: { name: 'catalog_restaurant_products' },
      });

      check(restaurant, { 'restaurant detail status is 200': (res) => res.status === 200 });
      check(branches, { 'restaurant branches status is 200': (res) => res.status === 200 });
      check(categories, { 'restaurant categories status is 200': (res) => res.status === 200 });
      check(products, { 'restaurant products status is 200': (res) => res.status === 200 });
    });
  }

  if (CATALOG_PRODUCT_ID) {
    group('public product details', () => {
      const product = http.get(endpoint(`/customer/products/${CATALOG_PRODUCT_ID}`), {
        tags: { name: 'catalog_product_detail' },
      });

      check(product, {
        'product detail status is 200': (res) => res.status === 200,
      });
    });
  }
}
