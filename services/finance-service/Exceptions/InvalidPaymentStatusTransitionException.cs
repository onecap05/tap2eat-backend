using FinanceService.Domain.Enums;

namespace FinanceService.Exceptions;

public sealed class InvalidPaymentStatusTransitionException : FinanceException
{
    public InvalidPaymentStatusTransitionException(PaymentStatus currentStatus, PaymentStatus requestedStatus)
        : base(
            "INVALID_PAYMENT_STATUS_TRANSITION",
            $"Payment status cannot change from {currentStatus} to {requestedStatus}.",
            StatusCodes.Status409Conflict)
    {
    }
}
