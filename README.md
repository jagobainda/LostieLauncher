<div align="center">

<img src="EricLostieLauncher/Assets/app.ico" width="96" height="96" alt="EricLostieLauncher icon" />

# EricLostieLauncher

**Lanzador de juegos moderno para Windows**

[![Version](https://img.shields.io/badge/version-0.8.0-blue?style=flat-square)](releases/) [![Platform](https://img.shields.io/badge/platform-Windows-0078D4?style=flat-square&logo=windows)](https://microsoft.com/windows) [![.NET](https://img.shields.io/badge/.NET-10.0-512BD4?style=flat-square&logo=dotnet)](https://dotnet.microsoft.com/) [![WPF](https://img.shields.io/badge/UI-WPF-68217A?style=flat-square)](https://learn.microsoft.com/en-us/dotnet/desktop/wpf/) [![Velopack](https://img.shields.io/badge/updates-Velopack-FFE084?style=flat-square)](https://velopack.io/) [![License](https://img.shields.io/badge/license-see%20LICENSE-lightgrey?style=flat-square)](LICENSE.txt)

Descubre, descarga, instala y gestiona tus juegos desde una interfaz limpia y con soporte multidioma.

</div>

---

## ✨ Características

| Característica                     | Descripción                                                                                 |
| ---------------------------------- | ------------------------------------------------------------------------------------------- |
| 🎮 **Biblioteca de juegos**        | Explora el catálogo completo de juegos disponibles desde el servidor de contenido           |
| ⬇️ **Descargas reanudables**       | Soporta pausar y reanudar descargas con archivos `.part` y barra de progreso en tiempo real |
| 🔑 **Sistema de claves**           | Acceso a contenido exclusivo mediante claves de descarga validadas por el servidor          |
| 🕹️ **Mis juegos**                  | Vista dedicada con los juegos instalados, seguimiento de versión y tiempo de juego          |
| 🔄 **Actualizaciones automáticas** | Delta updates con [Velopack](https://velopack.io/) — el launcher se actualiza solo          |
| 📰 **Noticias y notificaciones**   | Feed de novedades y anuncios desde la pantalla de inicio                                    |
| 🎨 **Temas**                       | Temas visuales intercambiables: **Volcarona** y **Zoroark**                                 |
| 🌍 **Multiidioma**                 | 8 idiomas disponibles con traducciones completas de la interfaz                             |
| 🖥️ **Bandeja del sistema**         | Minimizar a bandeja con menú contextual (Abrir / Salir)                                     |
| 🚀 **Inicio con Windows**          | Opción de arrancar el launcher al iniciar sesión, en modo normal o minimizado               |

---

## 🌍 Idiomas soportados

Español · English · Català · Euskera · Galego · Português · Valencià · Français

El idioma se selecciona en los ajustes y se aplica al reiniciar la aplicación.

---

## 🏗️ Arquitectura

El proyecto sigue el patrón **MVVM** con **Inyección de Dependencias** centralizada.

```
EricLostieLauncher/
├── Core/               # Configuración del contenedor DI
├── Models/             # Modelos de datos
├── Services/           # Capa de servicios
├── ViewModels/         # ViewModels con CommunityToolkit.Mvvm
├── Views/              # Ventanas, diálogos y componentes WPF
│   ├── Components/     # GameCard, NewsCard, NotificationCard
│   └── Dialogs/        # DownloadConfirmDialog, WelcomeDialog, CustomMessageBox
├── Converters/         # Value converters XAML
├── Styles/             # Estilos globales
├── Themes/             # Recursos de tema
├── Content/            # Strings localizados
└── Assets/             # Iconos y recursos gráficos
```

### Servicios principales

| Servicio                 | Responsabilidad                                                      |
| ------------------------ | -------------------------------------------------------------------- |
| `IContentService`        | Obtiene el catálogo de juegos, noticias y registra juegos instalados |
| `IDownloadService`       | Gestiona descargas, extracción de archivos y sistema de claves       |
| `ISettingsService`       | Carga y persiste la configuración en `launcher_settings.json`        |
| `ITelemetryService`      | Envía datos de descarga y consulta estadísticas                      |
| `IWindowsStartupService` | Integración con el registro de Windows para el inicio automático     |

### ViewModels

| ViewModel           | Vista                                                 |
| ------------------- | ----------------------------------------------------- |
| `MainViewModel`     | Hub de navegación principal                           |
| `HomeViewModel`     | Pantalla de inicio con noticias y notificaciones      |
| `LibraryViewModel`  | Catálogo de juegos disponibles y gestión de descargas |
| `GamesViewModel`    | Juegos instalados                                     |
| `SettingsViewModel` | Panel de configuración                                |
| `GlobalViewModel`   | Estado global compartido                              |

---

## 🛠️ Tecnologías

| Paquete                                                                                                             | Versión  | Uso                                            |
| ------------------------------------------------------------------------------------------------------------------- | -------- | ---------------------------------------------- |
| [CommunityToolkit.Mvvm](https://github.com/CommunityToolkit/dotnet)                                                 | 8.4.1    | MVVM con `ObservableProperty` y `RelayCommand` |
| [MahApps.Metro.IconPacks](https://github.com/MahApps/MahApps.Metro.IconPacks)                                       | 6.2.1    | Iconos vectoriales en la UI                    |
| [Microsoft.Extensions.DependencyInjection](https://www.nuget.org/packages/Microsoft.Extensions.DependencyInjection) | 10.0.5   | Contenedor IoC                                 |
| [Microsoft.Extensions.Http](https://www.nuget.org/packages/Microsoft.Extensions.Http)                               | 10.0.5   | `IHttpClientFactory` con clientes nombrados    |
| [SharpCompress](https://github.com/adamhathcock/sharpcompress)                                                      | 0.47.3   | Extracción de archivos ZIP/7z descargados      |
| [Velopack](https://velopack.io/)                                                                                    | 0.0.1298 | Sistema de delta updates automáticos           |

---

## 🚀 Compilar y publicar

### Requisitos previos

- [.NET 10 SDK](https://dotnet.microsoft.com/download/dotnet/10.0)
- Windows 10/11
- PowerShell 7+ (para el script de release)
- `vpk` CLI de Velopack instalado globalmente

### Build de desarrollo

```powershell
cd EricLostieLauncher
dotnet build
```

### Build de release (local)

```powershell
.\scripts\build-release.ps1
```

Los artefactos se generan en `releases/`:

- `EricLostieLauncher-0.8.0-full.nupkg` — paquete de instalación inicial
- Paquetes delta (en builds sucesivas)
- `releases.win.json` — manifiesto de actualizaciones
- `RELEASES` — metadatos de Velopack

### Build de release con subida al servidor

```powershell
.\scripts\build-release.ps1 -Upload -SshHost "user@mi-servidor.com" -SshPath "/var/www/installer/"
```

---

## ⚙️ Configuración

La configuración se guarda automáticamente en `launcher_settings.json` junto al ejecutable.

| Opción              | Tipo          | Por defecto    | Descripción                                     |
| ------------------- | ------------- | -------------- | ----------------------------------------------- |
| `Language`          | `AppLanguage` | `Esp`          | Idioma de la interfaz                           |
| `Theme`             | `AppTheme`    | `Volcarona`    | Tema visual                                     |
| `StartWithWindows`  | `bool`        | `false`        | Arrancar al iniciar Windows                     |
| `StartMinimized`    | `bool`        | `false`        | Iniciar en la bandeja del sistema               |
| `AutoUpdate`        | `bool`        | `true`         | Buscar actualizaciones al iniciar               |
| `DownloadDirectory` | `string`      | Mis Documentos | Carpeta de instalación de juegos                |
| `HasSeenWelcome`    | `bool`        | `false`        | Controla si se muestra el diálogo de bienvenida |

---

## 📡 Endpoints de la API

| Endpoint                                                                 | Descripción                          |
| ------------------------------------------------------------------------ | ------------------------------------ |
| `https://ericlostie-launcher.jagoba.dev/games/listado.json`              | Catálogo de juegos disponibles       |
| `https://cdn.jagoba.dev/ericlostie-launcher/homepage-notifications.json` | Noticias y notificaciones del inicio |
| `https://ericlostie-launcher.jagoba.dev/games`                           | Base URL para descargas              |
| `https://ericlostie-launcher.jagoba.dev/public/installer/`               | Feed de actualizaciones Velopack     |

---

<div align="center">

EricLostieLauncher © 2026

</div>
