# Contributing to FTC Snippets

Thanks for helping out! This plugin is maintained by a small team, so clear,
focused pull requests are much easier to land than large ones.

## Prerequisites

- **JDK 17** or newer (the IntelliJ Platform 2025.3 toolchain itself runs on 21)
- No local IDE install is needed — Gradle downloads the matching IntelliJ
  Platform on the first build. Expect the first run to take a while.

## Building

All commands run from the repository root. On Windows use `gradlew.bat` in place
of `./gradlew`.

Build the distributable plugin ZIP into `build/distributions/`:

```bash
./gradlew buildPlugin
```

Launch a sandboxed IDE with the plugin installed:

```bash
./gradlew runIde
```

The sandbox keeps its own settings and plugin list, so it will not touch your
day-to-day Android Studio or IntelliJ configuration.

## Checks to run before opening a PR

Validate the plugin descriptor and the project's platform configuration:

```bash
./gradlew verifyPluginProjectConfiguration verifyPluginStructure
```

Run the JetBrains Plugin Verifier against the supported IDE range:

```bash
./gradlew verifyPlugin
```

Run the tests:

```bash
./gradlew test
```

Coverage is currently limited to `FtcFileTemplates`, which renders every
kind/language/framework/pattern combination and checks the result is well formed.
Anything that touches the IDE (actions, inspections, the settings page) still has
to be smoke-tested by hand: run `runIde`, open a real FTC project (a `TeamCode`
module), and say what you tried in the PR description.

## Project layout

| Path | What lives there |
| --- | --- |
| `src/main/kotlin/ontalent/ftcsnippets/actions/` | Toolbar and context-menu actions (imports, skeletons, coordinate converters) |
| `src/main/kotlin/ontalent/ftcsnippets/templates/` | The `New → FTC` file generators and their templates |
| `src/main/kotlin/ontalent/ftcsnippets/settings/` | Persisted defaults and the `Tools → FTC Snippets` settings page |
| `src/main/kotlin/ontalent/ftcsnippets/inspections/` | The FTC error helper inspection and its quick fixes |
| `src/main/resources/liveTemplates/` | Live template sets (Java and Kotlin) |
| `src/main/resources/META-INF/plugin.xml` | Plugin descriptor: actions, extensions, dependencies |
| `src/main/resources/META-INF/ftc-kotlin.xml` | Loaded only when the Kotlin plugin is present |

## How to add things

**A live template.** Add a `<template>` entry to
`src/main/resources/liveTemplates/FTCSnippets.xml`, and the Kotlin equivalent to
`FTCSnippetsKotlin.xml`. Newlines inside the `value` attribute must be written as
`&#10;` — a literal newline is collapsed to a space by the XML parser.

**A file generator.** Add a case to `FtcTemplateKind`, render it in
`FtcFileTemplates`, then declare an `<action>` and add it to the `FTC.New.Group`
in `plugin.xml`. Cover all four language/framework combinations.

**An inspection check.** Add the check plus its `LocalQuickFix` to
`FtcErrorInspection`, and document it in
`src/main/resources/inspectionDescriptions/FtcErrorInspection.html`. Register the
problem on the narrowest element you can, and remember that quick fixes receive
that element — walk up with `PsiTreeUtil.getParentOfType` rather than casting.

## Code style

- Kotlin, 4-space indent, matching the surrounding files.
- Comments explain *why*, not *what*. Skip them when the code already says it.
- Keep the Kotlin plugin dependency optional: nothing outside `ftc-kotlin.xml`
  may reference Kotlin plugin APIs, so Java-only users are never forced to
  install Kotlin support. Generate Kotlin source as text, not PSI.

## Pull requests

1. Branch off `main`.
2. Keep one logical change per commit; write commit messages that say why.
3. If the change is user-visible, add a line to `<change-notes>` in `plugin.xml`
   and bump `version` in `build.gradle.kts`.
4. Confirm `./gradlew buildPlugin` succeeds and describe your smoke test.

Feature ideas are also welcome through the
[request form](https://docs.google.com/forms/d/e/1FAIpQLSfiPetSgcHS7rM5jUP8UZqCLXNSI1tj04dkuW9TgNxn5z_iNg/viewform).
