# NGA 清漪 V8 交接（2026-10-10）

本轮代码位于隔离工作树 `G:\AI\coding\10-nga\NgaClient-ui-v8`、分支 `feat/ui-v8-adaptive`。原工作树 `G:\AI\coding\10-nga\NgaClient` 保留，不在本轮修改或合并。视觉基准是同级 `NGA_Qing_UI_V8_Agent_Kit/nga-qing-ios-v8-inner-portrait.html` 及其 PNG；实施映射见 `docs/ui-v8-audit.md`，设备与场景记录见 `docs/ui-v8-qa/validation.md`。

## 已实现

- 发现、版块、收藏、我的四个根入口；宽屏为顶部导航，紧凑窗口为底部浮动导航。全部原有子路由仍使用同一个 `NavController`，账号 revision 切换继续重建旧会话。
- 可用窗口宽高、系统字号及折痕安全矩形决定布局。内屏竖屏单列舒适字号；宽屏发现两区、收藏双列、主题列表与阅读正文双栏；低高度/过大字号按实际可用空间收敛。
- THREADS 宽屏是左主题列表加右空态；POSTS 复用原 THREADS back stack 的 ViewModel，左列表与右正文独立滚动，250–370dp 左栏可调。收藏、历史和深链直达 POSTS 时没有虚构的左栏，也不显示无效“恢复双栏”；版块目录仍可临时打开。
- 原账户、收藏同步、阅读锚点、回复草稿、网页正文直渲染与安全返回路径保留。HTML 原型的在线数、阅读百分比等无真实来源字段没有填充假数据。

## 已完成验证

冻结生产源码后的 `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-parallel --max-workers=1` 成功：45 个 suite、191 项测试、0 失败/错误/跳过；Lint 0 error、35 warning。较旧基线为 34 warning；新增 1 项仅是 AndroidTest runner 1.5.0 可升级至 1.5.2 的版本提示，不涉及无障碍或代码正确性。此前 `:app:assembleDebugAndroidTest` 已成功；独立 Runner 的启动等待策略正在真机阶段调试，其后要重新构建测试 APK。

Debug 候选 `app/build/outputs/apk/debug/nga-qing-debug.apk` 为 `com.qingyi5427.ngaqing`、1.2.0 / versionCode 5，SHA-256 `A63A6DEC3974743479EA7E2EEEC601FE3C1A1911FF674937636C197C88029588`；签名证书 SHA-256 `3774e86da5188b94341d667ad7e4a871ad8cda74f81088fb8445654d6068e0d6` 与旧装包一致。QA 已用保留数据的覆盖安装；主程序冷启动、窗口焦点与界面节点初检正常，完整设备矩阵以 `docs/ui-v8-qa/validation.md` 后续记录为准。此 Debug 包不是最终 Release。

Compose 原生截图在 `app/build/outputs/ui-evidence/`：`v8-discover-innerL-html-size.png`、`v8-thread-html-innerL-986x698.png`、`v8-posts-html-innerL-986x698.png`。前两者分别是发现双区与主题空态；POSTS 截图来自生产 `PostRouteContent` 和正式 `PostScreen` 参数，测试还验证专注阅读隐藏左栏及恢复。`ReaderStateTransitionRenderTest` 在同一生产双栏容器下覆盖 #5 楼、宽窄/专注切换与回复编辑选区。截图使用测试样本，不能当作真机 NGA 数据。

## 接下来

QA 独占 ADB：更新并运行只读 AndroidTest Runner，完成真实直板机与六种窗口预设的页面、返回、字号、夜间、离线和状态恢复检查；所有模拟尺寸明确标注为模拟，真实折叠机尚不可测。不发帖、回帖或私信，不卸载或清除数据。主代理审核设备结果后才构建正式 `nga-qing-1.2.0-v8.apk`、签名/散列复核及 Release 包保数据烟测。当前不合并、不推送、不发布 GitHub Release。
