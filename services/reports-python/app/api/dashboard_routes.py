from fastapi import APIRouter, Header, HTTPException, Query
from httpx import HTTPError, HTTPStatusError

from app.schemas.dashboard_schema import OwnerDashboardResponse
from app.services.dashboard_service import DashboardService

router = APIRouter(prefix="/dashboard", tags=["Dashboard"])


@router.get(
    "/owner/{restaurant_id}",
    response_model=OwnerDashboardResponse
)
async def get_owner_dashboard(
    restaurant_id: str,
    from_date: str | None = Query(default=None, alias="from"),
    to_date: str | None = Query(default=None, alias="to"),
    authorization: str | None = Header(default=None)
) -> OwnerDashboardResponse:
    try:
        dashboard_service = DashboardService()

        return await dashboard_service.get_owner_dashboard(
            restaurant_id=restaurant_id,
            from_date=from_date,
            to_date=to_date,
            authorization_header=authorization
        )
    except HTTPStatusError as error:
        raise HTTPException(
            status_code=502,
            detail=f"Required service returned {error.response.status_code}: {error.response.text}"
        ) from error
    except HTTPError as error:
        raise HTTPException(
            status_code=502,
            detail=f"Could not get data from required services: {str(error)}"
        ) from error