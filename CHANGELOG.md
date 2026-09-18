# Changelog / 更新记录

## 4.6.0

### 中文

- 使用共享 Java 源码，按 Windows x64、Linux x64、macOS ARM64、macOS Intel 分别原生打包，自带 Java 21。
- 新空数据库由用户自行创建管理员，不再注入固定默认账号。
- 密码使用随机盐与 PBKDF2 哈希；旧版明文密码成功登录后迁移，账号列表不再暴露密码。
- 账号管理执行管理员校验；切换数据库后重新登录，避免沿用其他数据库的身份。
- macOS 使用 Application Support，Linux 支持 XDG_DATA_HOME，Windows 保持原存档路径。
- 改善小屏幕窗口尺寸、平台快捷键、退出保存，以及不支持透明窗口的 Linux 桌面兼容性。
- 新增四平台自动构建、隔离数据库回归与启动检查，以及中英文安装、使用和构建说明。
- 提供 50 人虚构 CSV 测试数据。

### English

- Native self-contained packages for Windows x64, Linux x64, macOS arm64 and macOS Intel, from one shared Java source tree.
- User-created first administrator, salted PBKDF2 password storage and migration of legacy accounts after successful login.
- No password disclosure in account lists; administrator checks and reauthentication after switching databases.
- Platform-correct data paths, Linux XDG support, smaller-screen layout improvements, platform shortcuts and desktop compatibility fixes.
- Four-platform build automation, isolated regression/startup checks, bilingual documentation and a synthetic 50-student CSV.

## 4.5

Initial import of the Swing desktop application, local data fixes and Windows packaging. Earlier locally assembled Mac bundles were not native CI builds; use the 4.6.0 release for the updated pipeline and account behavior.
