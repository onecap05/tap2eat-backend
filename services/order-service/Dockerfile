FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

COPY services/order-service/OrderService.csproj services/order-service/
RUN dotnet restore services/order-service/OrderService.csproj

COPY services/order-service services/order-service
WORKDIR /src/services/order-service
RUN dotnet publish OrderService.csproj -c Release -o /app/publish

FROM mcr.microsoft.com/dotnet/aspnet:10.0
WORKDIR /app

COPY --from=build /app/publish .

ENV ASPNETCORE_HTTP_PORTS=8085
EXPOSE 8085

ENTRYPOINT ["dotnet", "OrderService.dll"]