namespace FinanceService.Exceptions;

public sealed class FinanceValidationException : FinanceException
{
    public FinanceValidationException(string message)
        : base("FINANCE_VALIDATION_ERROR", message, StatusCodes.Status400BadRequest)
    {
    }
}
