using System.Net;
using System.Net.Http.Json;
using FluentAssertions;
using Microsoft.Extensions.Options;
using OrderService.Exceptions;
using OrderService.Integrations.Catalog;
using OrderService.Integrations.Catalog.Dtos;
using System.Text.Json;
using OrderService.Tests.Fakes;
using OrderService.Tests.TestData;

namespace OrderService.Tests.Integrations.Catalog;

public sealed class CatalogClientTests
{
    [Fact]
    public async Task ValidateOrderAsync_ShouldSendExpectedRequest()
    {
        var handler = FakeHttpMessageHandler.Json(HttpStatusCode.OK, OrderTestData.ValidatedOrderResponse());
        var client = CreateClient(handler);
        var request = OrderTestData.CreateOrderRequestWithModifier();

        await client.ValidateOrderAsync(request);

        handler.LastRequest!.RequestUri!.PathAndQuery.Should().Be("/internal/catalog/orders/validate");

        handler.LastRequest.Headers
            .GetValues("X-Internal-Service-Token")
            .Should()
            .ContainSingle("test-internal-token");

        var body = JsonSerializer.Deserialize<ValidateOrderRequest>(
    handler.LastRequestBody!,
    new JsonSerializerOptions
    {
        PropertyNameCaseInsensitive = true
    });

        body!.RestaurantId.Should().Be(request.RestaurantId);
        body.BranchId.Should().Be(request.BranchId);
        body.Items.Should().ContainSingle();
        body.Items[0].ProductId.Should().Be("product-1");
        body.Items[0].Quantity.Should().Be(2);
        body.Items[0].SelectedModifierOptionIds.Should().ContainSingle("option-1");
    }

    [Fact]
    public async Task ValidateOrderAsync_ShouldInterpretValidResponse()
    {
        var expected = OrderTestData.ValidatedOrderResponse(unitPrice: 75, modifierPrice: 15);
        var client = CreateClient(FakeHttpMessageHandler.Json(HttpStatusCode.OK, expected));

        var response = await client.ValidateOrderAsync(OrderTestData.CreateOrderRequest());

        response.Valid.Should().BeTrue();
        response.Items[0].ProductName.Should().Be(expected.Items[0].ProductName);
        response.Total.Should().Be(180);
    }

    [Theory]
    [InlineData(HttpStatusCode.BadRequest)]
    [InlineData(HttpStatusCode.Conflict)]
    public async Task ValidateOrderAsync_WhenCatalogRejectsRequest_ShouldThrowValidation(HttpStatusCode statusCode)
    {
        var client = CreateClient(FakeHttpMessageHandler.Json(statusCode, new { message = "Invalid" }));

        var act = async () => await client.ValidateOrderAsync(OrderTestData.CreateOrderRequest());

        await act.Should().ThrowAsync<CatalogValidationException>();
    }

    [Fact]
    public async Task ValidateOrderAsync_WhenCatalogReturnsServerError_ShouldThrowUnavailable()
    {
        var client = CreateClient(FakeHttpMessageHandler.Json(HttpStatusCode.InternalServerError, null));

        var act = async () => await client.ValidateOrderAsync(OrderTestData.CreateOrderRequest());

        await act.Should().ThrowAsync<CatalogServiceUnavailableException>();
    }

    [Fact]
    public async Task ValidateOrderAsync_WhenCatalogTimesOut_ShouldThrowUnavailable()
    {
        var handler = new FakeHttpMessageHandler((_, _) => throw new TaskCanceledException());
        var client = CreateClient(handler);

        var act = async () => await client.ValidateOrderAsync(OrderTestData.CreateOrderRequest());

        await act.Should().ThrowAsync<CatalogServiceUnavailableException>();
    }

    private static CatalogClient CreateClient(HttpMessageHandler handler)
    {
        var httpClient = new HttpClient(handler)
        {
            BaseAddress = new Uri("http://catalog-service")
        };

        var options = Options.Create(new CatalogServiceSettings
        {
            BaseUrl = "http://catalog-service",
            InternalServiceToken = "test-internal-token"
        });

        return new CatalogClient(httpClient, options);
    }
}