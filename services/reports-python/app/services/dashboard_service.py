import asyncio
from collections import Counter
from decimal import Decimal, InvalidOperation
from typing import Any

from app.clients.catalog_client import CatalogClient
from app.clients.order_client import OrderClient
from app.schemas.dashboard_schema import (
    CatalogReport,
    OrdersReport,
    OwnerDashboardResponse,
    ProductTypeMetric,
    StatusMetric,
)


class DashboardService:
    def __init__(self) -> None:
        self.order_client = OrderClient()
        self.catalog_client = CatalogClient()

    async def get_owner_dashboard(
        self,
        restaurant_id: str,
        from_date: str | None = None,
        to_date: str | None = None
    ) -> OwnerDashboardResponse:
        orders, products, categories = await asyncio.gather(
            self.order_client.get_orders_by_restaurant(
                restaurant_id=restaurant_id,
                from_date=from_date,
                to_date=to_date
            ),
            self.catalog_client.get_products_by_restaurant(restaurant_id),
            self.catalog_client.get_categories_by_restaurant(restaurant_id)
        )

        return OwnerDashboardResponse(
            restaurant_id=restaurant_id,
            orders=self._build_orders_report(orders),
            catalog=self._build_catalog_report(products, categories)
        )

    def _build_orders_report(self, orders: list[dict[str, Any]]) -> OrdersReport:
        status_counter = Counter(
            self._normalize_status(order.get("status"))
            for order in orders
        )

        sales_orders = [
            order for order in orders
            if self._normalize_status(order.get("status")) != "CANCELLED"
        ]

        delivered_orders = [
            order for order in orders
            if self._normalize_status(order.get("status")) == "DELIVERED"
        ]

        total_sales = sum(
            self._to_decimal(order.get("total"))
            for order in sales_orders
        )

        delivered_sales = sum(
            self._to_decimal(order.get("total"))
            for order in delivered_orders
        )

        sales_order_count = len(sales_orders)

        average_ticket = (
            total_sales / sales_order_count
            if sales_order_count > 0
            else Decimal("0")
        )

        return OrdersReport(
            total_orders=len(orders),
            sales_order_count=sales_order_count,
            total_sales=self._to_float(total_sales),
            delivered_sales=self._to_float(delivered_sales),
            average_ticket=self._to_float(average_ticket),

            created_orders=status_counter.get("CREATED", 0),
            accepted_orders=status_counter.get("ACCEPTED", 0),
            preparing_orders=status_counter.get("PREPARING", 0),
            ready_orders=status_counter.get("READY", 0),
            delivered_orders=status_counter.get("DELIVERED", 0),
            cancelled_orders=status_counter.get("CANCELLED", 0),

            orders_by_status=[
                StatusMetric(status=status, total=total)
                for status, total in status_counter.items()
            ]
        )

    def _build_catalog_report(
        self,
        products: list[dict[str, Any]],
        categories: list[dict[str, Any]]
    ) -> CatalogReport:
        active_products = [
            product for product in products
            if self._is_active(product)
        ]

        active_categories = [
            category for category in categories
            if self._is_active(category)
        ]

        product_status_counter = Counter(
            self._get_product_status(product)
            for product in active_products
        )

        product_type_counter = Counter(
            self._normalize_text(product.get("productType"), "UNKNOWN")
            for product in active_products
        )

        return CatalogReport(
            total_products=len(active_products),
            available_products=product_status_counter.get("AVAILABLE", 0),
            paused_products=self._count_paused_products(product_status_counter),
            simple_products=product_type_counter.get("SIMPLE", 0),
            customizable_products=product_type_counter.get("CUSTOMIZABLE", 0),
            featured_products=sum(
                1 for product in active_products
                if bool(product.get("featured", False))
            ),
            products_with_custom_schedule=sum(
                1 for product in active_products
                if self._has_custom_schedule(product)
            ),
            total_categories=len(categories),
            active_categories=len(active_categories),
            products_by_type=[
                ProductTypeMetric(product_type=product_type, total=total)
                for product_type, total in product_type_counter.items()
            ],
            products_by_status=[
                StatusMetric(status=status, total=total)
                for status, total in product_status_counter.items()
            ]
        )

    def _is_active(self, document: dict[str, Any]) -> bool:
        if "active" in document:
            return bool(document["active"])

        if "isActive" in document:
            return bool(document["isActive"])

        return True

    def _get_product_status(self, product: dict[str, Any]) -> str:
        availability = product.get("availability")

        if not isinstance(availability, dict):
            return "UNKNOWN"

        return self._normalize_text(availability.get("status"), "UNKNOWN")

    def _count_paused_products(self, status_counter: Counter) -> int:
        paused_statuses = {
            "PAUSED",
            "UNAVAILABLE",
            "OUT_OF_STOCK",
            "TEMPORARILY_UNAVAILABLE"
        }

        return sum(
            total for status, total in status_counter.items()
            if status in paused_statuses
        )

    def _has_custom_schedule(self, product: dict[str, Any]) -> bool:
        availability = product.get("availability")

        if not isinstance(availability, dict):
            return False

        weekly_schedule = availability.get("weeklySchedule")

        return isinstance(weekly_schedule, list) and len(weekly_schedule) > 0

    def _normalize_status(self, value: Any) -> str:
        return self._normalize_text(value, "UNKNOWN")

    def _normalize_text(self, value: Any, default: str) -> str:
        if value is None:
            return default

        text = str(value).strip()

        if not text:
            return default

        return text.upper()

    def _to_decimal(self, value: Any) -> Decimal:
        if value is None:
            return Decimal("0")

        try:
            return Decimal(str(value))
        except (InvalidOperation, ValueError):
            return Decimal("0")

    def _to_float(self, value: Decimal) -> float:
        return float(round(value, 2))