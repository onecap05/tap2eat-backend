using System.Text.Json.Serialization;
using FinanceService.Config;
using FinanceService.Data;
using FinanceService.Messaging.Consumers;
using FinanceService.Messaging.Publishers;
using FinanceService.Middleware;
using FinanceService.Repositories.Implementations;
using FinanceService.Repositories.Interfaces;
using FinanceService.Services.Implementations;
using FinanceService.Services.Interfaces;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;

var builder = WebApplication.CreateBuilder(args);

builder.Services.Configure<PostgresSettings>(
    builder.Configuration.GetSection(PostgresSettings.SectionName));

builder.Services.Configure<RabbitMqSettings>(
    builder.Configuration.GetSection(RabbitMqSettings.SectionName));

builder.Services.AddDbContext<FinanceDbContext>((serviceProvider, options) =>
{
    var settings = serviceProvider
        .GetRequiredService<IOptions<PostgresSettings>>()
        .Value;

    if (string.IsNullOrWhiteSpace(settings.ConnectionString))
    {
        throw new InvalidOperationException("PostgreSQL connection string is not configured.");
    }

    options.UseNpgsql(settings.ConnectionString);
});

builder.Services.AddScoped<IPaymentRepository, PaymentRepository>();
builder.Services.AddScoped<IPaymentService, PaymentServiceImpl>();
builder.Services.AddScoped<IOrderEventProcessor, OrderEventProcessorImpl>();
builder.Services.AddScoped<IPaymentEventPublisher, RabbitMqPaymentEventPublisherImpl>();

var rabbitMqSettings = builder.Configuration
    .GetSection(RabbitMqSettings.SectionName)
    .Get<RabbitMqSettings>() ?? new RabbitMqSettings();

if (rabbitMqSettings.Enabled)
{
    builder.Services.AddHostedService<OrderEventConsumerBackgroundService>();
}

builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.Converters.Add(new JsonStringEnumConverter());
    });
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

var app = builder.Build();

app.UseMiddleware<GlobalExceptionHandlingMiddleware>();

app.UseSwagger();
app.UseSwaggerUI();

app.MapControllers();

await DatabaseInitializer.InitializeAsync(app.Services);

app.Run();

public partial class Program
{
}
