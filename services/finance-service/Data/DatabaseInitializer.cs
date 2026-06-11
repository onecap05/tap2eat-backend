using Microsoft.EntityFrameworkCore;

namespace FinanceService.Data;

public static class DatabaseInitializer
{
    public static async Task InitializeAsync(IServiceProvider serviceProvider)
    {
        await using var scope = serviceProvider.CreateAsyncScope();

        var dbContext = scope.ServiceProvider.GetRequiredService<FinanceDbContext>();

        await dbContext.Database.MigrateAsync();
    }
}
