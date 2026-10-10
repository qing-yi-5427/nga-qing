# 发现页横屏重复画面定位

实体折叠屏截图（2026-10-10）里，发现页横屏左右两侧都绘出了“继续阅读”和“常逛版块”，卡片宽度也相同。对应生产 Compose 分支的左侧 `LazyColumn` 实际只包含“正在讨论”，右侧才包含前两项，因此重复发生在绘制层，不是首页数据或布局分支把条目放了两次。

本工作树使用 `dev.chrisbanes.haze:haze:1.0.2`。检查本机解析的 `haze-android-1.0.2/haze-release.aar` 中 `classes.jar` 字节码：`HazeState` 只有一个 `contentLayer: GraphicsLayer` 和一个 `positionOnScreen`；`HazeNode.draw` 从该 state 取得同一 layer，调用 `record(drawContent)` 后再 `drawLayer`；各节点的 `onGloballyPositioned` 也写入同一个位置。原发现页在同一 `NgaBackdropScope` 内，给宽屏左右两个 `LazyColumn` 都附加 `.ngaBackdropSource()`。两个来源共用可覆写的绘制层，与截图中右侧画面覆盖左侧、宽度也像右栏的现象一致。静态布局测试只检查 Compose 语义节点的位置，因此未发现绘制错误。

发现页的悬浮底栏 `AppBottomBar` 使用普通 `Surface`，不调用 `NgaGlassSurface` 或 `hazeChild`，宽屏也无底栏消费者。因此修复仅移除发现页的 `NgaBackdropScope` 及三个列表上的 `.ngaBackdropSource()`；保留其他页面的玻璃效果和导航/折叠状态机制。动态窄宽切换及画面回归结果另见 `docs/ui-v8-qa/validation.md`。

本轮同时把首页搜索缩为标题行的 48 dp 图标按钮，并将版块区放在继续阅读之前。动态切换测试还发现条件分支销毁时首页列表滚动位置会归零；三个列表现在各自在分支外保存 `LazyListState`，窄屏返回窄屏、宽屏返回宽屏时恢复原列表的索引和偏移，不改变内容路由或跨栏映射。
