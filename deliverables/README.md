# 课程提交材料（HUSTer's DDL Saver 1.0.0）

华中科技大学计算机科学与技术学院《软件工程》课程设计 · 花园鳗小组（支盟渊、付泽文、岳羽泉）

每份文档都有两种格式：Markdown（在 GitHub 上直接阅读）和 Word（`word/` 目录，用于提交和打印）。图表的 PlantUML 源文件在 `diagrams/src/`，Word 文件可用 `python scripts/build_deliverables_docx.py` 重新生成。

## 材料清单

| 老师要求的材料 | 文档 | Word | 状态 |
| --- | --- | --- | --- |
| 选题 | [01-选题报告](01-选题报告.md) | [docx](word/01-选题报告.docx) | 已完成 |
| 需求分析文档 | [02-需求规格说明书](02-需求规格说明书.md) | [docx](word/02-需求规格说明书.docx) | 已完成 |
| 设计文档 | [03-设计说明书](03-设计说明书.md) | [docx](word/03-设计说明书.docx) | 已完成 |
| 测试报告 | [04-测试报告](04-测试报告.md) | [docx](word/04-测试报告.docx) | 已完成 |
| 关键源码 | [05-关键源码说明](05-关键源码说明.md)；完整源码见 [`app/src/main/java/cn/edu/hust/ddlsaver/`](../app/src/main/java/cn/edu/hust/ddlsaver/) | [docx](word/05-关键源码说明.docx) | 已完成 |
| 小组分工 | [06-小组分工](06-小组分工.md) | [docx](word/06-小组分工.docx) | 已完成 |
| 可运行的软件 | [Android 安装包 1.0.0](../releases/android/README.md) | — | 已完成，荣耀 X50 实测 |
| 代码与多次提交记录 | 本仓库 `main` 分支、[Pull Requests](https://github.com/zidodo1124/hust26_software_engineering_homework/pulls?q=is%3Apr) | — | 已完成 |
| 实验报告 | — | — | 待编写 |

## 各文档包含的图表

| 文档 | 图 |
| --- | --- |
| 选题报告 | 项目进度甘特图 |
| 需求规格说明书 | 用例图、事项状态图、概念 E-R 图、真机界面截图 |
| 设计说明书 | 分层架构图、部署图、功能模块结构图、类图、数据库 E-R 图、保存 / 提醒 / 图片 / 重复事项时序图、提醒计划状态图、紧急程度判定流程图、页面跳转图 |
| 测试报告 | 真机验收截图；用例表、等价类与边界值表、缺陷统计表、需求覆盖矩阵 |
| 关键源码说明 | 关键函数调用关系图；10 段带注释的关键代码 |

## 过程记录

开发过程中的阶段性文档（需求访谈、各轮测试记录、审核记录、成员交互记录等）保留在 [`docs/`](../docs/README.md)、[`team/`](../team/README.md) 和 [`interaction-records/`](../interaction-records/README.md)，本目录的文档是在此基础上按 1.0.0 实际代码整理的最终版本。
