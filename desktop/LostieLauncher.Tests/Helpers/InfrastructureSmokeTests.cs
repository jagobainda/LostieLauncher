using LostieLauncher.Services;
using Microsoft.Extensions.DependencyInjection;

namespace LostieLauncher.Tests.Helpers;

public class InfrastructureSmokeTests
{
    [Fact]
    public void TestServiceProviderBuilder_DefaultBuild_ResolvesStubbedServices()
    {
        var builder = new TestServiceProviderBuilder();

        using var provider = builder.Build();
        var settings = provider.GetRequiredService<ISettingsService>();
        var http = provider.GetRequiredService<IHttpClientFactory>();

        settings.ShouldNotBeNull();
        http.ShouldBeOfType<HttpClientFactoryStub>();
    }

    [Fact]
    public void TestServiceProviderBuilder_WithOverride_ReplacesPreviousRegistration()
    {
        var custom = Substitute.For<ISettingsService>();
        var builder = new TestServiceProviderBuilder().With(custom);

        using var provider = builder.Build();
        var resolved = provider.GetRequiredService<ISettingsService>();

        resolved.ShouldBeSameAs(custom);
    }

    [Fact]
    public async Task FakeHttpMessageHandler_RespondWithJson_ReturnsConfiguredPayload()
    {
        var handler = new FakeHttpMessageHandler()
            .RespondWithJson("example.test", "{\"ok\":true}");
        using var client = new HttpClient(handler);

        var response = await client.GetAsync("https://example.test/data");
        var body = await response.Content.ReadAsStringAsync();

        response.StatusCode.ShouldBe(HttpStatusCode.OK);
        body.ShouldBe("{\"ok\":true}");
        handler.Requests.ShouldHaveSingleItem();
    }

    [Fact]
    public void TempDirectoryFixture_Dispose_RemovesDirectory()
    {
        string capturedPath;

        using (var fixture = new TempDirectoryFixture("smoke"))
        {
            capturedPath = fixture.Path;
            File.WriteAllText(fixture.Combine("a.txt"), "x");
            Directory.Exists(capturedPath).ShouldBeTrue();
        }

        Directory.Exists(capturedPath).ShouldBeFalse();
    }
}
