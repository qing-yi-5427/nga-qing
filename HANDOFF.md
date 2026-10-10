# NGA 清漪 V8 交接（2026-10-10）

本轮代码位于隔离工作树 `G:\AI\coding\10-nga\NgaClient-ui-v8`、分支 `feat/ui-v8-adaptive`。原工作树 `G:\AI\coding\10-nga\NgaClient` 保留，不在本轮修改或合并。视觉基准是同级 `NGA_Qing_UI_V8_Agent_Kit/nga-qing-ios-v8-inner-portrait.html` 及其 PNG；实施映射见 `docs/ui-v8-audit.md`，设备与场景记录见 `docs/ui-v8-qa/validation.md`。

## 已实现

- 发现、版块、收藏、我的四个根入口；宽屏为顶部导航，紧凑窗口为底部浮动导航。全部原有子路由仍使用同一个 `NavController`，账号 revision 切换继续重建旧会话。
- 可用窗口宽高、系统字号及折痕安全矩形决定布局。内屏竖屏单列舒适字号；宽屏发现两区、收藏双列、主题列表与阅读正文双栏；低高度/过大字号按实际可用空间收敛。
- THREADS 宽屏是左主题列表加右空态；POSTS 复用原 THREADS back stack 的 ViewModel，左列表与右正文独立滚动，250–370dp 左栏可调。收藏、历史和深链直达 POSTS 时没有虚构的左栏，也不显示无效“恢复双栏”；版块目录仍可临时打开。
- 原账户、收藏同步、阅读锚点、回复草稿、网页正文直渲染与安全返回路径保留。HTML 原型的在线数、阅读百分比等无真实来源字段没有填充假数据。

## 已完成验证

纳入续读持久化修复、发现页中文网络错误提示和六窗口模拟测试后，`:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --no-parallel --max-workers=1` 全部成功。JUnit XML 汇总为 48 个 suite、205 项测试、0 失败/错误/跳过；Lint 0 error、35 warning。较旧基线为 34 warning；新增 1 项仅是 AndroidTest runner 1.5.0 可升级至 1.5.2 的版本提示，不涉及无障碍或代码正确性。

当前 Debug 候选 `app/build/outputs/apk/debug/nga-qing-debug.apk` 为 `com.qingyi5427.ngaqing`、1.2.0 / versionCode 5，SHA-256 `80B64AC1E9E45DB62B03829F4D75F7960C322E97A864D1BAA195A077B4D59A7F`；AndroidTest APK 为 `app/build/outputs/apk/androidTest/debug/nga-qing-debug-androidTest.apk`，SHA-256 `22714231484B996B34E8EB8BB0846129A9AB8B3A9C40CBCF8C053D5E415F869F`。两包签名证书 SHA-256 均为 `3774e86da5188b94341d667ad7e4a871ad8cda74f81088fb8445654d6068e0d6`，与旧装包一致。前一候选 `A63A6DEC…` 的实体测试发现 #5 楼续读间歇丢失；当前包等待 QA 保数据覆盖安装复验，不得沿用前一候选的设备通过结论。此 Debug 包不是最终 Release。

Compose 原生截图在 `app/build/outputs/ui-evidence/`：`v8-discover-innerL-html-size.png`、`v8-thread-html-innerL-986x698.png`、`v8-posts-html-innerL-986x698.png`。前两者分别是发现双区与主题空态；POSTS 截图来自生产 `PostRouteContent` 和正式 `PostScreen` 参数，测试还验证专注阅读隐藏左栏及恢复。`ReaderStateTransitionRenderTest` 在同一生产双栏容器下覆盖 #5 楼、宽窄/专注切换与回复编辑选区。截图使用测试样本，不能当作真机 NGA 数据。

六种窗口预设的发现、收藏、主题、阅读四页 Compose 模拟共 24 图，生成路径和 dp 尺寸见 `docs/ui-v8-window-presets.md`。它们是本地测试数据与假定密度下的布局证据，不是实体折叠屏测试。

## 接下来

QA 独占 ADB：用当前 Debug/Test 包保数据覆盖安装，重复公开帖 #5 楼返回续读、离线错误提示及只读 Runner；实体直板机其他结果见 `docs/ui-v8-qa/validation.md`。系统拒绝 `wm size/density` 和字体设置覆盖，六预设仅能用本地 Compose 模拟，真实折叠机尚不可测。不发帖、回帖或私信，不卸载或清除数据。主代理审核设备结果后才构建正式 `nga-qing-1.2.0-v8.apk`、签名/散列复核及 Release 包保数据烟测。当前不合并、不推送、不发布 GitHub Release。
