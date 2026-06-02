using OrderService.Domain.Documents;
using OrderService.Domain.Embedded;
using OrderService.Domain.Enums;
using OrderService.Dtos.Requests;
using OrderService.Dtos.Responses;
using OrderService.Integrations.Catalog.Dtos;

namespace OrderService.Mapping;

public static class OrderMapper
{
    public static OrderDocument ToDocument(CreateOrderRequest request)
    {
        var now = DateTime.UtcNow;

        var items = request.Items
            .Select(ToDocumentItem)
            .ToList();

        var subtotal = items.Sum(item => item.Subtotal);

        return new OrderDocument
        {
            CustomerAccountId = request.CustomerAccountId,
            RestaurantId = request.RestaurantId,
            BranchId = request.BranchId,
            PublicTrackingCode = GeneratePublicTrackingCode(),
            Items = items,
            Subtotal = subtotal,
            Total = subtotal,
            Status = OrderStatus.Created,
            Notes = request.Notes,
            CreatedAt = now,
            UpdatedAt = now
        };
    }

    public static OrderDocument ToDocument(
        CreateOrderRequest request,
        ValidateOrderResponse validatedOrder)
    {
        var now = DateTime.UtcNow;

        var items = validatedOrder.Items
            .Select(ToDocumentItem)
            .ToList();
        var subtotal = items.Sum(item => item.Subtotal);

        return new OrderDocument
        {
            CustomerAccountId = request.CustomerAccountId,
            RestaurantId = validatedOrder.RestaurantId,
            BranchId = validatedOrder.BranchId,
            PublicTrackingCode = GeneratePublicTrackingCode(),
            Items = items,
            Subtotal = subtotal,
            Total = subtotal,
            Status = OrderStatus.Created,
            Notes = request.Notes,
            CreatedAt = now,
            UpdatedAt = now
        };
    }

    public static OrderResponse ToResponse(OrderDocument document)
    {
        return new OrderResponse
        {
            Id = document.Id ?? string.Empty,
            CustomerAccountId = document.CustomerAccountId,
            RestaurantId = document.RestaurantId,
            BranchId = document.BranchId,
            PublicTrackingCode = document.PublicTrackingCode,
            Items = document.Items.Select(ToResponseItem).ToList(),
            Subtotal = document.Subtotal,
            Total = document.Total,
            Status = document.Status,
            EstimatedPreparationMinutes = document.EstimatedPreparationMinutes,
            EstimatedReadyAt = document.EstimatedReadyAt,
            Notes = document.Notes,
            CreatedAt = document.CreatedAt,
            UpdatedAt = document.UpdatedAt
        };
    }

    public static PublicOrderTrackingResponse ToPublicTrackingResponse(OrderDocument document)
    {
        return new PublicOrderTrackingResponse
        {
            PublicTrackingCode = document.PublicTrackingCode ?? string.Empty,
            ShortOrderId = GetShortOrderId(document.Id),
            Status = document.Status,
            EstimatedPreparationMinutes = document.EstimatedPreparationMinutes,
            EstimatedReadyAt = document.EstimatedReadyAt,
            RestaurantNameSnapshot = null,
            BranchNameSnapshot = null,
            Items = document.Items.Select(ToPublicTrackingItemResponse).ToList(),
            Subtotal = document.Subtotal,
            Total = document.Total,
            CreatedAt = document.CreatedAt,
            UpdatedAt = document.UpdatedAt
        };
    }

    private static OrderItem ToDocumentItem(CreateOrderItemRequest request)
    {
        var modifiers = request.SelectedModifiers
            .Select(ToDocumentModifier)
            .ToList();

        var modifiersTotal = modifiers.Sum(modifier => modifier.PriceAdjustment);
        var subtotal = (request.UnitPriceSnapshot + modifiersTotal) * request.Quantity;

        return new OrderItem
        {
            ProductId = request.ProductId,
            ProductNameSnapshot = request.ProductNameSnapshot,
            Quantity = request.Quantity,
            UnitPriceSnapshot = request.UnitPriceSnapshot,
            SelectedModifiers = modifiers,
            Subtotal = subtotal
        };
    }

    private static OrderItem ToDocumentItem(ValidatedOrderItemResponse item)
    {
        var modifiers = item.SelectedModifiers
            .Select(ToDocumentModifier)
            .ToList();
        var modifiersTotal = modifiers.Sum(modifier => modifier.PriceAdjustment);
        var subtotal = (item.UnitPrice + modifiersTotal) * item.Quantity;

        return new OrderItem
        {
            ProductId = item.ProductId,
            ProductNameSnapshot = item.ProductName,
            Quantity = item.Quantity,
            UnitPriceSnapshot = item.UnitPrice,
            SelectedModifiers = modifiers,
            Subtotal = subtotal
        };
    }

    private static SelectedModifier ToDocumentModifier(SelectedModifierRequest request)
    {
        return new SelectedModifier
        {
            ModifierGroupId = request.ModifierGroupId,
            ModifierGroupName = request.ModifierGroupName,
            ModifierOptionId = request.ModifierOptionId,
            ModifierOptionName = request.ModifierOptionName,
            PriceAdjustment = request.PriceAdjustment
        };
    }

    private static SelectedModifier ToDocumentModifier(ValidatedModifierResponse response)
    {
        return new SelectedModifier
        {
            ModifierGroupId = response.ModifierGroupId,
            ModifierGroupName = response.ModifierGroupName,
            ModifierOptionId = response.ModifierOptionId,
            ModifierOptionName = response.ModifierOptionName,
            PriceAdjustment = response.PriceAdjustment
        };
    }

    private static OrderItemResponse ToResponseItem(OrderItem item)
    {
        return new OrderItemResponse
        {
            ProductId = item.ProductId,
            ProductNameSnapshot = item.ProductNameSnapshot,
            Quantity = item.Quantity,
            UnitPriceSnapshot = item.UnitPriceSnapshot,
            SelectedModifiers = item.SelectedModifiers.Select(ToResponseModifier).ToList(),
            Subtotal = item.Subtotal
        };
    }

    private static PublicOrderTrackingItemResponse ToPublicTrackingItemResponse(OrderItem item)
    {
        return new PublicOrderTrackingItemResponse
        {
            ProductNameSnapshot = item.ProductNameSnapshot,
            Quantity = item.Quantity,
            UnitPriceSnapshot = item.UnitPriceSnapshot,
            SelectedModifiers = item.SelectedModifiers.Select(ToPublicSelectedModifierResponse).ToList(),
            Subtotal = item.Subtotal
        };
    }

    private static SelectedModifierResponse ToResponseModifier(SelectedModifier modifier)
    {
        return new SelectedModifierResponse
        {
            ModifierGroupId = modifier.ModifierGroupId,
            ModifierGroupName = modifier.ModifierGroupName,
            ModifierOptionId = modifier.ModifierOptionId,
            ModifierOptionName = modifier.ModifierOptionName,
            PriceAdjustment = modifier.PriceAdjustment
        };
    }

    private static PublicSelectedModifierResponse ToPublicSelectedModifierResponse(SelectedModifier modifier)
    {
        return new PublicSelectedModifierResponse
        {
            ModifierGroupName = modifier.ModifierGroupName,
            ModifierOptionName = modifier.ModifierOptionName,
            PriceAdjustment = modifier.PriceAdjustment
        };
    }

    private static string GeneratePublicTrackingCode()
    {
        return Guid.NewGuid().ToString("N");
    }

    private static string GetShortOrderId(string? orderId)
    {
        if (string.IsNullOrWhiteSpace(orderId))
        {
            return string.Empty;
        }

        return orderId.Length <= 8
            ? orderId.ToUpperInvariant()
            : orderId[^8..].ToUpperInvariant();
    }
}
