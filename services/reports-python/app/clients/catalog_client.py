from typing import Any

import httpx

from app.core.config import settings


class CatalogClient:
    def __init__(self) -> None:
        self.base_url = settings.catalog_service_base_url.rstrip("/")
        self.timeout = settings.request_timeout_seconds

    async def get_products_by_restaurant(
        self,
        restaurant_id: str
    ) -> list[dict[str, Any]]:
        return await self._get_list(f"/api/products/restaurant/{restaurant_id}")

    async def get_categories_by_restaurant(
        self,
        restaurant_id: str
    ) -> list[dict[str, Any]]:
        return await self._get_list(f"/api/categories/restaurant/{restaurant_id}")

    async def _get_list(self, path: str) -> list[dict[str, Any]]:
        url = f"{self.base_url}{path}"

        headers = {}

        if settings.internal_service_token:
            headers["X-Internal-Service-Token"] = settings.internal_service_token

        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.get(url, headers=headers)
            response.raise_for_status()

        body = response.json()

        if isinstance(body, list):
            return body

        return []