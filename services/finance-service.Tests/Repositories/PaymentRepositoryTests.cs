using FinanceService.Data;
using FinanceService.Domain.Enums;
using FinanceService.Repositories.Implementations;
using FinanceService.Tests.TestData;
using FluentAssertions;
using Microsoft.EntityFrameworkCore;

namespace FinanceService.Tests.Repositories;

public sealed class PaymentRepositoryTests
{
    [Fact]
    public async Task CreateAsync_SavesPayment()
    {
        await using var dbContext = CreateDbContext();
        var repository = new PaymentRepository(dbContext);
        var payment = PaymentTestData.Payment(orderId: "order-create");

        var createdPayment = await repository.CreateAsync(payment);

        createdPayment.Id.Should().Be(payment.Id);
        var persistedPayment = await dbContext.Payments.SingleAsync();
        persistedPayment.OrderId.Should().Be("order-create");
        persistedPayment.Status.Should().Be(PaymentStatus.Pending);
    }

    [Fact]
    public async Task FindByIdAsync_WhenPaymentExists_ReturnsPaymentWithoutTracking()
    {
        await using var dbContext = CreateDbContext();
        var payment = PaymentTestData.Payment(orderId: "order-find-id");
        dbContext.Payments.Add(payment);
        await dbContext.SaveChangesAsync();
        dbContext.ChangeTracker.Clear();

        var repository = new PaymentRepository(dbContext);

        var result = await repository.FindByIdAsync(payment.Id);

        result.Should().NotBeNull();
        result!.OrderId.Should().Be("order-find-id");
        dbContext.ChangeTracker.Entries().Should().BeEmpty();
    }

    [Fact]
    public async Task FindByIdAsync_WhenPaymentDoesNotExist_ReturnsNull()
    {
        await using var dbContext = CreateDbContext();
        var repository = new PaymentRepository(dbContext);

        var result = await repository.FindByIdAsync(Guid.NewGuid());

        result.Should().BeNull();
    }

    [Fact]
    public async Task FindByOrderIdAsync_WhenPaymentExists_ReturnsPayment()
    {
        await using var dbContext = CreateDbContext();
        var payment = PaymentTestData.Payment(orderId: "order-find-order");
        dbContext.Payments.Add(payment);
        await dbContext.SaveChangesAsync();

        var repository = new PaymentRepository(dbContext);

        var result = await repository.FindByOrderIdAsync("order-find-order");

        result.Should().NotBeNull();
        result!.Id.Should().Be(payment.Id);
    }

    [Fact]
    public async Task FindByOrderIdAsync_WhenPaymentDoesNotExist_ReturnsNull()
    {
        await using var dbContext = CreateDbContext();
        var repository = new PaymentRepository(dbContext);

        var result = await repository.FindByOrderIdAsync("missing-order");

        result.Should().BeNull();
    }

    [Fact]
    public async Task FindByCustomerAccountIdAsync_ReturnsMatchingPaymentsNewestFirst()
    {
        await using var dbContext = CreateDbContext();
        dbContext.Payments.AddRange(
            PaymentTestData.Payment(orderId: "old-customer-payment"),
            PaymentTestData.Payment(orderId: "other-customer-payment"));
        dbContext.Payments.Local.Single(payment => payment.OrderId == "old-customer-payment").CreatedAt = DateTime.UtcNow.AddDays(-2);
        dbContext.Payments.Local.Single(payment => payment.OrderId == "other-customer-payment").CustomerAccountId = "customer-2";

        var newestPayment = PaymentTestData.Payment(orderId: "new-customer-payment");
        newestPayment.CreatedAt = DateTime.UtcNow;
        dbContext.Payments.Add(newestPayment);
        await dbContext.SaveChangesAsync();

        var repository = new PaymentRepository(dbContext);

        var result = await repository.FindByCustomerAccountIdAsync("customer-1");

        result.Select(payment => payment.OrderId).Should().Equal(
            "new-customer-payment",
            "old-customer-payment");
    }

    [Fact]
    public async Task FindByRestaurantIdAsync_ReturnsMatchingPaymentsNewestFirst()
    {
        await using var dbContext = CreateDbContext();
        dbContext.Payments.AddRange(
            PaymentTestData.Payment(orderId: "old-restaurant-payment"),
            PaymentTestData.Payment(orderId: "other-restaurant-payment"));
        dbContext.Payments.Local.Single(payment => payment.OrderId == "old-restaurant-payment").CreatedAt = DateTime.UtcNow.AddDays(-2);
        dbContext.Payments.Local.Single(payment => payment.OrderId == "other-restaurant-payment").RestaurantId = "restaurant-2";

        var newestPayment = PaymentTestData.Payment(orderId: "new-restaurant-payment");
        newestPayment.CreatedAt = DateTime.UtcNow;
        dbContext.Payments.Add(newestPayment);
        await dbContext.SaveChangesAsync();

        var repository = new PaymentRepository(dbContext);

        var result = await repository.FindByRestaurantIdAsync("restaurant-1");

        result.Select(payment => payment.OrderId).Should().Equal(
            "new-restaurant-payment",
            "old-restaurant-payment");
    }

    [Fact]
    public async Task UpdateAsync_PersistsPaymentChanges()
    {
        await using var dbContext = CreateDbContext();
        var payment = PaymentTestData.Payment(orderId: "order-update");
        dbContext.Payments.Add(payment);
        await dbContext.SaveChangesAsync();
        dbContext.ChangeTracker.Clear();

        payment.Status = PaymentStatus.Approved;
        payment.ProviderReference = "provider-ref";
        var repository = new PaymentRepository(dbContext);

        var updatedPayment = await repository.UpdateAsync(payment);

        updatedPayment.Status.Should().Be(PaymentStatus.Approved);
        var persistedPayment = await dbContext.Payments.SingleAsync();
        persistedPayment.Status.Should().Be(PaymentStatus.Approved);
        persistedPayment.ProviderReference.Should().Be("provider-ref");
    }

    private static FinanceDbContext CreateDbContext()
    {
        var options = new DbContextOptionsBuilder<FinanceDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;

        return new FinanceDbContext(options);
    }
}
