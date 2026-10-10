# V8 独立验收记录（2026-10-10 起）

本文件是本轮重新执行的清单。`FOLDABLE_REVIEW.md` 的 92 项和 `HANDOFF.md` 的 156 项均是旧结果，不计入 V8 验收。结论必须区分：实体直板机、模拟尺寸/折痕、Robolectric、未验证实体折叠屏。HTML 原型截图只用于视觉对照。公开提交的截图仅保留无账号/私信/草稿内容的页面；含个人信息的候选截图留在工作树外的本地 QA 目录或先遮蔽。

## 设备与安装基线

这里有两个不同基线，后续视觉差异不能混同：**现装手机 APK 是 1.0.0**；**V8 源码起点是隔离工作树提交 `0ee4c9af5070c7329337867fabea2bbba086fe20`**，其中完整保留原 checkout `916633a7ca6da0cc243aa81f2550cbf9eb4d790b` 上未提交的 1.1.1 修复。原 checkout 没有用于候选构建。手机 1.0.0 截图只记录安装前设备状态，不作为 V8 相对 1.1.1 的视觉改进依据。已装 APK 本体仅保存在本地 `G:\AI\coding\10-nga\V8_QA_local\baseline-installed-v1.0.0.apk`，不进入 Git。

| 项目 | 只读实测 |
| --- | --- |
| ADB | `116e3768`, `25102RKBEC` (`myron`) |
| 系统 | Android 16 / API 36，HyperOS `OS3.0.310.0.WPMCNXM` |
| 显示 | 1200×2608 物理 px，480 dpi，整窗配置 400×869 dp，竖屏，系统字号 1.0 |
| 安全区 | 圆孔 cutout 顶部 144 px；窗口 edge-to-edge，系统手势导航模式 2；Compose 内容 Insets 仍需候选 APK 逐页核验 |
| 待恢复设备设置 | 旋转自动 `accelerometer_rotation=1`、`user_rotation=0`；系统夜间模式 `custom_schedule`/3；飞行模式 0、移动数据 0、Wi-Fi 1。后续模拟字号/旋转/离线须恢复这些原值 |
| 已装包 | `com.qingyi5427.ngaqing`，1.0.0 / versionCode 1，首次/最近安装 2026-08-20 08:37:05 |
| 已装包签名 | Android Debug，证书 SHA-256 `3774e86da5188b94341d667ad7e4a871ad8cda74f81088fb8445654d6068e0d6` |
| 已装 APK SHA-256 | `DFF1482C5A49513A55440EBE871D1AF7E086ED9A32B42011A61E08674B9B55EB` |
| 当前前台 | `com.qingyi5427.ngaqing/.MainActivity`，现装 1.0.0 UI 为版块/收藏/我的三 Tab；原始截图保存在工作树外 `V8_QA_local/baseline-phone-20261010.png`，不进入公开提交 |

只读采样 `logcat -b crash` 最近 500 行，未出现包含本包名或 `FATAL EXCEPTION` 的行；这不是完整历史的无崩溃证明。

后续设备可操作性复核（同日）：ADB 仍报告同一设备为 `device`。一次限定本包的标准 launcher Monkey 事件成功启动现装 1.0.0 进程；系统同时报告 Keyguard `showing=true`、该 Activity `visibleRequested=false`，因此**尚不能据此认定界面可见或可点**。HyperOS 拒绝 shell `KEYCODE_WAKEUP` 注入（缺 `INJECT_EVENTS`）；当前没有本包已安装 instrumentation runner。未采集可能含其他通知的锁屏 UI dump，也未绕过锁屏。候选安装后的实体操作优先使用受限本包启动与正规 UI 自动化；若锁屏仍遮挡，请用户手动解锁或按系统方式开放权限再继续。此项不构成 APK 功能失败。

继续使用系统公开的 `cmd power wakeup` 与 `wm dismiss-keyguard` 后，非安全锁正常解除（`showing=false`）；再执行一次限定本包 launcher Monkey，现装 Activity 成为 topResumed 且 `reportedDrawn=true`。标准 `uiautomator dump` 可读取前台界面，解析到 105 个节点且包名均为本包；只打印了包归属和计数，未输出节点文字，临时设备 XML 已删。**可见与层级读取路径已确认**，确定性点击仍待候选阶段验证。先前 shell 全局 `input keyevent` 被拒不能被当成整机 QA 阻塞。

已新增独立 `app/src/androidTest/java/com/qingyi5427/ngaqing/RootReadOnlySmokeTest.kt`，准备用正规 AndroidJUnitRunner/UiAutomation 在候选包上验证四个根 Tab、版块到全站搜索、我的到阅读记录及返回。测试只观察当前账号的可见页面；没有测试账号时明确报告 `AUTH_REQUIRED`，不伪造会话。它已完成 Kotlin 编译，尚未安装/运行，不计作实体通过。候选核包并保数据安装后，仅手动构建 `:app:assembleDebugAndroidTest`、安装测试 APK，再用 `am instrument -w` 执行；不运行可能重装目标包的 `connectedAndroidTest` 或 Macrobenchmark 安装流程。正式 Release 包升级后可复用同一独立 Runner 做只读烟测。

候选包安装前逐包核验 `applicationId`、`versionCode > 1`、签名证书 SHA-256 等于已装包。源码基线 Debug APK 已单独核验：同包名、versionCode 4 / 1.1.1、证书 SHA-256 与现装包一致，因此存在保留数据的覆盖升级路径；它**不含 V8 改动且没有安装**。后续 V8 候选包仍须重新核验。仅用 `adb install -r` 保留数据；不卸载、不清除数据、不运行会覆盖数据的宏基准安装。实体视觉检查重点：底栏不得过高挤正文、首屏信息密度、顶部 144px 打孔安全区。

## 实际代码与现有回归锚点

| 关注点 | 基线真实实现 | 本轮应验收的证据 |
| --- | --- | --- |
| Activity / 窗口 / 折痕 / NavHost | `MainActivity.kt`、`ui/NgaNavHost.kt`、`ui/adaptive/PaneLayout.kt` | 单个 NavController；真实 Insets 与折痕安全矩形；断点、高度和字号策略单测；根/子页返回 |
| 主题与文字 | `ui/theme/Theme.kt`、`ui/design/DesignComponents.kt` | V8 浅深色、角色字号、阅读设置乘数、1.3/1.5 系统字号截图/渲染 |
| 顶层导航 | `ui/chrome/BottomNav.kt`、`ui/NavigationMotion.kt`、`ui/NgaNavHost.kt` | 发现/版块/收藏/我的 4 Tab、一触可达；DISCOVER 加入后 tab ordinal、pop/saveState/restoreState、深链去重及返回栈 |
| 发现与版块 | `ui/board/BoardScreen.kt`、`BoardViewModel.kt`、`ui/community/CommunityScreen.kt`、`ui/history/HistoryScreen.kt`；新增 `ui/discover/` 待集成后核验 | 真实 VM/Repository 数据；继续阅读、空/错/重试；宽屏两区、窄屏单列；版块目录未丢；无收藏时不将目录前 4 项伪称「常逛」 |
| 主题列表 | `ui/thread/ThreadListScreen.kt`、`ThreadListViewModel.kt` | 选中高亮、左侧独立滚动、折叠/返回恢复、搜索/筛选/只看楼主 |
| 帖子/网页兜底 | `ui/post/PostScreen.kt`、`PostViewModel.kt`、`PostContentParser.kt` | 正文宽度、楼层锚点、双栏/专注、工具条状态；WebPageView 继续在 PullToRefreshBox 与 `ngaBackdropSource` 外直接渲染 |
| 收藏 | `ui/favorites/FavoritesScreen.kt`、`FavoritesViewModel.kt` | 单/双列与可用筛选、缓存/同步失败、账号隔离、切屏过滤与滚动恢复 |
| 草稿、历史、账号 | `ui/compose/NewTopicViewModel.kt`、`ui/post/PostViewModel.kt`、`data/local/AppDatabase.kt`、`data/local/UserPreferences.kt`、`data/repository/NgaRepository.kt` | 草稿与阅读位置、账号修订号隔离，避免重新提交；不实际发帖/回帖/私信 |
| 我的/设置/其他路由 | `ui/profile/ProfileScreen.kt`、`ui/settings/SettingsScreen.kt`、`ui/NgaNavHost.kt` | 登录/账号切换入口、私信/发帖入口、设置、深链、系统返回可达 |
| 登录入口与隔离 | `ui/login/LoginScreen.kt`、`LoginViewModel.kt`、`data/remote/LoginHelper.kt` | 受信主机固定 `login_ui` 入口、账号/扫码/验证帮助与失败恢复；新增 `LoginEntryTest.kt`，旧 `LoginHelperTest.kt`/`NgaAuthPolicyTest.kt` 回归；真实登录仍需授权测试账号 |

现有测试锚点：`ui/adaptive/PaneLayoutTest.kt`、`ui/primary/RootPaneRenderTest.kt`、`ui/content/ContentScreensRenderTest.kt`、`ui/post/ReadingPositionTest.kt`、`ui/compose/NewTopicDraftTest.kt`、`data/repository/AccountRepositoryTest.kt`、`data/local/AccountProfileTest.kt`、`ui/chrome/ScrollDockStateTest.kt`。这些仅说明可沿用的覆盖位置，本轮结果待重跑。QA 新增 `ui/post/ReaderDockQaRenderTest.kt`，核查 320dp 窗口在 fontScale 1.5 时展开动作范围与可点击性；`ui/login/LoginEntryTest.kt` 核查固定登录入口与准备/帮助/错误/刷新/添加账号退出。运行结论以本轮最终报告为准。

## P0/P1 可追溯矩阵

状态：`待实现` / `待自动测` / `待实体机` / `通过` / `失败` / `实体折叠不可测`。每一行须补确切测试日志或截图路径与结果。

| ID | 设计要求与通过标准 | 实现/测试映射 | 当前状态 |
| --- | --- | --- | --- |
| P0-0 | 保全脏基线；真实文件映射与新分支；本轮全量基线和最终全量结果分开记录 | worktree `feat/ui-v8-adaptive`；本文件；基线 `:app:testDebugUnitTest :app:assembleDebug` | 基线通过；最终待测 |
| P0-1 | 中宽竖屏发现、收藏、主题和正文四处字号提高；直板竖屏不缩；1.3/1.5 不裁切；深浅主题 | Theme、四页面；布局/Robolectric/实体直板截图 | 待实现 |
| P0-2 | 发现接真实数据与加载/空/错状态；离线本地继续阅读；无收藏时不伪造常逛；宽高足够两区；收藏宽屏双列、竖屏单列；不全局限宽 760dp | NavHost、Discover/Board/History、Favorites、各 VM | 待实现 |
| P0-3 | 840dp/480dp 与字号、Insets、折痕安全区域共同决策；阅读左栏约 28–32%，正文保留 460–520dp；无第三栏；专注恢复 | PaneLayout、NavHost、ThreadList、Post；策略单测/渲染 | 待实现 |
| P0-4 | 下滑隐藏、上滑胶囊、点击完整工具条；320dp/1.5 字号展开不溢出；BOOKS 空白唤起后下一次滚动立即隐藏；运行中开启 TalkBack 不触发误收起；定时、IME/弹窗安全 | PostScreen、ReaderDock/State/设置；状态机单测+窄屏渲染+实体操作 | 待实现 |
| P0-5 | 四 Tab 和全部原路由；Discover→讨论正确 THREADS 来源且可返回发现；继续阅读恢复楼层；tab ordinal/pop/saveState/restoreState/深链去重；账号、草稿、收藏同步、WebView 无回归；Insets/返回/无障碍 | NavHost、BottomNav、NavigationMotion、Post/Repository/VM；本轮全量单测/Lint/构建/设备 | 待实现 |
| P1-1 | 细腻动效、过渡与轻微模糊 | 视觉对照/性能观察；不得顶替 P0 | 待实现 |
| P1-2 | 主题栏宽度偏好、键鼠快捷键 | 设置/大屏交互，若延后列明 | 待实现 |
| P1-3 | NGA 富文本边界专项优化 | Parser/Reader 特殊内容回归，若延后列明 | 待实现 |

## 尺寸与环境矩阵

| 场景 | 证据方式 | 状态 |
| --- | --- | --- |
| 直板竖屏真实 1200×2608 px / 400×869 dp（与设计 1208×2624 接近，非同尺寸） | 实体截图/UI dump/操作录像或日志 | 基线已取；候选待测 |
| 直板横屏真实旋转 | 实体截图与 Activity 窗口 dp | 待测 |
| Fold 外屏 1168×1712、1712×1168 px | 模拟尺寸或 Robolectric；不得称为实体折叠 | 待测 |
| Fold 内屏 1672×2364、2364×1672 px | 模拟尺寸/折痕策略或 Robolectric；不得称为实体折叠 | 待测 |
| 599/600/839/840/900 dp，高度 `<480dp`，分隔横/竖折痕 | 纯函数单测 + Compose 渲染/设备 `wm` 模拟明确标注 | 待测 |
| 系统 fontScale 1.0/1.3/1.5（可选 2.0）、浅/深主题、断网/缓存、IME、TalkBack | 单测/实体机组合；恢复原系统设置 | 待测 |
| 实体 Fold 合/开与真实铰链遮挡、实体内外屏截图 | 当前只连接直板机 | 实体折叠不可测 |

### 冻结后的实体机执行顺序（尚未执行）

先记最终 Git SHA 与工作树状态，再取候选 APK 的包名、版本号、证书与 SHA-256。只有包名仍为 `com.qingyi5427.ngaqing`、versionCode 高于现装 1、证书 SHA-256 与上述基线相同，才对 `116e3768` 用保留数据的覆盖安装。安装前后分别记录 `dumpsys package` 中版本和用户数据目录状态；不读取或导出账号 Cookie，不卸载、不清数据。正式 release 产物等主代理验收后另行生成。

1. **实体原生 400×869dp 竖屏**：冷启动发现→版块→收藏→我的四 Tab，观察顶部 144px 安全区、底栏占高、首屏信息密度；搜索/筛选、进帖子、滚动阅读、返回继续阅读；仅查看账号是否仍在和本地历史是否可见，不显示/提交隐私截图。进入发帖/回复编辑器只检查草稿和 IME，绝不发送。
2. **实体横屏与字号**：原始设置为自动旋转、字号 1.0。临时切横屏检查短高布局与正文，恢复竖屏；分别用系统 fontScale 1.3、1.5 检查发现/收藏/主题/阅读/底栏的裁切及点击范围，确认后恢复 1.0。深色模式检查同组关键页面，恢复原 `custom_schedule`。
3. **离线与无障碍**：只在已有本地历史可见时临时断网，查继续阅读、收藏缓存和真实错误/重试；恢复 Wi-Fi 1/飞行模式 0。TalkBack 若可经设备现有权限启用，运行中打开后核阅读工具条定时不误收起；若权限不允许则单测替代并标注实体未验。记录状态栏、手势导航和 IME 覆盖。
4. **六个设计预设**：`phoneP` 1208×2624、`phoneL` 2624×1208、`coverP` 1168×1712、`coverL` 1712×1168、`innerP` 1672×2364、`innerL` 2364×1672 px。依据 440dpi 仅作为 Fold 假定密度，记录每次 `wm size`/`wm density` 与 Activity 实际窗口 dp；在此直板机上改 `wm` 所得截图只能称**模拟窗口**，不能称实体 Fold 或铰链实测。每组至少发现、收藏、主题、阅读的可访问页；若真实业务受未登录或网络阻挡，用明确的空/错态截图与 Robolectric 数据渲染证据分开记录，绝不用 fixture 冒充实机内容。最后重置 `wm size`/`wm density`，核原生 1200×2608/480dpi、旋转/字号/夜间/网络设置均恢复。

任何配置或应用操作失败，先还原设备设置再定位；页面含账号、私信或草稿等隐私时只存工作树外本地 QA 证据，公开文档仅写脱敏状态。六预设覆盖宽高规则，铰链分隔只由策略/Robolectric 模拟测试覆盖，实体折叠屏开合与真实折痕遮挡仍不可测。

## 交互场景 A–J

| ID | 验收动作 | 证据/边界 | 状态 |
| --- | --- | --- | --- |
| A | #5 楼阅读时宽→窄→宽，主题与楼层接近原处 | 实体折叠机缺席；同一帖子运行时模拟尺寸切换并读楼层锚点，VM/Compose 回归作替代；不以布局结构推断状态必丢 | 实体折叠不可测 |
| B | 双栏左侧切帖、滚动与选中不跳，返回版块后恢复 | 模拟宽窗/Robolectric + 实体手机单栏返回 | 待测 |
| C | 回复草稿、光标跨旋转、宽→窄→宽、专注及编辑器重开 | 实体直板旋转/IME + 模拟宽度/专注切换；不按发送，先测实际恢复再判断是否存在状态问题 | 待测 |
| D | 正文下滑隐藏、上滑胶囊、点击展开及功能入口、闲置收起 | 单测 + 实体直板；收藏只在明确不影响已有状态时操作 | 待测 |
| E | 纯净阅读空白唤起；图片/链接/代码/引用交互不误触 | 实体直板 + 渲染/状态测试 | 待测 |
| F | 收藏筛选/滚动，窄↔宽时 query/folder/readFilter 与滚动锚点一致；断网缓存和真实错误/重试 | 实体直板 + 宽窗渲染/VM/Repository；不伪报远端成功 | 待测 |
| G | 1.5 字号下卡片、筛选、导航无裁切且按钮可达 | 实体直板截图 + 宽窗渲染 | 待测 |
| H | 我的、账号切换入口、私信、发帖/回帖入口、同步、网页兜底、返回 | 只检查入口与草稿，不发送；真实认证缺失要记录 | 待测 |
| I | 状态栏/圆孔/手势区/IME/模拟铰链无遮挡 | 实体直板 + 折痕策略测试 | 待测 |
| J | 内屏横屏两栏、无常驻第三栏；专注切换后恢复原帖 #5 楼及回复草稿 | 模拟内屏横屏运行时交互/渲染；实体 Fold 缺席，不以 Compose 条件子组形态直接断言重建 | 待测 |
| K | 发现讨论条目打开主题/宽屏 THREADS 来源正确，返回恢复发现；继续阅读回到原楼层 | 导航单测/渲染 + 实体直板楼层实测；宽屏仅模拟 | 待测 |
| L | 发现加入后四 Tab 切换的 ordinal、pop/saveState/restoreState 和深链无重复；离线继续阅读仍有本地记录；无收藏显示真实空态 | NavigationMotion/VM 单测 + 实体直板断网/缓存检查 | 待测 |

优先验证的新增边界：① 320dp 可用宽、fontScale 1.5 时完整阅读工具条所有动作在安全矩形内且可点击；② BOOKS 空白唤起后立即向任一方向滚动，胶囊隐藏；③ 打开阅读页后再开启 TalkBack，既有计时协程应取消；④ 收藏页从窄单列转宽双列再返回时 query/folder/readFilter 和可见项锚点不能分裂；⑤ Discover 进帖后的 THREADS 来源、返回和继续阅读楼层。实施中的自动测试尚未运行，现阶段不宣称通过。

## 本轮运行日志

实施者已在 V8 改动前的隔离工作树 `0ee4c9a` 完成 `:app:testDebugUnitTest :app:assembleDebug`；独立查看本轮 `app/build/test-results/testDebugUnitTest/*.xml` 得到 **156 tests / 0 failures / 0 errors / 0 skipped**，Debug APK 构建通过，生成 `app/build/outputs/apk/debug/nga-qing-debug.apk`。这是源码 1.1.1 工作树基线结果，不是 V8 最终测试，也没有安装到手机。

阶段中完成的登录定向 18/18、Reader 定向 19/19、Profile/阅读工具条 12/12 与版块 4/4 渲染及旧版块内容 5/5 均只说明当时对应源码批次；其他代理后续仍在改共享布局。最新整合批次的 `compileDebugKotlin`、`compileDebugUnitTestKotlin`、`compileDebugAndroidTestKotlin` 已通过，说明独立真机 Runner 可编译，**尚未执行**。这些阶段结果不能相加成最终测试总数，也不能代替冻结提交后的全量重跑。

新核心样张达到 HTML 视觉验收、生产源码冻结提交后，重新运行完整 `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`，以已核验签名的 Debug 候选保留数据覆盖原手机，执行真实/模拟矩阵。主代理接受功能与视觉后才生成正式 `1.2.0/code5` Release APK；对 R8 最终包再次核包名/签名/versionCode，保留数据安装并冷启基本路径烟测，再记录 SHA-256 与交付。Debug 通过不得替代 Release 包可安装/可运行结论。每项记录命令、开始结束时间、测试计数、失败详情、包 SHA-256、截图路径。绝不沿用旧文档的通过计数。

正式 Release 产物另验 APK Manifest 的 `debuggable=false`、没有测试 runner/fixture 入口、没有 CAMERA 权限（扫码登录尚未实现）；`androidTest` 只能存在于独立测试 APK。验收 `applicationId`、1.2.0/code5、原证书与 SHA-256 后，才在保留数据覆盖的实体机上冷启并复用只读烟测。此清单目前仅准备，正式 Release 尚未构建。
