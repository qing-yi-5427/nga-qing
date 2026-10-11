# 1.2.5 云收藏增删与末条确认 QA（2026-10-11）

本次在独立 `NgaClient-main-delivery` 工作树修复云收藏写入后必须手动“重新读取”才能确认的问题。普通新增、移除各发送一次 NGA APP 签名请求，并分别用签名收藏列表和完整网页目录快照确认目标成员关系，确认后才更新 Room 与当前弹窗。请求固定 `ngabbs.com`、禁止重定向与自动重试，并在实际网络发送前核对账号会话。协议根据公开的 [lnga_harmony 收藏设计记录](https://github.com/apap6628114/lnga_harmony/blob/512cb8a30466bd5cf3733b362d85b45d469e22b3/docs/FAVORITE_DESIGN.md) 独立实现。

真机发现服务端对目录最后一篇主题的普通 `del` 返回“操作成功”但成员仍在；同一默认目录 2→1 普通删除成功，新建非默认目录 1→0 也不生效。因此最后一篇改为明确二次确认：弹窗显示文件夹名、说明文件夹本身会消失；确认前重新读取完整目录及签名主题集合，只有该目录仍唯一包含目标帖、名称和账号未变化时，才发送一次 `del_folder`。删除后须完整回读确认目标目录消失、其他原有目录仍在，才更新本地。取消确认零写；操作意图和显示名在弹窗打开时固定，不会在后台状态变化后升级为删夹。

定向 `ServerFavoriteRepositoryTest` 20/20、`ContentScreensRenderTest` 20/20，均零失败；`lintDebug` 0 error/23 warning；`assembleRelease` 成功。构建日志 `..\V8_QA_local\live-2026-10-11\final-125-build-v2.log`。单元测试覆盖账号切换、分页读回、未确认不更新、原有其他目录缺失不清缓存、本地副本保留、名称变化零写、普通确认不能升级为删夹；收藏页渲染测试覆盖大字多目录布局、取消零写、显示名传递。帖子页的确认取消通过下面的 Debug 实机检查；正式版 Post 入口做普通增删，收藏页入口做末条确认删除。

设备 `058A8840499C3540` 用同签名 `adb install -r` 保数据覆盖至 1.2.5/code10。原始状态：默认目录 `4719076` 仅有原帖 `47697364`。仅为验收创建一个非默认临时夹 `4719104`，加入公开控制帖 `47701477`；Debug 版点末条“移除”出现明确删夹确认，点“保留”后重新打开仍是 1 条。正式版原帖未动：Post 入口把控制帖加入默认目录后界面立即显示默认 2 条、临时 1 条；普通移除后立即显示默认 1 条、临时 1 条，无手动重新读取。收藏页入口对临时夹唯一控制帖确认“删除收藏夹”后，收藏列表立即从 4 项变 3 项，管理弹窗只剩原默认目录。重新进入原帖选择器，默认目录仍为 1 条且原帖“已收藏”；临时夹不再出现。所有这些操作均只涉及上述两个帖、两个目录，原目录从未删除或重建。

截图在 `..\V8_QA_local\live-2026-10-11\`：`acceptance-debug-confirm.png`、`acceptance-debug-reopen.png`、`release-before-add.png`、`release-after-add.png`、`release-ordinary-confirm.png`、`release-after-ordinary-remove.png`、`release-favorites-delete-confirm.png`、`release-favorites-after-delete.png`、`release-original-restored.png`。临时 Debug 入口、目标 ID 硬编码和探针已从正式源码移除。设备最终停留原帖，临时常亮原值 `screen_off_timeout=60000`、`stay_on_while_plugged_in=0` 已恢复。

交付 APK `..\V8_Release\nga-qing-1.2.5-v8.apk`，摘要见同目录 `.sha256` 与 `build-verification-1.2.5.md`。本轮未执行其他账号、目录的破坏性操作，也不把单元/渲染测试误写为所有入口的实机验证。
