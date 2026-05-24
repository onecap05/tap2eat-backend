namespace FinanceService.Exceptions;

public sealed class PayPalPaymentException : FinanceException
{
    public PayPalPaymentException(string message, int statusCode = StatusCodes.Status502BadGateway)
        : base("PAYPAL_PAYMENT_ERROR", message, statusCode)
    {
    }
}
