namespace LostieLauncher.Tests.Helpers;

public sealed class HttpClientFactoryStub : IHttpClientFactory
{
    private readonly Dictionary<string, FakeHttpMessageHandler> _handlers = new(StringComparer.OrdinalIgnoreCase);

    public FakeHttpMessageHandler HandlerFor(string name)
    {
        if (!_handlers.TryGetValue(name, out var handler))
        {
            handler = new FakeHttpMessageHandler();
            _handlers[name] = handler;
        }
        return handler;
    }

    public HttpClient CreateClient(string name)
    {
        var handler = HandlerFor(name);
        return new HttpClient(handler, disposeHandler: false);
    }
}
