using FinanceService.Services.Interfaces;

namespace FinanceService.Tests.Fakes;

public sealed class FakePayPalClient : IPayPalClient
{
    public int CreateOrderCalls { get; private set; }

    public int CaptureOrderCalls { get; private set; }

    public decimal LastAmount { get; private set; }

    public string LastCurrency { get; private set; } = string.Empty;

    public string LastReferenceId { get; private set; } = string.Empty;

    public string LastPaypalOrderId { get; private set; } = string.Empty;

    public string PaypalOrderId { get; set; } = "PAYPAL-ORDER-1";

    public PayPalCaptureResult CaptureResult { get; set; } = new(
        "PAYPAL-ORDER-1",
        "COMPLETED",
        "CAPTURE-1",
        "CAPTURE-1");

    public Exception? CreateOrderException { get; set; }

    public Exception? CaptureOrderException { get; set; }

    public Task<string> CreateOrderAsync(
        decimal amount,
        string currency,
        string referenceId,
        CancellationToken cancellationToken = default)
    {
        CreateOrderCalls++;
        LastAmount = amount;
        LastCurrency = currency;
        LastReferenceId = referenceId;

        if (CreateOrderException is not null)
        {
            throw CreateOrderException;
        }

        return Task.FromResult(PaypalOrderId);
    }

    public Task<PayPalCaptureResult> CaptureOrderAsync(
        string paypalOrderId,
        CancellationToken cancellationToken = default)
    {
        CaptureOrderCalls++;
        LastPaypalOrderId = paypalOrderId;

        if (CaptureOrderException is not null)
        {
            throw CaptureOrderException;
        }

        return Task.FromResult(CaptureResult);
    }

    public void Reset()
    {
        CreateOrderCalls = 0;
        CaptureOrderCalls = 0;
        LastAmount = 0;
        LastCurrency = string.Empty;
        LastReferenceId = string.Empty;
        LastPaypalOrderId = string.Empty;
        PaypalOrderId = "PAYPAL-ORDER-1";
        CaptureResult = new PayPalCaptureResult(
            "PAYPAL-ORDER-1",
            "COMPLETED",
            "CAPTURE-1",
            "CAPTURE-1");
        CreateOrderException = null;
        CaptureOrderException = null;
    }
}
