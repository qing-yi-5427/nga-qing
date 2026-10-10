# V8 帖子头像调研与修复（2026-10-10）

## 现有链路与截图

用户截图中 #2–#4 的作者行均显示姓名首字占位。`NgaRepository.parsePosts` 从 `data.__U[authorid].avatar` 取值并存入 `Post.avatar`，`PostScreen.AuthorAvatar` 已用 Coil `AsyncImage` 加载，成功后显示圆形裁切图片；加载中、空值或失败时保留占位。因此首字截图本身无法区分“原始头像为空”“规范化 URL 错误”和“图片网络失败”。不按 UID 猜测头像地址，也不为每个楼层另发账户查询。

## 公开源码与协议证据

- [MNGA `logic/service/src/user.rs`，提交 `4f75d762`](https://github.com/BugenZhao/MNGA/blob/4f75d762582071a4e0fde7b96d520e6113382e9d/logic/service/src/user.rs)：从用户对象的 `avatar` 字段读取 `avatar_url`；匿名用户有单独的上下文身份，远程用户资料查询属于独立入口。帖子列表头像应优先使用已返回的用户字典。
- [MNGA `app/Shared/Views/UserView.swift`，同提交](https://github.com/BugenZhao/MNGA/blob/4f75d762582071a4e0fde7b96d520e6113382e9d/app/Shared/Views/UserView.swift)：使用该 URL 加载头像，空值/加载失败使用占位；匿名占位与普通用户分开。
- [MNGA `app/Shared/Utilities/URLs.swift`，同提交](https://github.com/BugenZhao/MNGA/blob/4f75d762582071a4e0fde7b96d520e6113382e9d/app/Shared/Utilities/URLs.swift)：旧的 `img*.nga.178.com`、`img*.ngacn.cc`、`img*.ngabbs.com` 图片域迁到 `nga.cn`。`/ngabbs/` 资源使用 `img4.nga.cn`，其余图片使用 `img.nga.cn`。
- [NGA API 补充文档 `README_v2.md`，提交 `51d4b76a`](https://github.com/AgMonk/nga-api-doc/blob/51d4b76acae1606372670c53b88a20c7ebebc4ee/README_v2.md#readphp)：`__U.<uid>.avatar` 在多头像时用 `|.a` 分隔，前缀相同；匿名回复的 `authorid` 为负数，同页 `__U` 有对应匿名名称。

同一文档还记载头像 BUFF 可改变展示图，需由用户 `buffs` 的特定位和官方静态映射确定。本轮没有对应真实响应样本，不能将任意空 `avatar` 推定为这种情况或凭 UID 合成地址。

MNGA 仓库声明无许可；这里只依据字段和协议行为重新实现少量解析，没有复制其代码。

## 本次修复

`normalizeNgaAvatarUrl` 原先把 `|.a` 后的多头像内容也交给 Coil，形成无效 URL。现在仅保留第一个完整头像 URL，其他现有 URL 规范化继续执行。补齐旧 `img*.ngabbs.com` 图片域迁移，并将文字 `null` 视为无头像。对应单测覆盖多头像、旧域、普通单 URL、空值和既有转义/相对地址行为。

现有 `AuthorAvatar` 已具备加载与占位逻辑，因此不抽新组件、不改帖子页面。匿名/无头像仍安全地落到占位，不生成 UID 地址或额外用户请求。

## 网络验证边界

本机对公开 `read.php?tid=25968165&__output=8` 试过 `bbs.nga.cn`、`ngabbs.com`、`bbs.ngacn.cc`，均得到 HTTP 403；对 `nga.cn` 得到 HTTP 404。随后按应用实际 URL 参数、`User-Agent`、`X-User-Agent` 和 `Referer` 对 `bbs.nga.cn` 再试一次，仍为 HTTP 403。全部请求均未带 Cookie 或账号信息。网页读取工具也无法打开该 API。未取得真实 `__U.avatar` 响应与对应的可下载头像图片，因此不能声称用户截图中所有占位都由 `|.a` 导致，也无法完成真实头像像素验收。测试用的 `/avatars/123/456.jpg` 是不存在的占位路径，HTTP 与 HTTPS 均返回 404，不可据此判断真实头像的协议可用性。

仓库此前因当时观测到头像 HTTPS 返回 567，将 `img.nga.cn`/`img4.nga.cn` 的 `/avatars/` 链接转 HTTP。本轮没有真实图片样本可推翻该记录，故未改此协议规则；需要在能访问 NGA 的环境用真实公开头像 URL 分别核验 HTTP/HTTPS 和最终 Coil 图片显示。
