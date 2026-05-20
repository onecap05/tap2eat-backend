FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

COPY services/finance-service/FinanceService.csproj services/finance-service/
RUN dotnet restore services/finance-service/FinanceService.csproj

COPY services/finance-service services/finance-service
WORKDIR /src/services/finance-service
RUN dotnet publish FinanceService.csproj -c Release -o /app/publish

FROM mcr.microsoft.com/dotnet/aspnet:10.0
WORKDIR /app

COPY --from=build /app/publish .

ENV ASPNETCORE_HTTP_PORTS=8083
EXPOSE 8083

ENTRYPOINT ["dotnet", "FinanceService.dll"]