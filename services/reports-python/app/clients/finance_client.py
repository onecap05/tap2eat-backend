from typing import Any

import httpx

from app.core.config import settings


class FinanceClient:
    def __init__(self) -> None:
        self.base_url = settings.finance_service_base_url.rstrip("/")
        self.timeout = settings.request_timeout_seconds

    async def get_payments_by_restaurant(
        self,
        restaurant_id: str,
        authorization_header: str | None = None
    ) -> list[dict[str, Any]]:
        headers = self._build_headers(authorization_header)
        url = f"{self.base_url}/api/payments/restaurant/{restaurant_id}"

        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.get(url, headers=headers)
            response.raise_for_status()

        body = response.json()

        if isinstance(body, list):
            return body

        return []

    def _build_headers(self, authorization_header: str | None) -> dict[str, str]:
        headers: dict[str, str] = {}

        if authorization_header:
            headers["Authorization"] = authorization_header

        return headers
