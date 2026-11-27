using LostieLauncher.Models;
using LostieLauncher.Services;

namespace LostieLauncher.Tests.Services;

public class TwitchLiveServiceTests
{
    private readonly HttpClientFactoryStub _httpFactory = new();
    private readonly TwitchLiveOptions _options = new(PreviewUrl: "https://preview.test/live_user_channel.jpg");

    private TwitchLiveService CreateSut() => new(_httpFactory, _options);

    [Fact]
    public async Task IsLiveAsync_WhenThePreviewExists_ReturnsTrue()
    {
        var handler = _httpFactory.HandlerFor("TwitchPreview").RespondWithStatus("live_user_channel.jpg", HttpStatusCode.OK, method: HttpMethod.Head);
        var sut = CreateSut();

        var live = await sut.IsLiveAsync();

        live.ShouldBeTrue();
        handler.Requests.ShouldHaveSingleItem().Method.ShouldBe(HttpMethod.Head);
    }

    [Theory]
    [InlineData(HttpStatusCode.Found)]
    [InlineData(HttpStatusCode.NotFound)]
    public async Task IsLiveAsync_WhenThePreviewIsNotServed_ReturnsFalse(HttpStatusCode statusCode)
    {
        _httpFactory.HandlerFor("TwitchPreview").RespondWithStatus("live_user_channel.jpg", statusCode);
        var sut = CreateSut();

        var live = await sut.IsLiveAsync();

        live.ShouldBeFalse();
    }

    [Theory]
    [InlineData(typeof(HttpRequestException))]
    [InlineData(typeof(TaskCanceledException))]
    [InlineData(typeof(InvalidOperationException))]
    public async Task IsLiveAsync_WhenTheCheckFails_ReturnsFalse(Type exceptionType)
    {
        _httpFactory.HandlerFor("TwitchPreview").Respond(_ => throw (Exception)Activator.CreateInstance(exceptionType)!);
        var sut = CreateSut();

        var live = await sut.IsLiveAsync();

        live.ShouldBeFalse();
    }
}
