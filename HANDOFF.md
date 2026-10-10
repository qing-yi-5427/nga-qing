# NGA 清漪 V8 交接（2026-10-10）

本轮代码位于隔离工作树 `G:\AI\coding\10-nga\NgaClient-ui-v8`、分支 `feat/ui-v8-adaptive`。原工作树 `G:\AI\coding\10-nga\NgaClient` 保留，不在本轮修改或合并。视觉基准是同级 `NGA_Qing_UI_V8_Agent_Kit/nga-qing-ios-v8-inner-portrait.html` 及其 PNG；实施映射见 `docs/ui-v8-audit.md`，设备与场景记录见 `docs/ui-v8-qa/validation.md`。

## 1.2.2 当前增量（2026-10-10）

生产源码至提交 `48e02a403d681b3c82d19cd838608397a0dc8f45`：修复阅读回复左侧空列、常逛版块卡内容居中、搜索框双层边缘、根 Tab 从版块子页返回发现，并换为与 V8 一致的蓝白 N 气泡图标。头像数据层处理多头像值和旧图域名；测试图片与空/错误占位通过，但实际 NGA 远端头像尚待在线具体样本。原单 `NavController`、阅读锚点、回复草稿与网页直渲染边界未改。

QA 全量为 50 suites、228 tests，0 失败/错误/跳过；Lint 0 error、23 warning。窄底栏/宽侧栏根 Tab 真 `NavHostController` 回归 4/4 通过。正式 1.2.2/code7 R8 包在 `G:\AI\coding\10-nga\V8_Release\nga-qing-1.2.2-v8.apk`，SHA-256 `42C393A710B5E61DFF352F19244FED0C1D5DDC1D7756CDC0CDD478EB94C4830D`；同目录有 `.sha256` 和 `build-verification-1.2.2.md`。旧 1.2.0/1.2.1 包保留。本轮设备未连接，用户已将实体操作延期，因此 1.2.2 尚无保数据安装与真实界面验收。下文是 1.2.0 的历史交接快照。

## 已实现

- 发现、版块、收藏、我的四个根入口；宽屏为顶部导航，紧凑窗口为底部浮动导航。全部原有子路由仍使用同一个 `NavController`，账号 revision 切换继续重建旧会话。
- 可用窗口宽高、系统字号及折痕安全矩形决定布局。内屏竖屏单列舒适字号；宽屏发现两区、收藏双列、主题列表与阅读正文双栏；低高度/过大字号按实际可用空间收敛。
- THREADS 宽屏是左主题列表加右空态；POSTS 复用原 THREADS back stack 的 ViewModel，左列表与右正文独立滚动，250–370dp 左栏可调。收藏、历史和深链直达 POSTS 时没有虚构的左栏，也不显示无效“恢复双栏”；版块目录仍可临时打开。
- 原账户、收藏同步、阅读锚点、回复草稿、网页正文直渲染与安全返回路径保留。HTML 原型的在线数、阅读百分比等无真实来源字段没有填充假数据。

## 已完成验证

纳入续读持久化修复、发现页中文网络错误提示和六窗口模拟测试后，`:app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest --no-parallel --max-workers=1` 全部成功。JUnit XML 汇总为 48 个 suite、205 项测试、0 失败/错误/跳过；Lint 0 error、35 warning。较旧基线为 34 warning；新增 1 项仅是 AndroidTest runner 1.5.0 可升级至 1.5.2 的版本提示，不涉及无障碍或代码正确性。

通过验收的 Debug 候选 SHA-256 为 `80B64AC1E9E45DB62B03829F4D75F7960C322E97A864D1BAA195A077B4D59A7F`。旧 `A63A6DEC…` 包的实体测试曾发现 #5 楼续读间歇丢失；修复后新包连续验证成功，细节见 `docs/ui-v8-qa/validation.md`。最终发布型 APK 已从相同生产源码构建，路径、签名与实体烟测见 `docs/ui-v8-release.md`。

Compose 原生截图在 `app/build/outputs/ui-evidence/`：`v8-discover-innerL-html-size.png`、`v8-thread-html-innerL-986x698.png`、`v8-posts-html-innerL-986x698.png`。前两者分别是发现双区与主题空态；POSTS 截图来自生产 `PostRouteContent` 和正式 `PostScreen` 参数，测试还验证专注阅读隐藏左栏及恢复。`ReaderStateTransitionRenderTest` 在同一生产双栏容器下覆盖 #5 楼、宽窄/专注切换与回复编辑选区。截图使用测试样本，不能当作真机 NGA 数据。

六种窗口预设的发现、收藏、主题、阅读四页 Compose 模拟共 24 图，生成路径和 dp 尺寸见 `docs/ui-v8-window-presets.md`。它们是本地测试数据与假定密度下的布局证据，不是实体折叠屏测试。

## 接下来

正式 APK `G:\AI\coding\10-nga\V8_Release\nga-qing-1.2.0-v8.apk`（SHA-256 `221A7F288157340E5CD91E291D5D8B0B7789F76EA405745058470B61DE6B2C45`）已完成 R8/资源压缩、版本/权限/签名核验和保数据实体安装；设备拉回的 `base.apk` 摘要一致，冷启、四入口与 #5 楼续读通过。QA 用独立纯 Java helper 测试容器操作 Release，本体没有注入测试框架。系统拒绝 `wm size/density` 和字体设置覆盖，六预设仅能用本地 Compose 模拟，真实折叠机尚不可测。不发帖、回帖或私信，不卸载或清除数据。当前不合并、不推送、不发布 GitHub Release。
