FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

COPY services/recommendation-service/RecommendationService.csproj services/recommendation-service/
RUN dotnet restore services/recommendation-service/RecommendationService.csproj

COPY services/recommendation-service services/recommendation-service
WORKDIR /src/services/recommendation-service
RUN dotnet publish RecommendationService.csproj -c Release -o /app/publish

FROM mcr.microsoft.com/dotnet/aspnet:10.0
WORKDIR /app

COPY --from=build /app/publish .

ENV ASPNETCORE_HTTP_PORTS=8086
EXPOSE 8086

ENTRYPOINT ["dotnet", "RecommendationService.dll"]