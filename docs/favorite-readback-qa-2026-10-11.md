# 1.2.4 云收藏操作回读 QA（2026-10-11）

用户反馈普通收藏和移除需要手动点“重新读取”。原实现只接受固定成功文案，并在写入后推算 UI 与本地成员关系；服务端若返回另一种文案，应用会报错，而推算也可能掩盖服务端未改变的状态。

现在普通添加、移除都只发送一次写请求，随后自动完整读取云目录和目标帖成员关系。只有目标关系确实变化才更新弹窗、本地缓存与收藏页；明确的服务端 `error`、回读失败或关系未变化会保留原状态并显示原因。“重新读取”仍只是只读操作，不会重发写请求。两个入口都使用回读结果。删除请求的 `__output=3`、表单 `page=1` 与 NGA 官方网页一致。新建目录的一次写后回读路径保留。

定向 `FavoriteMutationTest`、`ServerFavoriteRepositoryTest`、`ServerFavoriteProtocolTest` 共 3 suites、16 tests，0 failure/error/skip；`:app:lintRelease` 为 0 error、23 warning，`:app:assembleRelease` 成功。构建日志在 `G:\AI\coding\10-nga\V8_QA_local\live-2026-10-11\favorite-readback-final-release.log`。这是本轮定向结果；前一版全量测试不算作 1.2.4 全量结果。

实机设备 `058A8840499C3540` 上仅操作当前帖 `tid=47697364` 与本轮创建的目录 `4719076`。初始云成员为 true。应用内按官方删除协议提交后，响应提示“操作成功”，但自动回读与两分钟后带独立请求的只读回读仍返回该帖，应用正确显示未确认，未把本地关系改成已删除。登录态 NGA 官方网页对同一目录勾选该帖并删除一次，网页刷新后该帖也仍存在；网页当前 UID 与 Cookie UID 一致（只检查相等，不记录值）。因此**普通移除的真实云端效果未通过**，不能宣称已解决服务端这次未变化的情况。为保留初始成员，没有再执行普通添加的实机写入；普通添加仅有定向自动化覆盖。没有删除其他收藏或目录。

正式 1.2.4/code9 已用 `adb install -r` 覆盖临时 Debug 包，未卸载或清数据。重新进入目标帖，B 站卡和后续正文仍显示；只读打开收藏选择器，仍为“未命名的收藏夹#076 / 1 个主题 · 默认 · 已收藏”，截图 `G:\AI\coding\10-nga\V8_QA_local\live-2026-10-11\final-1.2.4-reader.png` 与成员层级 `final-1.2.4-membership.xml`。最终包 `G:\AI\coding\10-nga\V8_Release\nga-qing-1.2.4-v8.apk`，SHA-256 `CC6F5F73999D52CF50124690367991F768FE64535170767236DF494CC61FD41B`；签名、版本、对齐及清单见同目录 `build-verification-1.2.4.md`。独立只读审包通过。临时常亮设置最终已读回原值 `screen_off_timeout=60000`、`stay_on_while_plugged_in=0`，离开时停在当前帖子页。
