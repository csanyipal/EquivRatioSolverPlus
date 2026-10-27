# EquivRatioSolverPlus

## About

**Direct** & **Inverse Proportionality** calculations, a **diagram** showing the **graph** of the proportionality function and its properties, **further mathematical processing** of one of the calculated proportionality factors, **saving** or **sending** the result, and a **comprehensive guide** to the theory of proportionality in text and images.

## License

EquivRatioSolverPlus is **free software**. Different parts of the project are released under different free licenses, as described in the [`LICENSE`](LICENSE) file:

- **Source code** – licensed under the **GNU General Public License, Version 3 or later** (GPL-3.0-or-later). The full license text is available in the [`COPYING`](COPYING) file. You may redistribute and/or modify the code under the terms of the GPL v3 or any later version.
- **Images, launcher icon components and HTML documentation** – licensed under the **Creative Commons Attribution-ShareAlike 4.0 International License** (CC BY-SA 4.0). This applies to the PNG images in `app/src/main/assets/`, the launcher icon components in `app/src/main/res/drawable/` and `app/src/main/res/mipmap-anydpi-v26/`, and the HTML documentation files in `app/src/main/assets/`.

CC BY-SA 4.0 is one-way compatible with the GPL v3, so the combined work may be distributed under the terms of the GPL v3, provided that the attribution and ShareAlike conditions of CC BY-SA 4.0 are also respected.

## Building

The application is written in **Kotlin** and uses **Jetpack Compose** for the user interface. It can be built with the standard Android/Gradle toolchain:

```bash
./gradlew assembleRelease
