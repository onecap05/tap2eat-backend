namespace OrderService.Config;

public sealed class InternalServiceSettings
{
    public const string SectionName = "InternalService";

    public string Token { get; set; } = "tap2eat-internal-dev-token";
}