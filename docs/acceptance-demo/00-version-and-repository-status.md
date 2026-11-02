# 验收版本与远程仓库核对

> **历史快照，已非最新状态：** 本页保留 00:34:52 的核对结果。PR #14 后续已经合并，当前同步与验证状态请查[项目状态](../project/STATUS.md)，当前本地功能请查[源码导航](../source-navigation.md)。以下旧时间点的结论不改写为后来的事实。

核对时间：**2026-11-02 00:34:52（America/Los_Angeles，UTC−8）**，对应 08:34:52 UTC；已在首次核对后再次 fetch 和查询 GitHub API。这是有时间边界的快照，后续提交需要重新核对。

结论：队友有新的 PR 分支提交，**没有发现 PR #13 之后的新合并**。本次演示与教学统一使用已经装机验证的 **`demo-v0.3.5`**；新增重复事项界面属于尚未合并的草稿 PR #14，不在这份手机安装包中。

## 1. 远程、本地、手机分别处于什么状态

| 对象 | 实际标识 | 状态与含义 |
| --- | --- | --- |
| 远程 `main` | [`5df4320`](https://github.com/zidodo1124/hust26_software_engineering_homework/commit/5df4320a3f481488cb4137dbdac8be1fa9358a91) | 最近一次合并为 PR #13；源码仍声明 0.3.4／versionCode 7、Room schema 4 |
| 本地 `main` | `5df4320` | 与本次 fetch 后的 `origin/main` 一致；核对时无主线提交待拉取 |
| 本次核对开始时的工作分支 | `codex/demo-final-20261101`，[`e542b91`](https://github.com/zidodo1124/hust26_software_engineering_homework/commit/e542b910c748410176e86c8c43bab4c5e23a795e) | 当时与同名远程分支一致，工作区干净；比 main 多 2 个提交：周历修复／版本号，以及实机证据／交付文档。本次新增说明书属于后续文档更新 |
| 固定演示标签 | [`demo-v0.3.5`](https://github.com/zidodo1124/hust26_software_engineering_homework/tree/demo-v0.3.5) → `e542b91` | 教材所有源码行号和片段均按此标签核对；产品源码提交为 `f024ece`，后续文档提交不改变产品实现 |
| 已验证手机包 | 0.3.5／versionCode 8、Room schema 4 | 荣耀 X50 已安装，同签名覆盖、日历分项和最终数据审计已完成 |
| 队友开发分支 | `feature/fuze-recurrence-storage`，[`85be7b5`](https://github.com/zidodo1124/hust26_software_engineering_homework/commit/85be7b53a5dc342c138155197814cd17d6c4d754) | 对应草稿 PR #14，尚未合并；源码仍标 0.3.4／7，但数据库已改为 schema 5。本次只抓取与阅读差异，没有安装此分支 |

本地与远程已经执行 `git fetch origin --prune --tags` 核对。fetch 会更新远程跟踪引用，不会自动把队友分支合入当前工作代码。

手机包 SHA-256：`e689b896156514a9da7b8be57c67b3399e51e244328782f4f73ca2595fde2cd4`。同字节本机副本位于项目根目录下 `.local-tools/demo-delivery-0.3.5/DDL-Saver-0.3.5.apk`；该本机路径不随 Git 克隆分发。签名和完整实测记录见[0.3.5 证据](../test-evidence/RUN-20261101-CALENDAR-FINAL/README.md)。

## 2. 队友最近具体做了什么

| PR | 合并／更新状态 | 内容 | 当前手机是否包含 |
| --- | --- | --- | --- |
| [#11](https://github.com/zidodo1124/hust26_software_engineering_homework/pull/11) | 已合并 | 月历日期格显示符合筛选条件的事项数量 | 包含，已在 0.3.5 实测 |
| [#12](https://github.com/zidodo1124/hust26_software_engineering_homework/pull/12) | 已合并 | 周／日视图、周期导航与筛选 | 包含，已在 0.3.5 实测 |
| [#13](https://github.com/zidodo1124/hust26_software_engineering_homework/pull/13) | 11 月 1 日 23:12:27 合并 | 重复任务的纯周期计算基础 | 包含计算代码；没有用户创建重复事项的界面 |
| [#14](https://github.com/zidodo1124/hust26_software_engineering_homework/pull/14) | 11 月 1 日 23:15:12 创建；11 月 2 日 00:31:19 更新；**open、draft、merged_at 为空** | 重复系列存储与迁移、模板生成、创建界面、未来周期扩展、图片限制，以及编辑／删除本次及后续事项 | 不包含；不能按该 PR 的新按钮讲当前手机 |

表中时间均为 America/Los_Angeles。开放 PR 数为 **1**。GitHub 为开放 PR 返回的 `merge_commit_sha` 可能是试合并提交；本次依据 `merged_at`、PR 状态和远程 main 核实，不能因为该字段非空就说已经 merge。

相比上一轮抓取到的 `84ceed6`，PR #14 分支新增 8 个提交，其中最后一条是在本次编写说明书期间抓取到的：

| 提交 | 新增工作 |
| --- | --- |
| `9a8fb33` | 修正升级版本断言并保存数据库结构 |
| `a3619e8` | 删除周期事项时保留跳过记录 |
| `bbae249` | 增加跳过周期的去重与回滚测试 |
| `a601525` | 保存重复系列模板并生成事项 |
| `47878e3` | 更新重复系列模板的数据库结构 |
| `f042677` | 接入重复事项创建界面与未来周期扩展 |
| `e194405` | 限制编辑重复事项时添加图片 |
| `85be7b5` | 支持编辑／删除本次及后续重复事项 |

本次阅读差异确认：草稿涉及 `EventEditor`、`RoomEventRepository`、`SaverDatabase`、`ReminderEngine` 等，新增重复系列／周期记录两张表，提供 4→5 迁移；创建界面包含每天／每周／每月以及按次数／日期结束选项，并在恢复安排流程中扩展未来生成窗口。最新提交又加入“本次及后续”的编辑／删除路径及月重复锚点字段。**这些是分支代码中已经写出的实现，不等于已完成合并审阅或手机验收。**

还应注意：本次实机修复的“周历收起后保持整周范围”在 `f024ece` 中；核对时 main 和 PR #14 的 `HomeScreen` 仍使用依赖 `calendarOpen` 的旧判断。后续集成重复事项时需要一起保留这项修复，不能仅按版本号判断哪个 APK 适合演示。

## 3. 自动检查与实机结果如何区分

| 对象 | 本次查到的证据 | 能得出的结论 |
| --- | --- | --- |
| main `5df4320` | [CI #54](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/41404793495) completed／success | 该主线提交的工作流成功；这里没有重新下载报告或推断测试次数 |
| PR #14 上一提交 `e194405` | [CI #62](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/41412061265) completed／success | 上一提交工作流成功，不能替代最新提交 |
| PR #14 当前 `85be7b5` | [CI #63](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/41413686009) completed／failure | 最新提交工作流失败；具体失败步骤按下方核对记录说明，不能写“最新 PR 已全部通过” |
| 演示版 `f024ece`／0.3.5 | 本机完整构建 224 次执行通过；29 XML、185 个独立方法；Lint 0 错误／16 警告 | 本机结果已保存，不是新远程 CI 的结果 |
| 同一演示 APK 的荣耀 X50 检查 | 月／周／日、计数、折叠重开、跨年、关键词、完成、详情返回与来源空交集通过；18 项最终审计通过 | 支持本说明书标注的实测路径，不扩大为全部课程用例或全部系统后台条件通过 |

CI #63 已进一步核对失败任务与日志：`Build and check` 中的 `:app:compileDebugUnitTestKotlin` 失败，报错为 **`RoundTwoRepositoryTest.kt:172:77 Unresolved reference 'UrgencyRules'`**。该精确提交在[第 172 行](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/85be7b53a5dc342c138155197814cd17d6c4d754/app/src/test/java/cn/edu/hust/ddlsaver/data/RoundTwoRepositoryTest.kt#L172)使用了 `UrgencyRules.BEIJING_ZONE`，导入列表缺少对应类型，与编译诊断一致。这是**测试代码编译失败**，不能写成已经执行某个测试后断言失败。checked APK 上传被跳过；报告上传成功不代表测试通过。本次没有下载其报告来推断测试次数或 Lint 数量，也没有修改或重新运行队友分支。

PR #14 历史运行有失败、取消和成功记录。演示标签没有新的远程 CI 结论；已验证的本机与手机证据按各自范围使用。

## 4. 本次验收材料采用的边界

1. **演示 D01—D09 与教学代码统一指向 `demo-v0.3.5`。** 新说明书加入工作分支后，固定标签仍用于读取相同的产品源码；不要通过切换标签覆盖正在编辑的本地资料。
2. 核心展示为事项与信息组织、图片、关键词、月／周／日历、完成状态和角色反馈。来源／多标签正向组合、归档／恢复、批量与通知等按[演示脚本](02-demo-script.md)注明的条件提前预演，不能把本轮日历分项当成全功能复验。
3. 重复事项创建、系列管理、账号同步、应用内备份／恢复、在线聊天不属于当前手机可展示功能。其中重复事项创建正在 PR #14 中开发，其余不能因为材料提到规划就说已经实现。
4. OBS-01 图片来源访问许可失效机制、OBS-02 重启后不打开 App 的提醒自动恢复仍开放。演示前正常打开应用检查提醒；图片优先使用已保存副本或预演过的相册导入。
5. 验收用 5 条 QA 已删除，原事项、附件、设置与全部原 33 条提醒保持。这里提供的是新的 DEMO 样本准备说明，没有替用户创建演示数据。

继续阅读：[演示材料入口](README.md) · [教学与答辩入口](../acceptance-code-guide/README.md) · [项目状态](../project/STATUS.md)。
