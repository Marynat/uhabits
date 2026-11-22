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
resources named `widget_weekly_habits*` and `strings_weekly_widget.xml`. Existing files
touched (each edit is marked with a `Fork:` comment):

- `AndroidManifest.xml`: registers the widget provider, its tap receiver and the list
  service.
- `widgets/WidgetUpdater.kt`: refreshes the weekly widget on every change. The per-habit
  filter there looks up habit ids stored for single-habit widgets, which this widget has
  none of.

The rows are the main screen's own checkmark and number panels drawn into bitmaps, so
upstream changes to those views show up in the widget. `WeeklyHabitRowView` copies the
sizes of `HabitCardView`; check them after a merge that touches the habit list.

## Building

- Android SDK path goes in `local.properties` (`sdk.dir=...`, not committed).
- The project's Gradle toolchain asks for **JDK 17**. Install one, e.g.
  `brew install --cask temurin@17`, then:

```bash
./gradlew :uhabits-android:assembleDebug
```

The APK ends up in `uhabits-android/build/outputs/apk/debug/`.

## Installing on a phone

- This build is signed with a different key than the Play Store / F-Droid app, so it
  cannot be installed over the official app. Export a backup in the official app first
  (Settings → Export full backup), uninstall it, install this build, then import the
  backup.
- `dev` uses database version 26 (habit categories); v2.3.2 uses 25. A backup made by this
  build cannot be imported into the official v2.3.2 app until upstream releases the next
  version.
- No automatic updates. Optional: a GitHub Actions workflow that builds an APK release on
  push, plus [Obtainium](https://github.com/ImranR98/Obtainium) on the phone.
