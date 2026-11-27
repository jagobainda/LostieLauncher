using LostieLauncher.Models;
using LostieLauncher.Services;
using Microsoft.Extensions.DependencyInjection;

namespace LostieLauncher.Tests.Helpers;

public sealed class TestServiceProviderBuilder
{
    private readonly ServiceCollection _services = [];
    private readonly HttpClientFactoryStub _httpClientFactory = new();

    public HttpClientFactoryStub HttpClientFactory => _httpClientFactory;

    public TestServiceProviderBuilder()
    {
        _services.AddSingleton(new ContentOptions(
            ContentEndpoint: "https://content.test/list.json",
            NotificationsEndpoint: "https://content.test/notifications.json",
            FlagEndpoint: "https://content.test/flag.txt"));
        _services.AddSingleton(new DownloadOptions(BaseUrl: "https://download.test/"));

        _services.AddSingleton<IHttpClientFactory>(_httpClientFactory);
        _services.AddSingleton(Substitute.For<ISettingsService>());
        _services.AddSingleton(Substitute.For<IWindowsStartupService>());
        _services.AddSingleton(Substitute.For<IContentService>());
        _services.AddSingleton(Substitute.For<IDownloadService>());
    }

    public TestServiceProviderBuilder With<TService>(TService instance) where TService : class
    {
        for (var i = _services.Count - 1; i >= 0; i--)
        {
            if (_services[i].ServiceType == typeof(TService)) _services.RemoveAt(i);
        }
        _services.AddSingleton(instance);
        return this;
    }

    public TestServiceProviderBuilder AddSingleton<TService>() where TService : class
    {
        _services.AddSingleton<TService>();
        return this;
    }

    public ServiceProvider Build() => _services.BuildServiceProvider(validateScopes: true);
}
