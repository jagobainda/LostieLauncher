using System.Windows;
using System.Windows.Input;

namespace LostieLauncher.Utils;

public static class KeyboardShortcuts
{
    public static void Bind(UIElement target, Key key, ModifierKeys modifiers, Action action) =>
        target.InputBindings.Add(CreateBinding(new ActionCommand(action), key, modifiers));

    public static void BindCommand(UIElement target, Key key, ModifierKeys modifiers, ICommand command) =>
        target.InputBindings.Add(CreateBinding(command, key, modifiers));

    private static KeyBinding CreateBinding(ICommand command, Key key, ModifierKeys modifiers) =>
        new() { Command = command, Key = key, Modifiers = modifiers };

    public static void RegisterDialog(
        Window window,
        Action? onConfirm = null,
        Action? onCancel = null,
        Action? onYes = null,
        Action? onNo = null)
    {
        if (onConfirm != null) Bind(window, Key.Enter, ModifierKeys.None, onConfirm);
        if (onCancel != null) Bind(window, Key.Escape, ModifierKeys.None, onCancel);
        if (onYes != null) Bind(window, Key.Y, ModifierKeys.None, onYes);
        if (onNo != null) Bind(window, Key.N, ModifierKeys.None, onNo);
    }

    private sealed class ActionCommand(Action execute) : ICommand
    {
        public event EventHandler? CanExecuteChanged { add { } remove { } }
        public bool CanExecute(object? parameter) => true;
        public void Execute(object? parameter) => execute();
    }
}
