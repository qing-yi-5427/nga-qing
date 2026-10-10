# V8 主题列表交接

主题列表按 HTML V8 的手机 feed 与内屏横屏紧凑左栏排版。顶部 Q. 品牌按钮打开现有临时版块目录，铅笔发帖、灰色搜索入口、最新/发帖/精华筛选和更多排序继续调用原路由或 ViewModel。`发帖` 对应真实的发帖时间排序；没有服务端热门排序，因此不把原型的“热门”标签伪装为功能。每行显示真实时间、推荐字段、标题、作者首字头像和回复数；`ThreadItem` 不提供正文摘要或在线人数，列表不填造演示文字。

双栏是否使用紧凑样式由主题列表自身的可用宽度与 `readerTwoPane` 决定，因此宽屏 THREADS 尚未选帖时也保持 294dp 左栏，右边是真实未选帖空态；进入 POSTS 后复用原 THREADS ViewModel、列表锚点及选中 tid。单栏手机在列表底部显示现有共享 `AppBottomBar`，选中“版块”；其点击使用当前 THREADS 路由判断，能一触进入版块根页。底栏固定在屏幕下方，列表留 96dp 尾距，末帖可滚到其上方。宽屏嵌入左栏不显示该底栏，也不增加第三条常驻导航。

原有搜索、发帖、收藏版块、刷新、两种排序、推荐筛选、子版块、分页加载/重试、离线缓存提示、行点击、已读与选中状态、列表滚动位置持久化均沿原控制流保留。基线行只有整行打开动作，没有行内更多菜单。手机顶部品牌与列表标题沿原型分级，不再有旧独立玻璃顶栏。

`ThreadHtmlVisualRenderTest` 使用与 HTML 数码硬件示例相同的标题和作者，但只注入测试假数据；输出 `app/build/outputs/ui-evidence/v8-thread-html-phoneP-394x854.png` 与 `v8-thread-html-innerL-986x698.png`。测试用真实 NavHost 图分别点击 Q. 品牌和底部“版块”Tab，核对底栏在下部、末帖可滚到其上方；宽屏直接渲染生产 `ThreadRouteContent`，核对左栏与右空态。`HeaderScrollRenderTest` 继续验证渐进折叠与重建恢复，320dp/2倍字号时从未被底栏覆盖的 header 上半部起拖。最近一次三类专属定向共 5/5 通过：

```powershell
./gradlew :app:testDebugUnitTest --tests '*ThreadHtmlVisualRenderTest' --tests '*HeaderScrollRenderTest' --tests '*ReaderStateTransitionRenderTest' --console=plain
```

这些是 Robolectric 原生 Compose 画面与状态证据；真机上的安全区、触摸、旋转和实体折叠由独立 QA 验收。
