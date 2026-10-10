# V8 原生 UI 实施审计

## 基线与隔离

- 原工作树：`G:\AI\coding\10-nga\NgaClient`，分支 `feat/ui-design-system`，HEAD `916633a7ca6da0cc243aa81f2550cbf9eb4d790b`。
- V8 工作树：`G:\AI\coding\10-nga\NgaClient-ui-v8`，分支 `feat/ui-v8-adaptive`。先复制原工作树全部 30 个已修改或未忽略的新文件，并逐一比较 SHA-256，差异为 0；复制状态保存于 `0ee4c9af5070c7329337867fabea2bbba086fe20`。原工作树保持未提交状态，未 reset 或覆盖。
- 基线命令：`:app:testDebugUnitTest :app:assembleDebug --no-parallel --max-workers=1`；33 个 suite、156 项测试、0 失败，Debug 构建成功（2m34s）。原有提示包括 KAPT 语言版本回退、旧 `SwipeToDismissBox`/Clipboard/WebView API 弃用及 NDK strip 提示。
- 工程原有 `applicationId=com.qingyi5427.ngaqing`、账号隔离、Room 迁移、网络认证边界、收藏同步、发布草稿、NavController 和阅读锚点机制均作为不变接口保留。

## 当前源码映射

| 关注点 | 真实文件 | V8 动作 |
| --- | --- | --- |
| Activity 与路由 | `ui/NgaNavHost.kt`、`ui/Routes.kt`、`ui/NavigationMotion.kt` | 保留同一 NavController 与认证 revision 隔离；加入发现根入口和阅读双栏容器。 |
| 根导航和顶栏 | `ui/chrome/BottomNav.kt`、`ui/chrome/EditorialChrome.kt` | 四 Tab；宽屏根页面使用顶部导航，紧凑窗口使用底部导航；阅读路由按需目录浮层，无常驻侧轨。 |
| 窗口与折痕 | `ui/adaptive/PaneLayout.kt`、`ui/NgaNavHost.kt` | 先计算折痕安全区和 safeDrawing 可用尺寸，再输出统一 V8 策略。 |
| 颜色与字体 | `ui/theme/Theme.kt`、`ui/theme/Type.kt` | V8 浅深色和按内容角色的字号；现有阅读偏好继续生效。 |
| 发现 | `ui/discover/DiscoverScreen.kt`、`ui/discover/DiscoverViewModel.kt` | 使用现有 `NgaRepository` 的真实目录、主题、历史和收藏版块；具备加载、空、错误和离线缓存状态。 |
| 版块与主题 | `ui/board/BoardScreen.kt`、`ui/thread/ThreadListScreen.kt` 及原 ViewModel | 保留版块选择、排序、搜索、收藏与列表锚点；提高中宽竖屏字号。 |
| 收藏 | `ui/favorites/FavoritesScreen.kt`、`ui/favorites/FavoritesViewModel.kt` | 保留本地/远端同步反馈和账号隔离；真实未读/已读筛选、单/双列卡片。 |
| 阅读 | `ui/post/PostScreen.kt`、`ui/post/PostViewModel.kt` | 保留楼层锚点、原生富文本/WebView、回复草稿；加入专注阅读与智能胶囊。 |
| 设置与账户 | `ui/settings/SettingsScreen.kt`、`ui/profile/ProfileScreen.kt`、`data/local/UserPreferences.kt` | 保留账户及原偏好，阅读操作模式复用现有偏好架构。 |

## 验收边界

HTML 与 PNG 仅是视觉参考。截图证据需要来自实际 Compose 页面；真机折叠、账号切换、回复等由独立 QA 按 `docs/ui-v8-qa/validation.md` 核对。最终产物应由完整集成检查后生成。

## V8 Debug 候选检查（2026-10-10）

- 正式生产布局容器 `ThreadRouteContent` 输出 986×698 左主题列表与右空态；`PostRouteContent` 输出同窗左列表加右 `PostScreen`，并在测试中验证专注隐藏与双栏恢复。对应原生 Compose 图在 `app/build/outputs/ui-evidence/`，文件名分别为 `v8-thread-html-innerL-986x698.png`、`v8-posts-html-innerL-986x698.png`。发现宽窗图是 `v8-discover-innerL-html-size.png`。三者均为 Robolectric 测试样本，不代表设备网络数据。
- 前一候选完整检查为 45 suite、191 test、0 failed/error/skipped；Lint 0 error / 35 warning；Debug SHA-256 `A63A6DEC3974743479EA7E2EEEC601FE3C1A1911FF674937636C197C88029588`。QA 保数据安装后重复实测发现 #5 楼续读间歇丢失，因此这些数值和设备结果仅代表前一候选。
- 当前候选纳入 `a589594` 的续读持久化修复、发现页离线中文提示及六预设模拟。完整 `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest` 成功：48 suite、205 test、0 failed/error/skipped；Lint 0 error / 35 warning。35 个 warning 中相对旧基线新增的一项只提示测试 runner 依赖可升级。JUnit XML 位于 `app/build/test-results/testDebugUnitTest/`，Lint XML 位于 `app/build/reports/lint-results-debug.xml`。
- 通过验收的 Debug APK `com.qingyi5427.ngaqing`、1.2.0/code5，SHA-256 `80B64AC1E9E45DB62B03829F4D75F7960C322E97A864D1BAA195A077B4D59A7F`；其真机复验连续确认第 5 楼返回续读。最终 `:app:assembleRelease` 的 R8 包及其独立实体烟测见 `docs/ui-v8-release.md`。Debug 测试 APK 的 SHA 随 QA 只读 Runner 修订而变化，不能把旧 `22714231…` 当作最终测试包。
- 六预设的本地 Compose 模拟索引、实际 dp 和密度假定见 `docs/ui-v8-window-presets.md`。系统拒绝实体设备的 `wm size/density` 覆盖；不得把模拟图称为物理折叠屏实测。
