using System.Net;
using System.Text.Json;
using FluentAssertions;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Http.Features;
using Microsoft.Extensions.Logging.Abstractions;
using OrderService.Domain.Enums;
using OrderService.Dtos.Responses;
using OrderService.Exceptions;
using OrderService.Middleware;

namespace OrderService.Tests.Middleware;

public sealed class GlobalExceptionHandlingMiddlewareTests
{
    [Theory]
    [MemberData(nameof(OrderExceptions))]
    public async Task InvokeAsync_WhenOrderExceptionIsThrown_WritesExpectedErrorResponse(
        Exception exception,
        string expectedCode,
        HttpStatusCode expectedStatusCode)
    {
        var middleware = CreateMiddleware(_ => throw exception);
        var context = CreateContext();

        await middleware.InvokeAsync(context);

        var response = await ReadResponseAsync(context);
        context.Response.StatusCode.Should().Be((int)expectedStatusCode);
        context.Response.ContentType.Should().StartWith("application/json");
        response.Code.Should().Be(expectedCode);
        response.Status.Should().Be((int)expectedStatusCode);
        response.TraceId.Should().Be(context.TraceIdentifier);
    }

    [Fact]
    public async Task InvokeAsync_WhenBadHttpRequestExceptionIsThrown_WritesBadRequestResponse()
    {
        var middleware = CreateMiddleware(_ => throw new BadHttpRequestException("Malformed order request."));
        var context = CreateContext();

        await middleware.InvokeAsync(context);

        var response = await ReadResponseAsync(context);
        context.Response.StatusCode.Should().Be(StatusCodes.Status400BadRequest);
        response.Code.Should().Be("INVALID_REQUEST");
        response.Message.Should().Be("Malformed order request.");
    }

    [Fact]
    public async Task InvokeAsync_WhenUnhandledExceptionIsThrown_WritesInternalServerErrorResponse()
    {
        var middleware = CreateMiddleware(_ => throw new InvalidOperationException("Mongo unavailable."));
        var context = CreateContext();

        await middleware.InvokeAsync(context);

        var response = await ReadResponseAsync(context);
        context.Response.StatusCode.Should().Be(StatusCodes.Status500InternalServerError);
        response.Code.Should().Be("INTERNAL_SERVER_ERROR");
        response.Message.Should().Be("An unexpected error occurred.");
    }

    [Fact]
    public async Task InvokeAsync_WhenResponseHasAlreadyStarted_DoesNotOverwriteResponse()
    {
        var feature = new StartedResponseFeature();
        var features = new FeatureCollection();
        features.Set<IHttpResponseFeature>(feature);
        var middleware = CreateMiddleware(_ => throw new OrderNotFoundException("order-1"));
        var context = new DefaultHttpContext(features);

        await middleware.InvokeAsync(context);

        context.Response.StatusCode.Should().Be(StatusCodes.Status200OK);
        feature.ContentType.Should().BeNull();
    }

    public static TheoryData<Exception, string, HttpStatusCode> OrderExceptions()
    {
        return new TheoryData<Exception, string, HttpStatusCode>
        {
            { new OrderNotFoundException("order-1"), "ORDER_NOT_FOUND", HttpStatusCode.NotFound },
            { new OrderValidationException("Quantity is invalid."), "ORDER_VALIDATION_ERROR", HttpStatusCode.BadRequest },
            { new CatalogValidationException("Invalid product."), "CATALOG_VALIDATION_FAILED", HttpStatusCode.BadRequest },
            { new InvalidOrderStatusTransitionException(OrderStatus.Created, OrderStatus.Ready), "INVALID_ORDER_STATUS_TRANSITION", HttpStatusCode.Conflict },
            { new CatalogServiceUnavailableException(), "CATALOG_SERVICE_UNAVAILABLE", HttpStatusCode.ServiceUnavailable }
        };
    }

    private static GlobalExceptionHandlingMiddleware CreateMiddleware(RequestDelegate next)
    {
        return new GlobalExceptionHandlingMiddleware(
            next,
            NullLogger<GlobalExceptionHandlingMiddleware>.Instance);
    }

    private static DefaultHttpContext CreateContext()
    {
        return new DefaultHttpContext
        {
            Response =
            {
                Body = new MemoryStream()
            }
        };
    }

    private static async Task<ErrorResponse> ReadResponseAsync(HttpContext context)
    {
        context.Response.Body.Seek(0, SeekOrigin.Begin);

        return (await JsonSerializer.DeserializeAsync<ErrorResponse>(
            context.Response.Body,
            new JsonSerializerOptions(JsonSerializerDefaults.Web)))!;
    }

    private sealed class StartedResponseFeature : IHttpResponseFeature
    {
        public int StatusCode { get; set; } = StatusCodes.Status200OK;

        public string? ReasonPhrase { get; set; }

        public IHeaderDictionary Headers { get; set; } = new HeaderDictionary();

        public Stream Body { get; set; } = new MemoryStream();

        public bool HasStarted => true;

        public string? ContentType
        {
            get => Headers.ContentType;
            set => Headers.ContentType = value;
        }

        public void OnCompleted(Func<object, Task> callback, object state)
        {
        }

        public void OnStarting(Func<object, Task> callback, object state)
        {
        }
    }
}
