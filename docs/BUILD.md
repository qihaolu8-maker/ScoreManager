# Build, test and release / 源码构建、测试与发布

## Requirements / 环境

- JDK 21, including `javac`, `jar`, `jlink`, `jpackage`.
- Python 3.9 or newer.
- Build on the target operating system and CPU architecture. Native `jpackage` launchers are not cross-compiled from Windows to other platforms.
- 项目运行依赖位于 `lib/`，由构建脚本的共享清单选择。开发辅助入口不进入生产 JAR。

## Local build / 本机构建

From the repository root, with JDK 21 on PATH:

```sh
python scripts/build.py --version 4.6.0 --test --smoke
```

For compilation and isolated regression tests without creating a release archive:

```sh
python scripts/build.py --version 4.6.0 --compile-only --test
```

Use `python scripts/build.py --help` for the exact runtime-supported options. Build output goes to ignored `build/` and `dist/` directories. 不需要把个人数据库或设置放进源码目录。

The build creates production classes, a JAR, a reduced private Java runtime and a native application image. ZIP is used on Windows and macOS; Linux uses tar.gz to preserve executable permissions. The complete application, bilingual documentation, third-party notices and synthetic example CSV are included.

## Validation / 检查范围

`--test` compiles and executes isolated Java regression tests. Test databases live in temporary directories and do not use real user data. `--smoke` checks the bundled runtime, starts the application with isolated storage and verifies the startup flow. Linux smoke tests need a display; CI supplies Xvfb.

数据库检查与基础启动检查不能替代对所有桌面缩放、输入法、字体、打印设备或系统版本的人工测试。日志和 `BUILD-INFO.json` 记录构建信息；GitHub Actions 展示具体工作流结果。

## GitHub Actions

The `release.yml` workflow builds four native artifacts: Windows x64, Linux x64, macOS arm64 and macOS x64. A manual dispatch can validate a version; version tags create a draft release for review before publication. See the workflow inputs for publishing controls.

1. Commit reviewed source, tests and documentation.
2. Run the workflow on the desired commit and verify every matrix job.
3. Create the version tag only for that verified commit, or run the documented draft-release mode.
4. Check all four archives and the release checksum list before publishing the draft.

发布前不要把旧版本、失败作业的输出或不同提交的产物混入同一版本。源码 ZIP、学生数据库、机器私有凭据与可运行包是不同内容；个人数据不可作为发行附件上传。

## Dependencies and signing

Versioned runtime dependencies are selected explicitly; old duplicate JARs and developer test classes are not put on the production classpath. Their embedded notices and license files remain intact. The bundled Java runtime retains its legal notices.

No Windows code-signing certificate or Apple Developer ID has been configured. Native packaging does not imply signing or Apple notarization. Never publish signing credentials in source; configure them separately only when authorized.
