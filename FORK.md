# Personal fork of Loop Habit Tracker

This is a personal fork of [iSoron/uhabits](https://github.com/iSoron/uhabits) that adds a
weekly multi-habit home-screen widget. It is not meant to be merged upstream.

## Remotes and branches

| Name | Points to |
| --- | --- |
| `origin` | `Marynat/uhabits` (this fork) |
| `upstream` | `iSoron/uhabits` (the original app) |
| `widget/weekly-habits` | Our work, based on `upstream/dev` |

The branch is based on `upstream/dev`, not on the latest release (v2.3.2). `dev` already
contains large changes that will ship in the next release: kotlin-inject instead of
Dagger, `LocalDate` instead of `Timestamp`, the Kotlin Multiplatform move, the habit list
refactor and habit categories. Building the widget on v2.3.2 would mean rewriting it
when that release comes out.

## Keeping up to date

```bash
git fetch upstream --tags
git merge upstream/dev        # or a release tag, e.g. git merge v2.4.0
```

Then build, install and check the widget.

## Rules that keep merges painless

- Put widget code in new files under `uhabits-android/.../widgets/` (own package or
  `Weekly*` prefix). Touch existing files only where unavoidable (manifest entry,
  resources) and keep those edits small.
- Build on the APIs upstream uses for its own widgets (`BaseWidget`, `HabitList`,
  `WidgetReceiver` intents). When upstream changes them it fixes its own widgets in the
  same commit, which shows how to fix ours.
- The widget deliberately reuses the main screen's drawing code (checkmark buttons, score
  ring, header) so it looks the same. That code gets refactored upstream now and then
  (last time in Oct 2026), so expect the occasional fix-up after a merge. Keep the
  reuse at the level of individual views and themes, not whole screens.
- Never change the database schema, so backups stay compatible with the official app.

## Where the widget lives

New code is in `uhabits-android/src/main/java/org/isoron/uhabits/widgets/weekly/` plus
resources named `widget_weekly_habits*`, `strings_weekly_widget.xml` and
`styles_weekly_widget.xml`. Existing files
touched (each edit is marked with a `Fork:` comment):

- `AndroidManifest.xml`: registers the widget provider, its tap receiver and the list
  service.
- `widgets/WidgetUpdater.kt`: refreshes the weekly widget on every change. The per-habit
  filter there looks up habit ids stored for single-habit widgets, which this widget has
  none of.
- `uhabits-android/build.gradle.kts`: adds the `fork` build type (see Building).

The rows are the main screen's own checkmark and number panels drawn into bitmaps, so
upstream changes to those views show up in the widget. `WeeklyHabitRowView` copies the
sizes of `HabitCardView`; check them after a merge that touches the habit list.

The day header is plain text views that copy `HeaderView`'s style, not a bitmap. On some
Android 17 builds, a bitmap in the widget's frame shifts every list row's bitmap by one, so
each row shows the habit above it and the first row shows the header.

## Building

- Android SDK path goes in `local.properties` (`sdk.dir=...`, not committed).
- The project's Gradle toolchain asks for **JDK 17**. Install one, e.g.
  `brew install --cask temurin@17`, then:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew :uhabits-android:assembleFork
```

The APK ends up in `uhabits-android/build/outputs/apk/fork/`.

The `fork` build type (in `uhabits-android/build.gradle.kts`, plus `src/fork/`) is a debug
build with the package name `org.isoron.uhabits.fork` and the label "Habits (fork)". Its
FileProvider gets its own authority. The plain `debug` build keeps upstream's package name.

## Installing on a phone

```bash
adb install -r uhabits-android/build/outputs/apk/fork/uhabits-android-fork.apk
```

- The fork build installs next to the official app. The two keep separate data and don't
  sync. To start from your real habits, export a backup in the official app
  (Settings → Export full backup) and import it in "Habits (fork)".
- Sharing an exported file from the fork through the share sheet doesn't work: upstream
  hardcodes the official app's FileProvider authority in `utils/ViewExtensions.kt`.
  Importing is not affected.
- The label override only covers the default (English) strings. On a phone set to a
  language with its own translation, both apps are called "Habits" in that language.
- `dev` uses database version 26 (habit categories); v2.3.2 uses 25. A backup made by this
  build cannot be imported into the official v2.3.2 app until upstream releases the next
  version.
- No automatic updates. Optional: a GitHub Actions workflow that builds an APK release on
  push, plus [Obtainium](https://github.com/ImranR98/Obtainium) on the phone.
