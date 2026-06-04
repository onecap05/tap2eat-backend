# Tap2Eat k6 performance tests

This folder contains the initial k6 performance test structure for Tap2Eat.

k6 is a load testing tool for HTTP APIs and web applications. These scripts enter through the API Gateway, the same way the real app does, using `BASE_URL` with a default value of `http://localhost:8080/api`.

## Install k6

On Windows, install k6 with one of these options:

```powershell
winget install k6.k6
```

```powershell
choco install k6
```

You can also download it from:

```text
https://k6.io/docs/get-started/installation/
```

Verify the installation:

```powershell
k6 version
```

## Start the backend first

Before running these scripts, start the Tap2Eat backend locally and make sure the API Gateway is available.

Default gateway base URL:

```text
http://localhost:8080/api
```

These tests do not start Docker, do not create databases, and do not modify backend services.

## Environment variables

All configurable values are passed with environment variables.

Required only for login smoke:

```powershell
$env:TEST_EMAIL="user@example.com"
$env:TEST_PASSWORD="your-test-password"
```

Optional base URL override:

```powershell
$env:BASE_URL="http://localhost:8080/api"
```

Optional catalog IDs for detail GET requests:

```powershell
$env:CATALOG_RESTAURANT_ID="restaurant-id"
$env:CATALOG_PRODUCT_ID="product-id"
$env:CATALOG_SEARCH_QUERY="pizza"
```

Do not store real credentials in these scripts.

## Run smoke tests

Gateway smoke:

```powershell
k6 run .\performance-tests\k6\smoke\gateway-smoke.js
```

Catalog smoke:

```powershell
k6 run .\performance-tests\k6\smoke\catalog-smoke.js
```

Identity login smoke:

```powershell
$env:TEST_EMAIL="user@example.com"
$env:TEST_PASSWORD="your-test-password"
k6 run .\performance-tests\k6\smoke\identity-login-smoke.js
```

The identity login smoke threshold is `p95 < 2000ms` because local Docker execution goes through the API Gateway and includes identity-service processing, PostgreSQL access, and JWT token generation.

## Run load tests

Catalog load:

```powershell
k6 run .\performance-tests\k6\load\catalog-load.js
```

With a custom base URL and catalog detail data:

```powershell
$env:BASE_URL="http://localhost:8080/api"
$env:CATALOG_RESTAURANT_ID="restaurant-id"
$env:CATALOG_SEARCH_QUERY="pizza"
k6 run .\performance-tests\k6\load\catalog-load.js
```

## Metrics

`http_req_duration`: Time spent on HTTP requests. The scripts use p95 thresholds to check that 95% of requests stay below the expected latency.

`http_req_failed`: Percentage of failed HTTP requests. The current smoke/load catalog tests require this to stay below 1%.

`checks`: Custom assertions declared in the script, such as checking that an endpoint returns status `200`.

`vus`: Virtual users. They simulate concurrent users running the script.

`iterations`: Number of times k6 runs the default function. Smoke tests use a small number of iterations.

## Current scope

Included now:

- Gateway smoke through public catalog.
- Public catalog smoke using GET requests only.
- Moderate public catalog load using GET requests only.
- Identity login smoke using `TEST_EMAIL` and `TEST_PASSWORD`.

Not included yet:

- Order tests.
- Payment tests.
- Favorites tests.
- Write scenarios.
- Real stress tests.

## Warning

Do not run stress tests against production. Do not run stress tests against the normal database if the scenario creates, updates, or deletes data. Future stress tests should use isolated environments and disposable data.
