# 安装教程 / 简体中文

适用版本：ScoreManager 4.6.0。运行包包含 Java 21，不需要另外安装 Java、Python 或开发工具。

## 1. 选择下载文件

前往 [GitHub Releases](https://github.com/qihaolu8-maker/ScoreManager/releases/latest)，展开 Assets。

| 电脑 | 文件 |
| --- | --- |
| Windows 64 位 Intel/AMD | `ScoreManager-4.6.0-windows-x64.zip` |
| Linux 64 位 Intel/AMD，glibc 图形桌面 | `ScoreManager-4.6.0-linux-x64.tar.gz` |
| Mac Apple 芯片，例如 M 系列 | `ScoreManager-4.6.0-macos-arm64.zip` |
| Mac Intel 芯片 | `ScoreManager-4.6.0-macos-x64.zip` |

Mac 点击苹果菜单 → 关于本机，查看“芯片”或“处理器”。[Apple 官方识别说明](https://support.apple.com/zh-cn/116943)。请勿下载 “Source code” 作为运行包。Windows ARM、Linux ARM 和 Alpine Linux 没有在本次发布中单独构建。

## 2. Windows

1. 下载 Windows ZIP，右键“全部解压”。
2. 将完整解压文件夹放在稳定的本地位置，例如“文档”中的 ScoreManager 文件夹。
3. 打开其中的 `ScoreManager` 文件夹，双击 `ScoreManager.exe`。
4. 保留旁边的 `app` 和 `runtime` 等目录，可为 EXE 创建桌面快捷方式。

程序尚未使用 Windows 开发者证书签名，首次下载运行可能出现系统提示。确认来源后按系统提示处理；如果安全软件明确报告恶意软件，应停止运行并反馈完整报告，而不是关闭保护。

构建及启动检查使用 Windows Server 2022 x64。其他桌面版本应以实际运行结果为准。

## 3. macOS

1. 下载对应芯片的 ZIP，**在 Mac 上**用 Finder 双击并由“归档实用工具”解压。
2. 找到 `ScoreManager.app`；Finder 隐藏扩展名时可能只显示 ScoreManager。
3. 将整个应用拖入“应用程序”，或保留在解压目录测试。
4. 双击应用，等待“创建管理员”或登录窗口。

不要先在 Windows 解压再复制散文件，这可能丢失执行权限；不要拆开 `.app` 中的启动器和 runtime。原生构建与启动检查使用 macOS 15，ARM64 和 Intel 分别构建；更旧的 macOS 未经过本次 CI 验证。

应用没有 Apple 开发者签名或公证。若提示无法验证开发者，确认来源后，先尝试打开一次，再进入“系统设置 → 隐私与安全性”，针对该应用选择“仍要打开”，完成系统确认。[Apple 官方操作说明](https://support.apple.com/zh-cn/102445)。旧系统的入口名称可能不同；被组织管理的电脑应联系管理员。

如果系统提示应用损坏或将损坏电脑，不要直接视为普通开发者提示。先重新获取原始压缩包、核对校验值并反馈报错。本教程不要求关闭系统安全保护。

## 4. Linux

使用带 glibc 的 x64 Linux 图形桌面，构建和启动检查使用 Ubuntu 22.04 与 Xvfb。实际使用需要 X11 或 XWayland 和相应桌面库。无图形界面的 SSH 会话无法直接显示窗口。

用归档管理器解压，或在下载目录运行：

```sh
tar -xzf ScoreManager-4.6.0-linux-x64.tar.gz
./ScoreManager-4.6.0-linux-x64/ScoreManager/bin/ScoreManager
```

请按压缩包实际顶层目录定位启动器。移动软件时保留整个解压目录，不要单独移动 `bin/ScoreManager`。不需要使用 root 运行，也不要将数据库保存到只读应用目录。

如果出现找不到 `libX11`、`libXext`、`libXi`、`libXrender`、`libXtst`、`fontconfig` 等库的错误，需要通过发行版的软件包管理器安装对应桌面运行库。包名随发行版不同；Ubuntu 可先确认已安装完整桌面环境。若提示无法连接显示器，检查是否处于正常桌面登录会话。

## 5. 首次登录与升级

新安装第一次打开空数据库时，按照“创建管理员”窗口设置自己的账号和至少 8 个字符的密码。4.6.0 没有通用默认密码。

升级用户先保存并退出旧版，备份实际使用的数据库后再运行新版本。现有账号继续有效，旧的明文密码会在成功登录后升级存储。迁移后的数据库应继续使用新版，旧版无法验证新密码哈希。旧备份及尚未登录过的账号仍可能保留旧明文密码。旧版生成的默认账号不会被静默删除；若仍在使用旧默认密码，请登录后通过账号管理修改。

## 6. 检查文件完整性

Release 附有 `SHA256SUMS.txt`。可用以下系统命令计算运行包哈希，与文件中对应行比较：

```powershell
# Windows PowerShell：替换为下载的文件名
Get-FileHash -Algorithm SHA256 .\ScoreManager-4.6.0-windows-x64.zip
```

```sh
# macOS
shasum -a 256 ScoreManager-4.6.0-macos-arm64.zip
# Linux
sha256sum ScoreManager-4.6.0-linux-x64.tar.gz
```

## 7. 启动后的下一步

阅读[使用说明](USER_GUIDE.zh-CN.md)。可用包内 `examples/ScoreManager-50-students.csv` 测试导入，或先建立专业和班级。正式数据请另行备份。

出现问题时记录：系统版本、芯片、运行包名称、启动错误和复现步骤。CI 启动检查能确认基础启动流程，不代表每台电脑的字体、窗口系统、打印机和缩放设置都完成了人工验收。
