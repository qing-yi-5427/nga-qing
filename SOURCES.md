# 来源台账

更新日期：2026-08-20

本文记录 `nga-qing` 的外部技术参考、数据来源和资源边界。

## nga-just-works

- 仓库：<https://github.com/tophtab/nga-just-works>
- 最近一次审计基线：`6d3b49d0aeb8db7520ebf31a7bc8b26d5557bfd8`
- 许可证：GNU GPL version 2
- 使用方式：用于理解公开的 NGA 请求协议、版块图标 URL 结构和历史图片域名兼容行为。
- 本项目没有把该仓库作为 Gradle 模块或二进制依赖，也没有把其图片素材打包进 APK。
- `nga-qing` 中对应行为以 Kotlin 集中实现，并由本地契约测试约束。由于这些实现曾受 GPL-2.0 项目启发，本项目整体采用 GPL-2.0，以保持许可边界清晰。

## MNGA

- 仓库：<https://github.com/BugenZhao/MNGA>
- 使用方式：仅参考公开截图和运行界面中的信息层级、阅读续接与操作组织。
- 未复制、修改或分发 MNGA 的源代码、图片、图标、字体或其他文件。
- MNGA 当前声明没有开源许可证，因此不得把其代码或资源纳入本项目。

## NGA 服务与内容

- 论坛入口、帖子、版块、用户资料、头像、附件、版块图标和表情由 NGA 服务提供。
- 版块图标和表情图片在运行时从 NGA 的图片域名加载，不作为本项目内置资源分发。
- `Smiles.kt` 的表情名称到文件名映射于 2026-08-16 从 NGA 公共脚本 `https://img4.nga.cn/common_res/js_bbscode_core.js` 的 `ubbcode.smiles` 数据整理生成。
- NGA 名称、内容、用户数据、AC 娘表情及其他站点资源的相关权利归其各自权利人所有。

## 项目自有资源

- Android 启动图标源图由项目所有者提供，派生尺寸由 `tools/generate_launcher_icons.py` 生成。
- Compose 页面、导航、数据库、网络封装、解析器和测试均在本项目中维护。
