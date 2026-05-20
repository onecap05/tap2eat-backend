using OrderService.Domain.Documents;
using OrderService.Domain.Embedded;
using OrderService.Domain.Enums;
using OrderService.Dtos.Requests;
using OrderService.Dtos.Responses;

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
            Items = document.Items.Select(ToResponseItem).ToList(),
            Subtotal = document.Subtotal,
            Total = document.Total,
            Status = document.Status,
            Notes = document.Notes,
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
}