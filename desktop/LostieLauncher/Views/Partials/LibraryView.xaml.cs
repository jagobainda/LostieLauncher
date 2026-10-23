using LostieLauncher.Models;
using LostieLauncher.ViewModels;
using System.Windows;
using System.Windows.Threading;

namespace LostieLauncher.Views.Partials;

public partial class LibraryView : System.Windows.Controls.UserControl
{
    public LibraryView()
    {
        InitializeComponent();
        DataContextChanged += (_, e) =>
        {
            if (e.OldValue is LibraryViewModel old) old.ScrollToGameRequested -= OnScrollToGameRequested;
            if (e.NewValue is LibraryViewModel vm)
            {
                vm.ScrollToGameRequested += OnScrollToGameRequested;
                if (vm.PendingScrollGameId is not null)
                    ScrollToGame(vm.PendingScrollGameId);
            }
        };
    }

    private void OnScrollToGameRequested(string gameId) => ScrollToGame(gameId);

    private void ScrollToGame(string gameId) => Dispatcher.BeginInvoke(() =>
                                                     {
                                                         var item = GamesItemsControl.Items.Cast<GameInfo>().FirstOrDefault(g => g.GameId == gameId);
                                                         if (item is null) return;
                                                         (GamesItemsControl.ItemContainerGenerator.ContainerFromItem(item) as FrameworkElement)?.BringIntoView();
                                                     }, DispatcherPriority.Loaded);
}

