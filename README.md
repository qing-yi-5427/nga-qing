# nga-qing

`nga-qing` 是一个使用 Kotlin、Jetpack Compose 和 Material 3 编写的非官方 NGA Android 客户端。

本项目与 NGA、NGA 官方客户端、`nga-just-works` 或 MNGA 不存在隶属、授权或背书关系。

## 当前能力

- 版块目录、父子版块和合集分类
- 主题列表、搜索、收藏、浏览历史和阅读位置恢复
- 原生帖子阅读、头像、图片、表情、楼层跳转、翻页及只看作者
- Android 系统返回和内容区右滑返回
- 多个 NGA 论坛入口域名及网页登录兜底

## 构建

需要 JDK 21 和 Android SDK 35。在项目目录执行：

```shell
./gradlew testDebugUnitTest lintDebug assembleRelease
```

Windows 可使用：

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleRelease
```

Android `applicationId` 暂时保留为 `com.ngaclient.app`，用于兼容现有安装、登录状态、收藏和浏览历史。

## 来源与许可

- 项目代码按 [GNU GPL version 2](LICENSE) 发布。
- 外部项目和 NGA 资源的使用边界见 [SOURCES.md](SOURCES.md) 与 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
- 应用图标来自项目所有者提供的原始图片，经本项目脚本生成 Android 各尺寸资源。

如果分发修改版 APK，应同时提供对应源代码、保留许可证与来源声明，并明确说明修改内容。
