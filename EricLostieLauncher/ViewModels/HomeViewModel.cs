using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using EricLostieLauncher.Models;

namespace EricLostieLauncher.ViewModels;

public partial class HomeViewModel : ObservableObject
{
    public ObservableCollection<NewsItem> News { get; } =
    [
        new()
        {
            Title = "Eric Lostie Launcher v1.2 disponible",
            Description = "Nueva versión con mejoras de rendimiento y corrección de errores. ¡Actualiza ya!",
            Tag = "Release",
            Date = new DateTime(2025, 7, 10)
        },
        new()
        {
            Title = "Próximamente: Modo cooperativo",
            Description = "Estamos trabajando en un modo cooperativo para 2 jugadores. Más detalles próximamente.",
            Tag = "Anuncio",
            Date = new DateTime(2025, 7, 5)
        },
        new()
        {
            Title = "Nuevo juego en desarrollo",
            Description = "Un nuevo proyecto está en camino. Mantente atento para más información.",
            Tag = "Preview",
            Date = new DateTime(2025, 6, 28)
        }
    ];

    public ObservableCollection<NotificationItem> Notifications { get; } =
    [
        new()
        {
            Title = "Actualización completada",
            Message = "El launcher se ha actualizado correctamente a la última versión.",
            Type = NotificationType.Info,
            Date = new DateTime(2025, 7, 10)
        },
        new()
        {
            Title = "Mantenimiento programado",
            Message = "El servidor estará en mantenimiento el 15 de julio de 02:00 a 06:00.",
            Type = NotificationType.Warning,
            Date = new DateTime(2025, 7, 8)
        },
        new()
        {
            Title = "Error de conexión detectado",
            Message = "No se pudo conectar al servidor de descargas. Revisa tu conexión a internet.",
            Type = NotificationType.Exclamation,
            Date = new DateTime(2025, 7, 7)
        },
        new()
        {
            Title = "Nuevo contenido disponible",
            Message = "Se han añadido 3 nuevos juegos a la biblioteca.",
            Type = NotificationType.Info,
            Date = new DateTime(2025, 7, 3)
        }
    ];
}
