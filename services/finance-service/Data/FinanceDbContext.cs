using FinanceService.Domain.Entities;
using FinanceService.Domain.Enums;
using Microsoft.EntityFrameworkCore;

namespace FinanceService.Data;

public sealed class FinanceDbContext : DbContext
{
    public FinanceDbContext(DbContextOptions<FinanceDbContext> options)
        : base(options)
    {
    }

    public DbSet<Payment> Payments => Set<Payment>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        var payment = modelBuilder.Entity<Payment>();

        payment.ToTable("payments");

        payment.HasKey(entity => entity.Id);

        payment.Property(entity => entity.OrderId)
            .IsRequired()
            .HasMaxLength(128);

        payment.Property(entity => entity.CustomerAccountId)
            .IsRequired()
            .HasMaxLength(128);

        payment.Property(entity => entity.RestaurantId)
            .IsRequired()
            .HasMaxLength(128);

        payment.Property(entity => entity.BranchId)
            .IsRequired()
            .HasMaxLength(128);

        payment.Property(entity => entity.Amount)
            .HasPrecision(18, 2)
            .IsRequired();

        payment.Property(entity => entity.Currency)
            .IsRequired()
            .HasMaxLength(3)
            .HasDefaultValue("MXN");

        payment.Property(entity => entity.Status)
            .HasConversion<string>()
            .IsRequired()
            .HasMaxLength(32)
            .HasDefaultValue(PaymentStatus.Pending);

        payment.Property(entity => entity.Provider)
            .HasMaxLength(64);

        payment.Property(entity => entity.ProviderReference)
            .HasMaxLength(128);

        payment.Property(entity => entity.AmountReceived)
            .HasPrecision(18, 2);

        payment.Property(entity => entity.ChangeAmount)
            .HasPrecision(18, 2);

        payment.Property(entity => entity.RejectionReason)
            .HasMaxLength(512);

        payment.Property(entity => entity.CreatedAt)
            .IsRequired();

        payment.Property(entity => entity.UpdatedAt)
            .IsRequired();

        payment.HasIndex(entity => entity.OrderId)
            .IsUnique();
    }
}
