import asyncio
from collections import Counter, defaultdict
from datetime import datetime
from decimal import Decimal, InvalidOperation
from typing import Any

from httpx import HTTPError

from app.clients.catalog_client import CatalogClient
from app.clients.finance_client import FinanceClient
from app.clients.order_client import OrderClient
from app.schemas.dashboard_schema import (
    CatalogReport,
    OrderDetailMetric,
    OrdersByHourMetric,
    OrdersReport,
    OwnerAnalyticsMetadata,
    OwnerAnalyticsReport,
    OwnerAnalyticsSummary,
    OwnerDashboardResponse,
    PaymentSummary,
    ProductTypeMetric,
    SalesByDayMetric,
    SoldProductDetailMetric,
    StatusMetric,
    TopProductMetric,
)


class DashboardService:
    def __init__(self) -> None:
        self.order_client = OrderClient()
        self.catalog_client = CatalogClient()
        self.finance_client = FinanceClient()

    async def get_owner_dashboard(
        self,
        restaurant_id: str,
        from_date: str | None = None,
        to_date: str | None = None,
        authorization_header: str | None = None
    ) -> OwnerDashboardResponse:
        orders, products, categories = await asyncio.gather(
            self.order_client.get_orders_by_restaurant(
                restaurant_id=restaurant_id,
                from_date=from_date,
                to_date=to_date
            ),
            self.catalog_client.get_products_by_restaurant(
                restaurant_id=restaurant_id,
                authorization_header=authorization_header
            ),
            self.catalog_client.get_categories_by_restaurant(
                restaurant_id=restaurant_id,
                authorization_header=authorization_header
            )
        )

        return OwnerDashboardResponse(
            restaurant_id=restaurant_id,
            orders=self._build_orders_report(orders),
            catalog=self._build_catalog_report(products, categories)
        )

    async def get_owner_analytics(
        self,
        restaurant_id: str,
        from_date: str | None = None,
        to_date: str | None = None,
        authorization_header: str | None = None,
        branch_id: str | None = None
    ) -> OwnerAnalyticsReport:
        orders, products, categories = await asyncio.gather(
            self.order_client.get_orders_by_restaurant(
                restaurant_id=restaurant_id,
                from_date=from_date,
                to_date=to_date
            ),
            self.catalog_client.get_products_by_restaurant(
                restaurant_id=restaurant_id,
                authorization_header=authorization_header
            ),
            self.catalog_client.get_categories_by_restaurant(
                restaurant_id=restaurant_id,
                authorization_header=authorization_header
            )
        )

        restaurant, branches = await asyncio.gather(
            self._safe_get_restaurant(restaurant_id, authorization_header),
            self._safe_get_branches(restaurant_id, authorization_header)
        )

        filtered_orders, branch_note = self._filter_orders_by_branch(orders, branch_id)
        filtered_payments, payment_summary = await self._load_payments_summary(
            restaurant_id=restaurant_id,
            from_date=from_date,
            to_date=to_date,
            branch_id=branch_id,
            authorization_header=authorization_header
        )
        payment_by_order_id = self._index_payments_by_order_id(filtered_payments)
        orders_report = self._build_orders_report(filtered_orders)

        return OwnerAnalyticsReport(
            restaurant_id=restaurant_id,
            metadata=self._build_metadata(
                restaurant_id=restaurant_id,
                restaurant=restaurant,
                branches=branches,
                orders=orders,
                branch_id=branch_id,
                branch_note=branch_note
            ),
            summary=self._build_analytics_summary(
                orders_report,
                filtered_orders,
                payment_summary
            ),
            sales_by_day=self._build_sales_by_day(filtered_orders),
            top_products=self._build_top_products(filtered_orders),
            orders_by_hour=self._build_orders_by_hour(filtered_orders),
            orders_by_status=self._build_orders_by_status(orders_report),
            payment_summary=payment_summary,
            order_details=self._build_order_details(filtered_orders, payment_by_order_id),
            sold_product_details=self._build_sold_product_details(filtered_orders),
            catalog=self._build_catalog_report(products, categories)
        )

    async def _safe_get_restaurant(
        self,
        restaurant_id: str,
        authorization_header: str | None
    ) -> dict[str, Any]:
        try:
            return await self.catalog_client.get_restaurant_by_id(
                restaurant_id=restaurant_id,
                authorization_header=authorization_header
            )
        except HTTPError:
            return {}

    async def _safe_get_branches(
        self,
        restaurant_id: str,
        authorization_header: str | None
    ) -> list[dict[str, Any]]:
        try:
            return await self.catalog_client.get_branches_by_restaurant(
                restaurant_id=restaurant_id,
                authorization_header=authorization_header
            )
        except HTTPError:
            return []

    async def _load_payments_summary(
        self,
        restaurant_id: str,
        from_date: str | None,
        to_date: str | None,
        branch_id: str | None,
        authorization_header: str | None
    ) -> tuple[list[dict[str, Any]], PaymentSummary]:
        try:
            payments = await self.finance_client.get_payments_by_restaurant(
                restaurant_id=restaurant_id,
                authorization_header=authorization_header
            )
        except HTTPError:
            return [], PaymentSummary(
                available=False,
                total_payments=0,
                total_approved=0,
                cash=0,
                online=0,
                pending=0,
                rejected_or_cancelled=0,
                approved_payments=0,
                pending_payments=0,
                cash_amount_received=0,
                cash_change_amount=0,
                message="No se pudo cargar la informaci\u00f3n de pagos."
            )

        filtered_payments = self._filter_payments(payments, from_date, to_date, branch_id)
        return filtered_payments, self._build_payment_summary(filtered_payments)

    def _filter_orders_by_branch(
        self,
        orders: list[dict[str, Any]],
        branch_id: str | None
    ) -> tuple[list[dict[str, Any]], str | None]:
        if not branch_id:
            return orders, None

        orders_with_branch = [
            order for order in orders
            if self._get_branch_id(order)
        ]

        if orders and not orders_with_branch:
            return [], (
                "Las \u00f3rdenes no incluyen informaci\u00f3n suficiente de "
                "sucursal para aplicar este filtro."
            )

        return [
            order for order in orders
            if self._get_branch_id(order) == branch_id
        ], None

    def _filter_payments(
        self,
        payments: list[dict[str, Any]],
        from_date: str | None,
        to_date: str | None,
        branch_id: str | None
    ) -> list[dict[str, Any]]:
        from_datetime = self._parse_datetime(from_date)
        to_datetime = self._parse_datetime(to_date)
        filtered: list[dict[str, Any]] = []

        for payment in payments:
            payment_datetime = self._get_payment_report_datetime(payment)

            if (from_datetime or to_datetime) and payment_datetime is None:
                continue

            if from_datetime and payment_datetime and self._to_naive(payment_datetime) < self._to_naive(from_datetime):
                continue

            if to_datetime and payment_datetime and self._to_naive(payment_datetime) > self._to_naive(to_datetime):
                continue

            if branch_id and self._get_branch_id(payment) != branch_id:
                continue

            filtered.append(payment)

        return filtered

    def _build_payment_summary(
        self,
        payments: list[dict[str, Any]]
    ) -> PaymentSummary:
        total_approved = Decimal("0")
        cash = Decimal("0")
        online = Decimal("0")
        cash_amount_received = Decimal("0")
        cash_change_amount = Decimal("0")
        approved_count = 0
        pending_count = 0
        rejected_or_cancelled_count = 0

        for payment in payments:
            status = self._normalize_status(payment.get("status"))
            provider = self._normalize_text(
                self._get_first_value(payment, ("provider", "paymentMethod", "method")),
                ""
            )
            amount = self._to_decimal(payment.get("amount"))

            if self._is_approved_status(status):
                approved_count += 1
                total_approved += amount

                if self._is_cash_provider(provider):
                    cash += amount
                    cash_amount_received += self._to_decimal(payment.get("amountReceived"))
                    cash_change_amount += self._to_decimal(payment.get("changeAmount"))
                elif self._is_online_provider(provider):
                    online += amount

            elif self._is_pending_status(status):
                pending_count += 1
            elif self._is_rejected_or_cancelled_status(status):
                rejected_or_cancelled_count += 1

        message = None
        if len(payments) == 0:
            message = "No hay pagos registrados para este rango."

        return PaymentSummary(
            available=True,
            total_payments=len(payments),
            total_approved=self._to_float(total_approved),
            cash=self._to_float(cash),
            online=self._to_float(online),
            pending=pending_count,
            rejected_or_cancelled=rejected_or_cancelled_count,
            approved_payments=approved_count,
            pending_payments=pending_count,
            cash_amount_received=self._to_float(cash_amount_received),
            cash_change_amount=self._to_float(cash_change_amount),
            message=message
        )

    def _build_metadata(
        self,
        restaurant_id: str,
        restaurant: dict[str, Any],
        branches: list[dict[str, Any]],
        orders: list[dict[str, Any]],
        branch_id: str | None,
        branch_note: str | None
    ) -> OwnerAnalyticsMetadata:
        branch_name = "Todas las sucursales"

        if branch_id:
            branch_name = self._get_branch_name(branches, branch_id)
            if branch_name == branch_id:
                branch_name = self._get_branch_name_from_orders(orders, branch_id) or branch_id

        return OwnerAnalyticsMetadata(
            restaurant_name=self._get_first_text(restaurant, ("name",), restaurant_id),
            restaurant_rfc=self._get_first_value(restaurant, ("rfc", "RFC")),
            branch_id=branch_id,
            branch_name=branch_name,
            branch_filter_note=branch_note
        )

    def _build_orders_report(self, orders: list[dict[str, Any]]) -> OrdersReport:
        status_counter = Counter(
            self._normalize_status(order.get("status"))
            for order in orders
        )

        sales_orders = self._get_sales_orders(orders)
        delivered_orders = [
            order for order in orders
            if self._normalize_status(order.get("status")) == "DELIVERED"
        ]

        total_sales = sum(
            (self._to_decimal(order.get("total")) for order in sales_orders),
            Decimal("0")
        )
        delivered_sales = sum(
            (self._to_decimal(order.get("total")) for order in delivered_orders),
            Decimal("0")
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

    def _build_analytics_summary(
        self,
        orders_report: OrdersReport,
        orders: list[dict[str, Any]],
        payment_summary: PaymentSummary
    ) -> OwnerAnalyticsSummary:
        cancellation_rate = (
            (orders_report.cancelled_orders / orders_report.total_orders) * 100
            if orders_report.total_orders > 0
            else 0
        )

        return OwnerAnalyticsSummary(
            total_sales=orders_report.total_sales,
            total_orders=orders_report.total_orders,
            average_ticket=orders_report.average_ticket,
            delivered_orders=orders_report.delivered_orders,
            cancelled_orders=orders_report.cancelled_orders,
            cancellation_rate=self._to_float(Decimal(str(cancellation_rate))),
            delivered_sales=orders_report.delivered_sales,
            total_products_sold=self._count_sold_products(orders),
            predominant_payment_method=self._get_predominant_payment_method(payment_summary)
        )

    def _build_sales_by_day(
        self,
        orders: list[dict[str, Any]]
    ) -> list[SalesByDayMetric]:
        metrics: dict[str, dict[str, Decimal | int]] = defaultdict(
            lambda: {"total_sales": Decimal("0"), "total_orders": 0}
        )

        for order in self._get_sales_orders(orders):
            created_at = self._parse_datetime(order.get("createdAt"))

            if created_at is None:
                continue

            day = created_at.date().isoformat()
            metrics[day]["total_sales"] += self._to_decimal(order.get("total"))
            metrics[day]["total_orders"] += 1

        return [
            SalesByDayMetric(
                date=day,
                total_sales=self._to_float(values["total_sales"]),
                total_orders=int(values["total_orders"]),
                average_ticket=self._to_float(
                    values["total_sales"] / int(values["total_orders"])
                    if int(values["total_orders"]) > 0
                    else Decimal("0")
                )
            )
            for day, values in sorted(metrics.items())
        ]

    def _build_top_products(
        self,
        orders: list[dict[str, Any]]
    ) -> list[TopProductMetric]:
        products: dict[str, dict[str, Decimal | int]] = defaultdict(
            lambda: {"quantity_sold": 0, "estimated_sales": Decimal("0")}
        )

        for order in self._get_sales_orders(orders):
            items = order.get("items")

            if not isinstance(items, list):
                continue

            for item in items:
                if not isinstance(item, dict):
                    continue

                product_name = self._get_item_product_name(item)

                if not product_name:
                    continue

                quantity = self._to_int(item.get("quantity"))
                subtotal = self._to_decimal(
                    self._get_first_value(item, ("subtotal", "total", "lineTotal"))
                )

                if quantity <= 0 and subtotal <= 0:
                    continue

                products[product_name]["quantity_sold"] += quantity
                products[product_name]["estimated_sales"] += subtotal

        sorted_products = sorted(
            products.items(),
            key=lambda item: (
                item[1]["quantity_sold"],
                item[1]["estimated_sales"]
            ),
            reverse=True
        )
        total_sales = sum(
            (values["estimated_sales"] for values in products.values()),
            Decimal("0")
        )

        return [
            TopProductMetric(
                product=product,
                quantity_sold=int(values["quantity_sold"]),
                estimated_sales=self._to_float(values["estimated_sales"]),
                sales_percentage=(
                    self._to_float((values["estimated_sales"] / total_sales) * 100)
                    if total_sales > 0
                    else None
                )
            )
            for product, values in sorted_products[:10]
        ]

    def _build_orders_by_hour(
        self,
        orders: list[dict[str, Any]]
    ) -> list[OrdersByHourMetric]:
        metrics: dict[str, dict[str, Decimal | int]] = defaultdict(
            lambda: {"total_orders": 0, "total_sales": Decimal("0")}
        )

        for order in self._get_sales_orders(orders):
            created_at = self._parse_datetime(order.get("createdAt"))

            if created_at is None:
                continue

            hour = f"{created_at.hour:02d}:00"
            metrics[hour]["total_orders"] += 1
            metrics[hour]["total_sales"] += self._to_decimal(order.get("total"))

        return [
            OrdersByHourMetric(
                hour=hour,
                total_orders=int(values["total_orders"]),
                total_sales=self._to_float(values["total_sales"])
            )
            for hour, values in sorted(metrics.items())
        ]

    def _build_orders_by_status(
        self,
        orders_report: OrdersReport
    ) -> list[StatusMetric]:
        return [
            StatusMetric(status="CREATED", total=orders_report.created_orders),
            StatusMetric(status="ACCEPTED", total=orders_report.accepted_orders),
            StatusMetric(status="PREPARING", total=orders_report.preparing_orders),
            StatusMetric(status="READY", total=orders_report.ready_orders),
            StatusMetric(status="DELIVERED", total=orders_report.delivered_orders),
            StatusMetric(status="CANCELLED", total=orders_report.cancelled_orders),
        ]

    def _build_order_details(
        self,
        orders: list[dict[str, Any]],
        payment_by_order_id: dict[str, dict[str, Any]]
    ) -> list[OrderDetailMetric]:
        details: list[OrderDetailMetric] = []

        for order in orders:
            created_at = self._parse_datetime(order.get("createdAt"))
            payment = payment_by_order_id.get(self._get_order_id(order), {})
            details.append(
                OrderDetailMetric(
                    order_folio=self._get_order_folio(order),
                    date=created_at.date().isoformat() if created_at else "",
                    time=created_at.strftime("%H:%M") if created_at else "",
                    branch=self._get_first_text(
                        order,
                        ("branchName", "branchNameSnapshot", "branchId", "branch_id"),
                        "No disponible"
                    ),
                    customer=self._get_customer_name(order),
                    order_status=self._normalize_status(order.get("status")),
                    payment_method=self._get_payment_method(order, payment),
                    payment_status=self._get_payment_status(order, payment),
                    subtotal=self._to_optional_float(
                        self._get_first_value(order, ("subtotal", "subTotal"))
                    ),
                    total=self._to_float(self._to_decimal(order.get("total"))),
                    amount_received=self._to_optional_float(
                        self._get_first_value(
                            payment or order,
                            ("amountReceived", "cashAmountProvided", "amount_received")
                        )
                    ),
                    change_amount=self._to_optional_float(
                        self._get_first_value(
                            payment or order,
                            ("changeAmount", "estimatedChange", "change_amount")
                        )
                    ),
                    payment_provider=self._get_first_text(
                        payment or order,
                        ("provider", "paymentProvider", "payment_provider"),
                        "No disponible"
                    ),
                    payment_reference=self._get_first_text(
                        payment or order,
                        (
                            "providerReference",
                            "paymentReference",
                            "externalReference",
                            "payment_reference"
                        ),
                        "No disponible"
                    ),
                    items_count=self._count_order_items(order),
                    notes=self._get_first_text(
                        order,
                        ("notes", "customerNotes", "specialInstructions"),
                        ""
                    )
                )
            )

        return details

    def _build_sold_product_details(
        self,
        orders: list[dict[str, Any]]
    ) -> list[SoldProductDetailMetric]:
        details: list[SoldProductDetailMetric] = []

        for order in self._get_sales_orders(orders):
            created_at = self._parse_datetime(order.get("createdAt"))
            items = order.get("items")

            if not isinstance(items, list):
                continue

            for item in items:
                if not isinstance(item, dict):
                    continue

                quantity = self._to_int(item.get("quantity"))
                product_total = self._to_decimal(
                    self._get_first_value(item, ("subtotal", "total", "lineTotal"))
                )
                unit_price = self._to_decimal(
                    self._get_first_value(item, ("unitPrice", "price", "priceSnapshot"))
                )

                if unit_price <= 0 and quantity > 0:
                    unit_price = product_total / quantity

                details.append(
                    SoldProductDetailMetric(
                        order_folio=self._get_order_folio(order),
                        date=created_at.date().isoformat() if created_at else "",
                        branch=self._get_first_text(
                            order,
                            ("branchName", "branchNameSnapshot", "branchId", "branch_id"),
                            "No disponible"
                        ),
                        product=self._get_item_product_name(item) or "No disponible",
                        quantity=quantity,
                        unit_price=self._to_float(unit_price),
                        product_total=self._to_float(product_total),
                        modifiers=self._get_item_modifiers(item),
                        order_status=self._normalize_status(order.get("status"))
                    )
                )

        return details

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

    def _index_payments_by_order_id(
        self,
        payments: list[dict[str, Any]]
    ) -> dict[str, dict[str, Any]]:
        indexed: dict[str, dict[str, Any]] = {}

        for payment in payments:
            order_id = self._get_first_text(payment, ("orderId", "order_id"), "")
            if order_id:
                indexed[order_id] = payment

        return indexed

    def _get_payment_method(
        self,
        order: dict[str, Any],
        payment: dict[str, Any]
    ) -> str:
        provider = self._get_first_text(payment, ("provider",), "")

        if provider:
            if self._is_cash_provider(self._normalize_text(provider, "")):
                return "Efectivo"
            if self._is_online_provider(self._normalize_text(provider, "")):
                return "Online"
            return provider

        return self._get_first_text(
            order,
            ("paymentMethod", "payment_method"),
            "No disponible"
        )

    def _get_payment_status(
        self,
        order: dict[str, Any],
        payment: dict[str, Any]
    ) -> str:
        return self._get_first_text(
            payment or order,
            ("status", "paymentStatus", "payment_status"),
            "No disponible"
        )

    def _get_payment_report_datetime(self, payment: dict[str, Any]) -> datetime | None:
        return self._parse_datetime(
            self._get_first_value(payment, ("approvedAt", "updatedAt", "createdAt"))
        )

    def _get_predominant_payment_method(
        self,
        payment_summary: PaymentSummary
    ) -> str | None:
        if not payment_summary.available or payment_summary.total_payments == 0:
            return None

        if payment_summary.cash > payment_summary.online:
            return "Efectivo"

        if payment_summary.online > payment_summary.cash:
            return "Online"

        return None

    def _is_approved_status(self, status: str) -> bool:
        return status in {"APPROVED", "PAID", "PAGADO"}

    def _is_pending_status(self, status: str) -> bool:
        return status == "PENDING"

    def _is_rejected_or_cancelled_status(self, status: str) -> bool:
        return status in {"REJECTED", "CANCELLED", "CANCELED", "FAILED"}

    def _is_cash_provider(self, provider: str) -> bool:
        return provider in {"CASH", "EFECTIVO"}

    def _is_online_provider(self, provider: str) -> bool:
        return provider in {"PAYPAL", "ONLINE", "CARD", "TARJETA", "SIMULATED"}

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

    def _get_sales_orders(
        self,
        orders: list[dict[str, Any]]
    ) -> list[dict[str, Any]]:
        return [
            order for order in orders
            if self._normalize_status(order.get("status")) != "CANCELLED"
        ]

    def _get_item_product_name(self, item: dict[str, Any]) -> str:
        for key in ("productNameSnapshot", "productName", "name"):
            value = item.get(key)

            if value:
                return str(value)

        return ""

    def _get_item_modifiers(self, item: dict[str, Any]) -> str:
        modifiers = self._get_first_value(
            item,
            ("modifiers", "selectedModifiers", "modifierOptions", "extras")
        )

        if not isinstance(modifiers, list):
            return ""

        names: list[str] = []
        for modifier in modifiers:
            if isinstance(modifier, dict):
                name = self._get_first_text(
                    modifier,
                    ("name", "optionName", "modifierName", "label"),
                    ""
                )
                if name:
                    names.append(name)
            elif modifier:
                names.append(str(modifier))

        return "; ".join(names)

    def _get_order_id(self, order: dict[str, Any]) -> str:
        return self._get_first_text(order, ("id", "_id", "orderId"), "")

    def _get_order_folio(self, order: dict[str, Any]) -> str:
        return self._get_first_text(
            order,
            ("folio", "shortId", "orderNumber", "id", "_id"),
            "No disponible"
        )

    def _get_customer_name(self, order: dict[str, Any]) -> str:
        for key in ("customerName", "customerNameSnapshot", "clientName"):
            value = order.get(key)
            if value:
                return str(value)

        customer = order.get("customer") or order.get("client")
        if isinstance(customer, dict):
            return self._get_first_text(customer, ("name", "fullName"), "No disponible")

        return "No disponible"

    def _get_branch_id(self, document: dict[str, Any]) -> str:
        branch_id = self._get_first_value(
            document,
            ("branchId", "branch_id", "branchID", "branch")
        )

        if isinstance(branch_id, dict):
            return self._get_first_text(branch_id, ("id", "_id"), "")

        return str(branch_id).strip() if branch_id else ""

    def _get_branch_name(
        self,
        branches: list[dict[str, Any]],
        branch_id: str
    ) -> str:
        for branch in branches:
            if self._get_first_text(branch, ("id", "_id"), "") == branch_id:
                return self._get_first_text(branch, ("name",), branch_id)

        return branch_id

    def _get_branch_name_from_orders(
        self,
        orders: list[dict[str, Any]],
        branch_id: str
    ) -> str | None:
        for order in orders:
            if self._get_branch_id(order) == branch_id:
                return self._get_first_text(
                    order,
                    ("branchName", "branchNameSnapshot"),
                    ""
                ) or None

        return None

    def _count_sold_products(self, orders: list[dict[str, Any]]) -> int:
        return sum(
            self._count_order_items(order)
            for order in self._get_sales_orders(orders)
        )

    def _count_order_items(self, order: dict[str, Any]) -> int:
        items = order.get("items")

        if not isinstance(items, list):
            return 0

        count = 0
        for item in items:
            if isinstance(item, dict):
                quantity = self._to_int(item.get("quantity"))
                count += quantity if quantity > 0 else 1
            else:
                count += 1

        return count

    def _get_first_value(self, document: dict[str, Any], keys: tuple[str, ...]) -> Any:
        for key in keys:
            value = document.get(key)

            if value is not None and value != "":
                return value

        return None

    def _get_first_text(
        self,
        document: dict[str, Any],
        keys: tuple[str, ...],
        default: str
    ) -> str:
        value = self._get_first_value(document, keys)

        if value is None:
            return default

        text = str(value).strip()
        return text if text else default

    def _parse_datetime(self, value: Any) -> datetime | None:
        if value is None:
            return None

        text = str(value).strip()

        if not text:
            return None

        try:
            return datetime.fromisoformat(text.replace("Z", "+00:00"))
        except ValueError:
            return None

    def _to_naive(self, value: datetime) -> datetime:
        return value.replace(tzinfo=None)

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

    def _to_optional_float(self, value: Any) -> float | None:
        if value is None or value == "":
            return None

        return self._to_float(self._to_decimal(value))

    def _to_int(self, value: Any) -> int:
        try:
            return int(value)
        except (TypeError, ValueError):
            return 0

    def _to_float(self, value: Decimal) -> float:
        return float(round(value, 2))
