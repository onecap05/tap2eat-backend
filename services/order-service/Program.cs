using Microsoft.Extensions.Options;
using MongoDB.Driver;
using OrderService.Config;
using OrderService.Repositories.Implementations;
using System.Text.Json.Serialization;
using OrderService.Repositories.Interfaces;
using OrderService.Services.Implementations;
using OrderService.Services.Interfaces;

var builder = WebApplication.CreateBuilder(args);

builder.Services.Configure<MongoDbSettings>(
    builder.Configuration.GetSection(MongoDbSettings.SectionName));

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

builder.Services.AddScoped<IOrderRepository, OrderRepository>();
builder.Services.AddScoped<IOrderService, OrderServiceImpl>();

builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        options.JsonSerializerOptions.Converters.Add(new JsonStringEnumConverter());
    });
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

var app = builder.Build();

app.UseSwagger();
app.UseSwaggerUI();

app.MapControllers();

app.Run();