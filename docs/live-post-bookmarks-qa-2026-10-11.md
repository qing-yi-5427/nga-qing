# 1.2.3 阅读页与云收藏 QA（2026-10-11）

本轮修复真实帖子 `tid=47697364` 首楼 B 站 iframe 原样显示，以及收藏书签未按 NGA 服务端文件夹操作的问题。工作树是 `G:\AI\coding\10-nga\NgaClient-main-delivery`；原 `NgaClient` 未改。设备序列号 `058A8840499C3540`。

## 自动化与发布构建

- 修订后的全量 `:app:testDebugUnitTest :app:lintDebug :app:assembleRelease` 成功，日志在 `G:\AI\coding\10-nga\V8_QA_local\live-2026-10-11\full-unit-lint-release-v2.log`。这是修复编码引号及目录 POST 后的全量检查；其后增加官网空目录新建路径，运行了相关定向测试和 Release 构建，记录见同目录 `folder-create-targeted-release.log`、`folder-create-final-candidate.log`、`folder-create-confirmed-id-release.log`、`final-server-favorite-targeted.log`。Lint XML 为 0 error、23 warning。
- 首轮全量 JUnit XML 当时为 55 suites、246 tests、0 failure；该数对应更早首候选。最终新增用例的定向 JUnit XML 为 3 suites、13 tests、0 failure/skip；不把首候选数量充作最终包全量结果。
- Room v4→v5 升级测试检查旧六表及收藏原值保留；云目录和 `(账号, 帖子, 目录)` 成员关系独立保存。定向回归覆盖同帖双目录、失败不写本地假成功、空/不完整快照不清旧关系、分页标志，以及新目录写后回读确认。
- Compose 检查覆盖放大字体和 12 目录时的可滚动选择器、显式移除二次确认；这些是本地渲染测试，实机证据见下。

## 真机阅读和收藏

- 1.2.3/code8 以 `adb install -r` 覆盖同签名 1.2.2/code7，未卸载、未清数据。继续阅读重新进入 `tid=47697364` 后，首楼显示“B站视频 / 点按打开视频”卡，后续中文正文及原有裸链接均保留；截图 `v2-reader.png`、层级 `v2-reader.xml` 在上述本地 QA 目录。
- 点视频卡发出 `ACTION_VIEW`，系统首次要求“本次允许”启动 Edge；允许后系统 Activity Intent 的地址为 `https://www.bilibili.com/video/BV1mYpb6XEFd/?p=1`。Edge 进入 B 站，但 B 站页面显示“页面不见了”；这证明应用传递的目标地址，不能证明视频仍在线或可播放。
- 首候选目录请求为 GET，服务端返回 `{"0":"请求错误"}`。改为 POST 后服务端 HTTP 200、`data[0]` 为空。NGA 官方网页脚本对此空态提供“新建收藏夹”选项，确认时一次请求使用 `folder=-1,tid,pid`。应用加入同样的显式选项和确认；请求后必须重新读取目录和该目录帖子成员关系，只有新目录包含当前帖才记为成功。
- 实机初次选择器显示“当前账号还没有云收藏文件夹”，并保留“此帖已有本地收藏”说明。只点击一次“新建并收藏”；服务器回读新增目录 ID `4719076`，`folder_topics` 返回 1 个主题，选择器显示“未命名的收藏夹#076 / 1 个主题 · 默认 · 已收藏”。关闭后再次打开，新的 POST 目录读取和主题读取仍显示该目录与已收藏状态。截图 `create-ready.png`、`create-after.png`、`create-reopened.png` 及同名 XML 在本地 QA 目录。没有删除既有本地收藏或云收藏。
- 此次只验证当前账号当前帖的一次新建/加入和独立回读；没有把其他账号切换、旧云目录删除、长时间同步或 B 站视频实际播放标为实机通过。

## APK 与设备收尾

- 交付包 `G:\AI\coding\10-nga\V8_Release\nga-qing-1.2.3-v8.apk`，2,319,009 bytes，SHA-256 `1726B29C44979E407F80FF625B89316ABC6D6045BC0C49DF3D4604338DAA6F9A`；同目录 `.sha256` 与 `build-verification-1.2.3.md`。`aapt` 显示包名 `com.qingyi5427.ngaqing`、versionName `1.2.3`、code `8`、min SDK 24、target SDK 35、arm64-v8a。`apksigner` v2 与 `zipalign` 通过；证书 SHA-256 `3774e86da5188b94341d667ad7e4a871ad8cda74f81088fb8445654d6068e0d6` 与设备原 1.2.2 相同。独立只读审包也通过。
- 临时手机设置原值 `screen_off_timeout=60000`、`stay_on_while_plugged_in=0`；验收时为 `1800000`、`7`，结束后已读回 `60000`、`0`。离开时 NGA 保持在该帖子阅读页。
