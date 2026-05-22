using FinanceService.Dtos.Responses;
using FinanceService.Exceptions;

namespace FinanceService.Middleware;

public sealed class GlobalExceptionHandlingMiddleware
{
    private readonly RequestDelegate _next;
    private readonly ILogger<GlobalExceptionHandlingMiddleware> _logger;

    public GlobalExceptionHandlingMiddleware(
        RequestDelegate next,
        ILogger<GlobalExceptionHandlingMiddleware> logger)
    {
        _next = next;
        _logger = logger;
    }

    public async Task InvokeAsync(HttpContext context)
    {
        try
        {
            await _next(context);
        }
        catch (FinanceException exception)
        {
            await WriteErrorAsync(context, exception.Code, exception.Message, exception.StatusCode);
        }
        catch (BadHttpRequestException exception)
        {
            await WriteErrorAsync(
                context,
                "INVALID_REQUEST",
                exception.Message,
                StatusCodes.Status400BadRequest);
        }
        catch (Exception exception)
        {
            _logger.LogError(exception, "Unhandled exception while processing finance request.");

            await WriteErrorAsync(
                context,
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred.",
                StatusCodes.Status500InternalServerError);
        }
    }

    private static async Task WriteErrorAsync(
        HttpContext context,
        string code,
        string message,
        int statusCode)
    {
        if (context.Response.HasStarted)
        {
            return;
        }

        context.Response.StatusCode = statusCode;
        context.Response.ContentType = "application/json";

        var response = new ErrorResponse
        {
            Code = code,
            Message = message,
            Status = statusCode,
            TraceId = context.TraceIdentifier
        };

        await context.Response.WriteAsJsonAsync(response);
    }
}
