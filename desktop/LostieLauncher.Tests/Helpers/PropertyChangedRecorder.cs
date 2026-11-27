using System.ComponentModel;

namespace LostieLauncher.Tests.Helpers;

public sealed class PropertyChangedRecorder : IDisposable
{
    private readonly INotifyPropertyChanged _source;
    private readonly List<string?> _changes = [];
    private readonly Lock _sync = new();

    public PropertyChangedRecorder(INotifyPropertyChanged source)
    {
        _source = source;
        _source.PropertyChanged += OnPropertyChanged;
    }

    public IReadOnlyList<string?> Changes
    {
        get { lock (_sync) { return [.. _changes]; } }
    }

    public int CountOf(string propertyName)
    {
        lock (_sync) { return _changes.Count(c => string.Equals(c, propertyName, StringComparison.Ordinal)); }
    }

    public bool WasRaised(string propertyName) => CountOf(propertyName) > 0;

    public void Clear() { lock (_sync) { _changes.Clear(); } }

    private void OnPropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
        lock (_sync) { _changes.Add(e.PropertyName); }
    }

    public void Dispose() => _source.PropertyChanged -= OnPropertyChanged;
}
