using FinanceService.Config;
using FluentAssertions;
using Microsoft.Extensions.Configuration;

namespace FinanceService.Tests.Config;

public sealed class PayPalSettingsTests
{
    [Fact]
    public void Defaults_shouldUseSandboxSettingsWithoutCredentials()
    {
        var settings = new PayPalSettings();

        settings.Mode.Should().Be("sandbox");
        settings.BaseUrl.Should().Be("https://api-m.sandbox.paypal.com");
        settings.ClientId.Should().BeEmpty();
        settings.ClientSecret.Should().BeEmpty();
        settings.Currency.Should().Be("MXN");
    }

    [Fact]
    public void Configuration_shouldBindSandboxSettings()
    {
        var configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["PayPal:Mode"] = "sandbox",
                ["PayPal:BaseUrl"] = "https://api-m.sandbox.paypal.com",
                ["PayPal:ClientId"] = "",
                ["PayPal:ClientSecret"] = "",
                ["PayPal:Currency"] = "MXN"
            })
            .Build();

        var settings = configuration
            .GetSection(PayPalSettings.SectionName)
            .Get<PayPalSettings>();

        settings.Should().NotBeNull();
        settings!.Mode.Should().Be("sandbox");
        settings.BaseUrl.Should().Be("https://api-m.sandbox.paypal.com");
        settings.ClientId.Should().BeEmpty();
        settings.ClientSecret.Should().BeEmpty();
        settings.Currency.Should().Be("MXN");
    }
}
