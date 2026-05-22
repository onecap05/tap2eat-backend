using FinanceService.Domain.Enums;
using FinanceService.Mapping;
using FinanceService.Tests.TestData;
using FluentAssertions;

namespace FinanceService.Tests.Mapping;

public sealed class PaymentMapperTests
{
    [Fact]
    public void ToResponse_mapsAllImportantFields()
    {
        var payment = PaymentTestData.Payment(status: PaymentStatus.Approved);
        payment.Provider = "SIMULATED";
        payment.ProviderReference = "provider-ref";
        payment.ApprovedAt = DateTime.UtcNow;

        var response = PaymentMapper.ToResponse(payment);

        response.Id.Should().Be(payment.Id);
        response.OrderId.Should().Be(payment.OrderId);
        response.CustomerAccountId.Should().Be(payment.CustomerAccountId);
        response.RestaurantId.Should().Be(payment.RestaurantId);
        response.BranchId.Should().Be(payment.BranchId);
        response.Amount.Should().Be(payment.Amount);
        response.Currency.Should().Be(payment.Currency);
        response.Status.Should().Be(payment.Status);
        response.Provider.Should().Be(payment.Provider);
        response.ProviderReference.Should().Be(payment.ProviderReference);
        response.CreatedAt.Should().Be(payment.CreatedAt);
        response.UpdatedAt.Should().Be(payment.UpdatedAt);
        response.ApprovedAt.Should().Be(payment.ApprovedAt);
    }
}
