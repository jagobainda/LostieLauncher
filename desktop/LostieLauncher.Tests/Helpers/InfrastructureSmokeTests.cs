using LostieLauncher.Services;
using Microsoft.Extensions.DependencyInjection;

namespace LostieLauncher.Tests.Helpers;

/// <summary>
/// Sanity tests that prove the test-infrastructure helpers themselves work. They are not
/// production-code tests; they exist so a broken helper is caught before it silently
/// undermines every real test in the suite. Follow the AAA convention used by all tests.
/// </summary>
public class InfrastructureSmokeTests
{
    [Fact]
    public void TestServiceProviderBuilder_DefaultBuild_ResolvesStubbedServices()
    {
        // Arrange
        var builder = new TestServiceProviderBuilder();

        // Act
        using var provider = builder.Build();
        var settings = provider.GetRequiredService<ISettingsService>();
        var http = provider.GetRequiredService<IHttpClientFactory>();

        // Assert
        settings.ShouldNotBeNull();
        http.ShouldBeOfType<HttpClientFactoryStub>();
    }

    [Fact]
    public void TestServiceProviderBuilder_WithOverride_ReplacesPreviousRegistration()
    {
        // Arrange
        var custom = Substitute.For<ISettingsService>();
        var builder = new TestServiceProviderBuilder().With(custom);

        // Act
        using var provider = builder.Build();
        var resolved = provider.GetRequiredService<ISettingsService>();

        // Assert
        resolved.ShouldBeSameAs(custom);
    }

    [Fact]
    public async Task FakeHttpMessageHandler_RespondWithJson_ReturnsConfiguredPayload()
    {
        // Arrange
        var handler = new FakeHttpMessageHandler()
            .RespondWithJson("example.test", "{\"ok\":true}");
        using var client = new HttpClient(handler);

        // Act
        var response = await client.GetAsync("https://example.test/data");
        var body = await response.Content.ReadAsStringAsync();

        // Assert
        response.StatusCode.ShouldBe(HttpStatusCode.OK);
        body.ShouldBe("{\"ok\":true}");
        handler.Requests.ShouldHaveSingleItem();
    }

    [Fact]
    public void TempDirectoryFixture_Dispose_RemovesDirectory()
    {
        // Arrange
        string capturedPath;

        // Act
        using (var fixture = new TempDirectoryFixture("smoke"))
        {
            capturedPath = fixture.Path;
            File.WriteAllText(fixture.Combine("a.txt"), "x");
            Directory.Exists(capturedPath).ShouldBeTrue();
        }

        // Assert
        Directory.Exists(capturedPath).ShouldBeFalse();
    }
}
