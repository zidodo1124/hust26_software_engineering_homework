# DDL Saver 文档总入口

所有项目说明从这里查找。原根目录的 `README.md`、`STATUS.md`、`idea.md`、`task_finish_tips.md` 已集中到 [`project/`](project/README.md)；工程根目录保留代码与构建文件。

## 现在最常用的材料

| 需要做什么 | 打开哪份文件 |
| --- | --- |
| 了解当前本地有哪些功能、由哪些代码实现 | [当前本地源码导航](source-navigation.md) |
| 核对远程同步、当前本地分支及测试状态 | [项目状态](project/STATUS.md) |
| 准备手机上已安装的 0.3.5 五分钟展示 | [现场速查卡](acceptance-demo/03-five-minute-cue-card.md) · [准备清单](acceptance-demo/01-preparation.md) · [完整脚本](acceptance-demo/02-demo-script.md) |
| 老师问到手机演示版的实现 | [0.3.5 答辩速查入口](acceptance-code-guide/README.md) · [31 个详细问答](acceptance-code-guide/03-defense-questions.md) |

**先分清版本：** 当前本地同步分支已纳入 PR #14 的重复事项实现，数据库为 schema 5；手机此前安装并验收的是 `demo-v0.3.5`／schema 4。五分钟脚本和原答辩材料继续对应手机版。阅读当前本地代码请使用新的源码导航，查看旧版实现请使用原材料中的固定标签链接。同步本地代码不表示已将新包安装到手机。

## 项目、课程与协作

| 类别 | 材料 |
| --- | --- |
| 项目介绍与当前进展 | [项目介绍](project/README.md) · [项目状态](project/STATUS.md) |
| 原始想法与课程要求 | [选题原始想法](project/idea.md) · [课程成果验收要求](project/task_finish_tips.md) |
| 需求与设计 | [需求](requirements.md) · [设计](design.md) · [重复事项实现说明](recurrence-design-draft.md) |
| 开发与使用 | [开发说明](development.md) · [技术栈](technology-stack.md) · [使用手册](user-guide.md) |
| 测试、维护与交付记录 | [测试报告](test-report.md) · [测试记录](test-records.md) · [维护日志](maintenance-log.md) · [0.3.5 交付记录](demo-final-0.3.5.md) |
| 小组及过程记录 | [任务板](../team/README.md) · [协作记录](../interaction-records/README.md) · [审阅记录](review-log.md) |
| 课程汇编 | [最终报告](final-report.md) · [完整验收安排](demo-and-acceptance.md) |

素材许可留在其素材目录，测试证据和团队记录也保留原来的分类位置；这次整理集中的是原来散落在项目根目录的四份 Markdown 文件。
