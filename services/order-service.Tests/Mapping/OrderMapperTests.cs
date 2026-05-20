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

        var document = OrderMapper.ToDocument(request);

        document.Items.Should().ContainSingle();
        document.Items[0].Subtotal.Should().Be(100);
    }

    [Fact]
    public void ToDocument_ShouldCalculateProductSubtotalWithModifiers()
    {
        var request = OrderTestData.CreateOrderRequestWithModifier();

        var document = OrderMapper.ToDocument(request);

        document.Items.Should().ContainSingle();
        document.Items[0].Subtotal.Should().Be(230);
    }

    [Fact]
    public void ToDocument_ShouldCalculateOrderTotal()
    {
        var request = OrderTestData.CreateOrderRequestWithModifier();

        var document = OrderMapper.ToDocument(request);

        document.Subtotal.Should().Be(230);
        document.Total.Should().Be(230);
    }

    [Fact]
    public void ToResponse_ShouldMapStatus()
    {
        var document = OrderTestData.OrderDocument(status: OrderStatus.Accepted);

        var response = OrderMapper.ToResponse(document);

        response.Status.Should().Be(OrderStatus.Accepted);
    }
}
