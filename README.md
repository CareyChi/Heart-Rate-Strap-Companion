# Heart Rate Strap Companion

Android 心率带伴侣。V0.0.1 首先实现标准 BLE 心率带的“持续记录”闭环，骑行记录暂不开发。

## V0.0.1

- Java / Android 原生实现。
- 标准 Bluetooth SIG Heart Rate Service (`0x180D`) / Heart Rate Measurement (`0x2A37`)。
- 心率每秒刷新与每秒记录。
- 1 小时实时曲线窗口，真实 `HH:mm` 时间轴，左右拖动、边缘阻尼和回弹。
- 25 bpm 分档坐标，主图显示最大档、中间档、平均档和 0。
- 绿色科技感曲线渐变填充。
- 后台持续记录前台服务。
- 可拖动悬浮窗，单击返回记录页。
- Room 本地历史记录与模糊背景详情弹层。
- 首次启动后台保活说明，以及悬浮窗/电池优化设置入口。
- 32 位 ARM 与 64 位 ARM 独立构建产物。

详细产品和工程规则见 [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md)。

## 工程结构

- `core/`：纯 Java；BLE 心率包解析、25 bpm 档位算法、统计逻辑。避免 Android API，为后续其他移动端实现保留稳定业务语义。
- `app/`：Android BLE、前台服务、Room、权限、悬浮窗、图表和界面。

## 构建

CI 使用 Gradle 8.9 + JDK 17：

```bash
gradle :core:test :app:testArm32DebugUnitTest :app:testArm64DebugUnitTest
gradle :app:lintArm32Debug :app:lintArm64Debug
gradle :app:assembleArm32Release :app:assembleArm64Release
```

V0.0.1 CI release APK 使用开发签名，因此可直接安装测试。正式长期分发时必须改为 GitHub Secrets 注入固定 release keystore，生产私钥不得提交到公开仓库。

## 兼容说明

Android UI、BLE、权限和悬浮窗属于平台代码，不能直接复用到 iOS。跨平台准备采用业务核心和平台适配层分离，而不是宣称 Java Android UI 可直接跨平台运行。
