from fastapi import FastAPI

from app.api.dashboard_routes import router as dashboard_router
from app.core.config import settings

app = FastAPI(
    title=settings.app_name,
    version="0.1.0"
)


@app.get(f"{settings.api_prefix}/health")
async def health_check() -> dict[str, str]:
    return {
        "status": "UP",
        "service": "report-service"
    }


app.include_router(
    dashboard_router,
    prefix=settings.api_prefix
)