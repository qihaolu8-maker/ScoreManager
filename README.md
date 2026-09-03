# ScoreManager 4.5

A Windows desktop application for managing student points, built with Java Swing,
FlatLaf, SQLite, Gson, and XChart.

## Features

- Organize students by major and class.
- Add or subtract points and keep a history of changes.
- Search, sort, and display score charts.
- Restore deleted records from the recycle bin and undo changes.
- Import CSV files and export CSV or printable HTML reports.
- Switch, rename, and move database files.

## Build a Windows application

Requirements: Windows x64 and JDK 21 with `javac`, `jar`, `jlink`, and `jpackage`.
The runtime dependencies are included in `lib`.

Run from PowerShell in the project directory:

```powershell
.\build-windows.ps1 -JdkHome 'C:\Path\To\Your\JDK21'
```

If `jpackage.exe` is on `PATH`, the JDK argument can be omitted:

```powershell
.\build-windows.ps1
```

The script compiles production sources, packages a private Java runtime, and
writes an application folder and a ZIP archive into `dist`. Existing release
folders are preserved; subsequent builds use a timestamped name when needed.

Extract the complete ZIP and launch `ScoreManager\ScoreManager.exe`. Keep the
EXE with its `app` and `runtime` directories. No separate Java installation is
required on the target computer. `TestMain` and `MockDbGenerator` are development
utilities and are excluded from the packaged application.

## Source layout

- `src/`: application source; the production entry point is `Main`.
- `lib/`: third-party JAR dependencies.
- `build-windows.ps1`: reproducible Windows build and packaging script.
- `build/`: generated intermediate files, excluded from Git.
- `dist/`: generated application packages, excluded from Git.

## Local data

The initial database and settings directory is:

```text
%USERPROFILE%\AppData\LocalLow\ScoreManager
```

The application remembers a database selected or moved through its interface.
Student data, database files, backups, local IDE settings, and generated build
outputs are excluded from version control. This repository does not include
student datasets. Distribute compiled ZIP packages separately from source,
for example as GitHub Release assets.

Third-party dependencies retain their own licenses and notices in their JARs.
