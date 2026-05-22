using OrderService.Dtos.Requests;
using OrderService.Exceptions;

namespace OrderService.Validation;

public static class OrderQueryValidator
{
    public static void Validate(OrderQueryRequest query)
    {
        if (query.From.HasValue && query.To.HasValue && query.From.Value > query.To.Value)
        {
            throw new OrderValidationException("From date must be less than or equal to To date.");
        }
    }
}
