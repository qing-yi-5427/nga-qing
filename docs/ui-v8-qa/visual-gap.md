# V8 HTML 原型对 Compose 的独立视觉差距（2026-10-10）

**当前结论：未通过视觉验收。** 下面的 Compose 图均是本轮 Robolectric 原生 Compose 渲染（不是实体手机），且与 HTML PNG 的视口尺寸、样本数据不同；用于定位明显的视觉结构差距，不能做像素误差排名，也不能把功能测试全绿当视觉通过。视觉目标以 [`nga-qing-ios-v8-inner-portrait.html`](../../../NGA_Qing_UI_V8_Agent_Kit/nga-qing-ios-v8-inner-portrait.html) 及它导出的 PNG 为准。原型是设计目标，不要求 Android 伪装 iOS 状态栏或虚构论坛数据。

| 页面与原型 | 当前 Compose 样张 | 阻断视觉验收的差距 |
| --- | --- | --- |
| 发现：[phoneP](../../../NGA_Qing_UI_V8_Agent_Kit/nga-qing-v8-phoneP-home.png)、[innerP](../../../NGA_Qing_UI_V8_Agent_Kit/nga-qing-v8-innerP-home.png)、[innerL](../../../NGA_Qing_UI_V8_Agent_Kit/nga-qing-v8-innerL-home.png) | [`phoneP`](../../app/build/outputs/ui-evidence/v8-discover-phoneP.png)、[`innerP`](../../app/build/outputs/ui-evidence/v8-discover-innerP.png)、[`innerL`](../../app/build/outputs/ui-evidence/v8-discover-innerL.png) | 当前顶部只有粗简品牌/大搜索图标与「你的社区」，缺原型品牌/社区双层、日期与副信息、紧凑大标题+蓝色句点、贯穿内容宽度的浅灰搜索区。原型首屏像信息丰富的首页；当前大块留白、各区标题/间距和标题层级不同。 |
| 继续阅读与版块（同上） | 同上 | 当前续读是浅蓝平面卡/简单文字，缺深蓝→灰蓝渐变、抽象纹理、顶部图标/进度百分比、底部进度条和右下继续操作。当前版块四格使用相同论坛图标、只有名称（或不对应真实的副文案）；原型有不同线性符号、在线数、手机四格与内屏竖屏 2×2 水平卡、宽屏右栏紧凑卡。在线数必须有真实来源；无来源时保留层级而诚实省略。 |
| 正在讨论（同上） | 同上 | 当前内容是文字/时间/回复数的普通行，缺原型彩色字母 avatar、标签/作者时间、摘要、回复图标、行间细分隔及圆角白色容器的密度。innerL 虽已是左右区域，却失去原型上方轻段控、左侧紧凑讨论卡与右侧续读/版块/热榜的信息结构。 |
| 收藏：[phoneP](../../../NGA_Qing_UI_V8_Agent_Kit/nga-qing-v8-phoneP-saved.png)、[innerP](../../../NGA_Qing_UI_V8_Agent_Kit/nga-qing-v8-innerP-saved.png)、[innerL](../../../NGA_Qing_UI_V8_Agent_Kit/nga-qing-v8-innerL-saved.png) | [`phone dark`](../../app/build/outputs/ui-evidence/favorites-phone-dark.png)、[`innerL`](../../app/build/outputs/ui-evidence/v8-favorites-860x608-two-columns.png) | 当前收藏有大标题和筛选、宽屏双列雏形，但缺原型的品牌/眉题、右侧收藏计数、轻分隔、内嵌分段筛选、每张卡的版块/状态徽标→两行标题→摘要→作者时间/回复的完整信息层级；旧卡还保留右下更多/删除工具，视觉焦点与原型不同。两张当前图用不同 fixture/主题，须在相同亮色与样本下重验。 |
| 版块主题：[phoneP](../../../NGA_Qing_UI_V8_Agent_Kit/nga-qing-v8-phoneP-feed.png)、[innerL](../../../NGA_Qing_UI_V8_Agent_Kit/nga-qing-v8-innerL-feed.png) | [`primary-thread`](../../app/build/outputs/ui-evidence/primary-thread-320-light-normal.png)、[`reading-wide`](../../app/build/outputs/ui-evidence/primary-reading-1200x800-light.png) | 当前沿用旧密集 AppBar、选中蓝色大行、纯文字主题列表；原型是品牌/版块选择、较大的版块标题/副信息、灰搜索框、轻线性排序、行内标签/摘要/avatar/回复图标。宽屏左栏应是该主题列表，右栏阅读，不保留旧式双工具栏。 |
| 阅读：[phoneP](../../../NGA_Qing_UI_V8_Agent_Kit/nga-qing-v8-phoneP-reader.png)、[innerP](../../../NGA_Qing_UI_V8_Agent_Kit/nga-qing-v8-innerP-reader.png)、[innerL](../../../NGA_Qing_UI_V8_Agent_Kit/nga-qing-v8-innerL-reader.png) | [`medium`](../../app/build/outputs/ui-evidence/v8-reader-medium-608x860-native.png)、[`rich`](../../app/build/outputs/ui-evidence/v8-reader-medium-rich-608x860-native.png)、[`wide`](../../app/build/outputs/ui-evidence/primary-reading-1200x800-light.png) | 当前正文排版仍是旧的大标题居中/灰色作者占位块/紧凑正文与回复卡，顶部控件偏重，wide 图出现双返回/双标题行。原型是轻薄顶栏、正文前蓝色版块与分类 eyebrow、左对齐文章标题、彩色作者圆标/时间、宽松行距、精细浅色引用和图片卡、底部较小白色回复胶囊。图片/引用必须保留真实交互与链接安全。 |
| 导航（发现/收藏/阅读） | 上述各图 | 原型手机/中宽是悬浮白色胶囊，三项代表首页/版块/收藏并在更多入口覆盖其他路由；当前已改四 Tab，功能一触可达，但视觉是贴底大框和明显 Material 选中底色。可以保留四 Tab 业务目标，同时调整轻白玻璃/阴影/高度/位置；宽屏改顶部分段导航，不恢复常驻旧 rail。 |

## 新样张的同窗对照门槛

1. **先核心页，后全局**：主实现先提供发现 phoneP、innerP、innerL，收藏 phoneP/innerL 和主题 phoneP；Reader 提供阅读 phoneP、innerP、innerL。每张标注 Git SHA、测试/设备方式、窗口实际 dp、density、fontScale、主题和样本数据来源。原型 PNG 尺寸分别为 phoneP **394×854**、innerP **698×986**、innerL **986×698** CSS px；当前 Compose 初稿是 412×915、608×860、860×608，不能直接声称同窗贴合。新增渲染尽量用相同窗口逻辑尺寸、fontScale 1.0、浅色主题，并裁掉原型假状态栏/Android 真状态栏的差异后比较应用内容。不能把 CSS px 数字直接变成 dp 常数；实际断点仍按安全 Insets/折痕区与字号判断。
2. **同结构和视觉节奏**：对照顶部品牌→日期/眉题→大标题→搜索的竖向顺序，续读卡、版块格、讨论容器、收藏卡、阅读正文与导航的形状、色阶、对齐和可见条目数。允许平台字形与真实内容不同；卡片必须有相同的信息角色。对照前后至少目视检查首屏与滚动后，不能仅由 bounds/non-overflow 断言代替。
3. **业务样本不可冒充真机**：Robolectric fixture 可以采用原型同名、同长度标题以比较排版，截图标签写清“测试样本”。设备只展示实际公开数据或真实空/错态，不伪造在线数、摘要、续读进度、收藏、热门榜、#5 楼或登录状态。若某项无可靠数据来源，要保留图形层级与真实字段可表达的信息，诚实省略缺字段或用确切真实字段替代，并说明偏差；视觉相似不能以伪数据换取。
4. **保留平台与功能约束**：Android 真实状态栏/导航手势/IME/触控目标、安全 Insets、字号 1.3/1.5、深色、宽窄切换、返回/选中/历史/草稿/账号均继续回归。尤其 WebView 直接渲染、防闪烁、无第三栏、阅读 dock 手势状态不可因视觉改造而倒退。核心页尚未贴近本表前不恢复最终全量/装机/版本冻结。

中文字体核查：当前主题 [`Type.kt`](../../app/src/main/java/com/qingyi5427/ngaqing/ui/theme/Type.kt) 明确使用 `FontFamily.SansSerif`，不是自带的特殊中文位图字。旧 Robolectric PNG 已按原始像素分别查看，`v8-discover-phoneP.png` 是 412×915、`v8-discover-innerP.png` 是 608×860、`v8-discover-innerL.png` 是 860×608；原型 PNG 则是 394×854、698×986、986×698。现有文字细小感可能同时受到字体角色字号、不同画布尺寸和看图缩放影响，**尚无像素级证据指向字体文件损坏**。新样张必须以原尺寸单张查看中文笔画与基线，最终再用真实 400×869dp 手机截图复核，不从缩小拼图推断字体质量。

复核状态：初稿差距已通知主实现与 Reader；待两者提交同窗口新样张后逐图更新。HTML 原型本身的 24 张浏览器截图与旧 `FOLDABLE_REVIEW`/`HANDOFF` 的测试结果不计入新 APK 通过数。

阶段样张复核（2026-10-10 08:43，仍非同窗）：更新后的 [`v8-discover-phoneP.png`](../../app/build/outputs/ui-evidence/v8-discover-phoneP.png) 已呈现品牌/日期/蓝点大标题、整行搜索、渐变续读与不同版块图标，中文笔画在 412×915 原尺寸查看无明显缺损；与 394×854 原型相比，续读卡约 206px 对 165px，占屏更高且标题下留白大，正在讨论标题已落到约 y775，首条几乎被导航遮住。原型约 y681 已进入讨论卡。真实进度无来源时无需造百分比，但应减小卡高/空白/区间距，让首条可读；导航视觉仍需更轻。此轮只记进步与待修项，不改“未通过视觉验收”的结论。

## 全路由旧视觉与功能入口只读盘点

`NgaNavHost.kt` 当前注册 `LOGIN/ADD_ACCOUNT`、`DISCOVER`、`BOARDS`、`THREADS`、`POSTS`、`FAVORITES`、`PROFILE`、`SETTINGS`、`HISTORY`、`SEARCH`、`NEW_TOPIC`、`COMMUNITY`、`USER`、`WEB_EDITOR`。登录态有发现/版块/收藏/我的四个顶层入口；我的继续进入消息与关注、浏览历史、设置，设置内有账号/域名/阅读偏好/退出入口。路由并未因新增发现被移除；这些是**源码可达性**，不是实机操作已通过。

旧视觉风险按优先级追踪：① `BoardScreen`、`ThreadListScreen`、`PostScreen`、`FavoritesScreen` 初稿仍调用共享 `AppTopBar`；若仅改发现/收藏内容，切 Tab 或进帖会立刻回到厚重旧顶栏。`ProfileScreen` 已在本轮改为共享 `EditorialRootTopBar`，尚待渲染验收。② `SearchScreen`、`HistoryScreen`、`CommunityScreen`、`SettingsScreen`、`NewTopicScreen`、`UserScreen`、`WebEditorScreen`、`LoginScreen` 仍使用共享旧式顶栏，但不在四页 HTML 原型的逐像素范围，至少应统一轻量控件、颜色与标题层级，不能破坏表单/网页。③ 四 Tab 底栏由多个根页面各自调用 `AppBottomBar`，所有状态应一致，不可发现页新胶囊、收藏页旧胶囊。④ `NgaNavHost.kt` 的宽屏 `RootPane/ReadingPane` 已移除常驻 rail 的布局，但 `LocalRootNavigationRail` 仍是宽屏页面导航呈现信号；需要以新样张/实体状态确认不会意外画旧 rail，且横屏有可发现的导航入口。

功能风险清单仍按原验收矩阵：继续阅读/历史的真实锚点、收藏本地与远端状态、版块目录和主题筛选、登录/加号账号退出恢复、回复草稿、私信入口、阅读设置、WebView 兜底、深链和账号切换都应存在。视觉变更不得删除这些入口或用不可点击的静态原型填充。

## 阶段更新：同窗原生样张

上方首轮差距表与旧风险盘点保留其当时的历史状态；以下是改版后的阶段证据，**不代表最终实机验收**。发现 394×854、698×986、986×698 与 HTML 同窗样张已重做品牌/日期/蓝点标题、搜索、渐变续读、不同版块图标和讨论容器。根审接受 394×854 的结构、间距与首屏密度方向；中文标题字重与真实设备 Insets、阴影还须整机复核。阅读 394×854 与 698×986 同内容图对照 HTML 时，标题/作者/正文/引用/图片/回复胶囊层级已贴近，原型假的回复数、进度与设备无可靠来源的字段未进入生产。

版块最新独立 Native Compose 图：[`phoneP 394×854`](../../app/build/outputs/ui-evidence/v8-board-root-phoneP.png)、[`innerL 986×698`](../../app/build/outputs/ui-evidence/v8-board-root-innerL.png)、[`嵌入左栏 280dp / 字号 1.5`](../../app/build/outputs/ui-evidence/v8-board-embedded-280-font1p5.png)、[`嵌入左栏 340dp / 字号 1.5`](../../app/build/outputs/ui-evidence/v8-board-embedded-340-font1p5.png)。根页已有轻品牌、大标题蓝点、浅灰搜索、白色细线分组与轻图标，宽屏顶部四段导航取代旧 rail；220dp「目录概览」仅在版块内部，根审同意保留。分组末行底角已收成小圆角；280/340dp 局部侧栏采用简短标题与本地宽度策略，未继承整窗宽导航或顶栏。最新 Board 专项 4/4 渲染通过，旧版块内容回归前轮 5/5；截图仅使用合成版块样本，不能说明真机内容/折叠铰链可达。完整根页与折叠过渡仍待冻结候选验证。
