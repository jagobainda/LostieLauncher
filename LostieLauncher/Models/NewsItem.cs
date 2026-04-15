namespace LostieLauncher.Models;

public class NewsItem
{
    public Guid Id { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public string Tag { get; set; } = string.Empty;
    public DateTime Date { get; set; }
    public DateTime? ExpiresAt { get; set; }
}
