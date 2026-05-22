namespace FinanceService.Exceptions;

public sealed class PaymentNotFoundException : FinanceException
{
    public PaymentNotFoundException(string id)
        : base("PAYMENT_NOT_FOUND", $"Payment '{id}' was not found.", StatusCodes.Status404NotFound)
    {
    }
}
