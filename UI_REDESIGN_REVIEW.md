# 现代阅读界面验收记录

## 设计与范围

本轮在原生 Jetpack Compose 架构内重排全套界面。以 [Origin UI Vue](https://www.originui-vue.com/) 的中性层级和清晰边界、[Ant Design Mobile](https://github.com/ant-design/ant-design-mobile) 的触屏操作原则为参考；实现是 NGA 客户端自己的 Compose 设计，并非两个库的逐组件移植。底色改为冷中性，正文用深墨色，钴蓝仅用于主操作、链接、选中和焦点。统一使用语义色、连续阅读列表、明确内容分隔及至少 48 dp 的主要触点。明暗两套主题、原生正文与 HTML 正文共同使用这套层级。

| 页面 | 可见结构与保留的操作 | Native 渲染证据 |
| --- | --- | --- |
| 根导航与版块 | 窄窗底栏、宽窗可滚动侧轨；宽屏版块目录侧栏可跳转分类，主列表保持同一 Lazy key 和选中态。搜索、分组折叠、收藏版块拖排、刷新均保留。 | [真实根壳+版块宽图](docs/ui-evidence/root-board-1200x800-light.png)、[版块手机](docs/ui-evidence/board-phone-light.png) |
| 主题列表 | 单一版块标题，筛选行与排序/发帖工具行，子版块选择面板，连续主题行；保留搜索、收藏版块、刷新、分页、访问/当前项、顶栏滚动显隐。 | [正常字号窄窗](docs/ui-evidence/primary-thread-320-light-normal.png) |
| 帖子阅读 | 首帖标题、连续楼层与细分隔；作者、楼层号、菜单有固定空间。底部回复/更多占独立布局空间，滚动时可收起；正文、引用、链接、代码、图片、签名、楼中楼继续由原生块渲染。分页、只看楼主、楼层跳转、收藏/关注、回复、网页版回退均保留。HTML 包装同步冷中性与钴蓝链接。 | [长段首帖](docs/ui-evidence/primary-post-320-light-normal.png)、[引用回复](docs/ui-evidence/primary-post-320-light-reply.png) |
| 新主题与高级编辑 | 标题/正文写作区、固定发布工具栏、草稿、提交中/失败、图片高级入口；网页登录/选文件逻辑不变。 | [320×320/2x 新主题](docs/ui-evidence/new-topic-short-2x-light.png)、[高级编辑暗色/2x](docs/ui-evidence/secondary-editor-320-dark-2x-font.png) |
| 收藏与历史 | 连续条目、搜索/筛选/分组和排序；收藏移动及删除确认、远端失败手动重试，历史滑删和清空确认保留。 | [收藏暗色](docs/ui-evidence/favorites-phone-dark.png)、[历史](docs/ui-evidence/history-phone-light.png) |
| 搜索 | 键盘提交、离线结果、空/加载/错误重试；短高或键盘出现时收起装饰标题，输入保持焦点。 | [搜索暗色](docs/ui-evidence/search-phone-dark.png) |
| 社区、用户、个人中心 | 通知/私信与关注切换、用户资料及主题、个人中心入口；长内容滚动与宽屏分区保留。 | [社区暗色](docs/ui-evidence/secondary-community-320-dark-content.png)、[用户内容](docs/ui-evidence/secondary-user-320-light-content.png)、[真实根壳+个人中心](docs/ui-evidence/root-profile-1200x800-light.png) |
| 登录、设置 | Web 登录、二维码/添加账号、网络错误和重试；设置的主题、阅读偏好、账号/域名与危险操作确认保留，小窗/键盘下滚动可达。 | [登录窄窗](docs/ui-evidence/secondary-login-320-light-header.png)、[设置大字](docs/ui-evidence/secondary-settings-320-light-large-font.png) |

截图由 Robolectric SDK 33、`GraphicsMode.NATIVE`、生产 Compose Screen 或生产 `RootPane` 直接绘制，并用 Activity `decorView.drawToBitmap()` 保存。它们使用可公开的固定测试状态，不含真实论坛账号和设备数据。窗口涵盖 320×568、320×320、840×320/2x、840 与 1200×800 dp；截图本身只证明可见布局，测试还单独断言主要触点、滚动、选中、失败重试与输入操作。先前 Windows 上的 `captureToImage`/PixelCopy 超时，故不把那次零字节图算作证据。

## 状态与行为回归

- 导航继续按账号会话 revision 隔离；普通折叠、窗口缩放沿用现有 NavController、两栏/折痕避让与阅读位置。宽屏根路由仅在无分隔铰链时显示侧轨；短高 840×320/2x 下末个“我的”可滚动触达。
- 当前版块与主题按既有路由身份高亮。主题顶栏显隐仍由版块 ViewModel/SavedStateHandle 共享，点击主题不创建默认展开状态；子版块选择只改变版块路由，不改变“全部/精华区”筛选语义。
- Board 宽屏分类高亮曾直接在 composition 读取 `LazyListState.layoutInfo`，导致生产 `RootPane` 测试 60 秒无法 idle；改为稳定派生状态后，同一生产根壳测试正常完成。Board 宽窄往返、分类侧栏跳转与返回重开有针对性测试。
- 收藏搜索结果非空↔空使用稳定的单一列表，输入框焦点和键盘不因空态重建而丢失。短高搜索、历史和写作区操作可达；草稿保存、严格发布成功判定与失败保留仍沿用既有实现及测试。
- 帖子离线缓存横幅不计入可见楼层索引，页码跨第 30 楼边界有单元测试。网页版回退仍只替换正文区域，左栏 VM、位置及数据请求逻辑未改。

## 构建、设备与限制

`./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease` 完整通过：122 项测试、0 失败/错误/跳过；Lint 0 error、32 warning。`git diff --check` 无空白错误。Release APK 为 [nga-qing-release.apk](app/build/outputs/apk/release/nga-qing-release.apk)，包名 `com.qingyi5427.ngaqing`、版本 1.1.0（versionCode 3），SHA-256 `FB70AD89455401A98A0306AC353843BB6522D79D773BBB7C0EBA396E0C476B57`。`apksigner verify` 通过 APK v2 签名；沿用项目现有 Android Debug 证书，尚未配置正式发布签名。

重构构建时没有执行 ADB。用户重新连接设备并要求安装后，2026-09-28 对原设备 `已连接的原设备` 执行 `adb install -r`，返回 `Success`，保留应用数据；只读核对已安装 1.1.0（code 3），`lastUpdateTime=2026-09-28 07:13:08`。未启动应用或开展交互验收，用户将自行测试。此前为验收暂改的设备 `stay_on_while_plugged_in` 原值为 `0`，本次安装前读得 `15`，现已恢复为 `0` 并读回确认；`wm size` 和旋转覆盖此前也已恢复原值。Native 截图可检查静态排版和多数 Compose 交互，但不能证明真实设备的折叠姿态、WebView 网络页面、系统输入法与“网页版打开”瞬态左栏是否闪动。

## 登录网页视口修复（2026-09-28）

用户报告真机 NGA 登录弹窗被裁切。修复前原始设备截图只保存在忽略的 `app/build/outputs/ui-evidence/login-before.png`，含临时登录二维码，因此不收入公开文档。原实现把 WebView 限为可用高度约一半，并与大段原生说明共同放入外层滚动列；网页固定宽度内容也未启用宽视口适配。现改为固定顶栏和底部“完成登录”按钮、中间自适应占满剩余空间的单一网页视口；帮助文字移入顶栏弹窗，并启用 WebView 宽视口、概览适配与双指缩放。认证地址、Cookie 捕获和登录事务未改。

Robolectric Native 的生产 LoginScreen 测试检查 320×420 dp / 1.4 倍字体、320×320 dp / 2 倍字体下网页容器仍有主要可用高度且不覆盖按钮，并检查 WebView 设置。ShadowWebView 在 Robolectric 下原生 `height` 报 0，因此测试测量 Compose 真实容器边界；这**不能代替真实 NGA 网页弹窗验收**。本次完整 `testDebugUnitTest lintDebug assembleRelease` 通过：123 项测试、0 失败/错误；Lint 0 error、32 warning。修复版 APK SHA-256 为 `20493E870A20C42D58E2EA898EE9CEA1F257A5F6B1FF2FB82E8017D66019D585`，保留数据覆盖安装返回 `Success`，设备显示 1.1.0（code 3），`lastUpdateTime=2026-09-28 07:30:13`。安装后一度锁屏，代理未取得修复后的真机截图，也未进行凭证输入或认证提交。随后用户自行检查并明确反馈“登录页面正常”，按用户要求结束设备验收。

## 原生玻璃导航与阅读界面落地（2026-09-28）

本节记录 [design-proposal](docs/design-proposal/README.md) 获批后的**实际 Compose 实现**，与上文上一版中性界面记录分开。最终主题使用提案的纸白 `#fbfbf8`、墨色 `#171d21`、灰色 `#677078`、边界 `#dfe3e1` 和克制蓝 `#356987`，暗色有对应语义色；原生帖子与 HTML 正文同步。实现了独立玻璃返回/操作控件、纸面标题、窄屏悬浮三入口导航、阅读页右下紧凑操作岛，以及宽屏根页和主题/帖子双栏共用的侧轨。根页浮岛覆盖内容，滚动列表为浮岛与系统导航栏预留末尾空间。登录页保持用户已确认的高 WebView 视口与固定完成按钮，认证路径未改。

宽屏侧轨图标组按扣除顶部、底部安全区后的可用高度居中；短高和 2 倍字体时可滚到最后一个入口，左侧安全区计入侧轨及阅读栏宽度。未分隔的 840 dp 窗口扣除侧轨后仍保持双栏；分隔铰链沿用既有避让布局。阅读页的“版块”图标保持选中，点击可返回版块目录；账号 revision、折叠时的导航/阅读位置、顶栏显隐、网页版回退和草稿/发布状态逻辑继续沿用原有实现。

玻璃控件在 Android API 32+ 且硬件加速可用时使用 [Haze 1.0.2 的背景采样与 hazeChild](https://github.com/chrisbanes/haze/tree/1.0.2)，叠加半透明底色、边缘高光与阴影；API 31 及以下、软件绘制或没有采样源的区域使用清晰的高不透明表面。没有把前景 `Modifier.blur` 当成背景模糊。项目保留 Kotlin/Room 工具链；Haze 1.0.2 的[版本目录](https://github.com/chrisbanes/haze/blob/1.0.2/gradle/libs.versions.toml)与其相容。Native 离线截图展示软件绘制 fallback，**不能证明真机模糊质量或帧率**。

| 最终生产 Composable 截图（公开固定 fixture） | 重点 |
| --- | --- |
| [根壳+版块宽图](docs/ui-evidence/glass-root-board-1200x800-light.png)、[主题+正文双栏](docs/ui-evidence/glass-primary-reading-1200x800-light.png)、[主题列表+侧轨](docs/ui-evidence/glass-reading-thread-1200x800-light.png) | 实际 RootPane/ReadingPane、安全区、侧轨与选中项 |
| [手机主题](docs/ui-evidence/glass-primary-thread-320-light-normal.png)、[连续阅读正文](docs/ui-evidence/glass-primary-post-320-light-normal.png)、[暗色大字长页码](docs/ui-evidence/glass-primary-post-320-dark-2x-long-page.png) | 主题工具区、玻璃操作岛、正文/楼层/页码触点 |
| [版块手机](docs/ui-evidence/glass-board-phone-light.png)、[收藏暗色](docs/ui-evidence/glass-favorites-phone-dark.png)、[收藏短窗大字](docs/ui-evidence/glass-favorites-320-dark-2x-row.png) | 根浮岛、目录/列表、搜索与移动入口 |
| [写作短窗大字](docs/ui-evidence/glass-new-topic-short-2x-light.png)、[登录短窗大字](docs/ui-evidence/glass-secondary-login-ready-320x320-2x.png) | 短高写作与网页登录视口 |
| [个人中心暗色](docs/ui-evidence/glass-secondary-profile-320-dark-identity.png)、[设置大字](docs/ui-evidence/glass-secondary-settings-320-light-large-font.png) | 明暗语义色、滚动及触屏入口 |

截图直接从生产 Screen、RootPane 或 ReadingPane 的 Robolectric Native 渲染通过 `decorView.drawToBitmap()` 获取，使用模拟数据，不含真实登录二维码、Cookie 或账号数据。各页生产 Screen 测试覆盖明暗色、窄宽和短高窗口、大字、选中高亮、回复/分页、输入与错误重试；截图本身不是设备交互验收。

最终 `:app:testDebugUnitTest`：**132 项、0 失败、0 错误、0 跳过**。首次把测试、Lint、Release 并列运行时，Lint/UAST 分析 21 个单元测试源码触发 Kotlin FIR 内部异常；不改代码或 Lint 配置，随后单工作器强制重新分析 `:app:lintDebug` 完整通过，**0 error、34 warning**。首次异常原因尚未确定。`:app:assembleRelease` 成功，`git diff --check` 无空白错误；Release APK [nga-qing-release.apk](app/build/outputs/apk/release/nga-qing-release.apk) 为 `com.qingyi5427.ngaqing` 1.1.0（code 3），SHA-256 `3FEB25154954D8FB5C4D29D389E086701A1B6BFE2235E5A4FAF8A97DDDEC6A64`，`apksigner verify` 确认 APK v2 签名。沿用项目现有调试证书，正式商店发布仍需独立签名配置。

本版离线验证不能证明折叠实体姿态、系统输入法、NGA 远端 WebView、网页版回退瞬态或玻璃真机动态效果。没有发帖、改账号或清除应用数据。

此前已连接的原设备在线后，按既有授权以 `adb install -r` 保留数据覆盖本版 APK，命令返回 `Performing Streamed Install` / `Success`。只读 `dumpsys package` 核对 `com.qingyi5427.ngaqing` 版本 1.1.0（code 3）、`lastUpdateTime=2026-09-28 08:15:16`，`pm path` 指向新安装的 `base.apk`。未启动或操作应用，也未更改设备设置；用户自行测试真机交互。

## 横屏侧轨、顶栏手势与页面过渡修正（最新构建，2026-09-28）

用户反馈横屏左侧导航轨的阴影不自然。当前手机已被用户拿走，未取得同一设备的横屏修前/修后画面；以下成因来自源码结构与生产 Compose 离线渲染，**并非已在该设备上确认的视觉根因**。原 88 dp 侧轨内玻璃组宽 76 dp，左右仅各 6 dp，却使用 12 dp elevation；纵向滚动修饰器还包在整块带阴影表面外，阴影会受滚动视口及 NavHost 裁剪影响。现仅侧轨改为 3 dp 阴影、68 dp 玻璃组（按钮仍有 56 dp 触点），左右各留 10 dp 绘制空间；滚动移入玻璃内部，短高视口上下各留 8 dp。普通顶栏/右下浮岛的阴影、玻璃采样源及侧轨 88 dp 分栏几何不变。[正常横屏含左安全区](docs/ui-evidence/glass-root-rail-1200x800-insets-light.png)和[840×320/2 倍字体短窗](docs/ui-evidence/glass-root-rail-840x320-2x-light.png)直接渲染生产侧轨，测试断言四周留白、整组安全区居中及末个入口可滚动触达；两图未见矩形影边。软件 Native 图不能证明设备硬件阴影的最终质感。

同轮把主题列表顶栏保存为连续隐藏比例，使上滑、下滑及顶栏自身拖动都能跟随手势，不再在阈值处突然切换；点击主题继续保留该版块顶栏状态。生产 Screen 的[初始](docs/ui-evidence/glass-thread-header-start-390.png)、[中间](docs/ui-evidence/glass-thread-header-middle-390.png)、[隐藏](docs/ui-evidence/glass-thread-header-hidden-390.png)及[重建恢复](docs/ui-evidence/glass-thread-header-restored-390.png)帧、反向手势和短窗/大字断言均通过。导航动效修正了返回时上层页面遮住下层内容导致的硬切：窄屏 push/pop 各有短距离与透明度过渡，根标签以淡变切换，宽屏共享列表与侧轨保持稳定。中间帧[进入](docs/ui-evidence/glass-motion-child-push-mid.png)、[返回](docs/ui-evidence/glass-motion-child-pop-mid.png)、[宽屏](docs/ui-evidence/glass-motion-wide-shared-mid.png)使用**真实 NavHost 转场与纯色测试夹具**验证像素，不是 NGA 实际页面截图。帖子页码及认证逻辑未改。

本版最新检查结果：`:app:testDebugUnitTest` **140 项、0 失败/错误/跳过**；单工作器完整重分析 `:app:lintDebug` **0 error、34 warning**；`:app:assembleRelease` 成功。APK v2 签名验证通过，包名 `com.qingyi5427.ngaqing`、版本 1.1.0（code 3），最新 Release [nga-qing-release.apk](app/build/outputs/apk/release/nga-qing-release.apk) SHA-256 为 **`1EB946D5E2FC6EC3704FD5F50338F2135CAD71FF6019C16AD894EB0873C7FED4`**；此哈希取代上文所有较早构建的哈希。用户已拿走手机，本版**未安装、未做真机横屏验收**，没有改设备设置、输入凭证或发送帖子。
