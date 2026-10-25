# NGA 清漪 V8 交接（2026-10-10）

## 当前发布：1.2.5（versionCode 10）

当前应用源码基线为 `95a3443efe0aa3d082fcce535185942f4201416c`。1.2.5 的安装包、源码标签和核验记录统一见 [发布核验](docs/releases/1.2.5.md) 与 [GitHub Release](https://github.com/qing-yi-5427/nga-qing/releases/tag/v1.2.5)。下方保留各阶段历史记录；1.2.4 的限制与未发布状态不代表 1.2.5 当前结论。


## 1.2.4 云收藏自动回读（2026-10-11）

隔离分支 `fix/live-post-bookmarks-20261011` 的 1.2.4/code9 已保数据覆盖安装。普通添加/移除现在各只写一次，自动回读云目录确认目标成员关系后才更新两个入口的界面和本地缓存；失败不伪造成功，也不重发写请求。定向 16 tests 全过、Lint 0 error；正式包在 `G:\AI\coding\10-nga\V8_Release\nga-qing-1.2.4-v8.apk`，SHA-256 `CC6F5F73999D52CF50124690367991F768FE64535170767236DF494CC61FD41B`，与旧版同证书。临时网页诊断代码已移除，手机常亮设置恢复 `60000/0`。

当前帖 `tid=47697364` 在测试目录 `4719076` 的原成员关系保持 true。实机应用删除与同账号官方网页删除均返回操作提示后仍可读到该帖，故真实云端移除**未通过**；普通添加未对该帖实机写入。准确证据与限制见 `docs/favorite-readback-qa-2026-10-11.md`，不能把自动回读的测试通过表述成服务器删除成功。本轮没有推送、合并或发布 GitHub Release。

## 1.2.3 阅读页与云收藏（2026-10-11）

隔离分支 `fix/live-post-bookmarks-20261011` 的 1.2.3/code8 已在设备 `058A8840499C3540` 保数据覆盖安装并验收：`tid=47697364` 首楼 B 站 iframe 改为可点视频卡，后续正文保留；NGA 账号原无云收藏文件夹，按官网空态流程显式新建后，服务器回读新目录 `4719076` 包含当前帖，关闭重开仍显示已收藏。原本地收藏未删除。视频链接正确传给 Edge，B 站自身提示该视频页面不存在，未验证实际播放。临时手机常亮设置已恢复 `60000` / `0`。

最终 APK：`G:\AI\coding\10-nga\V8_Release\nga-qing-1.2.3-v8.apk`，SHA-256 `1726B29C44979E407F80FF625B89316ABC6D6045BC0C49DF3D4604338DAA6F9A`，同签名、v2 验证与 zipalign 均通过。完整测试、真机截图、协议边界和未验范围见 `docs/live-post-bookmarks-qa-2026-10-11.md`；发布包核验见 `G:\AI\coding\10-nga\V8_Release\build-verification-1.2.3.md`。原 `NgaClient` 工作树未动，本轮未推送或发布 GitHub Release。

V8 研发在隔离工作树 `G:\AI\coding\10-nga\NgaClient-ui-v8`、分支 `feat/ui-v8-adaptive` 完成；原 `G:\AI\coding\10-nga\NgaClient` 工作树中的未提交修改一直保留。视觉基准是同级 `NGA_Qing_UI_V8_Agent_Kit/nga-qing-ios-v8-inner-portrait.html` 及其 PNG；实施映射见 `docs/ui-v8-audit.md`，设备与场景记录见 `docs/ui-v8-qa/validation.md`。

**2026-10-10 交付进展：**用户已在实体设备上试用 1.2.2，确认帖子头像显示并表示试用通过，授权合并并推送到自己的仓库。开发侧此前没有连接该设备；以下旧 QA 记录的「未实机验证」指当时的独立验证范围，不抹去该历史边界，也不将用户试用写成开发侧测试。Git 合并及推送结果应以远端 `main` 为准。

## 1.2.2 当前增量（2026-10-10）

生产源码至提交 `48e02a403d681b3c82d19cd838608397a0dc8f45`：修复阅读回复左侧空列、常逛版块卡内容居中、搜索框双层边缘、根 Tab 从版块子页返回发现，并换为与 V8 一致的蓝白 N 气泡图标。头像数据层处理多头像值和旧图域名；开发侧当时通过了本地图片与空/错误占位测试，未拿到 NGA 在线具体样本，用户随后在实体设备上确认头像显示。原单 `NavController`、阅读锚点、回复草稿与网页直渲染边界未改。

QA 全量为 50 suites、228 tests，0 失败/错误/跳过；Lint 0 error、23 warning。窄底栏/宽侧栏根 Tab 真 `NavHostController` 回归 4/4 通过。正式 1.2.2/code7 R8 包在 `G:\AI\coding\10-nga\V8_Release\nga-qing-1.2.2-v8.apk`，SHA-256 `42C393A710B5E61DFF352F19244FED0C1D5DDC1D7756CDC0CDD478EB94C4830D`；同目录有 `.sha256` 和 `build-verification-1.2.2.md`。旧 1.2.0/1.2.1 包保留。开发侧的 1.2.2 检查没有连接设备；用户随后完成实体试用并确认头像显示。保数据安装、设备包摘要和完整场景矩阵未由开发侧独立复核。下文是 1.2.0 的历史交接快照。

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

## 1.2.0 历史交接结论

正式 APK `G:\AI\coding\10-nga\V8_Release\nga-qing-1.2.0-v8.apk`（SHA-256 `221A7F288157340E5CD91E291D5D8B0B7789F76EA405745058470B61DE6B2C45`）当时已完成 R8/资源压缩、版本/权限/签名核验和保数据实体安装；设备拉回的 `base.apk` 摘要一致，冷启、四入口与 #5 楼续读通过。QA 用独立纯 Java helper 测试容器操作 Release，本体没有注入测试框架。系统拒绝 `wm size/density` 和字体设置覆盖，六预设仅能用本地 Compose 模拟，真实折叠机当时尚不可测。该次 QA 未发帖、回帖或私信，未卸载或清除数据。当时决定暂不合并、推送或发布 GitHub Release；1.2.2 用户试用通过后，合并与推送已获授权，GitHub Release 不在本次范围。
