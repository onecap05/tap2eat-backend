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
}
