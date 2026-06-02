using FluentAssertions;
using OrderService.Domain.Enums;
using OrderService.Mapping;
using OrderService.Tests.TestData;

namespace OrderService.Tests.Mapping;

public sealed class OrderMapperTests
{
    [Fact]
    public void ToDocument_ShouldCalculateSimpleProductSubtotal()
    {
        var request = OrderTestData.CreateOrderRequest();
        var validatedOrder = OrderTestData.ValidatedOrderResponse(unitPrice: 50);

        var document = OrderMapper.ToDocument(request, validatedOrder);

        document.Items.Should().ContainSingle();
        document.Items[0].Subtotal.Should().Be(100);
    }

    [Fact]
    public void ToDocument_ShouldCalculateProductSubtotalWithModifiers()
    {
        var request = OrderTestData.CreateOrderRequestWithModifier();
        var validatedOrder = OrderTestData.ValidatedOrderResponse(unitPrice: 100, modifierPrice: 15);

        var document = OrderMapper.ToDocument(request, validatedOrder);

        document.Items.Should().ContainSingle();
        document.Items[0].Subtotal.Should().Be(230);
    }

    [Fact]
    public void ToDocument_ShouldCalculateOrderTotal()
    {
        var request = OrderTestData.CreateOrderRequestWithModifier();
        var validatedOrder = OrderTestData.ValidatedOrderResponse(unitPrice: 100, modifierPrice: 15);

        var document = OrderMapper.ToDocument(request, validatedOrder);

        document.Subtotal.Should().Be(230);
        document.Total.Should().Be(230);
    }

    [Fact]
    public void ToDocument_ShouldGeneratePublicTrackingCode()
    {
        var request = OrderTestData.CreateOrderRequest();
        var validatedOrder = OrderTestData.ValidatedOrderResponse();

        var document = OrderMapper.ToDocument(request, validatedOrder);

        document.PublicTrackingCode.Should().NotBeNullOrWhiteSpace();
        document.PublicTrackingCode.Should().HaveLength(32);
    }

    [Fact]
    public void ToDocument_ShouldMapValidatedCatalogSnapshot()
    {
        var request = OrderTestData.CreateOrderRequest();
        request.Items[0].ProductNameSnapshot = "Client Name";
        request.Items[0].UnitPriceSnapshot = 1;
        var validatedOrder = OrderTestData.ValidatedOrderResponse(
            productName: "Catalog Name",
            unitPrice: 80,
            modifierPrice: 10);

        var document = OrderMapper.ToDocument(request, validatedOrder);

        document.Items[0].ProductNameSnapshot.Should().Be("Catalog Name");
        document.Items[0].UnitPriceSnapshot.Should().Be(80);
        document.Items[0].SelectedModifiers[0].ModifierOptionName.Should().Be("Catalog Cheese");
        document.Total.Should().Be(180);
    }

    [Fact]
    public void ToResponse_ShouldMapStatus()
    {
        var document = OrderTestData.OrderDocument(status: OrderStatus.Accepted);

        var response = OrderMapper.ToResponse(document);

        response.Status.Should().Be(OrderStatus.Accepted);
    }

    [Fact]
    public void ToResponse_ShouldMapPublicTrackingCode()
    {
        var document = OrderTestData.OrderDocument(publicTrackingCode: "track-code-1");

        var response = OrderMapper.ToResponse(document);

        response.PublicTrackingCode.Should().Be("track-code-1");
    }

    [Fact]
    public void ToResponse_ShouldMapEstimatedPreparationTime()
    {
        var estimatedReadyAt = new DateTime(2026, 5, 22, 16, 30, 0, DateTimeKind.Utc);
        var document = OrderTestData.OrderDocument(
            estimatedPreparationMinutes: 20,
            estimatedReadyAt: estimatedReadyAt);

        var response = OrderMapper.ToResponse(document);

        response.EstimatedPreparationMinutes.Should().Be(20);
        response.EstimatedReadyAt.Should().Be(estimatedReadyAt);
    }

    [Fact]
    public void ToPublicTrackingResponse_ShouldNotExposeCustomerAccountIdOrInternalItemIds()
    {
        var document = OrderTestData.OrderDocument(publicTrackingCode: "track-code-1");

        var response = OrderMapper.ToPublicTrackingResponse(document);

        response.PublicTrackingCode.Should().Be("track-code-1");
        response.ShortOrderId.Should().Be(document.Id![^8..].ToUpperInvariant());
        response.Items.Should().ContainSingle();
        response.Items[0].ProductNameSnapshot.Should().Be("Taco");
        response.Items[0].UnitPriceSnapshot.Should().Be(50);
    }

    [Fact]
    public void ToPublicTrackingResponse_ShouldMapEstimatedPreparationTime()
    {
        var estimatedReadyAt = new DateTime(2026, 5, 22, 16, 30, 0, DateTimeKind.Utc);
        var document = OrderTestData.OrderDocument(
            estimatedPreparationMinutes: 20,
            estimatedReadyAt: estimatedReadyAt);

        var response = OrderMapper.ToPublicTrackingResponse(document);

        response.EstimatedPreparationMinutes.Should().Be(20);
        response.EstimatedReadyAt.Should().Be(estimatedReadyAt);
    }
}
