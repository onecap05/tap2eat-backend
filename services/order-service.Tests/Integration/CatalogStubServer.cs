using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Hosting.Server.Features;
using Microsoft.Extensions.DependencyInjection;
using OrderService.Integrations.Catalog.Dtos;
using OrderService.Tests.TestData;

namespace OrderService.Tests.Integration;

public sealed class CatalogStubServer : IAsyncDisposable
{
    private readonly WebApplication _app;
    private ValidateOrderResponse _response = OrderTestData.ValidatedOrderResponse(unitPrice: 50);
    private int _statusCode = StatusCodes.Status200OK;

    public CatalogStubServer()
    {
        var builder = WebApplication.CreateBuilder();
        builder.WebHost.UseKestrel().UseUrls("http://127.0.0.1:0");

        _app = builder.Build();
        _app.MapPost("/internal/catalog/orders/validate", async context =>
        {
            Requests++;
            LastRequest = await context.Request.ReadFromJsonAsync<ValidateOrderRequest>();

            context.Response.StatusCode = _statusCode;

            if (_statusCode is >= 200 and < 300)
            {
                await context.Response.WriteAsJsonAsync(_response);
                return;
            }

            await context.Response.WriteAsJsonAsync(new { message = "Catalog validation failed." });
        });
    }

    public string BaseUrl { get; private set; } = string.Empty;

    public int Requests { get; private set; }

    public ValidateOrderRequest? LastRequest { get; private set; }

    public async Task StartAsync()
    {
        await _app.StartAsync();
        BaseUrl = _app.Urls.FirstOrDefault()
            ?? _app.Services.GetRequiredService<Microsoft.AspNetCore.Hosting.Server.IServer>()
                .Features.Get<IServerAddressesFeature>()!
                .Addresses.First();
    }

    public void Reset()
    {
        _response = OrderTestData.ValidatedOrderResponse(unitPrice: 50);
        _statusCode = StatusCodes.Status200OK;
        Requests = 0;
        LastRequest = null;
    }

    public void RespondWith(ValidateOrderResponse response)
    {
        _response = response;
        _statusCode = StatusCodes.Status200OK;
    }

    public void RespondWithStatus(int statusCode)
    {
        _statusCode = statusCode;
    }

    public async ValueTask DisposeAsync()
    {
        await _app.DisposeAsync();
    }
}
