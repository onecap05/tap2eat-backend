from pydantic import BaseModel, ConfigDict


def to_camel(value: str) -> str:
    parts = value.split("_")
    return parts[0] + "".join(word.capitalize() for word in parts[1:])


class CamelModel(BaseModel):
    model_config = ConfigDict(
        alias_generator=to_camel,
        populate_by_name=True
    )


class StatusMetric(CamelModel):
    status: str
    total: int


class ProductTypeMetric(CamelModel):
    product_type: str
    total: int


class OrdersReport(CamelModel):
    total_orders: int
    sales_order_count: int
    total_sales: float
    delivered_sales: float
    average_ticket: float

    created_orders: int
    accepted_orders: int
    preparing_orders: int
    ready_orders: int
    delivered_orders: int
    cancelled_orders: int

    orders_by_status: list[StatusMetric]


class CatalogReport(CamelModel):
    total_products: int
    available_products: int
    paused_products: int
    simple_products: int
    customizable_products: int
    featured_products: int
    products_with_custom_schedule: int
    total_categories: int
    active_categories: int
    products_by_type: list[ProductTypeMetric]
    products_by_status: list[StatusMetric]


class OwnerDashboardResponse(CamelModel):
    restaurant_id: str
    orders: OrdersReport
    catalog: CatalogReport


class OwnerAnalyticsSummary(CamelModel):
    total_sales: float
    total_orders: int
    average_ticket: float
    delivered_orders: int
    cancelled_orders: int
    cancellation_rate: float
    delivered_sales: float
    total_products_sold: int
    predominant_payment_method: str | None = None


class SalesByDayMetric(CamelModel):
    date: str
    total_sales: float
    total_orders: int
    average_ticket: float


class TopProductMetric(CamelModel):
    product: str
    quantity_sold: int
    estimated_sales: float
    sales_percentage: float | None = None


class OrdersByHourMetric(CamelModel):
    hour: str
    total_orders: int
    total_sales: float


class PaymentSummary(CamelModel):
    available: bool
    total_payments: int = 0
    total_approved: float
    cash: float
    online: float
    pending: int
    rejected_or_cancelled: int
    approved_payments: int = 0
    pending_payments: int = 0
    cash_amount_received: float = 0
    cash_change_amount: float = 0
    message: str | None = None


class OwnerAnalyticsMetadata(CamelModel):
    restaurant_name: str
    restaurant_rfc: str | None = None
    branch_id: str | None = None
    branch_name: str
    branch_filter_note: str | None = None


class OrderDetailMetric(CamelModel):
    order_folio: str
    date: str
    time: str
    branch: str
    customer: str
    order_status: str
    payment_method: str
    payment_status: str
    subtotal: float | None = None
    total: float
    amount_received: float | None = None
    change_amount: float | None = None
    payment_provider: str
    payment_reference: str
    items_count: int
    notes: str


class SoldProductDetailMetric(CamelModel):
    order_folio: str
    date: str
    branch: str
    product: str
    quantity: int
    unit_price: float
    product_total: float
    modifiers: str
    order_status: str


class OwnerAnalyticsReport(CamelModel):
    restaurant_id: str
    metadata: OwnerAnalyticsMetadata
    summary: OwnerAnalyticsSummary
    sales_by_day: list[SalesByDayMetric]
    top_products: list[TopProductMetric]
    orders_by_hour: list[OrdersByHourMetric]
    orders_by_status: list[StatusMetric]
    payment_summary: PaymentSummary
    order_details: list[OrderDetailMetric]
    sold_product_details: list[SoldProductDetailMetric]
    catalog: CatalogReport
