# V8 1.2.2/code7 独立 QA

2026-10-10。本记录只计本轮源码与 1.2.2 候选包的检查，不把 1.2.0/1.2.1 的设备烟测续算为新版通过。用户提供的三张实体截图分别显示回复正文额外左缩进、常逛卡内容偏上、搜索框蓝色四边露出；后续又报告从常逛进入版块帖子后无法点“发现”回首页，并要求启动图标匹配 V8 风格。

## 本轮定向验收

| 范围 | 可区分的检查 | 结果与边界 |
| --- | --- | --- |
| Reader 左边缘 | `ReaderHtmlVisualRenderTest` 在 425×623、698×986、986×698dp 对比主楼正文与回复正文、引用块和操作行左边缘，允许最大 4dp 差；另检查引用内容仍可见 | 三种尺寸通过，Native Compose 软件图 `app/build/outputs/ui-evidence/v8-122-reader-reply-{cover,innerP,wide}.png`。原实体截图中的 38dp 额外缩进已从生产布局删除；实体新版 GPU 未复测 |
| 常逛卡居中 | `DiscoverRenderTest` 对无说明和有说明两种卡，取实际图标上缘、标题/末行下缘，要求可见内容组落在卡内且组中心距卡中心 ≤7dp；425×623、425×623/font1.5、698×986、860×608dp | 四种软件尺寸通过，图 `v8-122-home-boards-{cover,cover-font1p5,innerP,wide}.png` 在同一输出目录。1.5 倍字号会切换为横向卡，仍检查实际图文边界 |
| 搜索框 | `SearchFieldPolishRenderTest` 在 425×623 和 320×480/font1.5 覆盖空且未聚焦、空且聚焦、有字且聚焦；检查外壳/输入框处于安全宽度与触控高度，并在输入“折叠屏”后执行 IME Search，验证 `SearchViewModel.search` 恰好调用一次 | 两种尺寸通过，Native 软件图 `v8-122-search-{cover,narrow-font1p5}-{empty,focused-empty,focused-text}.png`；目视单层轮廓无原图的四边蓝线。真实系统 IME/GPU 尚未复测 |
| 头像数据和显隐 | `AvatarUrlTest` 覆盖 NGA 多头像字段、相对路径、旧图片域迁移和失败回退。`ReaderHtmlVisualRenderTest` 用公开 PNG 本地 fixture 走生产 PostScreen→Coil，检查照片中心像素出现且首字占位消失；空地址、无效 file URL 保留占位。Profile 头像测试使用同一 fixture 并检查可见像素 | 本地图片加载与回退通过，图 `v8-122-avatar-{loaded-public-fixture,blank-fallback,error-fallback}.png` 和 `v8-122-profile-local-avatar-loaded.png`。公开 NGA 页面直连返回 403，本轮没有拿到真实 NGA 头像样本；不宣称线上头像已验证 |
| 根 Tab 导航 | `RootTabNavigationRenderTest` 使用真实 `NavHostController`、实际底栏/宽屏 rail、真实 `Routes.THREADS` 模式。原版在发现→常逛直进 Threads→点发现时 2 项红灯，修复后 4 项绿灯；另覆盖跨 Tab 已存子栈、重复点根 Tab、窄→宽切换及 Boards 根页 `rememberSaveable` 状态 | 点“发现”回 Discover 根，重复点击不增加根项；点“版块”回目录根且根页状态保留。没有把软件 NavHost 结果冒称实体折叠屏操作 |
| 启动图标 | 独立核脚本与 Vector 路径一致；108dp 图层的中央 72dp 可见区用于 mask 预览及五密度 PNG。蓝色前景最大半径 31.953dp、阴影 32.894dp，均在中心 33dp 安全圆内；气泡主体与 N 几何中心均为 `(54,54)`。API 33 的 monochrome 为 evenOdd 路径、N 镂空；五密度为 48/72/96/144/192px，圆图四角透明 | 预览 `design/icon-concepts/v8-blue-preview.png` 经目视检查，XML 的 `mipmap-anydpi-v26/v33` normal/round 及 Manifest 引用已核。正式 APK 的 foreground、monochrome、background 和各密度 normal/round 资源引用通过独立只读检查 |

## 完整本地检查

稳定源码上执行 `:app:testDebugUnitTest :app:lintDebug --no-parallel --max-workers=1` 成功：**50 suites、228 tests、0 failure/error/skipped；Lint 0 error、23 warning**。日志保留在工作树外 `G:\AI\coding\10-nga\V8_QA_local\unit-lint-1.2.2.log`，测试 XML 位于 `app/build/test-results/testDebugUnitTest/`。旧 Profile 测试原把启动图标 PNG 当橙色头像夹具；本轮启动图标改为 Vector 后，先以同一公开 PNG 替换该夹具，单项复测与完整检查均通过。下载地址虽无扩展名，图片实际编码为 PNG；将 fixture 文件扩展名校正后，Reader 与 Profile 的两个图片显示单项复测通过，日志为 `G:\AI\coding\10-nga\V8_QA_local\fixture-retarget-1.2.2.log`。最后一次图标仅调整矢量几何及派生 PNG；因此其后只复核资源几何、遮罩和 APK 引用，没有重跑业务全量。

## 设备与交付边界

本轮 `adb devices -l` 返回空列表，随后只读 window 查询因 ADB daemon 未能连接而停止。没有要求用户解锁、没有改系统设置，也没有安装、卸载、清空主包或执行 `connectedAndroidTest`/Macrobenchmark 安装链。1.2.2 实体 GPU、真实系统键盘、在线 NGA 头像加载和实体折叠开合均待以后有设备时核验；此前 1.2.1/code6 保数据安装成功不代表 1.2.2 已安装。

**后续用户试用（2026-10-10）：**用户在实体设备上试用 1.2.2，确认头像显示，表示试用通过并授权合并与推送。此反馈解决了当时「线上头像是否可显示」的待确认项；本节前段仍是开发侧独立 QA 当时的范围，不代表开发侧完成了设备包摘要、保数据安装或全部场景的复测。

正式交付包为 `G:\AI\coding\10-nga\V8_Release\nga-qing-1.2.2-v8.apk`，2,302,625 字节。独立核验 SHA-256 为 `42C393A710B5E61DFF352F19244FED0C1D5DDC1D7756CDC0CDD478EB94C4830D`，与旁附 `.sha256` 一致；`zipalign -c -v 4` 成功。`apksigner` 核实 v2 单签名，证书 SHA-256 为 `3774e86da5188b94341d667ad7e4a871ad8cda74f81088fb8445654d6068e0d6`，与 1.2.1 包一致。`aapt` 核实包名 `com.qingyi5427.ngaqing`、versionName `1.2.2`、versionCode `7`，Manifest 未含 `debuggable`、`testOnly` 或 instrumentation；包内没有测试类或公开头像 fixture。最终 APK 的 adaptive normal/round 图标引用及 foreground、monochrome、background 资源均存在，五密度 legacy normal/round PNG 也均存在。本轮未安装该包。
