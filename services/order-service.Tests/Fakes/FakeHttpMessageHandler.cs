using System.Net;
using System.Net.Http.Json;

namespace OrderService.Tests.Fakes;

public sealed class FakeHttpMessageHandler : HttpMessageHandler
{
    private readonly Func<HttpRequestMessage, CancellationToken, HttpResponseMessage> _handler;

    public FakeHttpMessageHandler(
        Func<HttpRequestMessage, CancellationToken, HttpResponseMessage> handler)
    {
        _handler = handler;
    }

    public HttpRequestMessage? LastRequest { get; private set; }

    public string? LastRequestBody { get; private set; }

    public static FakeHttpMessageHandler Json(HttpStatusCode statusCode, object? body)
    {
        return new FakeHttpMessageHandler((_, _) =>
        {
            var response = new HttpResponseMessage(statusCode);

            if (body is not null)
            {
                response.Content = JsonContent.Create(body);
            }

            return response;
        });
    }

    protected override async Task<HttpResponseMessage> SendAsync(
        HttpRequestMessage request,
        CancellationToken cancellationToken)
    {
        LastRequest = request;

        if (request.Content is not null)
        {
            LastRequestBody = await request.Content.ReadAsStringAsync(cancellationToken);
        }

        return _handler(request, cancellationToken);
    }
}