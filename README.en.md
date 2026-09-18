# ScoreManager 4.6.0

[简体中文 / Bilingual README](README.md)

| 系统 / Platform | 下载 / Download | 启动 / Launch |
| --- | --- | --- |
| Windows x64 | [ScoreManager-4.6.0-windows-x64.zip](https://github.com/qihaolu8-maker/ScoreManager/releases/download/v4.6.0/ScoreManager-4.6.0-windows-x64.zip) | `ScoreManager/ScoreManager.exe` |
| Linux x64 | [ScoreManager-4.6.0-linux-x64.tar.gz](https://github.com/qihaolu8-maker/ScoreManager/releases/download/v4.6.0/ScoreManager-4.6.0-linux-x64.tar.gz) | `ScoreManager/bin/ScoreManager` |
| macOS Apple Silicon / ARM64 | [ScoreManager-4.6.0-macos-arm64.zip](https://github.com/qihaolu8-maker/ScoreManager/releases/download/v4.6.0/ScoreManager-4.6.0-macos-arm64.zip) | `ScoreManager.app` |
| macOS Intel / x64 | [ScoreManager-4.6.0-macos-x64.zip](https://github.com/qihaolu8-maker/ScoreManager/releases/download/v4.6.0/ScoreManager-4.6.0-macos-x64.zip) | `ScoreManager.app` |


ScoreManager is a Java Swing desktop application for managing student points on Windows, Linux and macOS. It uses FlatLaf for themes, SQLite for local storage and XChart for charts.

Download the matching package from [Releases](https://github.com/qihaolu8-maker/ScoreManager/releases/latest). **All application packages include a Java runtime.** Extract the complete package and use the launcher listed above. GitHub's automatically generated source archives are for developers, not ready-to-run applications.

On a new database, create your own administrator account and a password of at least eight characters. Version 4.6.0 has no built-in default credentials. Existing database accounts remain usable; legacy plaintext passwords are upgraded to salted hashes after a successful login. Back up your database before upgrading. Once an account is migrated, older versions cannot verify its new password hash; keep using the new version.

Features include majors and classes, student points, change history, search and sorting, charts, recycle bin and undo, CSV import/export, printable HTML reports and local administrator-managed accounts. A synthetic [50-student CSV](examples/ScoreManager-50-students.csv) is included for testing.

Read the [installation guide](docs/INSTALL.en.md), [user guide](docs/USER_GUIDE.en.md), and [build/test/release guide](docs/BUILD.md). Native CI covers Windows x64, Linux x64, macOS arm64 and macOS x64, with database regression tests and isolated startup checks. Check the [workflow results](https://github.com/qihaolu8-maker/ScoreManager/actions/workflows/release.yml) for the exact release. Full manual desktop and printer testing is still separate from CI.

Data is stored outside the application. macOS uses `~/Library/Application Support/ScoreManager`; Linux uses an absolute `$XDG_DATA_HOME/ScoreManager` or `~/.local/share/ScoreManager`; Windows uses `%USERPROFILE%\AppData\LocalLow\ScoreManager`. Switching to a different database requires signing in to that database again.

## Source layout

```text
src/                 Shared cross-platform application source
lib/                 Versioned runtime dependencies
tests/               Isolated regression tests
scripts/             Native build and smoke-test tools
.github/workflows/   Four-platform CI and draft releases
docs/                Chinese and English documentation
examples/            Synthetic import data
build/               Generated intermediate files (ignored)
dist/                Ready-to-run archives (ignored)
```

个人数据、密码数据库、本地设置和构建输出不会提交到仓库。User databases, local settings and build outputs are excluded from Git. Third-party notices are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
