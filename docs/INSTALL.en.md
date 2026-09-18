# Installation guide / English

Version: ScoreManager 4.6.0. Every application package includes Java 21. End users do not need Java, Python, or development tools installed separately.

## 1. Choose a package

Open [GitHub Releases](https://github.com/qihaolu8-maker/ScoreManager/releases/latest) and expand Assets.

| Computer | File |
| --- | --- |
| Windows Intel/AMD 64-bit | `ScoreManager-4.6.0-windows-x64.zip` |
| Linux Intel/AMD 64-bit, glibc graphical desktop | `ScoreManager-4.6.0-linux-x64.tar.gz` |
| Mac with Apple Silicon | `ScoreManager-4.6.0-macos-arm64.zip` |
| Mac with an Intel processor | `ScoreManager-4.6.0-macos-x64.zip` |

On a Mac, open Apple menu → About This Mac and look for Chip or Processor. [Apple's identification guide](https://support.apple.com/116943). The automatically generated “Source code” archives are not application packages. Windows ARM, Linux ARM and Alpine Linux are not separately built for this release.

## 2. Windows

1. Download the Windows ZIP and use Extract All.
2. Keep the entire extracted directory in a stable local location.
3. Open its `ScoreManager` directory and launch `ScoreManager.exe`.
4. Keep the adjacent application and runtime directories. A shortcut to the EXE is fine.

The application is not Windows developer-signed. Handle any first-run prompt only after checking the source. If security software explicitly identifies malware, stop and report the full message instead of disabling protection.

The build and startup checks use Windows Server 2022 x64. Other desktop configurations still require real-world validation.

## 3. macOS

1. Download the ZIP for your chip. Extract the original ZIP on the Mac with Archive Utility.
2. Find `ScoreManager.app`; Finder may hide the extension.
3. Move the whole app to Applications, or run it from the extracted folder for testing.
4. Open it and wait for administrator setup or login.

Do not extract on Windows and then copy individual files, as UNIX executable permissions may be lost. Do not separate the runtime from the app. Native builds and startup checks use macOS 15 for both architectures; older macOS versions have not been tested by this workflow.

The application is not Apple Developer ID signed or notarized. For an unidentified-developer prompt, verify the source, attempt to open the app, then use System Settings → Privacy & Security → Open Anyway for this specific app. Follow [Apple's instructions](https://support.apple.com/102445). Managed devices may require an administrator.

A damaged-app or malware warning is not the same as an unidentified-developer prompt. Obtain the original archive again, verify its checksum and report the error. This guide does not require disabling system security.

## 4. Linux

Use an x64 Linux graphical desktop with glibc. CI uses Ubuntu 22.04 and Xvfb. Interactive use needs X11 or XWayland and the usual desktop libraries; an SSH session without a display cannot show a window.

```sh
tar -xzf ScoreManager-4.6.0-linux-x64.tar.gz
./ScoreManager-4.6.0-linux-x64/ScoreManager/bin/ScoreManager
```

You can also use an archive manager. Keep the entire extracted directory and launch its `ScoreManager/bin/ScoreManager`. Run as a regular user. If shared-library errors mention X11, Xext, Xi, Xrender, Xtst or fontconfig, install the corresponding desktop libraries using your distribution's package manager. Package names vary by distribution.

## 5. First account and upgrades

A new empty database prompts you to create an administrator account with a password of at least eight characters. There is no universal default password in 4.6.0.

For upgrades, save and close the old version and back up your actual database before opening the new one. Existing accounts keep working. Legacy plaintext passwords are migrated after successful login. Older app versions cannot verify migrated password hashes; keep using the new version. Previous backups and accounts that have not logged in may still contain legacy plaintext passwords. Existing default accounts are not silently deleted; change any old default password through account management.

## 6. Verify downloads

Compare the package's SHA-256 against the matching entry in the release's `SHA256SUMS.txt`:

```powershell
Get-FileHash -Algorithm SHA256 .\ScoreManager-4.6.0-windows-x64.zip
```

```sh
# macOS
shasum -a 256 ScoreManager-4.6.0-macos-arm64.zip
# Linux
sha256sum ScoreManager-4.6.0-linux-x64.tar.gz
```

## 7. Start using the app

Read the [user guide](USER_GUIDE.en.md). The package includes `examples/ScoreManager-50-students.csv` with synthetic data for import testing. Report issues with your OS version, chip, package filename, error and reproduction steps. Startup CI is not full manual coverage of fonts, scaling, desktop environments or printers.
