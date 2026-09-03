# ScoreManager 4.5

[简体中文](#简体中文) | [English](#english)

## 简体中文

ScoreManager 是一款用于管理学生积分的 Windows 桌面应用，基于 Java Swing、
FlatLaf、SQLite、Gson 和 XChart 构建。

### 功能

- 按专业和班级组织学生信息。
- 为学生加分、扣分，并记录积分变更历史。
- 搜索、排序学生数据，查看积分统计图表。
- 从回收站还原删除的记录，并撤销操作。
- 导入 CSV 文件，导出 CSV 或可打印的 HTML 报表。
- 切换、重命名和移动数据库文件。

### 构建 Windows 应用

环境要求：Windows x64，以及包含 `javac`、`jar`、`jlink` 和 `jpackage` 的 JDK 21。
运行时依赖已包含在 `lib` 目录中。

在项目目录打开 PowerShell，执行：

```powershell
.\build-windows.ps1 -JdkHome 'C:\Path\To\Your\JDK21'
```

如果 `jpackage.exe` 已加入 `PATH`，可以省略 JDK 路径：

```powershell
.\build-windows.ps1
```

脚本会编译正式程序源码，打包独立的 Java 运行环境，并将应用文件夹和 ZIP 压缩包
输出到 `dist` 目录。已有的发布目录会保留；重复构建时，必要情况下会在名称中添加时间戳。

完整解压 ZIP 后，双击 `ScoreManager\ScoreManager.exe` 启动程序。
请保留 EXE 所在目录中的 `app` 和 `runtime` 文件夹，不要单独移动 EXE。
目标电脑无需另外安装 Java。`TestMain` 和 `MockDbGenerator` 是开发辅助程序，
不会包含在打包后的应用中。

### 项目结构

- `src/`：应用源码，正式程序入口为 `Main`。
- `lib/`：第三方 JAR 依赖。
- `build-windows.ps1`：Windows 编译与打包脚本。
- `build/`：生成的中间文件，不提交到 Git。
- `dist/`：生成的应用发布包，不提交到 Git。

### 本地数据

初始数据库和设置文件位于：

```text
%USERPROFILE%\AppData\LocalLow\ScoreManager
```

通过程序界面选择、重命名或移动数据库后，程序会记住新的存档路径。
学生数据、数据库文件、备份、本地 IDE 设置以及生成的构建产物均不纳入版本控制。
本仓库不包含学生数据集。编译后的 ZIP 包应与源码分开发送，例如作为 GitHub Release 附件。

第三方依赖的许可证和声明保留在各自的 JAR 文件中。

## English

A Windows desktop application for managing student points, built with Java Swing,
FlatLaf, SQLite, Gson, and XChart.

### Features

- Organize students by major and class.
- Add or subtract points and keep a history of changes.
- Search, sort, and display score charts.
- Restore deleted records from the recycle bin and undo changes.
- Import CSV files and export CSV or printable HTML reports.
- Switch, rename, and move database files.

### Build a Windows application

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

### Source layout

- `src/`: application source; the production entry point is `Main`.
- `lib/`: third-party JAR dependencies.
- `build-windows.ps1`: reproducible Windows build and packaging script.
- `build/`: generated intermediate files, excluded from Git.
- `dist/`: generated application packages, excluded from Git.

### Local data

The initial database and settings directory is:

```text
%USERPROFILE%\AppData\LocalLow\ScoreManager
```

The application remembers the new database path after a file is selected,
renamed, or moved through its interface.
Student data, database files, backups, local IDE settings, and generated build
outputs are excluded from version control. This repository does not include
student datasets. Distribute compiled ZIP packages separately from source,
for example as GitHub Release assets.

Third-party dependencies retain their own licenses and notices in their JARs.
