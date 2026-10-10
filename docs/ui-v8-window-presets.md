# V8 六窗口 Compose 模拟索引

这些图由 `app/src/test/java/com/qingyi5427/ngaqing/ui/primary/WindowPresetRenderTest.kt` 使用 Robolectric 原生 Compose 渲染生成。测试逐组调用生产 `RootPane`、`ThreadRouteContent`、`PostRouteContent` 和 `PostScreen`，使用固定本地样本，不请求 NGA 网络。它检查页面主入口、阅读单双栏、根导航和紧凑窗口底栏边界。六个测试均通过；完整测试批次为 48 suite、205 test，0 failed/error/skipped。

| 预设 | 模拟可用尺寸 | 密度说明 | 页面截图前缀 |
| --- | --- | --- | --- |
| phoneP | 403×875dp | 480dpi 对应直板机尺寸换算 | `v8-sim-phoneP-480dpi-` |
| phoneL | 875×403dp | 480dpi 对应直板机横向换算 | `v8-sim-phoneL-480dpi-` |
| coverP | 425×623dp | 假定 440dpi 的折叠外屏 | `v8-sim-coverP-440dpi-assumed-` |
| coverL | 623×425dp | 假定 440dpi 的折叠外屏 | `v8-sim-coverL-440dpi-assumed-` |
| innerP | 608×860dp | 假定 440dpi 的折叠内屏 | `v8-sim-innerP-440dpi-assumed-` |
| innerL | 860×608dp | 假定 440dpi 的折叠内屏 | `v8-sim-innerL-440dpi-assumed-` |

每个前缀各有 `discover.png`、`favorites.png`、`threads.png`、`reader.png` 四图，共 24 图，均保存在本地 `app/build/outputs/ui-evidence/`（构建产物，不入 Git）。再次运行 `:app:testDebugUnitTest --tests '*WindowPresetRenderTest*' --no-parallel --max-workers=1` 可重建。只有 innerL 的高度和宽度同时满足阅读双栏门槛；phoneL 虽宽，403dp 高度下保持单栏。屏幕取向、字体、折痕及网络状态的实体结果需分别参考 `docs/ui-v8-qa/validation.md`；本组图不能证明物理折叠机表现。
