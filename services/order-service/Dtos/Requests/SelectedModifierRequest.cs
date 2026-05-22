using System.ComponentModel.DataAnnotations;

namespace OrderService.Dtos.Requests;

public sealed class SelectedModifierRequest
{
    public string? ModifierGroupId { get; set; }

    [Required]
    public string ModifierGroupName { get; set; } = string.Empty;

    public string? ModifierOptionId { get; set; }

    [Required]
    public string ModifierOptionName { get; set; } = string.Empty;

    [Range(typeof(decimal), "0", "79228162514264337593543950335")]
    public decimal PriceAdjustment { get; set; }
}