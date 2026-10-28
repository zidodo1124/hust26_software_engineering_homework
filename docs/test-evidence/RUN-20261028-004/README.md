# RUN-20261028-004：荣耀 X50 界面复验与 Q 版表情修复

2026-10-28。支盟渊连接手机并要求升级、实机检查，随后确认“安装好了”；AI 助手执行受控操作、比对与截图审阅。安装输出与用户确认分别记录，不据安装命令成功推断全程无人参与。

## 版本与环境

| 项目 | 实际记录 |
| --- | --- |
| 手机 | 荣耀 X50／ALI-AN00，Android 15／API 35，MagicOS 9.0.0，对应系统构建 ALI-AN00 9.0.0.230(CNC00E206R8P4) |
| 初始应用 | 0.3.0／versionCode 3／schema 3 |
| 首次覆盖 | 11:04:12 UTC 安装成功，11:04:34 UTC 核对 0.3.2／5；源码 3a01cf3，APK SHA-256 为 ADE517B79B21C83EE98524288F8151CBA536ECFC22624F5C449DA066C45069D3 |
| 显示 | 1200×2652，密度 520；用户原有手写字体，font_scale 初始为 1.0 |
| 权限 | 通知已允许、精确提醒 app-op 为 allow；没有重新更改授权 |
| 临时样本 | 唯一 QA-UI-CHAT-1028，截止北京时间 22:13，速通时间 240 分钟；录入后尚余约 178 分钟，仅进入危险提醒范围 |

首次覆盖采用同签名 `adb install -r`，没有卸载或清空产品。schema 3→4 后，全部六张业务表的行数与加盐摘要一致：3 条事项、1 个来源、1 个标签、1 条关联、1 个自定义字段、4 个附件。四个附件均实际读取字节并比对；旧设置一致，仅新增 CLASSIC。14 条提醒计划、其中 7 条 SENT 及控制表摘要均保持。原始数据库和本地盐不入库。

## 0.3.2 实机检查与发现

- Q 版预览、继续编辑、放弃修改后保持经典；保存 Q 版并重新打开仍保留，经典形象可切回，两种署名入口均可打开。
- 检查空表单标题和截止时间的具体错误、QA 危险状态、空描述、取消删除及相册原图说明。空附件详情按现有设计不显示附件区，未将其记作空图片提示已显示。
- font_scale 临时设为 1.5，检查角色和气泡上下排列、设置底部保存、隐藏输入法后的编辑底部与未保存退出确认，随后恢复为 1.0。首张局部截图中悬浮按钮遮住台词末行；正常滚动后整张卡片和完整台词可读，以 16b 的截图及同次坐标核对，未将取景问题登记为永久遮挡缺陷。
- **BUG-UI-001：Q 版选中图格内部带有相邻画面残片。** 认真／逾期帧的左侧碎片首先在真实手机截图中发现；进一步检查发现危险帧和快乐帧也有边缘残片。原取图坐标符合图集网格，但整个原格逐像素一致的旧测试无法判定内容是否干净，不能用自动测试通过覆盖实际缺陷。

普通状态不通过修改原用户逾期事项制造；角色依然按全局有效事项判定。截图只保留 QA 筛选范围、专用 QA 详情或设置与署名页面。用户表示可能造成中断并要求继续后，执行器先核对连接和前台，再继续已有样本，未重复新建。

## 0.3.3 修复与开发机验证

源码 [93118d6](https://github.com/zidodo1124/hust26_software_engineering_homework/commit/93118d6d7282fbf92d22e843d207233a84414947)，版本 0.3.3／6，schema 4。对已审阅的快乐、危险和认真帧固定小区域作运行时透明处理；普通帧、192×208 画布、人物位置及排除区域外的每个像素保持，原图与许可文件不修改。坐标记录在随包 `sources.json`，界面署名同步说明处理方式。

完整执行 `scripts/gradle.ps1 :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon --console=plain --continue --max-workers=2`，3 分 57 秒成功、退出 0。**132 个不同方法／151 次执行全部通过，失败、错误、跳过均为 0；Lint 0 错误／16 条原有维护警告。** 新增真实残片点与相邻头发的双 API 回归，并保留全部未排除像素的独立预期比较。没有删测试或关闭 Lint 规则。

APK 15,102,627 字节，SHA-256 为 `7B8F8D6872FCC1526DD5E64E9AC47A8F989BE71CD4CAC67230FA989409E3ECC8`。包名 `cn.edu.hust.ddlsaver`，minSdk 26／targetSdk 35，无 INTERNET 权限。签名证书 SHA-256 为 `978833BCAF0FC001248F5D8501BFA1F9D1273922474ED601F2C60A4548A652B9`，与前版本本机包一致；11 份上游原文件在工作区和 APK 内均符合固定清单。

[开发机摘要](build-checks/summary.json)、[被测文件哈希](build-checks/tested-files.json)、[构建日志](build-attempt-01.log)、[APK 核验](apk-verification.json)及同目录 JUnit／Lint XML 为原始检查记录。摘要的 baseCommit 为执行时基线，sourceCommit 为冻结测试源码后的实际提交；软件渲染 PNG 与实机截图分目录记录，不累计手机操作为自动测试次数。没有核对本批 CI 结果。

## 修复版手机复验与收尾

安装前手机自动锁屏，前台检查停止输入，未绕过锁屏。支盟渊确认已解锁并打开应用后继续，并再次确认安装完成。11:55:14 UTC 覆盖安装成功，11:55:38 UTC 比较当前全部业务表（含唯一 QA）、附件、设置和提醒账本均一致，schema 4 与 Q 版选择保留；实际已装版本及哈希符合上节 0.3.3 产物。

主线程逐张审阅修复后的 Q 版普通预览、危险、快乐和认真画面：原残片消失，人物主体、状态和完整气泡可读。快乐图来自实际完成 QA 后 2.938 秒的第二帧；三次快速采集均保留时间记录，交付选择其中第二张。完成后，全局原有逾期事项自然触发认真表情，未修改原事项制造状态。经典回切保持原画面，Q 版署名显示运行时透明处理说明。font_scale 1.5 下再次滚动检查整张角色卡及署名位于悬浮按钮上方，随后恢复 1.0。**BUG-UI-001 在该固定图集和本机复验范围内关闭。**

本次还观察了纯形象切换前后的一个 QA 危险通知：系统记录数仍为 1，tag、创建及更新时间不变；后续数据库身份核对确认两次观察均属本轮 QA。形象单独保存阶段，既有业务数据与全部提醒账本也保持一致。这些结果限实际观察窗口，不表示所有系统条件下严格无重复。

12:03:23 UTC（America/Los_Angeles 05:03:23）完成收尾：通过详情确认删除唯一 QA，最终本轮事项 0、两条 QA 计划均 inactive 且无待取消；原六张业务表、全部四附件、既有提醒及 SENT 摘要与升级基线一致。业务设置及 CLASSIC、font_scale 1.0 恢复；通知权限、精确提醒、分辨率和密度保持。手机回正常待办首页、清除筛选，本机／手机临时 UI XML 和原始数据库临时目录清零。此后停止 ADB，已告知用户可拔线。

本批没有重跑完整通知时序、主动锁屏送达、Doze、省电、重启或完整权限组合；安装前遇到锁屏只验证执行器停止，不等于锁屏提醒测试。原 OBS-01 继续待定位，完整 TC-01／08／29 保留未覆盖步骤，用户未对最终审美作整体验收。隐藏输入法后的编辑按钮可用，不扩大为所有输入法状态均通过。

## 证据索引与范围

- 环境与升级：[device-before.json](device-before.json)、[device-after.json](device-after.json)、[upgrade-comparison.json](upgrade-comparison.json)、[hotfix-package.json](hotfix-package.json)、[hotfix-comparison.json](hotfix-comparison.json)、[device-final.json](device-final.json)。
- 业务保留：[before-upgrade.json](before-upgrade.json)、[after-upgrade.json](after-upgrade.json)、[q-style-saved.json](q-style-saved.json)、[before-hotfix.json](before-hotfix.json)、[after-hotfix.json](after-hotfix.json)、[final-state.json](final-state.json)。全部为计数、设置与本地同盐摘要，不含原始用户内容。
- 测试样本及提醒：[qa-plan.json](qa-plan.json)、[notification-style-comparison.json](notification-style-comparison.json)、[notification-identity-crosscheck.json](notification-identity-crosscheck.json)、[final-qa-audit.json](final-qa-audit.json)。早期通知 JSON 保留“待身份核对”标志，其后续完成情况以独立 crosscheck 文件为准，不覆盖原观察。
- 大字号：[large-font-result.json](large-font-result.json)、[16b-q-large-font-full-card.json](16b-q-large-font-full-card.json)、[23-fixed-q-large-font-full-card.json](23-fixed-q-large-font-full-card.json)。早期结果只记语义完整及上下关系，无遮挡结论以整卡截图和同次 FAB 坐标为准。
- 完成与清理：[24-fixed-completion.json](24-fixed-completion.json)、[steps.json](steps.json)、[final-comparison.json](final-comparison.json)、[completion.json](completion.json)。步骤记录中的完整本地采集文件名不代表全部图片都已导出；交付图片按以下清单选择。
- 汇总及校验：[summary.json](summary.json)、[manifest.json](manifest.json)。清单保存其余证据的原始字节数与 SHA-256，自身不作递归校验。

### 已审阅的实机原图

| 图片 | 判定范围 |
| --- | --- |
| [01 升级设置](01-upgraded-settings.png)、[02 Q 版预览](02-q-preview.png)、[03 放弃确认](03-settings-discard-confirm.png)、[04 放弃后经典](04-classic-after-discard.png) | 0.3.2 设置与放弃流程；动作结果另由步骤和数据库核对 |
| [05 修复前认真帧](05-q-home-saved.png)、[15 修复前危险帧](15-q-danger-bubble.png) | BUG-UI-001 原始缺陷证据，不登记画面通过 |
| [10 必填提示](10-required-fields.png)、[11 QA 详情](11-qa-danger-detail.png)、[13 删除确认](13-delete-confirm-cancel.png)、[14 经典危险气泡](14-classic-danger-bubble.png) | 必填、空描述、删除文案和经典角色；删除图为点击取消之前 |
| [16 局部取景](16-q-large-font-bubble.png)、[16b 完整卡片](16b-q-large-font-full-card.png) | 首张末行被 FAB 遮住，正常滚动后可完整阅读；两者均为修复前包 |
| [17 设置底部](17-large-font-settings-bottom.png)、[18 编辑底部](18-large-font-editor-bottom.png)、[19 放弃编辑](19-large-font-discard.png) | 1.5 字号可滚动操作；编辑图为 IME 隐藏后，实际放弃结果见步骤 |
| [20 修复后危险](20-fixed-q-danger.png)、[21 普通预览](21-fixed-q-preview.png)、[22 更新署名](22-fixed-q-credits.png) | 0.3.3 角色与说明；长署名内容可滚动，截图不是全文 |
| [23 修复后大字号整卡](23-fixed-q-large-font-full-card.png)、[24 快乐反馈](24-fixed-happy-2.png)、[25 认真表情](25-fixed-q-serious.png)、[26 经典回切](26-fixed-classic-restored.png) | 修复后人物与气泡；24 为真实完成反馈，25 未更改原逾期事项 |

图片均未经修改，来源为本机真实截图，仅含测试范围、应用设置和系统状态栏。源码、设备操作、独立审图、文档整理均由 AI 辅助完成；支盟渊实际参与连接、解锁及安装确认，未把自动代理工作算作其他三位成员的个人贡献。
