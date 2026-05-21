using MongoDB.Bson;
using OrderService.Domain.Documents;
using OrderService.Domain.Embedded;
using OrderService.Domain.Enums;
using OrderService.Dtos.Requests;
using OrderService.Integrations.Catalog.Dtos;

namespace OrderService.Tests.TestData;

internal static class OrderTestData
{
    public static CreateOrderRequest CreateOrderRequest(
        string customerAccountId = "customer-1",
        string restaurantId = "restaurant-1",
        string branchId = "branch-1")
    {
        return new CreateOrderRequest
        {
            CustomerAccountId = customerAccountId,
            RestaurantId = restaurantId,
            BranchId = branchId,
            Items =
            [
                new CreateOrderItemRequest
                {
                    ProductId = "product-1",
                    ProductNameSnapshot = "Taco",
                    Quantity = 2,
                    UnitPriceSnapshot = 50,
                    SelectedModifierOptionIds = [],
                    SelectedModifiers = []
                }
            ],
            Notes = "No onion"
        };
    }

    public static CreateOrderRequest CreateOrderRequestWithModifier()
    {
        return new CreateOrderRequest
        {
            CustomerAccountId = "customer-1",
            RestaurantId = "restaurant-1",
            BranchId = "branch-1",
            Items =
            [
                new CreateOrderItemRequest
                {
                    ProductId = "product-1",
                    ProductNameSnapshot = "Burger",
                    Quantity = 2,
                    UnitPriceSnapshot = 100,
                    SelectedModifierOptionIds = ["option-1"],
                    SelectedModifiers =
                    [
                        new SelectedModifierRequest
                        {
                            ModifierGroupId = "group-1",
                            ModifierGroupName = "Extras",
                            ModifierOptionId = "option-1",
                            ModifierOptionName = "Cheese",
                            PriceAdjustment = 15
                        }
                    ]
                }
            ]
        };
    }

    public static OrderDocument OrderDocument(
        string? id = null,
        string customerAccountId = "customer-1",
        string restaurantId = "restaurant-1",
        OrderStatus status = OrderStatus.Created,
        DateTime? createdAt = null)
    {
        var now = createdAt ?? DateTime.UtcNow;

        return new OrderDocument
        {
            Id = id ?? ObjectId.GenerateNewId().ToString(),
            CustomerAccountId = customerAccountId,
            RestaurantId = restaurantId,
            BranchId = "branch-1",
            Items =
            [
                new OrderItem
                {
                    ProductId = "product-1",
                    ProductNameSnapshot = "Taco",
                    Quantity = 1,
                    UnitPriceSnapshot = 50,
                    SelectedModifiers = [],
                    Subtotal = 50
                }
            ],
            Subtotal = 50,
            Total = 50,
            Status = status,
            Notes = "No onion",
            CreatedAt = now,
            UpdatedAt = now
        };
    }

    public static ValidateOrderResponse ValidatedOrderResponse(
        string restaurantId = "restaurant-1",
        string branchId = "branch-1",
        string productName = "Catalog Taco",
        decimal unitPrice = 75,
        decimal modifierPrice = 0)
    {
        var modifiers = modifierPrice > 0
            ?
            [
                new ValidatedModifierResponse
                {
                    ModifierGroupId = "group-catalog",
                    ModifierGroupName = "Catalog Extras",
                    ModifierOptionId = "option-1",
                    ModifierOptionName = "Catalog Cheese",
                    PriceAdjustment = modifierPrice
                }
            ]
            : new List<ValidatedModifierResponse>();
        var quantity = 2;
        var subtotal = (unitPrice + modifierPrice) * quantity;

        return new ValidateOrderResponse
        {
            RestaurantId = restaurantId,
            BranchId = branchId,
            Valid = true,
            Items =
            [
                new ValidatedOrderItemResponse
                {
                    ProductId = "product-1",
                    ProductName = productName,
                    Quantity = quantity,
                    UnitPrice = unitPrice,
                    SelectedModifiers = modifiers,
                    Subtotal = subtotal
                }
            ],
            Subtotal = subtotal,
            Total = subtotal
        };
    }
}
