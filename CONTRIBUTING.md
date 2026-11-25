# Contributing to Tonal

Thanks for your interest. Tonal is a small, personal-scale project, so a few
notes keep things easy for everyone.

## Reporting a bug

Open an issue and use the **Bug report** form. The most useful details are:

- your phone and Android version,
- what you did and what happened,
- a log, if you can get one: connect the phone and run
  `adb logcat -d > log.txt` right after the problem. Look through it first and
  remove anything private.

Tonal's own log lines use tags starting with `Tonal` or `TONAL`.

## Suggestions

Use the **Suggestion** form. Describe the situation the idea solves rather than
only the feature. Small, focused ideas are easier to act on.

## Building

- Android Studio with a recent Android Gradle Plugin, or `./gradlew assembleDebug`.
- Java 17, `minSdk` 24, `compileSdk`/`targetSdk` 37.
- There is no prebuilt APK in this repository.

## Pull requests

- Keep each pull request to one change, and describe what it does and how you
  tested it (device and Android version).
- Keep the existing behaviour, public APIs and preference keys unless the
  change is about them, so users' settings and caches keep working.
- Match the surrounding style. New text shown to users belongs in
  `res/values/strings.xml`.
- Do not add code or art you do not have the right to contribute. Do not add
  album covers, song titles or other third-party media (screenshots must use
  generic sample content).
- By contributing you agree that your contribution is released under the
  repository's [MIT License](LICENSE).

## Credit

Tonal is an unofficial fork of Minima Player by BKR APPS. See
[NOTICE.md](NOTICE.md). If you are a rights holder and want something changed
or removed, please open an issue or get in touch.
