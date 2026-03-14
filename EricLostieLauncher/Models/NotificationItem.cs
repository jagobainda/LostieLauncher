namespace EricLostieLauncher.Models;

public class NotificationItem
{
    public Guid Id { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Message { get; set; } = string.Empty;
    public NotificationType Type { get; set; }
    public DateTime Date { get; set; }
    public DateTime? ExpiresAt { get; set; }
}
