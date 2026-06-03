using System.Net;
using System.Text.Json;
using FinanceService.Domain.Enums;
using FinanceService.Dtos.Responses;
using FinanceService.Exceptions;
using FinanceService.Middleware;
using FluentAssertions;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Http.Features;
using Microsoft.Extensions.Logging.Abstractions;

namespace FinanceService.Tests.Middleware;

public sealed class GlobalExceptionHandlingMiddlewareTests
{
    [Theory]
    [MemberData(nameof(FinanceExceptions))]
    public async Task InvokeAsync_WhenFinanceExceptionIsThrown_WritesExpectedErrorResponse(
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
        var middleware = CreateMiddleware(_ => throw new BadHttpRequestException("Malformed payment request."));
        var context = CreateContext();

        await middleware.InvokeAsync(context);

        var response = await ReadResponseAsync(context);
        context.Response.StatusCode.Should().Be(StatusCodes.Status400BadRequest);
        response.Code.Should().Be("INVALID_REQUEST");
        response.Message.Should().Be("Malformed payment request.");
    }

    [Fact]
    public async Task InvokeAsync_WhenUnhandledExceptionIsThrown_WritesInternalServerErrorResponse()
    {
        var middleware = CreateMiddleware(_ => throw new InvalidOperationException("Database unavailable."));
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
        var responseFeature = new StartedResponseFeature();
        var features = new FeatureCollection();
        features.Set<IHttpResponseFeature>(responseFeature);
        var middleware = CreateMiddleware(_ => throw new PaymentNotFoundException("payment-1"));
        var context = new DefaultHttpContext(features);

        await middleware.InvokeAsync(context);

        context.Response.StatusCode.Should().Be(StatusCodes.Status200OK);
        responseFeature.ContentType.Should().BeNull();
    }

    public static TheoryData<Exception, string, HttpStatusCode> FinanceExceptions()
    {
        return new TheoryData<Exception, string, HttpStatusCode>
        {
            { new PaymentNotFoundException("payment-1"), "PAYMENT_NOT_FOUND", HttpStatusCode.NotFound },
            { new FinanceValidationException("Amount is required."), "FINANCE_VALIDATION_ERROR", HttpStatusCode.BadRequest },
            { new InvalidPaymentStatusTransitionException(PaymentStatus.Approved, PaymentStatus.Cancelled), "INVALID_PAYMENT_STATUS_TRANSITION", HttpStatusCode.Conflict }
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
        var body = await ReadBodyAsStringAsync(context);
        return JsonSerializer.Deserialize<ErrorResponse>(
            body,
            new JsonSerializerOptions(JsonSerializerDefaults.Web))!;
    }

    private static async Task<string> ReadBodyAsStringAsync(HttpContext context)
    {
        context.Response.Body.Seek(0, SeekOrigin.Begin);

        using var reader = new StreamReader(context.Response.Body, leaveOpen: true);
        return await reader.ReadToEndAsync();
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
