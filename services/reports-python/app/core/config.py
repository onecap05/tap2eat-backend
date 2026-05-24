from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "Tap2Eat Report Service"
    api_prefix: str = "/api/reports"

    order_service_base_url: str = "http://localhost:8085"
    catalog_service_base_url: str = "http://localhost:8082"

    internal_service_token: str = "tap2eat-internal-dev-token"
    request_timeout_seconds: float = 10.0

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore"
    )


settings = Settings()