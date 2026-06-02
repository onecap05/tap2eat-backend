using System.ComponentModel.DataAnnotations;
using OrderService.Domain.Enums;

namespace OrderService.Dtos.Requests;

public sealed class UpdateOrderStatusRequest
{
    [Required]
    public OrderStatus? Status { get; set; }

    public int? EstimatedPreparationMinutes { get; set; }
}
