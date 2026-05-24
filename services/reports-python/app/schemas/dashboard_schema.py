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