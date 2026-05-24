namespace FinanceService.Config;

public sealed class PaymentSimulationSettings
{
    public const string SectionName = "PaymentSimulation";

    public string Token { get; set; } = "tap2eat-payment-dev-token";
}
