# NGA Qing 登录方案源码核查（V8）

核查日期：2026-10-10。本报告只读检查 NGA Qing 当前源码、两个开源客户端的固定提交，以及 NGA 官方未登录页面；未使用账号、凭证或登录请求，未测试真实登录成功。这里的“扫码”区分 **本 APK 展示网页二维码、供另一设备扫描以登录本 APK** 和 **本 APK 扫电脑网页二维码、授权电脑登录**；后者的手机授权协议仍无可信实现证据，见 [专项调查](../../NGA_Qing_UI_V8_Agent_Kit/QR_LOGIN_FEASIBILITY.md)。

## 源码所能证明的选项

| 选项 | 固定源码证据 | 验证边界与 V8 适用性 |
|---|---|---|
| 内嵌官方网页登录 | `mlzzen/open-nga` [`7caf9a9` 的 `LoginWebFragment`](https://github.com/mlzzen/open-nga/blob/7caf9a910091569c1fe612b2a324a6dbe4ee67f4/nga_phone_base_3.0/src/main/java/sp/phone/ui/fragment/LoginWebFragment.java) 直接打开 `https://ngabbs.com/nuke.php?__lib=login&__act=account&login`，启用 JavaScript，在 `onPause` 取 WebView Cookie；该客户端 [`LoginActivity`](https://github.com/mlzzen/open-nga/blob/7caf9a910091569c1fe612b2a324a6dbe4ee67f4/nga_phone_base_3.0/src/main/java/gov/anzong/androidnga/activity/LoginActivity.java) 默认装载网页登录 Fragment。 | 证明旧 Android 客户端采用此入口和 Cookie 路径，不证明 2026 年实际登录成功，也不宜照搬其在任意网页 URL 上取 Cookie 的做法。NGA Qing 已有更严格的受信域捕获与账号隔离，可保留。 |
| 原生账号密码与图形验证码 | 同仓库 [`LoginModel.java`](https://github.com/mlzzen/open-nga/blob/7caf9a910091569c1fe612b2a324a6dbe4ee67f4/nga_phone_base_3.0/src/main/java/sp/phone/mvp/model/LoginModel.java#L29-L100) 请求旧域名 `bbs.ngacn.cc` 的验证码，表单含 `name/password/rid/captcha`，其中 `qrkey` 是空字符串；[`LoginPresenter`](https://github.com/mlzzen/open-nga/blob/7caf9a910091569c1fe612b2a324a6dbe4ee67f4/nga_phone_base_3.0/src/main/java/sp/phone/mvp/presenter/LoginPresenter.java) 做输入校验和错误时验证码更新。 | `login()` 的成功响应路径只 Toast 原文，未调用成功回调；旧验证码域名、字段和响应不构成当前有效协议。不能据此承诺密码、验证码或二次验证可用，也不能把空 `qrkey` 当扫码授权。V8 不建议重写此协议。 |
| 手工导入桌面 Cookie | `youcai922/open-NGA-client` [`582e0d1` 的 Cookie 页面](https://github.com/youcai922/open-NGA-client/blob/582e0d17aa1f2ea50b80474e93cdf64f2db27fe5/src/views/Cookie/index.vue#L166-L247) 提供手填 Cookie；同仓库 [`login.rs`](https://github.com/youcai922/open-NGA-client/blob/582e0d17aa1f2ea50b80474e93cdf64f2db27fe5/src-tauri/src/commands/login.rs#L288-L375) 可将桌面取得的 UID/令牌设置成客户端 Cookie。 | 证明桌面端存在手工导入，不证明浏览器会话能安全迁入 Android WebView。复制 CID 增加凭证泄露、错号、过期和恢复问题；源码还有记录令牌/Cookie 的日志语句，不能照搬。V8 不新增粘贴凭证入口。 |
| 官方二维码登录本 APK | 桌面客户端 [`二维码生成、显示与轮询`](https://github.com/youcai922/open-NGA-client/blob/582e0d17aa1f2ea50b80474e93cdf64f2db27fe5/src/views/Cookie/index.vue#L423-L510) 说明其**接收登录**的一端。NGA Qing 当前 WebView 页面也可展示官方提供的登录方式。 | 这只能支持“别的设备扫描本 APK 页面”的方向；是否在当前手机成功落 Cookie 要实机账号验收。 |
| 本 APK 反向扫码授权电脑 | 上述桌面代码只含挑战生成、轮询和设置电脑 Cookie；`open-nga` 密码表单的 `qrkey` 为空。 | 两仓库均未给出手机端授权请求、二次确认、错误码或重放规则。不能交付或声称可用；详见专项调查。 |

## 官方页面与本项目的最小可靠路径

2026-10-10 对 [NGA 官方登录入口](https://bbs.nga.cn/nuke.php?__lib=login&__act=login_ui) 作未登录 GET，返回 HTTP 200；页面包裹同站 `nuke/account_copy.html?login` iframe。该 [同站页面](https://bbs.nga.cn/nuke/account_copy.html?login) 的脚本包含账号密码、验证码、腾讯验证和二维码登录 UI 分支，也出现 `MATCH_PHONE` 补充手机验证错误分支。此观察说明直接入口比论坛首页更接近完整网页登录流程；不证明各分支能在 NGA Qing 当前 WebView 中完成，也不证明它们的线上协议稳定。

基线 NGA Qing 的 [`LoginScreen.kt`](../app/src/main/java/com/qingyi5427/ngaqing/ui/login/LoginScreen.kt) 启用 JavaScript/DOM 和缩放，但初始加载论坛首页，用户还要自己找到登录入口。它已提供刷新、加载失败重试和“完成登录”。基线 [`LoginViewModel.kt`](../app/src/main/java/com/qingyi5427/ngaqing/ui/login/LoginViewModel.kt) 捕获失败时只提示扫码，尽管帮助文案也写了账号登录。本次 V8 工作树已落实以下最小改进：

1. 初始页使用所选受信 NGA 主机的 `nuke.php?__lib=login&__act=login_ui`，保持现有 WebView 的 Cookie 环境、缩放和失败重试；不要使用第三方仓库的旧域名或硬编码非所选主机。官方页面可能改版，需在设备上看见账号/验证码/扫码入口，并验证失败后可重试。
2. 帮助及捕获失败文案统一说明“可在 NGA 网页用账号或扫码完成登录；若扫码后未自动完成，请刷新后检测”。“未检测到凭证”不能等同于“必须扫码”；不输出 Cookie、UID、CID 或完整跳转 URL。
3. 保留现有 [`LoginHelper.kt`](../app/src/main/java/com/qingyi5427/ngaqing/data/remote/LoginHelper.kt) 的确切 HTTPS 受信域筛选、唯一 UID/CID 解析、`session_revision`/事务串行化、多账号切换清理和 WebView Cookie 同步。额外注意验证码/风控页面所需 Cookie 在同一账号下不要被清理；当前 helper 已有相应保留逻辑。

入口与文案的代码及专门测试见 [`LoginEntryTest.kt`](../app/src/test/java/com/qingyi5427/ngaqing/ui/login/LoginEntryTest.kt)；**尚非线上登录验收**。需以合法测试账号在候选 APK 上分别验证密码+验证码、二维码落 Cookie、可能的手机二次验证、失败重试、退出与新增/切换账号；不可在 QA 中动用用户实际凭证。网络离线和官方风控可能使本次无法完成，必须如实报告。V8 本身无需引入原生密码表单、浏览器 Cookie 粘贴或反向扫码授权功能。

## 错误恢复与凭证保护边界

旧 `open-nga` WebView 可返回、显示加载进度，但它的 Cookie 捕获在 `onPause`，不提供登录成功后即时闭环证据；其账号密码模型只展示原始响应，验证码虽能刷新却无可靠成功回调。桌面客户端 Cookie 手填/二维码轮询是不同架构，客户端自己的轮询次数也不是官方服务端过期时间。当前 NGA Qing 的即时 `onPageFinished` + 手动检测、受信域 Cookie 捕获与可重试 UI 更接近可恢复路径，应在此基础上改善入口和准确提示。

本项目 `UserPreferences` 保存 UID/CID 于 Preferences DataStore，源码未做字段级加密；`allowBackup=false` 不等于加密。研究或 UI 验收不应采集、打印、提交任何真实 CID、完整 Cookie、登录跳转 URL、账号页面截图或网络请求正文。对账号切换、验证码/二次验证及 Cookie 生命周期的结论只在实际设备和授权测试账号端到端验证后成立。
