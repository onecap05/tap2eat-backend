using FinanceService.Config;
using Microsoft.Extensions.Options;

namespace FinanceService.Security;

public sealed class PaymentSimulationTokenValidator : IPaymentSimulationTokenValidator
{
    private readonly PaymentSimulationSettings _settings;

    public PaymentSimulationTokenValidator(IOptions<PaymentSimulationSettings> options)
    {
        _settings = options.Value;
    }

    public bool IsValid(string? token)
    {
        if (string.IsNullOrWhiteSpace(token))
        {
            return false;
        }

        return string.Equals(token, _settings.Token, StringComparison.Ordinal);
    }
}
