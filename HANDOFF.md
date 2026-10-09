# NGA 清漪 1.1.1 交接（2026-09-28）

本文件供切换模型后直接继续当前工程，不需要调用 Codex `handoff_thread`（那是切换任务检出环境，本次只是交接上下文）。工程位于 `G:\AI\coding\10-nga\NgaClient`；分支 `feat/ui-design-system`，HEAD `916633a7ca6da0cc243aa81f2550cbf9eb4d790b`。自该提交后的 1.1.1 工作仍在工作树中，**未提交、未推送**，应保留所有现有修改和未跟踪文件。用户长期要求：所有代码修改与构建交给 **GPT-6 Sol high 子代理**执行，root 只审核；后续轮次沿用，除非用户改口。

## 当前实现

- `app/build.gradle.kts` 将版本设为 **1.1.1 / versionCode 4**。Release 仍用项目现有 Android Debug 证书签名，走 R8/资源收缩。
- 版块目录 `BoardScreen.kt`：顶栏为“版块目录”，正文重复大标题及其 Lazy item 已移除；分类侧栏的首项索引从 1 改为 0，恢复位置与最后一项测试索引同步调整。窄屏列表、宽屏目录侧栏、嵌入阅读布局均保留。
- 主题列表 `ThreadListScreen.kt`：顶栏显示当前版块名（清理 HTML），固定在安全区域；正文重复大版块名已移除。其下的“全部主题/精华区”和排序/发帖工具行仍随手势连续收放，点顶栏标题可恢复工具行并返回列表顶部；ViewModel 的 `headerHiddenFraction` 只作用于工具区。
- 阅读页 `PostScreen.kt`：顶栏显示当前主题标题并单行省略，正文标题保留。窄屏版块、收藏、个人中心的底部浮岛及原生阅读页操作岛随用户上滑隐藏、下滑出现；短列表边界也可触发，宽屏侧轨保持原行为。
- 收藏/历史将搜索移入顶栏并保留清除筛选；版块页只留顶栏搜索；搜索页明确全站或版内范围，其他重复装饰文案已精简。相关修改分布在 `BottomNav.kt`、`ScrollDockState.kt`、`BoardScreen.kt`、`FavoritesScreen.kt`、`HistoryScreen.kt`、`CommunityScreen.kt`、`SearchScreen.kt`。
- 个人中心通过当前会话抓取公开昵称和头像；旧账号资料空白可刷新，读取失败保留已有展示并允许重试，账号 revision 变化后旧请求结果不得覆盖新账号。相关修改在 `UserPreferences.kt`、`NgaRepository.kt`、`RootViewModel.kt`、`ProfileScreen.kt`。新增/改动的测试与本地证据图也尚未提交。

## 构建与验证

- 最新签名包：[nga-qing-1.1.1.apk](app/build/outputs/apk/release/nga-qing-1.1.1.apk)；Gradle 标准输出：[nga-qing-release.apk](app/build/outputs/apk/release/nga-qing-release.apk)。包名 `com.qingyi5427.ngaqing`，两文件 SHA-256 均为 `43DDCB20C79E4CB92304667DF9627ED13391682FECA30CF0E182149B22686F1D`。`:app:assembleRelease` 成功，`apksigner verify` 确认 APK v2 签名。
- **本次版块标题变更之后**：定向 Robolectric 测试 **7/7** 通过（`ContentScreensRenderTest` 的 5 个 Board 用例、`RootPaneRenderTest` 的 2 个宽屏嵌入用例）；验证了分类跳转、收藏区域、滚动恢复与宽屏侧轨。此前固定主题顶栏的定向测试为 **13/13**，阅读页标题相关定向测试为 **8/8**。
- **较早的 1.1.1 功能基线**：完整 `:app:testDebugUnitTest` **156 项全部通过**，完整 `:app:lintDebug` **0 error / 34 warning**。这些全量检查发生在后续标题调整之前；本次没有重跑全量或完整 Lint。`git diff --check` 无空白错误。
- 生产页面 Robolectric/NATIVE 截图：[版块手机](docs/ui-evidence/board-title-1.1.1-phone.png)、[版块宽屏](docs/ui-evidence/board-title-1.1.1-wide.png)、[主题工具栏隐藏但标题固定](docs/ui-evidence/thread-fixed-title-hidden-1.1.1.png)。截图由 `decorView.drawToBitmap()` 生成。
- Windows 环境使用 JDK 21 `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot` 与 Android SDK `C:\Users\qingy\AppData\Local\Android\Sdk`；Gradle 使用 `--no-parallel --max-workers=1`。

本版未安装到手机，也未重新做真机手势或远端头像网络验收；没有执行 ADB、Git 提交或推送。当前无待实现任务。后续若用户提出体验修改或要求提交/推送，再按新指示继续，并先检查工作树。较长的设计与历史验证记录见 [UI_REDESIGN_REVIEW.md](UI_REDESIGN_REVIEW.md)。
