# V8 独立验收记录

2026-10-10。本文件只记录本轮可复核结果。旧 `FOLDABLE_REVIEW.md` 的 92 项和 `HANDOFF.md` 的 156 项均不作为 V8 通过数。证据分为真实直板机、Robolectric 原生 Compose 测试样张、策略/状态单测和未验证的实体折叠屏；测试样张不能冒称实机。

## 当前结论与版本边界

**正式 Release 1.2.0/code5 已保数据覆盖安装并通过实体直板机冷启、四 Tab/搜索/历史和第 5 楼即时续读烟测。** 交付 APK `G:\AI\coding\10-nga\V8_Release\nga-qing-1.2.0-v8.apk`，SHA-256 `221A7F288157340E5CD91E291D5D8B0B7789F76EA405745058470B61DE6B2C45`；从设备取回的已安装 `base.apk` 散列完全相同。独立 Java 测试容器只注入自身进程，从外部操控 Release，四 Tab/搜索/历史通过；公开帖子“跳后首个可见 #5→立即返回→卡片第 5 楼→再入首个可见 #5”两次通过。Release 原尺寸现场图留在工作树外：`G:\AI\coding\10-nga\V8_QA_local\release-home-card5.png`、`release-reader-floor5.png`，前者显示续读卡第 5 楼，后者首个可见楼层为 #5。

旧 A63 Debug 包曾出现可见 #5 后立即返回却误记 #9 的真实竞态；Reader 修复 `a589594` 后，80B64A Debug 包的增强只读 Runner 共三次确认正确恢复。该 Debug 包另有一次跳楼后 8 秒内楼层列表为空、尚未到返回步骤的前置瞬态；未采到有效窗口，原因未定位，不能归为续读持久化复发，也不能抹去。Release 外部容器第二轮曾因自身未回到首页，`am start` 复用 Reader route，未进入跳楼步骤；为容器加保数据 `am force-stop` 冷启前置后通过，不计产品失败。

| 固定事实 | 本轮证据 |
| --- | --- |
| 源码起点 | 独立 worktree `feat/ui-v8-adaptive`，基线 `0ee4c9af5070c7329337867fabea2bbba086fe20` 完整保留原 checkout `916633a7ca6da0cc243aa81f2550cbf9eb4d790b` 的未提交内容；原 checkout 未修改 |
| 原装实体包 | `com.qingyi5427.ngaqing` 1.0.0/code1；不是上述含 1.1.1 修复的源码基线。原装 APK 仅本地备份 `G:\AI\coding\10-nga\V8_QA_local\baseline-installed-v1.0.0.apk` |
| A63 Debug 候选 | 1.2.0/code5；SHA-256 `A63A6DEC3974743479EA7E2EEEC601FE3C1A1911FF674937636C197C88029588`；签名 SHA-256 `3774e86da5188b94341d667ad7e4a871ad8cda74f81088fb8445654d6068e0d6` 与原装一致；`adb install -r` 成功，`/data/user/0/com.qingyi5427.ngaqing` 保留 |
| A63 源码测试 | `testDebugUnitTest lintDebug assembleDebug`：45 suites / 191 tests / 0 fail-error-skip；Lint 0 errors / 35 warnings；Debug 构建通过。此结果在楼层修复及离线文案修复前，不能计为新包最终结果 |
| 最终 Debug 候选 | 源码整合 `fe9a960`；1.2.0/code5，主 APK SHA-256 `80B64AC1E9E45DB62B03829F4D75F7960C322E97A864D1BAA195A077B4D59A7F`；最终独立 AndroidTest APK SHA-256 `14C6BF6250A9DFC4F68B526DE7F800F3D221FA3B21050ED7B3DA1935B52BD21F`。两包同原装签名，Debug `adb install -r` 后 dataDir 不变；AndroidTest 只适用于 Debug，不向 R8 Release 注入 |
| 当前源码测试 | `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest`：48 suites / 205 tests / 0 fail-error-skip；Lint 0 errors / 35 warnings；六预设四页 Native Compose 24 张模拟渲染已覆盖。正式 Release 另测 |
| 正式 Release | `150c003` 所记 R8/资源压缩签名包；SHA-256 `221A7F288157340E5CD91E291D5D8B0B7789F76EA405745058470B61DE6B2C45`，2,519,181 字节；1.2.0/code5，原签名证书 SHA-256 `3774e86da5188b94341d667ad7e4a871ad8cda74f81088fb8445654d6068e0d6`；无 debuggable/testOnly/测试 Runner/CAMERA 权限。`adb install -r` 保持目标 dataDir，取回设备已装 `base.apk` 散列与交付包一致；非注入冷启 103/128ms、前台可见，独立外部 Java 容器的四 Tab/搜索/历史与 #5 两轮 PASS |
| Release 测试容器 | 工作树外 `G:\AI\coding\10-nga\V8_QA_local\release-smoke`，纯 Java Framework Instrumentation，包名和 instrumentation target 均为 `com.qingyi5427.ngaqing.smokehost`；只读使用 UiAutomation 操作目标 Release。`release-smoke-results.txt` 记录逐轮结果；测试包已从设备卸载。此前把 Debug AndroidTest 注入 R8 Release 曾触发测试框架缺 Kotlin Intrinsics 的一次 11:34 崩溃；正常独立冷启和外部容器操作期间未观察到新的本包 FATAL/ANR |
| 真机 | Xiaomi `25102RKBEC`，Android 16/HyperOS；`116e3768`，1200×2608px/480dpi = 400×869dp，顶部 cutout 144px；原系统 fontScale 1.0、自动旋转、`custom_schedule` 夜间模式 |

以上 Debug/Test APK 与正式 Release 是不同构建；205 项单测和模拟图不替代 Release 实机烟测。实体折叠设备、实体 1.3/1.5 字号与账号切换仍未验证。

## 可追溯 P0/P1 矩阵

| ID | 验收边界 | 实现与测试锚点 | 当前证据/未验证 |
| --- | --- | --- | --- |
| P0-0 | 保留基线并能覆盖升级 | worktree、`app/build.gradle`、包签名 | 基线 `testDebugUnitTest assembleDebug` 156/156；正式 Release 同证书保数据安装，设备 `base.apk` 与交付包逐字节同散列 |
| P0-1 | HTML V8 视觉、四主页面字号/深浅 | `Theme`、Discover/Favorites/Thread/Post、`RootPaneRenderTest`、[四张固定测试样张](evidence/index.md) | 根代理接受四核心同窗样图及 A63 真机 Home/Reader/深色方向；非像素一致。实体字体 1.3/1.5 受系统权限阻挡，仅 Compose/Robolectric 覆盖 |
| P0-2 | 发现真实内容/状态、离线本地续读、版块/收藏自适应 | Discover/Board/Favorites、对应渲染/VM 测试 | 当前包真机公开讨论与真实续读卡可见；断网无默认网络时卡片仍显示第 5 楼，错误文案已为中文“当前无法连接 NGA，请检查网络后重试”并有重试入口；实体宽窗未测 |
| P0-3 | 折痕安全矩形、840dp 与短高/字号降级、正文宽度 | `PaneLayout`、`RootContentPolicy`、`RootPaneRenderTest`、Reader 宽窗渲染 | 策略与 Native Compose 模拟通过；A63 实体 869×400dp 横屏使用底部导航，不误显示宽屏顶导航。真实铰链与实体 Fold 未测 |
| P0-4 | 阅读 Dock 四动作、折叠/展开/计时/触摸/IME | `PostScreen`、`ReaderDockQaRenderTest`、`ReaderDockStateTest`、`ReaderStateTransitionRenderTest` | 320dp/font1.5 与状态定向测试已通过；Debug 实机编辑器获焦、系统 IME 显示且输入区无遮挡，未输入/发送。TalkBack 运行中启用与 dock 全手势未实体实测 |
| P0-5 | 四 Tab、来源/返回、账号与历史、阅读位置、草稿、WebView | `NgaNavHost`、`NavigationMotionTest`、`RootReadOnlySmokeTest`、Repository/草稿/Reader 单测 | Debug Runner 四 Tab/版块搜索/历史通过，#5 三次通过；正式 Release 独立外部容器四 Tab/搜索/历史 PASS、#5 两次 PASS。原账号数据保留。一次 Debug 跳楼前置空楼层的瞬态未定位。真实发送、账号切换与实体 Fold 不执行 |
| P1-1 | 动效、渐变、阴影 | 同窗样图、实机截图 | 根代理接受视觉方向；未做逐帧/性能量化 |
| P1-2 | 主题栏宽度偏好、键鼠快捷键 | 设置/大屏交互 | 本轮无实体键鼠，未验证 |
| P1-3 | 富文本边界 | Reader/Parser 专项 | 现有自动测试覆盖；无穷举真实内容证明 |

WebView 阅读兜底须继续在 PullToRefreshBox 与 `ngaBackdropSource` 之外直接渲染，避免旧闪烁问题。扫码登录没有实现，不应加入 CAMERA 权限；受信域名 `login_ui` 入口、帮助和错误恢复由 `LoginEntryTest` 与原 Cookie/账号隔离测试覆盖，未使用真实凭证做端到端登录。

## 设备与模拟矩阵

| 场景 | 本次结果及边界 |
| --- | --- |
| 实体竖屏 400×869dp/font1.0 | 正式 Release Home/Reader 实际窗口、打孔/手势 Insets、四 Tab、公开帖子与状态栏已看；第 5 楼真机首页与 Reader 原图 `G:\AI\coding\10-nga\V8_QA_local\release-home-card5.png`、`release-reader-floor5.png`，不进入公共仓库 |
| 实体横屏 869×400dp/font1.0 | 标准 `wm user-rotation lock 1` 成功，A63 首页短高回退底部四 Tab；本地 `A63-real-phone-landscape.png`。已恢复 `wm user-rotation free`、自动旋转 1/rotation0 |
| 实体深色/浅色 | 标准 `cmd uimode night yes/no` + 冷启，A63 两种主题可见；本地 `A63-real-phone-dark-cold-loaded.png`、`A63-real-phone-light-cold.png`。已恢复原 `custom_schedule`/3 |
| 实体离线/恢复 | 当前包 `svc wifi disable` 后默认网络 `none`，Home 本地第 5 楼续读卡留存，公开讨论显示中文错误和“重试”；本地 `new80-real-phone-offline.png`。Wi-Fi 已恢复 1、默认网络恢复；飞行模式 0 |
| phoneP/phoneL、coverP/coverL、innerP/innerL 六预设 | HyperOS 拒绝标准 `wm size`/`wm density` 覆盖（`WRITE_SECURE_SETTINGS`）；六次请求均未改变物理 1200×2608/480。故**没有实体窗口模拟截图**；[同窗 Native Compose/Robolectric fixture](evidence/index.md)与策略测试是替代，不能称实体 Fold |
| fontScale 1.3/1.5 | 标准 `settings put system font_scale` 被 `WRITE_SETTINGS` 拒绝，值保持 1.0；只计入 Compose/Robolectric 窄宽字号测试。没有实体大字结论 |
| 实体 Fold 开合/折痕/铰链 | 当前只连接直板机；未验证 |
| IME、TalkBack、真实账号切换 | 当前包真实回复菜单→空白编辑器→点击输入框：系统 `mImeWindowVis=3`、`mInputShown=true`，键盘与输入区/取消栏无覆盖，退出未输入/发送；本地私密截图不公开。TalkBack、跨账号和编辑器宽窄切换未实体实测 |

设备已核回 1200×2608/480、font1.0、自动旋转1/rotation0、`custom_schedule`/3、Wi-Fi1、飞行0；仅目标正式 App `com.qingyi5427.ngaqing` 留在设备上，本轮独立 helper、Debug 测试包及 `/sdcard/Download/nga_v8_*.png` 临时图均清理。公开截图证据与含私密信息的现场截图分开管理。六预设失败尝试所得图片全部是原生尺寸，不纳入成果。

## 交互 A–L

| ID | 本轮证据/结果 |
| --- | --- |
| A/J | #5 宽→窄→宽、专注与实体铰链不可实测；Reader Compose 状态转换测试覆盖部分；实体 Fold 未验证 |
| B | 宽栏主题选中/切帖由 Root/Thread 渲染与状态测试覆盖；实体手机仅单栏导航，宽窗实体未验证 |
| C | 回复草稿/光标跨布局由 Reader Compose 编辑器测试覆盖；实体 IME 与真实横竖旋转中编辑器未验证，绝不发送 |
| D/E | Dock 展开/计时、空白唤起、局部点击由 Reader 状态/渲染测试覆盖；实体滑动与 TalkBack 同时开启未验证 |
| F | 收藏筛选/账号隔离由 Favorites/Repository 自动测试覆盖；A63 离线实体只验证了发现页，不把它算收藏同步 |
| G | 1.5 字号渲染测试覆盖；实体被系统权限阻挡 |
| H | 当前包 Profile 菜单→阅读记录→返回、回复编辑器与IME已由只读 Runner/系统标志验证；未输入/发送，登录/私信/发帖发送未执行 |
| I | 当前包实体竖横屏 cutout/手势区、回复编辑器IME无遮挡已看；实体铰链未验证 |
| K | A63 旧包真实 #5 曾错回 #9；当前修复包连续两轮卡片第 5 楼、再入首个可见 #5，另初轮同结果。一次跳楼前置空楼层未定位 |
| L | 四 Tab/返回已 A63 实体验证；深链与状态栈由自动测试覆盖；离线本地续读实体可见，真实远端行为不推断 |

## 最终验收边界

1. 当前 Debug 候选已完成全量 205 项、Lint、包名/签名核查、保数据安装、四 Tab/续读/离线/IME/横屏真机验证；短横屏最后讨论卡完整在手势条上方，见工作树外 `new80-landscape-scroll-final.png`。自动化对“正在讨论”中途标题仍可见的断言是假红，已从最终 Runner 删除；接受依据是实体原尺寸截图，未声称每个节点稳定可访问。
2. 正式 R8 Release 已独立核 `debuggable=false`、无测试 Runner/fixture/CAMERA 权限、包名/1.2.0-code5/原签名与交付 SHA；保数据升级、冷启及外部容器只读主流程通过。独立容器源码与结果日志仅留本地 QA 目录，不注入或修改 Release。实体 Fold 开合、系统 1.3/1.5 字号、TalkBack 与真实账号切换没有现场结论。
