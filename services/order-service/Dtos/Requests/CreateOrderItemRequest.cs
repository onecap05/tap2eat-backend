using System.ComponentModel.DataAnnotations;

namespace OrderService.Dtos.Requests;

public sealed class CreateOrderItemRequest
{
    [Required]
    public string ProductId { get; set; } = string.Empty;

    public string ProductNameSnapshot { get; set; } = string.Empty;

    [Range(1, int.MaxValue)]
    public int Quantity { get; set; }

    [Range(typeof(decimal), "0", "79228162514264337593543950335")]
    public decimal UnitPriceSnapshot { get; set; }

    public List<SelectedModifierRequest> SelectedModifiers { get; set; } = [];

    public List<string> SelectedModifierOptionIds { get; set; } = [];
}
