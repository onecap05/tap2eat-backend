using FinanceService.Domain.Entities;

namespace FinanceService.Messaging.Publishers;

public interface IPaymentEventPublisher
{
    Task PublishPaymentApprovedAsync(Payment payment, CancellationToken cancellationToken = default);

    Task PublishPaymentRejectedAsync(Payment payment, CancellationToken cancellationToken = default);

    Task PublishPaymentCancelledAsync(
        Payment payment,
        string? reason = null,
        CancellationToken cancellationToken = default);
}
