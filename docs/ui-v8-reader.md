# V8 阅读页交接

原生帖子仍使用现有富文本块渲染，保留引用、图片、链接、楼层菜单、回复、收藏、跳楼、翻页、只看楼主及网页版回退。中屏内屏正文基准为 18sp、标题 32sp，手机正文基准为 16sp；用户阅读倍率由现有主题施加一次，系统字体缩放继续由 Compose 密度处理。帖子内容最大宽度 720dp 并居中，中屏正文左右各 24dp，手机仍为 20dp。

自动工具条从短暂展开收为胶囊，再隐藏；只观察原生正文实际消费的用户滚动，下滑 26dp 隐藏、上滑显示，2dp 内抖动不触发。展开约 2.8 秒后收起、再约 4.5 秒后隐藏。点击动作会重新计时；按住工具条、键盘或 TalkBack 焦点、输入法、楼层菜单和弹窗打开时暂停计时。所有操作热区至少 48dp，窄屏与放大字体下自动换行。纯净阅读默认隐藏，滚动后保持隐藏，只能轻触正文最外侧 20dp 空白边缘暂时唤出；边缘不覆盖正文富文本。顶部更多菜单始终提供原有功能与恢复入口。网页版不叠加原生工具条，也保持 WebView 脱离拉刷新背景层的既有修复。

设置里的“阅读工具条”可选“自动显示”或“纯净阅读”，复用现有 Preferences DataStore。模式只涉及阅读 UI，不改变账号、会话、草稿或网络请求。

验证命令：`./gradlew :app:testDebugUnitTest --tests com.qingyi5427.ngaqing.ui.post.ReaderDockStateTest --tests com.qingyi5427.ngaqing.data.local.ReadingDockPreferencesTest --tests com.qingyi5427.ngaqing.ui.primary.PrimaryScreensRenderTest --tests com.qingyi5427.ngaqing.ui.post.ReaderDockQaRenderTest --console=plain`。2026-10-10 最终定向复跑 **14 项通过、0 失败，BUILD SUCCESSFUL（18 秒）**；日志为 `app/build/outputs/v8-reader-targeted-tests.log`。覆盖真实 Compose 点击/按下释放、楼层菜单和收藏失败弹窗阻塞计时、短窗口最后一行避让、正文链接与纯净模式边缘、持久化模式和滚动状态转换。

Robolectric Native 样图位于 `app/build/outputs/ui-evidence/`：`v8-reader-medium-608x860-native.png` 为长帖首屏，`v8-reader-medium-rich-608x860-native.png` 为三楼、引用与本地生成的测试图片，`v8-reader-dock-320-font1p5-expanded.png` 为 320dp 窗口内 288dp 安全宽度、系统字体 1.5 倍时的独立工具条布局。前者图片是无网络依赖的测试位图。真机折叠角度、TalkBack 与系统字体仍交独立 QA 验收。
