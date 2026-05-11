using System.Text;

namespace LostieLauncher.Tests.Helpers;

/// <summary>
/// Test double for <see cref="HttpMessageHandler"/> that records every outgoing request
/// and lets a test script the response per (method, URL-substring) pair, or via a
/// custom delegate.
/// </summary>
/// <remarks>
/// Use together with <see cref="HttpClientFactoryStub"/> when the system under test
/// depends on <see cref="IHttpClientFactory"/>. Production code in this repo uses
/// named clients ("Telemetry", "Content", "SecurityFlag", "Download"); register one
/// handler per name as needed.
/// </remarks>
public sealed class FakeHttpMessageHandler : HttpMessageHandler
{
    private readonly List<Func<HttpRequestMessage, HttpResponseMessage?>> _matchers = [];

    /// <summary>All requests received, in arrival order. Useful for AAA-style assertions.</summary>
    public List<HttpRequestMessage> Requests { get; } = [];

    /// <summary>Default response when no matcher returns a value. 404 by default.</summary>
    public Func<HttpRequestMessage, HttpResponseMessage> DefaultResponder { get; set; } =
        _ => new HttpResponseMessage(HttpStatusCode.NotFound);

    /// <summary>Register a JSON response for any request whose URL contains <paramref name="urlSubstring"/>.</summary>
    public FakeHttpMessageHandler RespondWithJson(
        string urlSubstring,
        string json,
        HttpStatusCode statusCode = HttpStatusCode.OK,
        HttpMethod? method = null)
    {
        _matchers.Add(req =>
        {
            if (method is not null && req.Method != method) return null;
            if (req.RequestUri is null || !req.RequestUri.ToString().Contains(urlSubstring, StringComparison.OrdinalIgnoreCase)) return null;

            return new HttpResponseMessage(statusCode)
            {
                Content = new StringContent(json, Encoding.UTF8, "application/json")
            };
        });
        return this;
    }

    /// <summary>Register a status-only response for any request whose URL contains <paramref name="urlSubstring"/>.</summary>
    public FakeHttpMessageHandler RespondWithStatus(
        string urlSubstring,
        HttpStatusCode statusCode,
        HttpMethod? method = null)
    {
        _matchers.Add(req =>
        {
            if (method is not null && req.Method != method) return null;
            if (req.RequestUri is null || !req.RequestUri.ToString().Contains(urlSubstring, StringComparison.OrdinalIgnoreCase)) return null;

            return new HttpResponseMessage(statusCode);
        });
        return this;
    }

    /// <summary>Register an arbitrary response factory.</summary>
    public FakeHttpMessageHandler Respond(Func<HttpRequestMessage, HttpResponseMessage?> matcher)
    {
        _matchers.Add(matcher);
        return this;
    }

    protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
    {
        Requests.Add(request);

        foreach (var matcher in _matchers)
        {
            var response = matcher(request);
            if (response is not null) return Task.FromResult(response);
        }

        return Task.FromResult(DefaultResponder(request));
    }
}
