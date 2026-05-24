from typing import Any

import httpx

from app.core.config import settings


class OrderClient:
    def __init__(self) -> None:
        self.base_url = settings.order_service_base_url.rstrip("/")
        self.timeout = settings.request_timeout_seconds

    async def get_orders_by_restaurant(
        self,
        restaurant_id: str,
        from_date: str | None = None,
        to_date: str | None = None
    ) -> list[dict[str, Any]]:
        params: dict[str, str] = {}

        if from_date:
            params["from"] = from_date

        if to_date:
            params["to"] = to_date

        url = f"{self.base_url}/api/orders/restaurant/{restaurant_id}"

        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.get(url, params=params)
            response.raise_for_status()

        body = response.json()

        if isinstance(body, list):
            return body

        return []