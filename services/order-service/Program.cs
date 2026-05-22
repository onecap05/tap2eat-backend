using System.Text.Json.Serialization;
using Microsoft.Extensions.Options;
using MongoDB.Driver;
using OrderService.Config;
using OrderService.Integrations.Catalog;
using OrderService.Middleware;
using OrderService.Messaging.Publishers;
using OrderService.Repositories.Implementations;
using OrderService.Repositories.Interfaces;
using OrderService.Services.Implementations;
using OrderService.Services.Interfaces;

var builder = WebApplication.CreateBuilder(args);

builder.Services.Configure<MongoDbSettings>(
    builder.Configuration.GetSection(MongoDbSettings.SectionName));

builder.Services.Configure<CatalogServiceSettings>(
    builder.Configuration.GetSection(CatalogServiceSettings.SectionName));

builder.Services.Configure<RabbitMqSettings>(
    builder.Configuration.GetSection(RabbitMqSettings.SectionName));

builder.Services.AddSingleton<IMongoClient>(serviceProvider =>
{
    var settings = serviceProvider
        .GetRequiredService<IOptions<MongoDbSettings>>()
        .Value;

    if (string.IsNullOrWhiteSpace(settings.ConnectionString))
    {
        throw new InvalidOperationException("MongoDB connection string is not configured.");
    }

    return new MongoClient(settings.ConnectionString);
});

builder.Services.AddHttpClient<ICatalogClient, CatalogClient>((serviceProvider, client) =>
{
    var settings = serviceProvider
        .GetRequiredService<IOptions<CatalogServiceSettings>>()
        .Value;

    if (string.IsNullOrWhiteSpace(settings.BaseUrl))
    {
        throw new InvalidOperationException("Catalog service base URL is not configured.");
    }

    client.BaseAddress = new Uri(settings.BaseUrl);
});

builder.Services.AddScoped<IOrderRepository, OrderRepository>();
builder.Services.AddScoped<IOrderService, OrderServiceImpl>();
builder.Services.AddScoped<IOrderEventPublisher, RabbitMqOrderEventPublisherImpl>();

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

app.Run();

public partial class Program
{
}