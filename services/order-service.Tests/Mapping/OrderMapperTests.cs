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
    public void ToDocument_FromRequest_ShouldMapEmptyModifiersAndNotes()
    {
        var request = OrderTestData.CreateOrderRequest();

        var document = OrderMapper.ToDocument(request);

        document.CustomerAccountId.Should().Be(request.CustomerAccountId);
        document.RestaurantId.Should().Be(request.RestaurantId);
        document.BranchId.Should().Be(request.BranchId);
        document.Notes.Should().Be("No onion");
        document.Items.Should().ContainSingle();
        document.Items[0].SelectedModifiers.Should().BeEmpty();
        document.Subtotal.Should().Be(100);
        document.Total.Should().Be(100);
        document.Status.Should().Be(OrderStatus.Created);
    }

    [Fact]
    public void ToDocument_FromRequest_ShouldMapCompleteModifiers()
    {
        var request = OrderTestData.CreateOrderRequestWithModifier();

        var document = OrderMapper.ToDocument(request);

        document.Items.Should().ContainSingle();
        document.Items[0].SelectedModifiers.Should().ContainSingle();
        document.Items[0].SelectedModifiers[0].ModifierGroupId.Should().Be("group-1");
        document.Items[0].SelectedModifiers[0].ModifierGroupName.Should().Be("Extras");
        document.Items[0].SelectedModifiers[0].ModifierOptionId.Should().Be("option-1");
        document.Items[0].SelectedModifiers[0].ModifierOptionName.Should().Be("Cheese");
        document.Items[0].SelectedModifiers[0].PriceAdjustment.Should().Be(15);
        document.Total.Should().Be(230);
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
    public void ToResponse_ShouldMapCompleteModifiers()
    {
        var document = OrderTestData.OrderDocument();
        document.Items[0].SelectedModifiers =
        [
            new OrderService.Domain.Embedded.SelectedModifier
            {
                ModifierGroupId = "group-1",
                ModifierGroupName = "Extras",
                ModifierOptionId = "option-1",
                ModifierOptionName = "Cheese",
                PriceAdjustment = 15
            }
        ];

        var response = OrderMapper.ToResponse(document);

        response.Items[0].SelectedModifiers.Should().ContainSingle();
        response.Items[0].SelectedModifiers[0].ModifierGroupId.Should().Be("group-1");
        response.Items[0].SelectedModifiers[0].ModifierGroupName.Should().Be("Extras");
        response.Items[0].SelectedModifiers[0].ModifierOptionId.Should().Be("option-1");
        response.Items[0].SelectedModifiers[0].ModifierOptionName.Should().Be("Cheese");
        response.Items[0].SelectedModifiers[0].PriceAdjustment.Should().Be(15);
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

    [Fact]
    public void ToPublicTrackingResponse_WhenOrderIdIsShort_ShouldUseUppercaseOrderId()
    {
        var document = OrderTestData.OrderDocument(id: "abc123");

        var response = OrderMapper.ToPublicTrackingResponse(document);

        response.ShortOrderId.Should().Be("ABC123");
    }

    [Fact]
    public void ToPublicTrackingResponse_WhenOrderIdIsMissing_ShouldUseEmptyShortOrderId()
    {
        var document = OrderTestData.OrderDocument();
        document.Id = null;

        var response = OrderMapper.ToPublicTrackingResponse(document);

        response.ShortOrderId.Should().BeEmpty();
    }

    [Fact]
    public void ToPublicTrackingResponse_ShouldMapPublicModifiersWithoutInternalIds()
    {
        var document = OrderTestData.OrderDocument();
        document.Items[0].SelectedModifiers =
        [
            new OrderService.Domain.Embedded.SelectedModifier
            {
                ModifierGroupId = "group-1",
                ModifierGroupName = "Extras",
                ModifierOptionId = "option-1",
                ModifierOptionName = "Cheese",
                PriceAdjustment = 15
            }
        ];

        var response = OrderMapper.ToPublicTrackingResponse(document);

        response.Items[0].SelectedModifiers.Should().ContainSingle();
        response.Items[0].SelectedModifiers[0].ModifierGroupName.Should().Be("Extras");
        response.Items[0].SelectedModifiers[0].ModifierOptionName.Should().Be("Cheese");
        response.Items[0].SelectedModifiers[0].PriceAdjustment.Should().Be(15);
    }
}
