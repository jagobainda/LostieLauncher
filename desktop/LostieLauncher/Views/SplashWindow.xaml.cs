using System.Windows;
using System.Windows.Media;
using System.Windows.Media.Animation;

namespace LostieLauncher.Views;

public partial class SplashWindow : Window
{
    private static readonly Duration ExitDuration = TimeSpan.FromMilliseconds(500);
    private static readonly TimeSpan FlapStep = TimeSpan.FromMilliseconds(200);
    private static readonly TimeSpan EmberPeak = TimeSpan.FromMilliseconds(180);
    private static readonly TimeSpan EmberRise = TimeSpan.FromMilliseconds(600);
    private const double EmberTravel = 16;
    private static readonly (string Name, int DelayMilliseconds)[] Embers =
        [("Ember1", 100), ("Ember2", 200), ("Ember3", 300), ("Ember4", 250), ("Ember5", 400), ("Ember6", 350)];

    private readonly Action _onIntroFinished;

    public SplashWindow(Action onIntroFinished)
    {
        InitializeComponent();
        _onIntroFinished = onIntroFinished;
        Loaded += (_, _) => PlayIntro();
    }

    private void PlayIntro()
    {
        var intro = new Storyboard();
        intro.Children.Add(Flap("LeftWingScale", "ScaleX", rest: 1, raised: 0.55));
        intro.Children.Add(Flap("MothOffset", "Y", rest: 0, raised: -2));

        foreach (var (name, delay) in Embers)
        {
            var begin = TimeSpan.FromMilliseconds(delay);
            intro.Children.Add(Target(new DoubleAnimation(-EmberTravel, EmberRise) { BeginTime = begin, EasingFunction = new QuadraticEase { EasingMode = EasingMode.EaseOut } }, $"{name}Offset", "Y"));
            intro.Children.Add(Target(Spark(begin), $"{name}Scale", "ScaleX"));
            intro.Children.Add(Target(Spark(begin), $"{name}Scale", "ScaleY"));
        }

        intro.Completed += OnIntroCompleted;
        intro.Begin(this);
    }

    private static Timeline Flap(string targetName, string property, double rest, double raised)
    {
        var ease = new SineEase { EasingMode = EasingMode.EaseInOut };
        var flap = new DoubleAnimationUsingKeyFrames();
        double[] values = [rest, raised, rest, raised, rest];
        for (var i = 0; i < values.Length; i++) flap.KeyFrames.Add(new EasingDoubleKeyFrame(values[i], FlapStep * i, ease));
        return Target(flap, targetName, property);
    }

    private static DoubleAnimationUsingKeyFrames Spark(TimeSpan begin) => new()
    {
        BeginTime = begin,
        KeyFrames = { new LinearDoubleKeyFrame(1, EmberPeak), new LinearDoubleKeyFrame(0, EmberRise) }
    };

    private static Timeline Target(Timeline timeline, string targetName, string property)
    {
        Storyboard.SetTargetName(timeline, targetName);
        Storyboard.SetTargetProperty(timeline, new PropertyPath(property));
        return timeline;
    }

    private void OnIntroCompleted(object? sender, EventArgs e)
    {
        _onIntroFinished();

        var iconBottom = SplashIcon.TranslatePoint(new(0, SplashIcon.ActualHeight), this).Y;
        SplashIconOffset.BeginAnimation(TranslateTransform.YProperty, new DoubleAnimation(-iconBottom, ExitDuration) { EasingFunction = new BackEase { EasingMode = EasingMode.EaseIn } });

        var fadeOut = new DoubleAnimation(0, ExitDuration);
        fadeOut.Completed += (_, _) => Close();
        BeginAnimation(OpacityProperty, fadeOut);
    }
}
