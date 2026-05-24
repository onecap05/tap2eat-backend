using System.Text.Json.Serialization;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;
using Neo4j.Driver;
using RecommendationService.Config;
using RecommendationService.Integrations.Catalog;
using RecommendationService.Messaging.Consumers;
using RecommendationService.Middleware;
using RecommendationService.Repositories;
using RecommendationService.Services;

var builder = WebApplication.CreateBuilder(args);

builder.Services.Configure<Neo4jSettings>(
    builder.Configuration.GetSection(Neo4jSettings.SectionName));
builder.Services.Configure<RabbitMqSettings>(
    builder.Configuration.GetSection(RabbitMqSettings.SectionName));
builder.Services.Configure<JwtSettings>(
    builder.Configuration.GetSection(JwtSettings.SectionName));
builder.Services.Configure<ExternalServiceSettings>(
    builder.Configuration.GetSection(ExternalServiceSettings.SectionName));
builder.Services.Configure<RecommendationSettings>(
    builder.Configuration.GetSection(RecommendationSettings.SectionName));

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

builder.Services.AddSingleton<IDriver>(serviceProvider =>
{
    var settings = serviceProvider.GetRequiredService<IOptions<Neo4jSettings>>().Value;

    return GraphDatabase.Driver(
        settings.Uri,
        AuthTokens.Basic(settings.Username, settings.Password));
});

builder.Services.AddHttpClient<ICatalogClient, CatalogClient>((serviceProvider, client) =>
{
    var settings = serviceProvider.GetRequiredService<IOptions<ExternalServiceSettings>>().Value;
    client.BaseAddress = new Uri(settings.CatalogServiceBaseUrl);
});

builder.Services.AddScoped<ILocationDistanceService, HaversineDistanceService>();
builder.Services.AddScoped<IRecommendationService, RecommendationServiceImpl>();
builder.Services.AddScoped<IRecommendationGraphRepository, RecommendationGraphRepository>();
builder.Services.AddScoped<IOrderDeliveredEventProcessor, OrderDeliveredEventProcessor>();

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

await InitializeGraphAsync(app.Services);

app.Run();

static async Task InitializeGraphAsync(IServiceProvider serviceProvider)
{
    using var scope = serviceProvider.CreateScope();
    var repository = scope.ServiceProvider.GetRequiredService<IRecommendationGraphRepository>();

    await repository.InitializeAsync();
}

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
