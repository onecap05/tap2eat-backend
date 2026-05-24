using FinanceService.Domain.Entities;
using FinanceService.Messaging.Publishers;

namespace FinanceService.Tests.Fakes;

public sealed class FakePaymentEventPublisher : IPaymentEventPublisher
{
    public int PaymentApprovedCalls { get; private set; }

    public int PaymentRejectedCalls { get; private set; }

    public int PaymentCancelledCalls { get; private set; }

    public Payment? LastApprovedPayment { get; private set; }

    public Payment? LastRejectedPayment { get; private set; }

    public Payment? LastCancelledPayment { get; private set; }

    public string? LastCancellationReason { get; private set; }

    public Task PublishPaymentApprovedAsync(
        Payment payment,
        CancellationToken cancellationToken = default)
    {
        PaymentApprovedCalls++;
        LastApprovedPayment = payment;

        return Task.CompletedTask;
    }

    public Task PublishPaymentRejectedAsync(
        Payment payment,
        CancellationToken cancellationToken = default)
    {
        PaymentRejectedCalls++;
        LastRejectedPayment = payment;

        return Task.CompletedTask;
    }

    public Task PublishPaymentCancelledAsync(
        Payment payment,
        string? reason = null,
        CancellationToken cancellationToken = default)
    {
        PaymentCancelledCalls++;
        LastCancelledPayment = payment;
        LastCancellationReason = reason;

        return Task.CompletedTask;
    }

    public void Reset()
    {
        PaymentApprovedCalls = 0;
        PaymentRejectedCalls = 0;
        PaymentCancelledCalls = 0;
        LastApprovedPayment = null;
        LastRejectedPayment = null;
        LastCancelledPayment = null;
        LastCancellationReason = null;
    }
}
