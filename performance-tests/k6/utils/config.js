export const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8080/api').replace(/\/$/, '');

export const TEST_EMAIL = __ENV.TEST_EMAIL;
export const TEST_PASSWORD = __ENV.TEST_PASSWORD;

export const CATALOG_SEARCH_QUERY = __ENV.CATALOG_SEARCH_QUERY || '';
export const CATALOG_RESTAURANT_ID = __ENV.CATALOG_RESTAURANT_ID;
export const CATALOG_PRODUCT_ID = __ENV.CATALOG_PRODUCT_ID;

export function endpoint(path) {
  return `${BASE_URL}${path.startsWith('/') ? path : `/${path}`}`;
}
