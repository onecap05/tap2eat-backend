using System.Net;
using System.Net.Http.Json;

namespace OrderService.Tests.Fakes;

internal sealed class FakeHttpMessageHandler : HttpMessageHandler
{
    private readonly Func<HttpRequestMessage, CancellationToken, Task<HttpResponseMessage>> _handler;

    public FakeHttpMessageHandler(Func<HttpRequestMessage, CancellationToken, Task<HttpResponseMessage>> handler)
    {
        _handler = handler;
    }

    public HttpRequestMessage? LastRequest { get; private set; }

    protected override async Task<HttpResponseMessage> SendAsync(
        HttpRequestMessage request,
        CancellationToken cancellationToken)
    {
        LastRequest = request;
        return await _handler(request, cancellationToken);
    }

    public static FakeHttpMessageHandler Json(HttpStatusCode statusCode, object? content)
    {
        return new FakeHttpMessageHandler((_, _) =>
        {
            var response = new HttpResponseMessage(statusCode);

            if (content is not null)
            {
                response.Content = JsonContent.Create(content);
            }

            return Task.FromResult(response);
        });
    }
}
