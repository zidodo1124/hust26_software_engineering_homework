# 演示后教师提问速查入口

> **版本提示：** 本套仍解释手机的 `demo-v0.3.5`，旧行号应配合固定标签链接阅读。本地已同步较新代码；当前功能与代码地址请查[本地源码导航](../source-navigation.md)，同步状态见[项目状态](../project/STATUS.md)。

**前 5 分钟只展示软件功能，不打开源码、不讲架构。** 按[功能演示脚本](../acceptance-demo/02-demo-script.md)完成 D01—D06 后结束展示；老师提问时，再从下表找到对应的一句话回答。只有老师继续追问实现或要求看代码，才打开详细问答和源码段，不默认接着主动进行代码讲解。

适用手机包 **0.3.5／versionCode 8、Room schema 4**，固定标签 `demo-v0.3.5`；产品源码 `f024ecea4b287c7a8768c2dc62af9be9228c9ae6`，交付提交 `e542b910c748410176e86c8c43bab4c5e23a795e`。更新：2026-11-02（America/Los_Angeles）。远程与安装包的区别见[已核对的版本快照](../acceptance-demo/00-version-and-repository-status.md)，本手册不继续追踪远程变动。

## 听到问题后，从这里查

按技术总览、演示出现顺序及追加功能排列。先用约 20 秒回答核心；详细问答中的例子、边界和代码只按追问展开。下表的“源码段”提供完整文件路径、固定行号与真实节选。

| 老师可能问什么 | 约 20 秒的回答 | 详细问答 | 源码段／关键函数 |
| --- | --- | --- | --- |
| 这是什么项目，用什么技术做的？ | 面向大学生的本机截止事项管理 App。Kotlin 编写，Compose 展示界面，Room／SQLite 保存结构化数据，图片另存应用私有目录。 | [Q1 项目](03-defense-questions.md#1-你们做的是什么解决什么问题)、[Q2 技术栈](03-defense-questions.md#2-技术栈是什么kotlincomposeroomsqlite-分别是什么) | [环境与依赖](01-architecture-and-stack.md#1-环境与依赖各自负责什么)；`app/build.gradle.kts` |
| 程序怎样分层，算 MVVM 吗？ | 采用状态驱动 UI、ViewModel 和 Repository 分层。操作向下交给存储，Room Flow 把结果送回界面；页面由单 Activity 中的状态切换。 | [Q3 架构](03-defense-questions.md#3-为什么用原生-android架构是不是-mvvm) | [架构图](01-architecture-and-stack.md#2-单-activity分层职责与页面状态)；`SaverApp`、`SaverViewModel` |
| D02 点保存后发生了什么？ | 编辑器组装草稿，领域层校验，Repository 用事务写事项、关联和提醒计划；Flow 发出新版本后页面再展示详情。 | [Q4 保存链](03-defense-questions.md#4-一次点击保存事项到底执行了哪些代码)、[Q7 事务](03-defense-questions.md#7-一次保存为什么需要事务数据库和图片文件也是同一个事务吗) | [D02](02-feature-source-map.md#d02-事项编辑分类字段与保存)；`save`、`reminderTransaction` |
| 草稿会立即保存吗？怎样防止覆盖旧数据？ | 输入先留在草稿，点击保存才提交。提交时比较事项 revision，发现草稿落后就拒绝覆盖并保留输入；未保存返回会检查修改。 | [Q5 草稿](03-defense-questions.md#5-输入文字时就写数据库了吗不保存直接返回会怎样)、[Q6 revision](03-defense-questions.md#6-单机应用为什么还要-expectedrevision) | [D02](02-feature-source-map.md#d02-事项编辑分类字段与保存)；`EventEditor`、`RoomEventRepository.save` |
| 来源、标签、字段如何存？有几张表？ | 来源单选，标签经中间表多对多，自定义字段归属具体事项。schema 4 共 9 张表，含六张业务表、设置和两张提醒表。 | [Q8 数据模型](03-defense-questions.md#8-数据库有几张表来源标签和自定义字段有什么区别) | [数据模型](01-architecture-and-stack.md#3-数据模型与事务为什么演示一次保存会涉及多张表)；`Entities.kt` |
| D03 图片为什么关掉页面后仍能看？会删原图吗？ | 图片导入为经过验证的私有副本，数据库存元数据。保存移除附件或删除事项后按数据库引用清理副本，不删除相册原图。 | [Q16 导入](03-defense-questions.md#16-图片为什么不能只存相册-uri为什么粘贴网址不能当图片)、[Q17 清理](03-defense-questions.md#17-删除附件会不会删掉相册原图如何避免删错文件) | [D03](02-feature-source-map.md#d03-图片附件与详情)；`importOne`、`cleanCandidates` |
| D04 搜索如何匹配？多个词怎么处理？ | 标题、描述和字段使用整段连续子串匹配，忽略大小写，不按空格分词。分类和日期条件取交集；搜索只改变查看范围。 | [Q9 搜索](03-defense-questions.md#9-为什么关键词有时搜不到多个词是-and-搜索吗)、[Q10 全局统计](03-defense-questions.md#10-为什么搜索结果只有一条今日-ddl却可能大于一) | [D04](02-feature-source-map.md#d04-关键词与分类组合筛选)；`EventQuery.select` |
| D05 选日为什么不清零其他日期？折叠为什么保持一周？ | 日期格统计与选日列表分开查询；周范围取决于视图模式，面板是否展开只控制显示。因此折叠不会把整周错误缩成一天。 | [Q11 日期计数](03-defense-questions.md#11-选中了今天为什么日历还要显示明天的数量)、[Q13 折叠修复](03-defense-questions.md#13-本轮修了什么日历-bug怎么证明修复有效) | [D05](02-feature-source-map.md#d05-月周日历计数与折叠)；`calendarEvents`、`inWeekView` |
| D06 完成后数据去哪了，和归档有什么区别？ | 完成写 `completedAt`，归档写独立的 `archivedAt`，都能恢复。待办排除完成项，所以样本日数量减少；已完成列表仍能找到 B。 | [Q14 状态区别](03-defense-questions.md#14-完成归档删除的区别是什么为什么恢复归档后可能仍在已完成列表)、[Q15 提醒历史](03-defense-questions.md#15-为什么删除或完成事项以后数据库里还有-reminder_plans-行) | [D06](02-feature-source-map.md#d06-完成恢复与归档)；`setCompleted`、`setArchived` |
| 首页角色是 AI 吗？能换形象吗？ | 角色是全局事项状态驱动的静态形象和本地预设台词，没有在线 AI。经典／Q 版是可保存偏好；完成后约 5 秒显示鼓励状态。 | [Q18 反馈](03-defense-questions.md#18-角色是不是-ai为什么过滤了任务表情没有按筛选变化)、[Q19 形象](03-defense-questions.md#19-设置里切到-q-版就已经保存了吗换形象会改提醒吗) | [D07](02-feature-source-map.md#d07-角色形象与本地反馈)；`selectCharacterFeedback`、`SettingsScreen` |
| 提醒如何保证有效？后台和重启可靠吗？ | 持久化账本判断当前事项是否仍应提醒，再通过系统 Alarm 和通知接口执行。已有去重与重试，但不能保证所有崩溃场景只发一次，重启自动恢复仍有限制。 | [Q25 去重](03-defense-questions.md#25-如何防止重复提醒能保证严格只发一次吗)、[Q26 重启](03-defense-questions.md#26-手机上重启后为什么不总能恢复你们代码不是有-boot_completed-吗) | [D08](02-feature-source-map.md#d08-通知提醒可选)；`reconcile`、`deliver` |
| 怎样测试？演示的版本和远程最新代码一致吗？ | 本机 185 个不同测试方法共执行 224 次通过；真机补充日历和收尾审计。演示包、讲解源码与证据固定到 0.3.5，开发中 PR 不当作手机已有功能。 | [Q27 测试](03-defense-questions.md#27-你们如何验证224-是什么为什么还要真机)、[Q28 版本](03-defense-questions.md#28-升级会不会丢数据为什么演示不用远程最新-pr-的包) | [测试证据](../test-evidence/RUN-20261101-CALENDAR-FINAL/README.md)、[固定版本快照](../acceptance-demo/00-version-and-repository-status.md) |

批量操作仅在老师要求时追加，参见 [D09](02-feature-source-map.md#d09-批量操作可选) 和 [Q20](03-defense-questions.md#20-批量操作怎么避免只改成功一半清除筛选会取消勾选吗)。角色切换、提醒、归档／恢复、跨年导航及“今天”同样不加入前 5 分钟；主线完成后的 B 保持已完成，会后再恢复样本。

## 演示前自学，不计入功能展示时间

先用约 5—10 分钟读[环境与架构](01-architecture-and-stack.md)，再用 15—25 分钟沿[完整源码对照](02-feature-source-map.md)理解 D01—D06，最后用约 10 分钟练习[31 个问答](03-defense-questions.md)。这些时间都是准备时间；问答末尾的代码走读也只是老师追问后的备选深入材料。

操作准备以[样本清单](../acceptance-demo/01-preparation.md)和[5 分钟脚本](../acceptance-demo/02-demo-script.md)为准：现场编辑预建 A、打开已有图片、按完整标题搜索、展示日历、完成 B，不现场新建全套事项或重选图片。

## 回答时保留的事实边界

- 本机 29 份 JUnit XML、185 个不同方法、224 次实际执行全部通过；Lint 0 错误／16 警告。本轮没有新的远程 CI 通过结论。
- 荣耀 X50 本轮验证日历矩阵和 18 项收尾审计；5 条 QA 已清理，原 3 条事项、4 份附件、原 33 条提醒行（14 SENT）保持。具体范围见[实机证据](../test-evidence/RUN-20261101-CALENDAR-FINAL/README.md)，不扩大成所有功能都在本轮重新实测。
- OBS-01 旧图片来源授权、OBS-02 重启后不打开 App 的提醒自动恢复仍未关闭；已有提示、重选和正常打开 App 后恢复路径，不作无条件成功承诺。
- 当前没有重复任务界面、账号、云同步、应用内备份恢复或在线 AI 对话；`RecurrenceRules.kt` 仅为纯计算基础。PR #14 的 schema 5 和新界面不属于本次手机包。
- 按[任务板](../../team/README.md)及实际记录区分个人贡献、小组协作与 AI 辅助，不把小组实现或共同验证说成个人独立完成。
