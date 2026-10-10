# V8 1.2.0 Release APK 记录

2026-10-10，在隔离分支 `feat/ui-v8-adaptive`、生产源码提交 `fe9a9602c2d1c6617fc76cbee15b5ffadf97aeb1` 上构建。Release 文件在 `G:\AI\coding\10-nga\V8_Release\nga-qing-1.2.0-v8.apk`，旁附 `.sha256` 和 `build-verification.md`。未合并原工作树，未推送或发布 GitHub Release。

| 项目 | 核验结果 |
| --- | --- |
| APK SHA-256 | `221A7F288157340E5CD91E291D5D8B0B7789F76EA405745058470B61DE6B2C45`；复制前后及真机拉回 `base.apk` 一致 |
| 包/版本 | `com.qingyi5427.ngaqing`、`1.2.0`、versionCode `5`；旧数据保留的覆盖安装成功 |
| 签名 | APK v2 验证通过；证书 SHA-256 `3774e86da5188b94341d667ad7e4a871ad8cda74f81088fb8445654d6068e0d6`，与已装 Debug 同证书 |
| 构建 | `:app:assembleRelease --no-parallel --max-workers=1` 成功；R8、资源压缩、随包 baseline profile 完成；任务图没有 connected/benchmark/安装链 |
| 包内边界 | Release 清单无 debuggable、测试 instrumentation 或 CAMERA 权限；DEX 无已知测试 Runner/fixture；详细核查见交付目录的报告 |

实体 Xiaomi 直板机上，QA 以 `install -r` 保留数据升级，正常冷启动约 103ms 且前台可见。独立的纯 Java QA helper 以自己的 `com.qingyi5427.ngaqing.smokehost` 为 instrumentation target，通过系统 UiAutomation 操作另装的 Release：四 Tab、搜索、历史通过；公开帖跳 #5 后立即返回，首页续读卡为第 5 楼，再入首个可见楼层为 #5，两次通过。一次复跑在测试 helper 未重置首页时找不到续读入口，尚未进入业务断言；加入保留数据的 force-stop 冷启动前置后通过。独立 helper 及原始结果在工作树外 `G:\AI\coding\10-nga\V8_QA_local\release-smoke\`，它不属于分发 APK。

本轮完整 Debug 源码检查为 48 suite、205 test、0 failed/error/skipped，Lint 0 error、35 warning。真实折叠机、实体 1.3/1.5 系统字号受设备/权限限制未验证；六预设证据仅为本地 Compose 模拟，见 `docs/ui-v8-window-presets.md`。账号切换、真实发送和私信不在只读烟测范围。Debug 真机矩阵及单次未定因的跳楼前置空楼层瞬态见 `docs/ui-v8-qa/validation.md`。
