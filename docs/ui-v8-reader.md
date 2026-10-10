# V8 阅读页交接

原生帖子仍使用现有富文本块渲染，保留引用、图片、链接、楼层菜单、回复、收藏、跳楼、翻页、只看楼主及网页版回退。中屏内屏正文基准为 18sp、标题 32sp，手机正文基准为 16sp、标题 26sp；用户阅读倍率由现有主题施加一次，系统字体缩放继续由 Compose 密度处理。帖子内容最大宽度 720dp 并居中，中屏正文左右各 40dp，手机各 24dp。轻顶栏使用文本返回、裸图标收藏和更多；双栏模式显示版块面包屑及专注操作。眉题使用真实版块名，缺失时显示“社区讨论”；楼层号保持 NGA 原始 `lou`，不按原型演示编号改写。

自动工具条从短暂展开收为胶囊，再隐藏；只观察原生正文实际消费的用户滚动，下滑 26dp 隐藏、上滑显示，2dp 内抖动不触发。展开约 2.8 秒后收起、再约 4.5 秒后隐藏。点击动作会重新计时；按住工具条、键盘或 TalkBack 焦点、输入法、楼层菜单和弹窗打开时暂停计时。胶囊视觉高 40dp，透明热区 48dp；展开只显示收藏、跳楼、只看楼主、蓝色写回复四项，320dp/大字体时以 2×2 排列。分页、手动收起和恢复均从顶部更多菜单进入。纯净阅读默认隐藏，滚动后保持隐藏，只能轻触正文最外侧 20dp 空白边缘暂时唤出；边缘不覆盖正文富文本。网页版不叠加原生工具条，也保持 WebView 脱离拉刷新背景层的既有修复。

设置里的“阅读工具条”可选“自动显示”或“纯净阅读”，复用现有 Preferences DataStore。模式只涉及阅读 UI，不改变账号、会话、草稿或网络请求。

验证命令：`./gradlew :app:testDebugUnitTest --tests '*ReaderDockStateTest' --tests '*ReadingDockPreferencesTest' --tests '*PrimaryScreensRenderTest' --tests '*ReaderDockQaRenderTest' --tests '*ReaderHtmlVisualRenderTest' --tests '*ReaderProgressTest' --tests '*ReaderStateTransitionRenderTest' --console=plain`。Reader 旧阶段的19项定向通过；本阶段 `ReaderStateTransitionRenderTest` 也通过。旧测试的“六项同排”和“工具条内翻页”断言不再对应 HTML 原型，已改为验证 2×2 不重叠、安全边界，以及在顶部菜单实际打开分页、收起和恢复。其余覆盖真实 Compose 点击/按下释放、楼层菜单和收藏失败弹窗阻塞计时、短窗口最后一行避让、正文链接与纯净模式边缘、持久化模式和滚动状态转换。

宽→窄→宽/专注切换使用生产 `PostRouteContent` 与 `resolvePostRouteLayout` 组合进行回归：先跳到真实 `#5` 楼，再验证左主题栏按双栏、专注和窄窗显隐；打开原生回复编辑器输入文字并将真实 Compose 选区设为 `TextRange(2,6)`，切换窗宽与专注后检查文字和选区，最后恢复双栏及楼层可见。该测试覆盖同一 Activity/Compose 树内的重布局；进程重建后的输入法选区与实体折叠角度仍需真机验证，不以 ViewModel 的 String 草稿测试替代。

Robolectric Native 样图位于 `app/build/outputs/ui-evidence/`。本轮已生成与 HTML 原型同标题和前四段正文的 `v8-reader-html-phoneP-394x854.png`、`v8-reader-html-innerP-698x986.png` 首屏及对应 `-rich-` 富文本滚动截图；图示是无网络依赖的本地生成测试位图，生产只渲染真实帖子内容。另保留 320dp 窗口、系统字体 1.5 倍的独立工具条布局截图。真机折叠角度、TalkBack 与系统字体仍交独立 QA 验收。
