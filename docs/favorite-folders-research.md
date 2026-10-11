# NGA 收藏文件夹修复依据（2026-10-11）

## 基线问题

1. NgaApi.addServerFavorite 原来把 folder 固定为 1，帖子页点书签时没有选择服务器目录。
2. 仓库原来先改本地 favorites，服务端返回错误时依然把书签显示为已收藏；手动“重试”也没有保存用户选过的目录 ID。
3. “同步 NGA 收藏”只读取 thread.php?favor=1。收藏页的 FavoriteEntity.folder 是本地自由文本标签，不能视作网站的目录 ID；旧客户端留下的高亮书签也可能只是本地记录。

## 协议来源

- [MNGA topic.rs，固定提交 4f75d762](https://github.com/BugenZhao/MNGA/blob/4f75d762582071a4e0fde7b96d520e6113382e9d/logic/service/src/topic.rs#L348-L374)：目录列表走 nuke.php?__lib=topic_favor_v2&__act=list_folder&page=1；data.0 的每个对象给出 id/name/length/default。default 以字段是否存在判定。
- [MNGA fetch.rs，同一固定提交](https://github.com/BugenZhao/MNGA/blob/4f75d762582071a4e0fde7b96d520e6113382e9d/logic/service/src/fetch.rs#L279-L323)：即使目录列表不带其他表单字段，公共请求层仍用 POST。首轮实机 GET 读目录得到 NGA 的“请求错误”；现已把该端点改为 POST，仍沿本客户端现有可信 NGA 域名 Cookie 通道，不猜测或新增 access_token。
- [MNGA 收藏读取](https://github.com/BugenZhao/MNGA/blob/4f75d762582071a4e0fde7b96d520e6113382e9d/logic/service/src/topic.rs#L312-L346) 与 [按目录增删](https://github.com/BugenZhao/MNGA/blob/4f75d762582071a4e0fde7b96d520e6113382e9d/logic/service/src/topic.rs#L729-L744)：thread.php?favor=<folder ID> 读取目录内容；topic_favor_v2 的 add / del 均携带 folder=<ID>，删除主题携带 tidarray。MNGA 的 folder_ids 是列表，说明一帖可在多个目录。
- [NGA API 文档，固定提交 51d4b76](https://github.com/AgMonk/nga-api-doc/blob/51d4b76acae1606372670c53b88a20c7ebebc4ee/README_v2.md#nukephp)：确认 list_folder、添加收藏和 favor=<ID>。文档的删除一节仍记录旧 topic_favor 接口；本次以 MNGA 当前 topic_favor_v2/del 的按目录语义实现，需以手机登录态实测确认。
- [NGA 官方网页 commonui 脚本](https://img4.nga.cn/common_res/js_commonui.js)：网页收藏选择器即使 `data[0]` 没有现有目录，也提供“新建收藏夹”选项，ID 为 `-1`。用户确定时 `topic_favor_v2/add` 提交 `folder=-1,tid,pid`，不传名称。官网 `__output=3` 返回形式与本客户端 JSON 解析不兼容；本客户端沿已实测成功的 `__output=8` 读取/写入，并在写入后回读新目录 ID 和当前帖成员关系才确认成功。`data[1]` 是目录续页标志，未完整读取时不得清旧缓存。

没有从 MNGA 或 NGA 网页复制大段代码。实机账号最初返回空 `data[0]`，按网页空态选项显式新建后，服务器回读目录 `4719076` 且包含当前帖；关闭重开再次回读仍在，细节见 `live-post-bookmarks-qa-2026-10-11.md`。

## 当前实现与边界

- 帖子书签点击先从 NGA 读取真实目录及各目录主题关系，高亮的历史本地收藏也走同一路径，避免误把“再点一次”当成取消云收藏。
- 已登录用户在所选云目录按 ID 增删；一次失败/不确定结果不更新本地成功状态，重新读取目录以核实。读目录仅在内存形成选择器状态，按“关闭”不会写本地收藏或云关系。
- Room v4→v5 增量迁移保留旧 favorites 主键、标签和条目，把它们标记为 source=local；新增按账号隔离的服务器目录与 (ownerUid,tid,folderId) 关系。新云缓存仅在最后一个云关系确认删除后移除，旧本地副本不因云操作消失。
- 收藏页原有自由文本分组明确标为“本地分组”；“同步 NGA 收藏”先完整读取所有云目录及分页，再一次性更新云关系。目录空响应、分页缺失或错误不清除已有缓存。
- 未登录时继续使用本地收藏。云目录可由用户在收藏选择器中显式新建并同时收藏当前帖；不提供单独重命名。空目录新建及服务器回读已在手机验收，其他账号和旧目录增删仍以单独实测为准。
