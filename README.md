# 屏幕卫士 (ScreenTimeGuardian)

帮助您戒断短视频瘾的 Android 应用。

## 📱 应用简介

屏幕卫士是一款帮助用户控制短视频使用时间的健康应用。当您连续使用抖音、快手、小红书、微信视频号等短视频应用超过设定时间（默认20分钟）时，应用会弹出全屏提醒，帮助您及时休息。

## ✨ 功能特性

- 🎯 **智能监控** - 自动监控抖音、快手、小红书、微信等短视频应用的使用时间
- ⏰ **自定义时限** - 可设置5-60分钟的使用时间限制
- 🔔 **强制提醒** - 超时后显示全屏悬浮窗提醒，并震动提示
- 📊 **使用统计** - 记录每日使用时间和提醒次数
- 🚀 **开机自启** - 支持开机自动启动监控服务
- 🔒 **后台运行** - 前台服务保证监控持续稳定

## 📋 监控的应用

| 应用 | 包名 |
|------|------|
| 抖音 | com.ss.android.ugc.aweme |
| 抖音极速版 | com.ss.android.ugc.aweme.lite |
| 快手 | com.smile.gifmaker |
| 快手极速版 | com.kuaishou.nebula |
| 小红书 | com.xingin.xhs |
| 微信 | com.tencent.mm |

## 🔐 所需权限

1. **使用情况访问权限** - 用于检测当前使用的应用
2. **悬浮窗权限** - 用于显示超时提醒窗口
3. **通知权限** - 用于显示前台服务通知
4. **开机启动权限** - 用于开机自动启动监控

## 📥 安装说明

### 方式一：直接下载APK

下载 [ScreenTimeGuardian-v1.0-debug.apk](./ScreenTimeGuardian-v1.0-debug.apk) 并安装。

### 方式二：源码编译

```bash
# 克隆仓库
git clone <repository-url>
cd ScreenTimeGuardian

# 编译Debug版本
./gradlew assembleDebug

# APK位于
# app/build/outputs/apk/debug/app-debug.apk
```

## 🎮 使用方法

1. 安装并打开应用
2. 授予必要权限（使用情况访问、悬浮窗）
3. 设置时间限制（默认20分钟）
4. 选择要监控的应用
5. 开启监控开关
6. 正常使用手机，超时会自动提醒

## 🛠 技术架构

- **最低 Android 版本**: Android 8.0 (API 26)
- **目标 Android 版本**: Android 14 (API 34)
- **开发语言**: Java
- **构建工具**: Gradle 8.2
- **核心技术**:
  - UsageStatsManager API - 应用使用统计
  - WindowManager - 悬浮窗显示
  - Foreground Service - 后台持续监控
  - SharedPreferences - 配置存储

## 📁 项目结构

```
app/src/main/
├── java/com/screentime/guardian/
│   ├── MainActivity.java          # 主界面
│   ├── service/
│   │   ├── UsageMonitorService.java  # 使用监控服务
│   │   └── OverlayService.java       # 悬浮窗服务
│   ├── receiver/
│   │   └── BootReceiver.java         # 开机启动接收器
│   └── util/
│       ├── Constants.java            # 常量定义
│       └── PreferenceManager.java    # 偏好设置管理
├── res/
│   ├── layout/
│   │   ├── activity_main.xml         # 主界面布局
│   │   └── overlay_reminder.xml      # 提醒弹窗布局
│   ├── drawable/                     # 图标资源
│   └── values/                       # 字符串、颜色、主题
└── AndroidManifest.xml               # 应用清单
```

## ⚠️ 注意事项

1. 首次使用需手动授予权限
2. 部分手机需要关闭"省电模式"或将应用加入白名单
3. 某些深度定制的系统可能需要额外设置才能保持后台运行

## 📄 开源协议

本项目采用 MIT 许可证。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

💡 **温馨提示**: 适度使用手机，保护眼睛健康！
