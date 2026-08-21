---
name: package-windows-distribution
description: Build and zip a self-contained Windows app-image of this JavaFX project for non-technical recipients.
---

# Package Windows Distribution

Create a portable Windows distribution of the current application. The recipient must be able to extract a ZIP and launch the bundled `.exe` without Java, JavaFX, Gradle, or developer tools installed.

## Scope and safety

- This is a packaging side task. Do not change source, Gradle configuration, application behaviour, or persistence behaviour.
- Do not commit anything.
- Record `git status --short` before starting and after finishing. Do not overwrite or clean unrelated user changes.
- Use the repository's ignored `out/` directory for final packaging output unless the user supplies another output directory. Use a uniquely named temporary directory for any staging work.
- If a build, discovery, or packaging step fails, report the failure and stop. Do not add permanent build configuration as a workaround.

## Discover the current project inputs

Inspect the project on every run; do not rely on paths from a previous package.

1. Read `build.gradle` and locate the `application.mainClass`, Java target, and configured JavaFX modules/version. If the main class cannot be established from the build, stop and ask for direction.
2. Run `java -version`, `jpackage --version`, and check that `jpackage` resolves from the selected JDK. `jpackage` must be available before continuing.
3. Build using the existing Windows Gradle wrapper:

   ```powershell
   .\gradlew.bat clean installDist --no-daemon
   ```

4. Discover the generated `build/install/*` distribution rather than assuming its application name. Select the directory containing `bin/` and `lib/`.
5. Find the application JAR in that `lib/` directory by inspecting candidate JARs with `jar tf` for the discovered main-class `.class` entry. Do not accidentally select a dependency JAR.
6. Confirm that the selected `lib/` directory contains the JavaFX dependency JARs required by the configured modules. Resources such as CSS are expected to be inside the application JAR produced by `installDist`.

## Create the portable app image

Use `jpackage --type app-image` with:

- `--name "Fencing Tournament Manager"`
- `--input <discovered installDist lib directory>`
- `--main-jar <discovered application JAR file name>`
- `--main-class <discovered main class>`
- `--add-modules <the configured JavaFX module list>`
- `--java-options "--enable-native-access=ALL-UNNAMED"` when the current JavaFX runtime emits or requires that native-access setting.
- `--dest <temporary app-image output directory>`

Keep all dependency JARs in the supplied `lib/` input. Do not package only the `.exe`; it depends on the generated runtime and app files.

Use a fresh output location so a partially failed package cannot overwrite a previously successful distribution. When packaging succeeds, copy or move the complete `Fencing Tournament Manager` app-image directory into an ignored output path such as:

```text
out\windows-distribution\Fencing Tournament Manager\
```

The resulting executable must be:

```text
out\windows-distribution\Fencing Tournament Manager\Fencing Tournament Manager.exe
```

Create `out\windows-distribution\Fencing-Tournament-Manager-Windows.zip` containing the *entire* app-image directory, preserving its top-level folder.

## Verify

1. Confirm the executable exists.
2. Confirm a bundled runtime exists inside the app image, normally `runtime\bin\java.exe`.
3. Launch the packaged executable from a temporary, non-repository working directory. A brief process smoke test is sufficient when interactive GUI inspection is unavailable; close only the process started by this verification.
4. Do not claim that a machine with no Java is verified unless the package contains the `runtime` directory. A jpackage app image with that runtime is self-contained and does not require a separately installed JDK/JRE.
5. Run `git status --short` again. Confirm no tracked project file was modified by the packaging task, while clearly distinguishing pre-existing user changes from packaging output.

## Deliverable report

Report the exact ZIP path and executable path, identify the ZIP as the file to share, and provide these recipient instructions:

1. Download the ZIP.
2. Extract the ZIP fully.
3. Open the extracted `Fencing Tournament Manager` folder.
4. Double-click `Fencing Tournament Manager.exe`.

Mention that an unsigned build can trigger Windows SmartScreen and that the recipient should inspect the publisher/source before choosing the operating system's appropriate option. Do not suggest disabling SmartScreen globally.

If WiX is already installed, an installer may be offered as an optional additional output. Do not install WiX or make it a requirement for the portable ZIP workflow.
