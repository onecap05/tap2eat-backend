from datetime import date

from fastapi import APIRouter, Header, HTTPException, Query
from fastapi.responses import StreamingResponse
from httpx import HTTPError, HTTPStatusError

from app.schemas.dashboard_schema import OwnerDashboardResponse
from app.services.dashboard_service import DashboardService
from app.services.excel_export_service import ExcelExportService
from app.services.report_date_range import resolve_report_date_range

router = APIRouter(prefix="/dashboard", tags=["Dashboard"])


@router.get(
    "/owner/{restaurant_id}",
    response_model=OwnerDashboardResponse
)
async def get_owner_dashboard(
    restaurant_id: str,
    from_date: date | None = Query(default=None, alias="from"),
    to_date: date | None = Query(default=None, alias="to"),
    authorization: str | None = Header(default=None)
) -> OwnerDashboardResponse:
    try:
        _, _, from_datetime, to_datetime = resolve_report_date_range(
            from_date,
            to_date
        )

        dashboard_service = DashboardService()

        return await dashboard_service.get_owner_dashboard(
            restaurant_id=restaurant_id,
            from_date=from_datetime,
            to_date=to_datetime,
            authorization_header=authorization
        )
    except ValueError as error:
        raise HTTPException(status_code=400, detail=str(error)) from error
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


@router.get("/owner/{restaurant_id}/export")
async def export_owner_dashboard(
    restaurant_id: str,
    from_date: date | None = Query(default=None, alias="from"),
    to_date: date | None = Query(default=None, alias="to"),
    authorization: str | None = Header(default=None)
) -> StreamingResponse:
    try:
        resolved_from_date, resolved_to_date, from_datetime, to_datetime = (
            resolve_report_date_range(from_date, to_date)
        )

        dashboard_service = DashboardService()
        excel_export_service = ExcelExportService()

        dashboard = await dashboard_service.get_owner_dashboard(
            restaurant_id=restaurant_id,
            from_date=from_datetime,
            to_date=to_datetime,
            authorization_header=authorization
        )

        excel_file = excel_export_service.build_owner_dashboard_workbook(
            dashboard=dashboard,
            from_date=resolved_from_date,
            to_date=resolved_to_date
        )

        filename = (
            f"tap2eat-report-{restaurant_id}-"
            f"{resolved_from_date.isoformat()}-"
            f"{resolved_to_date.isoformat()}.xlsx"
        )

        return StreamingResponse(
            excel_file,
            media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            headers={
                "Content-Disposition": f'attachment; filename="{filename}"'
            }
        )
    except ValueError as error:
        raise HTTPException(status_code=400, detail=str(error)) from error
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