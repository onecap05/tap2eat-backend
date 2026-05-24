using FinanceService.Config;
using FinanceService.Security;
using FluentAssertions;
using Microsoft.Extensions.Options;

namespace FinanceService.Tests.Security;

public sealed class PaymentSimulationTokenValidatorTests
{
    [Fact]
    public void IsValid_returnsTrue_whenTokenMatches()
    {
        var validator = CreateValidator("expected-token");

        var result = validator.IsValid("expected-token");

        result.Should().BeTrue();
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData(" ")]
    public void IsValid_returnsFalse_whenTokenIsMissing(string? token)
    {
        var validator = CreateValidator("expected-token");

        var result = validator.IsValid(token);

        result.Should().BeFalse();
    }

    [Fact]
    public void IsValid_returnsFalse_whenTokenDoesNotMatch()
    {
        var validator = CreateValidator("expected-token");

        var result = validator.IsValid("wrong-token");

        result.Should().BeFalse();
    }

    private static PaymentSimulationTokenValidator CreateValidator(string expectedToken)
    {
        return new PaymentSimulationTokenValidator(
            Options.Create(new PaymentSimulationSettings
            {
                Token = expectedToken
            }));
    }
}
