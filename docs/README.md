# DDL Saver 文档总入口

> **课程最终提交材料（1.0.0）：** 选题报告、需求规格说明书、设计说明书、测试报告、关键源码说明、小组分工的最终版集中在 [deliverables/](../deliverables/README.md)，每份都有 Markdown 和 Word 两种格式。本目录保留开发过程中的各版本文档与测试证据。1.0.0 安装包见 [releases/android/](../releases/android/README.md)。

所有项目说明从这里查找。原根目录的 `README.md`、`STATUS.md`、`idea.md`、`task_finish_tips.md` 已集中到 [`project/`](project/README.md)；工程根目录保留代码与构建文件。

## 手机安装包（1.0.0）

**[下载 DDL-Saver-1.0.0.apk](https://github.com/zidodo1124/hust26_software_engineering_homework/raw/refs/heads/main/releases/android/DDL-Saver-1.0.0.apk)**（也可在 [Releases](https://github.com/zidodo1124/hust26_software_engineering_homework/releases/tag/v1.0.0) 下载）

1. 把 APK 下载到 Android 手机（Android 8.0 及以上），用文件管理打开，按系统提示允许安装。
2. 打开 DDL Saver，在“设置”中允许通知和精确提醒。
3. 从 0.3.x 升级需先卸载旧版（1.0.0 更换了签名证书）。

该 APK 与 2026-11-05 荣耀 X50 真机验收的安装包完全一致；SHA-256 与历史版本见 [`releases/android/`](../releases/android/README.md)。

## 现在最常用的材料

| 需要做什么 | 打开哪份文件 |
| --- | --- |
| 了解当前本地有哪些功能、由哪些代码实现 | [当前本地源码导航](source-navigation.md) |
| 查看新版标题、操作提示和鲸鱼娘的 36 条台词 | [界面文案与台词](ui-copy-guide.md) |
| 核对远程同步、当前本地分支及测试状态 | [项目状态](project/STATUS.md) |
| 参考原五分钟路线，准备当前手机演示 | [版本提示与演示入口](acceptance-demo/README.md) · [现场速查卡](acceptance-demo/03-five-minute-cue-card.md) · [准备清单](acceptance-demo/01-preparation.md) |
| 老师问到当前手机版的实现 | [当前源码导航](source-navigation.md)；原 0.3.5 的固定源码与问答见[历史答辩入口](acceptance-code-guide/README.md) |

**当前版本：** 1.0.0／12、schema 5，`main` 提交 `3d8ee79`；荣耀 X50 真机验收见 [RUN-20261105-FINAL-1.0.0](test-evidence/RUN-20261105-FINAL-1.0.0/README.md)。五分钟演示脚本和原答辩材料固定于 `demo-v0.3.5`，基本操作可供排练参考；讲当前实现请用[关键源码说明](../deliverables/05-关键源码说明.md)。

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
