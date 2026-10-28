# RUN-20261028-003：用户手册、提示文案与聊天气泡

2026-10-28；0.3.2／versionCode 5／Room schema 4。被测源码：[3a01cf3](https://github.com/zidodo1124/hust26_software_engineering_homework/commit/3a01cf380fe9792a07f30966c652bbd5722a5ef6)。环境为 Windows、JDK 17、Gradle 8.11.1、Android SDK 35、Robolectric 4.14.1。

**最终结果：131 个不同测试方法／149 次执行全部通过，失败、错误、跳过均为 0；Debug APK 构建成功，Lint 0 错误／16 条原有维护警告。** 本批没有连接手机或安装新包，没有核对本次提交的 CI 结果。

## 执行与审查

两次完整命令均为：

```powershell
scripts/gradle.ps1 :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon --console=plain --continue --max-workers=2
```

| 记录 | 实际结果 |
| --- | --- |
| attempt-01 | 3 分 20 秒构建成功；131 方法／149 次执行通过，Lint 0／16。独立审查随后建议每日汇总文字明确“今天截止”，手册按实际条件说明备用权限按钮 |
| attempt-02 | 两处产品文字和一处手册说明校正后，3 分 58 秒完整构建成功，退出 0；131 方法／149 次执行通过，Lint 0／16 |
| final-results | attempt-02 报告与截图的交付副本，绑定已提交源码 3a01cf3；不是第三次执行，不累计次数 |

新增三个气泡布局方法及一个台词方法：两种形象的横向布局与指向、320dp／150% 字体下纵排及完整长文本、图片槽为空时状态和署名可用，以及六类共 36 条台词的轮换。空图片槽检查不等同于真实损坏文件注入。既有事项、附件、迁移、设置、提醒与路由回归一并执行。

AI 助手核对五张真实生产组件渲染图：经典和 Q 版首页、320dp／150% 首页、两种形象的长台词气泡。首页使用合成事项，长台词为布局压力样本；图像不包含系统栏、完整 Activity 的悬浮按钮，不当作真机截图。宽度不足的工具行可横向滚动，长页面可纵向滚动。独立只读审查另核对手册与实际菜单、保存／取消、批量、归档、附件和权限条件的一致性。

## 安装包与素材

包名 `cn.edu.hust.ddlsaver`；版本 0.3.2／5；minSdk 26、targetSdk 35；无 INTERNET 权限。APK 大小 15,102,307 字节，SHA-256：`ADE517B79B21C83EE98524288F8151CBA536ECFC22624F5C449DA066C45069D3`。

签名证书 SHA-256：`978833BCAF0FC001248F5D8501BFA1F9D1273922474ED601F2C60A4548A652B9`，与此前本机交付一致。工作区与 APK 中经典 7 份、Q 版 4 份上游原文件均符合固定来源清单，未修改素材。签名和静态核对不代表本版设备覆盖升级已经完成。

Lint 保留 ExifInterface 1、InlinedApi 1、GradleDependency 10、ObsoleteSdkInt 1、StaticFieldLeak 1、UseKtx 2，共 16 条维护警告；编译仍提示既有 LocalClipboardManager API 弃用。未通过关闭规则或排除测试减少报告。

## 证据入口

- [最终摘要](final-results/summary.json)、[被测文件哈希](final-results/tested-files.json)、[APK 静态核验](apk-verification.json)。
- [首次日志](attempt-01.log)、[最终日志](attempt-02.log)及各目录内的 JUnit XML、Lint XML。
- [经典首页](final-results/home-classic.png)、[Q 版首页](final-results/home-chibi.png)、[放大字体首页](final-results/home-320dp-font150.png)。
- [经典长台词](final-results/bubble-long-text-classic.png)、[Q 版长台词](final-results/bubble-long-text-chibi.png)。
- [证据校验清单](manifest.json)保存其余证据的字节数与 SHA-256，自身不纳入递归校验。

两次测试前均存在未提交改动，attempt 目录的 baseCommit 是当时工作分支基线，具体被测字节另由 tested-files.json 标识；final-results 的 baseCommit 为实际源码提交。最终报告副本与 attempt-02 一致，摘要的目录名和提交标识除外。

荣耀 X50 的新版显示、覆盖升级及用户视觉审核尚待进行。锁屏、省电、重启等已有通知边界和 OBS-01 保留原状态，未因本次界面测试关闭；完整业务用例不按本机子项成功登记全部验收通过。操作步骤见[用户使用手册](../../user-guide.md)。
