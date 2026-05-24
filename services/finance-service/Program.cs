using System.Text.Json.Serialization;
using FinanceService.Config;
using FinanceService.Data;
using FinanceService.Messaging.Consumers;
using FinanceService.Messaging.Publishers;
using FinanceService.Middleware;
using FinanceService.Repositories.Implementations;
using FinanceService.Repositories.Interfaces;
using FinanceService.Security;
using FinanceService.Services.Implementations;
using FinanceService.Services.Interfaces;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;

var builder = WebApplication.CreateBuilder(args);

builder.Services.Configure<PostgresSettings>(
    builder.Configuration.GetSection(PostgresSettings.SectionName));

builder.Services.Configure<RabbitMqSettings>(
    builder.Configuration.GetSection(RabbitMqSettings.SectionName));

builder.Services.Configure<PaymentSimulationSettings>(
    builder.Configuration.GetSection(PaymentSimulationSettings.SectionName));

builder.Services.Configure<PayPalSettings>(
    builder.Configuration.GetSection(PayPalSettings.SectionName));

builder.Services.Configure<JwtSettings>(
    builder.Configuration.GetSection(JwtSettings.SectionName));

var jwtSettings = builder.Configuration
    .GetSection(JwtSettings.SectionName)
    .Get<JwtSettings>() ?? new JwtSettings();

builder.Services
    .AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = false,
            ValidateAudience = false,
            ValidateLifetime = true,
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = LoadRsaSecurityKey(jwtSettings.PublicKeyPath, builder.Environment.ContentRootPath),
            ValidAlgorithms = [SecurityAlgorithms.RsaSha256]
        };
    });

builder.Services.AddAuthorization();

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
builder.Services.AddScoped<IPayPalPaymentService, PayPalPaymentService>();
builder.Services.AddScoped<IOrderEventProcessor, OrderEventProcessorImpl>();
builder.Services.AddScoped<IPaymentEventPublisher, RabbitMqPaymentEventPublisherImpl>();
builder.Services.AddSingleton<IPaymentSimulationTokenValidator, PaymentSimulationTokenValidator>();
builder.Services.AddHttpClient<IPayPalClient, PayPalClient>();

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

app.UseAuthentication();
app.UseAuthorization();

app.MapMethods("{*path}", ["OPTIONS"], () => Results.Ok());
app.MapControllers();

await DatabaseInitializer.InitializeAsync(app.Services);

app.Run();

static RsaSecurityKey LoadRsaSecurityKey(string publicKeyPath, string contentRootPath)
{
    var resolvedPath = Path.IsPathRooted(publicKeyPath)
        ? publicKeyPath
        : Path.GetFullPath(Path.Combine(contentRootPath, publicKeyPath));

    if (!File.Exists(resolvedPath))
    {
        throw new InvalidOperationException($"JWT public key file was not found at '{resolvedPath}'.");
    }

    var rsa = System.Security.Cryptography.RSA.Create();
    rsa.ImportFromPem(File.ReadAllText(resolvedPath));

    return new RsaSecurityKey(rsa);
}

public partial class Program
{
}
