using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using EricLostieLauncher.Models;

namespace EricLostieLauncher.Views.Components;

public partial class GameCardComponent : UserControl
{
    public GameCardComponent()
    {
        InitializeComponent();
    }

    // --- Common properties ---

    public static readonly DependencyProperty GameLogoProperty = DependencyProperty.Register(nameof(GameLogo), typeof(ImageSource), typeof(GameCardComponent));

    public ImageSource? GameLogo
    {
        get => (ImageSource?)GetValue(GameLogoProperty);
        set => SetValue(GameLogoProperty, value);
    }

    public static readonly DependencyProperty GameTitleProperty = DependencyProperty.Register(nameof(GameTitle), typeof(string), typeof(GameCardComponent), new PropertyMetadata(string.Empty));

    public string GameTitle
    {
        get => (string)GetValue(GameTitleProperty);
        set => SetValue(GameTitleProperty, value);
    }

    public static readonly DependencyProperty CardModeProperty = DependencyProperty.Register(nameof(CardMode), typeof(GameCardMode), typeof(GameCardComponent), new PropertyMetadata(GameCardMode.Library));

    public GameCardMode CardMode
    {
        get => (GameCardMode)GetValue(CardModeProperty);
        set => SetValue(CardModeProperty, value);
    }

    // --- Library properties ---

    public static readonly DependencyProperty GameSizeProperty = DependencyProperty.Register(nameof(GameSize), typeof(string), typeof(GameCardComponent), new PropertyMetadata(string.Empty));

    public string GameSize
    {
        get => (string)GetValue(GameSizeProperty);
        set => SetValue(GameSizeProperty, value);
    }

    public static readonly DependencyProperty TotalDownloadsProperty = DependencyProperty.Register(nameof(TotalDownloads), typeof(string), typeof(GameCardComponent), new PropertyMetadata(string.Empty));

    public string TotalDownloads
    {
        get => (string)GetValue(TotalDownloadsProperty);
        set => SetValue(TotalDownloadsProperty, value);
    }

    public static readonly DependencyProperty LatestVersionProperty = DependencyProperty.Register(nameof(LatestVersion), typeof(string), typeof(GameCardComponent), new PropertyMetadata(string.Empty));

    public string LatestVersion
    {
        get => (string)GetValue(LatestVersionProperty);
        set => SetValue(LatestVersionProperty, value);
    }

    public static readonly DependencyProperty DownloadStatusProperty = DependencyProperty.Register(nameof(DownloadStatus), typeof(GameDownloadStatus), typeof(GameCardComponent), new PropertyMetadata(GameDownloadStatus.Available));

    public GameDownloadStatus DownloadStatus
    {
        get => (GameDownloadStatus)GetValue(DownloadStatusProperty);
        set => SetValue(DownloadStatusProperty, value);
    }

    public static readonly DependencyProperty DownloadProgressProperty = DependencyProperty.Register(nameof(DownloadProgress), typeof(double), typeof(GameCardComponent), new PropertyMetadata(0.0));

    public double DownloadProgress
    {
        get => (double)GetValue(DownloadProgressProperty);
        set => SetValue(DownloadProgressProperty, value);
    }

    public static readonly DependencyProperty DownloadCommandProperty = DependencyProperty.Register(nameof(DownloadCommand), typeof(ICommand), typeof(GameCardComponent));

    public ICommand? DownloadCommand
    {
        get => (ICommand?)GetValue(DownloadCommandProperty);
        set => SetValue(DownloadCommandProperty, value);
    }

    // --- Games properties ---

    public static readonly DependencyProperty InstalledVersionProperty = DependencyProperty.Register(nameof(InstalledVersion), typeof(string), typeof(GameCardComponent), new PropertyMetadata(string.Empty));

    public string InstalledVersion
    {
        get => (string)GetValue(InstalledVersionProperty);
        set => SetValue(InstalledVersionProperty, value);
    }

    public static readonly DependencyProperty UpdateVersionProperty = DependencyProperty.Register(nameof(UpdateVersion), typeof(string), typeof(GameCardComponent), new PropertyMetadata(string.Empty));

    public string UpdateVersion
    {
        get => (string)GetValue(UpdateVersionProperty);
        set => SetValue(UpdateVersionProperty, value);
    }

    public static readonly DependencyProperty HasUpdateProperty = DependencyProperty.Register(nameof(HasUpdate), typeof(bool), typeof(GameCardComponent), new PropertyMetadata(false));

    public bool HasUpdate
    {
        get => (bool)GetValue(HasUpdateProperty);
        set => SetValue(HasUpdateProperty, value);
    }

    public static readonly DependencyProperty PlayCommandProperty = DependencyProperty.Register(nameof(PlayCommand), typeof(ICommand), typeof(GameCardComponent));

    public ICommand? PlayCommand
    {
        get => (ICommand?)GetValue(PlayCommandProperty);
        set => SetValue(PlayCommandProperty, value);
    }

    public static readonly DependencyProperty UpdateCommandProperty = DependencyProperty.Register(nameof(UpdateCommand), typeof(ICommand), typeof(GameCardComponent));

    public ICommand? UpdateCommand
    {
        get => (ICommand?)GetValue(UpdateCommandProperty);
        set => SetValue(UpdateCommandProperty, value);
    }

    public static readonly DependencyProperty OpenFolderCommandProperty = DependencyProperty.Register(nameof(OpenFolderCommand), typeof(ICommand), typeof(GameCardComponent));

    public ICommand? OpenFolderCommand
    {
        get => (ICommand?)GetValue(OpenFolderCommandProperty);
        set => SetValue(OpenFolderCommandProperty, value);
    }
}
