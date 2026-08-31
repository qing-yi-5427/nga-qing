# nga-qing

`nga-qing` 是一个使用 Kotlin、Jetpack Compose 和 Material 3 编写的非官方 NGA Android 客户端。

## 当前能力

- 版块目录、父子版块和合集分类
- 主题列表、搜索、离线缓存、收藏分组/同步、浏览历史和阅读位置恢复
- 原生帖子阅读、头像、图片、表情、代码/表格/折叠、楼层操作、翻页及只看楼主
- 新建主题、逐楼回复/引用、自动草稿，以及支持图片与附件的应用内高级编辑器
- 回复提醒、私信、关注主题、用户主页与最近主题
- 阅读字号/行距/签名偏好及本机多账号切换
- Android 系统返回和内容区右滑返回
- 多个 NGA 论坛入口域名及网页登录兜底

主题、回复和收藏同步依赖 NGA 当前网页/API 行为；当站点写接口变更时，应用会保留草稿并显示错误，不会自动重复提交。账号凭证仅保存在 Android 应用私有目录，应用已禁用系统云备份。

## 构建

需要 JDK 21 和 Android SDK 35。在项目目录执行：

```shell
./gradlew testDebugUnitTest lintDebug assembleRelease
```

Windows 可使用：

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleRelease
```

Android `applicationId` 为 `com.qingyi5427.ngaqing`。从旧版 `com.ngaclient.app` 迁移时，Android 会将其视为独立应用，不继承原有登录状态、收藏或浏览历史。

## 来源与许可

- 项目代码按 [GNU GPL version 2](LICENSE) 发布。
- 外部项目和 NGA 资源的使用边界见 [SOURCES.md](SOURCES.md) 与 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
- 应用图标来自项目所有者提供的原始图片，经本项目脚本生成 Android 各尺寸资源。

如果分发修改版 APK，应同时提供对应源代码、保留许可证与来源声明，并明确说明修改内容。
