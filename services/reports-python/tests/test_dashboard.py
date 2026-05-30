import unittest
from unittest.mock import AsyncMock, MagicMock, patch

import httpx
from fastapi.testclient import TestClient

from app.clients.catalog_client import CatalogClient
from app.clients.order_client import OrderClient
from app.core.config import settings
from app.main import app
from app.schemas.dashboard_schema import (
    CatalogReport,
    OrdersReport,
    OwnerDashboardResponse,
)
from app.services.dashboard_service import DashboardService


def build_dashboard_response() -> OwnerDashboardResponse:
    return OwnerDashboardResponse(
        restaurant_id="restaurant-1",
        orders=OrdersReport(
            total_orders=1,
            sales_order_count=1,
            total_sales=25.5,
            delivered_sales=25.5,
            average_ticket=25.5,
            created_orders=0,
            accepted_orders=0,
            preparing_orders=0,
            ready_orders=0,
            delivered_orders=1,
            cancelled_orders=0,
            orders_by_status=[{"status": "DELIVERED", "total": 1}],
        ),
        catalog=CatalogReport(
            total_products=1,
            available_products=1,
            paused_products=0,
            simple_products=1,
            customizable_products=0,
            featured_products=1,
            products_with_custom_schedule=0,
            total_categories=1,
            active_categories=1,
            products_by_type=[{"productType": "SIMPLE", "total": 1}],
            products_by_status=[{"status": "AVAILABLE", "total": 1}],
        ),
    )


class DashboardRoutesTest(unittest.TestCase):
    def setUp(self) -> None:
        self.client = TestClient(app)

    def test_health_check_returns_service_status(self) -> None:
        response = self.client.get("/api/reports/health")

        self.assertEqual(200, response.status_code)
        self.assertEqual(
            {"status": "UP", "service": "report-service"},
            response.json(),
        )

    def test_owner_dashboard_returns_response_and_forwards_inputs(self) -> None:
        service = MagicMock()
        service.get_owner_dashboard = AsyncMock(return_value=build_dashboard_response())

        with patch("app.api.dashboard_routes.DashboardService", return_value=service):
            response = self.client.get(
                "/api/reports/dashboard/owner/restaurant-1",
                params={"from": "2026-01-01", "to": "2026-01-31"},
                headers={"Authorization": "Bearer token"},
            )

        self.assertEqual(200, response.status_code)
        body = response.json()
        self.assertEqual("restaurant-1", body["restaurantId"])
        self.assertEqual(1, body["orders"]["salesOrderCount"])
        self.assertEqual(1, body["catalog"]["availableProducts"])
        service.get_owner_dashboard.assert_awaited_once_with(
            restaurant_id="restaurant-1",
            from_date="2026-01-01",
            to_date="2026-01-31",
            authorization_header="Bearer token",
        )

    def test_owner_dashboard_converts_http_status_error_to_bad_gateway(self) -> None:
        request = httpx.Request("GET", "http://orders")
        upstream_response = httpx.Response(503, text="service unavailable", request=request)
        service = MagicMock()
        service.get_owner_dashboard = AsyncMock(
            side_effect=httpx.HTTPStatusError(
                "upstream error",
                request=request,
                response=upstream_response,
            )
        )

        with patch("app.api.dashboard_routes.DashboardService", return_value=service):
            response = self.client.get("/api/reports/dashboard/owner/restaurant-1")

        self.assertEqual(502, response.status_code)
        self.assertEqual(
            "Required service returned 503: service unavailable",
            response.json()["detail"],
        )

    def test_owner_dashboard_converts_http_error_to_bad_gateway(self) -> None:
        service = MagicMock()
        service.get_owner_dashboard = AsyncMock(
            side_effect=httpx.ConnectError("connection refused")
        )

        with patch("app.api.dashboard_routes.DashboardService", return_value=service):
            response = self.client.get("/api/reports/dashboard/owner/restaurant-1")

        self.assertEqual(502, response.status_code)
        self.assertEqual(
            "Could not get data from required services: connection refused",
            response.json()["detail"],
        )


class DashboardServiceTest(unittest.IsolatedAsyncioTestCase):
    async def test_get_owner_dashboard_aggregates_orders_and_catalog_data(self) -> None:
        service = DashboardService()
        service.order_client = MagicMock()
        service.catalog_client = MagicMock()

        service.order_client.get_orders_by_restaurant = AsyncMock(
            return_value=[
                {"status": "created", "total": "10.50"},
                {"status": "DELIVERED", "total": "20.25"},
                {"status": "cancelled", "total": "999.99"},
                {"status": "", "total": "invalid"},
                {"total": None},
            ]
        )
        service.catalog_client.get_products_by_restaurant = AsyncMock(
            return_value=[
                {
                    "active": True,
                    "productType": "simple",
                    "featured": True,
                    "availability": {
                        "status": "available",
                        "weeklySchedule": [{"day": "MONDAY"}],
                    },
                },
                {
                    "isActive": True,
                    "productType": "customizable",
                    "availability": {"status": "OUT_OF_STOCK"},
                },
                {
                    "active": False,
                    "productType": "simple",
                    "availability": {"status": "AVAILABLE"},
                },
                {
                    "productType": None,
                    "availability": "always",
                },
            ]
        )
        service.catalog_client.get_categories_by_restaurant = AsyncMock(
            return_value=[
                {"active": True},
                {"isActive": False},
                {},
            ]
        )

        result = await service.get_owner_dashboard(
            restaurant_id="restaurant-1",
            from_date="2026-01-01",
            to_date="2026-01-31",
            authorization_header="Bearer token",
        )

        self.assertEqual("restaurant-1", result.restaurant_id)
        self.assertEqual(5, result.orders.total_orders)
        self.assertEqual(4, result.orders.sales_order_count)
        self.assertEqual(30.75, result.orders.total_sales)
        self.assertEqual(20.25, result.orders.delivered_sales)
        self.assertEqual(7.69, result.orders.average_ticket)
        self.assertEqual(1, result.orders.created_orders)
        self.assertEqual(1, result.orders.delivered_orders)
        self.assertEqual(1, result.orders.cancelled_orders)
        self.assertEqual(
            {"CREATED": 1, "DELIVERED": 1, "CANCELLED": 1, "UNKNOWN": 2},
            {metric.status: metric.total for metric in result.orders.orders_by_status},
        )
        self.assertEqual(3, result.catalog.total_products)
        self.assertEqual(1, result.catalog.available_products)
        self.assertEqual(1, result.catalog.paused_products)
        self.assertEqual(1, result.catalog.simple_products)
        self.assertEqual(1, result.catalog.customizable_products)
        self.assertEqual(1, result.catalog.featured_products)
        self.assertEqual(1, result.catalog.products_with_custom_schedule)
        self.assertEqual(3, result.catalog.total_categories)
        self.assertEqual(2, result.catalog.active_categories)
        self.assertEqual(
            {"SIMPLE": 1, "CUSTOMIZABLE": 1, "UNKNOWN": 1},
            {
                metric.product_type: metric.total
                for metric in result.catalog.products_by_type
            },
        )
        self.assertEqual(
            {"AVAILABLE": 1, "OUT_OF_STOCK": 1, "UNKNOWN": 1},
            {metric.status: metric.total for metric in result.catalog.products_by_status},
        )
        service.order_client.get_orders_by_restaurant.assert_awaited_once_with(
            restaurant_id="restaurant-1",
            from_date="2026-01-01",
            to_date="2026-01-31",
        )
        service.catalog_client.get_products_by_restaurant.assert_awaited_once_with(
            restaurant_id="restaurant-1",
            authorization_header="Bearer token",
        )
        service.catalog_client.get_categories_by_restaurant.assert_awaited_once_with(
            restaurant_id="restaurant-1",
            authorization_header="Bearer token",
        )

    def test_empty_orders_report_uses_zero_totals(self) -> None:
        service = DashboardService()

        result = service._build_orders_report([])

        self.assertEqual(0, result.total_orders)
        self.assertEqual(0, result.sales_order_count)
        self.assertEqual(0.0, result.total_sales)
        self.assertEqual(0.0, result.delivered_sales)
        self.assertEqual(0.0, result.average_ticket)
        self.assertEqual([], result.orders_by_status)


class HttpClientsTest(unittest.IsolatedAsyncioTestCase):
    async def test_order_client_sends_internal_token_and_date_filters(self) -> None:
        response = MagicMock()
        response.json.return_value = [{"id": "order-1"}]
        http_client = MagicMock()
        http_client.get = AsyncMock(return_value=response)
        context_manager = MagicMock()
        context_manager.__aenter__ = AsyncMock(return_value=http_client)
        context_manager.__aexit__ = AsyncMock(return_value=None)

        with patch(
            "app.clients.order_client.httpx.AsyncClient",
            return_value=context_manager,
        ) as async_client:
            client = OrderClient()
            client.base_url = "http://orders"
            client.timeout = 3.5

            result = await client.get_orders_by_restaurant(
                restaurant_id="restaurant-1",
                from_date="2026-01-01",
                to_date="2026-01-31",
            )

        self.assertEqual([{"id": "order-1"}], result)
        async_client.assert_called_once_with(timeout=3.5)
        http_client.get.assert_awaited_once_with(
            "http://orders/api/internal/orders/restaurant/restaurant-1",
            params={"from": "2026-01-01", "to": "2026-01-31"},
            headers={"X-Internal-Service-Token": settings.internal_service_token},
        )
        response.raise_for_status.assert_called_once_with()

    async def test_order_client_returns_empty_list_for_non_list_body(self) -> None:
        response = MagicMock()
        response.json.return_value = {"items": []}
        http_client = MagicMock()
        http_client.get = AsyncMock(return_value=response)
        context_manager = MagicMock()
        context_manager.__aenter__ = AsyncMock(return_value=http_client)
        context_manager.__aexit__ = AsyncMock(return_value=None)

        with patch("app.clients.order_client.httpx.AsyncClient", return_value=context_manager):
            client = OrderClient()
            client.base_url = "http://orders"

            result = await client.get_orders_by_restaurant("restaurant-1")

        self.assertEqual([], result)
        http_client.get.assert_awaited_once_with(
            "http://orders/api/internal/orders/restaurant/restaurant-1",
            params={},
            headers={"X-Internal-Service-Token": settings.internal_service_token},
        )

    async def test_catalog_client_sends_authorization_header(self) -> None:
        response = MagicMock()
        response.json.return_value = [{"id": "product-1"}]
        http_client = MagicMock()
        http_client.get = AsyncMock(return_value=response)
        context_manager = MagicMock()
        context_manager.__aenter__ = AsyncMock(return_value=http_client)
        context_manager.__aexit__ = AsyncMock(return_value=None)

        with patch(
            "app.clients.catalog_client.httpx.AsyncClient",
            return_value=context_manager,
        ):
            client = CatalogClient()
            client.base_url = "http://catalog"

            result = await client.get_products_by_restaurant(
                restaurant_id="restaurant-1",
                authorization_header="Bearer token",
            )

        self.assertEqual([{"id": "product-1"}], result)
        http_client.get.assert_awaited_once_with(
            "http://catalog/api/products/restaurant/restaurant-1",
            headers={"Authorization": "Bearer token"},
        )
        response.raise_for_status.assert_called_once_with()

    async def test_catalog_client_returns_empty_list_for_non_list_products_body(self) -> None:
        response = MagicMock()
        response.json.return_value = {"items": []}
        http_client = MagicMock()
        http_client.get = AsyncMock(return_value=response)
        context_manager = MagicMock()
        context_manager.__aenter__ = AsyncMock(return_value=http_client)
        context_manager.__aexit__ = AsyncMock(return_value=None)

        with patch("app.clients.catalog_client.httpx.AsyncClient", return_value=context_manager):
            client = CatalogClient()
            client.base_url = "http://catalog"

            result = await client.get_products_by_restaurant("restaurant-1")

        self.assertEqual([], result)
        http_client.get.assert_awaited_once_with(
            "http://catalog/api/products/restaurant/restaurant-1",
            headers={},
        )

    async def test_catalog_client_returns_categories_list(self) -> None:
        response = MagicMock()
        response.json.return_value = [{"id": "category-1"}]
        http_client = MagicMock()
        http_client.get = AsyncMock(return_value=response)
        context_manager = MagicMock()
        context_manager.__aenter__ = AsyncMock(return_value=http_client)
        context_manager.__aexit__ = AsyncMock(return_value=None)

        with patch("app.clients.catalog_client.httpx.AsyncClient", return_value=context_manager):
            client = CatalogClient()
            client.base_url = "http://catalog"

            result = await client.get_categories_by_restaurant(
                restaurant_id="restaurant-1",
                authorization_header="Bearer token",
            )

        self.assertEqual([{"id": "category-1"}], result)
        http_client.get.assert_awaited_once_with(
            "http://catalog/api/categories/restaurant/restaurant-1",
            headers={"Authorization": "Bearer token"},
        )
        response.raise_for_status.assert_called_once_with()

    async def test_catalog_client_returns_empty_list_for_non_list_categories_body(self) -> None:
        response = MagicMock()
        response.json.return_value = {"items": []}
        http_client = MagicMock()
        http_client.get = AsyncMock(return_value=response)
        context_manager = MagicMock()
        context_manager.__aenter__ = AsyncMock(return_value=http_client)
        context_manager.__aexit__ = AsyncMock(return_value=None)

        with patch("app.clients.catalog_client.httpx.AsyncClient", return_value=context_manager):
            client = CatalogClient()
            client.base_url = "http://catalog"

            result = await client.get_categories_by_restaurant("restaurant-1")

        self.assertEqual([], result)
        http_client.get.assert_awaited_once_with(
            "http://catalog/api/categories/restaurant/restaurant-1",
            headers={},
        )


if __name__ == "__main__":
    unittest.main()
