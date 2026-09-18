# FTC Snippets Plugin

[![JetBrains Marketplace](https://img.shields.io/jetbrains/plugin/v/28911-ftc-snippets?label=marketplace)](https://plugins.jetbrains.com/plugin/28911-ftc-snippets)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/28911-ftc-snippets)](https://plugins.jetbrains.com/plugin/28911-ftc-snippets)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

An Android Studio / IntelliJ plugin that speeds up **FIRST Tech Challenge** robot
development: complete class generation, live templates, hardware-map helpers, an
error helper, and a Pedro Pathing ↔ RoadRunner coordinate converter.

> **Note:** this plugin assumes the FTC SDK. Set up your `ftc_app` / `FtcRobotController`
> project first, otherwise there is nothing for it to hook into.

## Features

| Feature | What it does |
| --- | --- |
| **New → FTC** | Generates complete class files — Autonomous OpMode, TeleOp OpMode, Subsystem, Command, Robot Container — with the right package statement for the target directory |
| **Framework flavors** | Every generator emits either plain FTC SDK or SolversLib command-based code |
| **Subsystem patterns** | Subsystems can use a simple wrapper or the structured `read()` / `loop()` / `write()` split |
| **Java and Kotlin** | Both languages for every generator and every live template |
| **Live templates** | `ftcmotor`, `ftcservo`, `ftccrservo`, `ftcimu`, `ftcstart`, `ftcloop`, `ftctel`, `ftcmecanum`, `ftcteleop`, `ftcauto` |
| **Import snippets** | One-click imports for motors, servos, sensors, IMU and vision |
| **Smart hardware map** | Declare a hardware field and have the matching `hardwareMap.get()` generated |
| **Error helper** | Inspection with quick fixes for missing `@TeleOp`/`@Autonomous`, missing `waitForStart()`, missing `telemetry.update()`, uninitialized hardware, `Thread.sleep()`, loops that ignore `opModeIsActive()`, and out-of-range power/position |
| **Coordinate converter** | Converts poses between the Pedro Pathing and RoadRunner coordinate systems |
| **Docs search** | Jumps straight to the FTC documentation for a reference or sample |

## Settings

**Tools → FTC Snippets** sets the defaults the generators start from: language
(Java/Kotlin), framework (FTC SDK / SolversLib), subsystem pattern, and the
fallback base package.

## Installation

1. **JetBrains Marketplace** — [FTC Snippets](https://plugins.jetbrains.com/plugin/28911-ftc-snippets),
   or search "FTC Snippets" under *Settings → Plugins → Marketplace*.

2. **Beta builds** — grab a ZIP from the
   [releases page](https://github.com/Dxvid-Codes/FTC-Snippets-Plugin/releases)
   and install it with *Settings → Plugins → ⚙ → Install Plugin from Disk…*.

3. **From source** — see below.

## Building from Source

Requires JDK 21 or newer. Gradle downloads the matching IntelliJ Platform on the
first build, so expect that one to be slow. On Windows use `gradlew.bat` instead
of `./gradlew`.

```bash
git clone https://github.com/Dxvid-Codes/FTC-Snippets-Plugin.git
```

Build the installable ZIP into `build/distributions/`:

```bash
./gradlew buildPlugin
```

Launch a sandboxed IDE with the plugin already installed:

```bash
./gradlew runIde
```

Validate the plugin descriptor and platform configuration:

```bash
./gradlew verifyPluginProjectConfiguration verifyPluginStructure
```

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Feature requests are welcome through the
[request form](https://docs.google.com/forms/d/e/1FAIpQLSfiPetSgcHS7rM5jUP8UZqCLXNSI1tj04dkuW9TgNxn5z_iNg/viewform).

## Credits

The file-template concept — generating complete FTC class files from the *New*
menu — was inspired by
[CohenHill/FTC_Code_Snippets](https://github.com/CohenHill/FTC_Code_Snippets) (MIT).

## License

[MIT](LICENSE)
