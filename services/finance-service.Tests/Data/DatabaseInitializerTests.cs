using FinanceService.Data;
using FinanceService.Tests.TestData;
using FluentAssertions;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;

namespace FinanceService.Tests.Data;

public sealed class DatabaseInitializerTests
{
    [Fact]
    public async Task InitializeAsync_EnsuresFinanceDatabaseIsCreated()
    {
        var services = new ServiceCollection();
        services.AddDbContext<FinanceDbContext>(options =>
            options.UseInMemoryDatabase(Guid.NewGuid().ToString()));
        await using var serviceProvider = services.BuildServiceProvider();

        await DatabaseInitializer.InitializeAsync(serviceProvider);

        await using var scope = serviceProvider.CreateAsyncScope();
        var dbContext = scope.ServiceProvider.GetRequiredService<FinanceDbContext>();
        dbContext.Payments.Add(PaymentTestData.Payment(orderId: "initializer-order"));
        await dbContext.SaveChangesAsync();

        var paymentExists = await dbContext.Payments.AnyAsync(payment =>
            payment.OrderId == "initializer-order");
        paymentExists.Should().BeTrue();
    }
}
