# Fencing Tournament Manager

A local JavaFX desktop application for organising small internal fencing club tournaments.

The project currently contains only the application and domain skeleton. Tournament workflows
(registration, pool generation, results, standings, and elimination) have not yet been implemented.

## Prerequisites

- JDK 21 or later. The build targets Java 21.
- Internet access the first time Gradle resolves JavaFX and test dependencies.

## Build and test

From the project root, run:

```powershell
.\gradlew.bat clean test
```

## Run the desktop skeleton

```powershell
.\gradlew.bat run
```

The application opens a placeholder JavaFX window. It does not yet manage tournament data.

## Structure

- `src/main/java/.../ui` — JavaFX application entry point and future presentation code.
- `src/main/java/.../application` — application-service and repository boundary.
- `src/main/java/.../domain` — framework-independent tournament concepts and rules.
- `src/main/java/.../persistence` — local JSON persistence implementation.
- `src/test/java` — automated tests.
