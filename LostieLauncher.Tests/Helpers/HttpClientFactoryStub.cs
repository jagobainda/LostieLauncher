namespace LostieLauncher.Tests.Helpers;

/// <summary>
/// In-memory <see cref="IHttpClientFactory"/> backed by <see cref="FakeHttpMessageHandler"/>
/// instances keyed by the named-client name used in production
/// (see <c>LostieLauncher.Core.DependencyInjection</c>).
/// </summary>
public sealed class HttpClientFactoryStub : IHttpClientFactory
{
    private readonly Dictionary<string, FakeHttpMessageHandler> _handlers = new(StringComparer.OrdinalIgnoreCase);

    /// <summary>
    /// Get (or create) the handler registered under <paramref name="name"/>. The same
    /// handler instance is reused for every <see cref="CreateClient"/> call with that name,
    /// so tests can pre-arrange responses and later inspect <see cref="FakeHttpMessageHandler.Requests"/>.
    /// </summary>
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
        // disposeHandler:false because handlers are owned by this factory and shared across clients.
        return new HttpClient(handler, disposeHandler: false);
    }
}
