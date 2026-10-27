# RUN-20261027-004：荣耀 X50 第二轮功能实测

本轮于 2026-10-27 05:47:39（America/Los_Angeles，UTC-07:00），即 12:47:39 UTC 开始准备。支盟渊要求测试新建事项、来源标签、自定义字段、日历、批量管理和图片附件，发现问题后修复、复测，并同步记录及 Git。AI 助手执行真实界面操作、只读数据核对和证据整理；支盟渊协助系统相册选择及独立测试工具的手机安装确认。

本轮六类功能的既定操作与收尾已完成：包括确认批删、移除测试来源与卸载夹具后重开预览，以及测试样本归档。具体结果与 OBS-01 旧剪贴板 URI 拒绝观察分列如下；TC-05 在本机和本轮样本范围内整例通过，其他用例按实际子场景记录，完整课程验收尚未完成。[执行记录第 11 节](../../test-records.md#11-run-20261027-004第二轮功能实机验证)保留详细经过；此前首次安装证据在 [RUN-003](../RUN-20261027-003/README.md)。

本轮于 13:40:58.645774 UTC（本地 06:40:58）结束。最终 A/C 已归档且各保留两图，B/D 已删除，测试分类已停用，独立夹具已卸载、测试剪贴板已清空、三张公共测试原图已移除，首页恢复正常待办视图。手机内产品 APK 重新计算哈希与上次一致。未对非 QA 事项执行编辑或删除，最终匿名计数为 1；未采集原事项完整内容指纹，不作逐字节内容一致承诺。

## 环境与工具标识

| 项目 | 实际记录 |
| --- | --- |
| 设备 | 荣耀 X50／HONOR ALI-AN00，Android 15／API 35、MagicOS 9.0.0 |
| 产品 | v0.2.0／versionCode 2；`cn.edu.hust.ddlsaver`；本轮产品源码和 APK 未变 |
| 产品源码 | [6255f3b](https://github.com/zidodo1124/hust26_software_engineering_homework/commit/6255f3bd3c36e2a605f963eb7a8bae9a6ad19842) |
| 产品 APK SHA-256 | `6B5167424691BD218E10314689990C37969FCD8018FBCAA282A2917B67B594A9` |
| 自建数据 | `QA1027-1249` 前缀的 A、B、C、D 及 D-CANCEL 草稿，专用来源和标签；已有用户事项保留 |
| 图片来源 | 两张 PNG、一张 EXIF6 JPEG、一个损坏文件，均为合成样本；[资源清单](../../../test-tools/clipboard-fixture/assets-manifest.json) |
| 剪贴板夹具 | 独立包 `cn.edu.hust.ddlsaver.qa.clipboard`，通过标准 ClipData URI 授权提供自带图片；[源码和复现说明](../../../test-tools/clipboard-fixture/README.md) |
| 实机夹具 APK SHA-256 | `FED09852FED3B1B114BAFFBEB0166AA122FE9849ADA71C5C87DAA11BDAA822C2`；该包由支盟渊在手机上确认安装 |

夹具源码提交 [db28277](https://github.com/zidodo1124/hust26_software_engineering_homework/commit/db28277)，资源换行保护提交 [8ea56b5](https://github.com/zidodo1124/hust26_software_engineering_homework/commit/8ea56b5)。后续为验证源码复现而新构建的夹具 APK 未代替上述实机包；各产物哈希分别记录。新增 Java、Python 和 Pillow 工具仅用于测试，产品运行依赖未改变。

## 功能结果与用例边界

| 方向 | 已观察结果 | 用例范围及限制 |
| --- | --- | --- |
| 新建、编辑与返回 | 空标题、未选完整日期时间会提示错误；A 带分类和字段、B 仅必填项均能保存；未保存可继续编辑或放弃；复制 URL 后保存为描述 | TC-02、TC-03 部分场景；非正速通、全空白标题及完整时间编辑组合等仍未覆盖 |
| 来源标签与筛选 | 新建、改名后 A 关系保留；关键词、来源和标签取交集仅 A；选日仍与分类条件取交集；收尾时已停用 QA 分类 | TC-04、TC-14 部分场景；停用后已有引用的完整核对、恢复、重要程度及全部字段搜索和排序组合仍未覆盖 |
| 自定义字段 | A 的两字段保存及重开后保留，B 未继承；URL 复制并粘贴，Location 修改、URL 移除后保存；恢复状态后修订仍在 | TC-07 部分场景；三组字段与不同事项同名不同值仍未覆盖 |
| 日历 | 10 月 28 日仅 A、29 日仅 B；11 月与 10 月往返；今日数量保持 1；保留分类时 29 日为 0，符合交集 | TC-12、TC-14 部分场景；昨天／今天／明天整组及全部计数边界仍未覆盖 |
| 批量与恢复 | 完成 A/B 后 C 仍待办；归档保留完成标记；A 恢复后数据保留；取消删除 B 有效；精确确认删除 B/D 后 A/C 完整 QA 快照未变，D 的 1 个副本清理 | TC-15、TC-16、TC-18 部分场景；详情单项删除及提醒相关步骤仍未实施 |
| 相册多图与方向 | 支盟渊协助选择三张 QA 图，AI 保存、逐张放大、前后切换；横图、竖图和 EXIF6 方向正确；进程重开后可读。删除三个自建原图后再次强停重开，C 保留的两图均可预览 | TC-05 整例通过，限定荣耀 X50 与这组三个合成样本；不代表所有格式或设备兼容 |
| 移除与副本保护 | 移除后取消仍保留 3 图；移除并保存后保留 2 图，被删私有文件已清理，三个相册测试原图哈希不变 | TC-05、TC-18 的附件子场景 |
| 跨应用图片粘贴 | 独立夹具的标准 URI 授权一次粘贴 2 图并保存到 A；D-CANCEL 导入 1 图后放弃，未落库且新副本清零；卸载夹具后强停重开，A 两图仍可预览 | TC-06 部分执行：双 URI 变体及单图／取消／空剪贴板已验证；原步骤的同事项连续两次粘贴增量未测 |
| 拒绝分支 | 损坏图、纯文本图片网址、空剪贴板均明确拒绝；A 原字段和已导入 2 图保留，每个拒绝分支均无新增私有文件 | TC-06、TC-30 的图片子场景；数据库、调度失败等其余故障未覆盖 |
| OBS-01：旧 URI 复用 | 13:32:28 UTC 强停并重开产品后复用旧剪贴板 URI 被拒，0 新附件、表单保留；13:32:50 UTC 重新 Copy blue 后 D 保存 1 图 | 原因尚未确定；每次点击重读剪贴板，通用异常提示缺少原因日志，不能直接归类为系统或产品缺陷 |

用例以[测试计划](../../test-plan.md)的全部前提、步骤和预期判定；一次操作项不等于一个完整用例。30 个用例中，本轮新增 TC-05 整例通过，其余按部分执行或未执行记录，不把未执行用例算作失败或以操作记录数计算通过率。13:36:03 UTC 确认删除 B/D，13:36:25 UTC 清空测试剪贴板并卸载夹具，13:37:03 UTC 验证 A 来源卸载后两图，13:38:45 UTC 验证 C 原图删除后两图，13:39:23 UTC 将 A/C 归档保留供审核。

## 数据与截图证据

| 证据 | 范围 |
| --- | --- |
| [steps.json](steps.json)、[final-summary.json](final-summary.json) | 最终原始操作记录与结束状态；45 条记录中 40 条标记 pass，另含准备、拒绝和工具保护记录，不等于 40 个完整业务用例通过 |
| [evidence-map.json](evidence-map.json)、[evidence-manifest.json](evidence-manifest.json) | 原记录引用的导出说明，以及 37 个证据文件的原始字节数和 SHA-256；清单不包含自身及说明文档 |
| [qa-state-after-gallery.json](qa-state-after-gallery.json) | 仅查询 QA 前缀事项；C 的三张私有副本与合成资源哈希一致 |
| [qa-state-after-removal.json](qa-state-after-removal.json) | C 保存移除后仅余两张 PNG；其他 QA 事项与字段可核对 |
| [qa-state-after-cancelled-new-draft.json](qa-state-after-cancelled-new-draft.json) | 带图新建草稿取消后，事项未落库且新副本清理 |
| [qa-state-before-delete.json](qa-state-before-delete.json)、[qa-state-after-delete.json](qa-state-after-delete.json) | B/D 确认删除前后；未选 A/C 的 QA 快照一致 |
| [qa-state-provider-uninstalled.json](qa-state-provider-uninstalled.json)、[qa-state-final.json](qa-state-final.json) | 来源夹具卸载后的私有附件及最终归档状态 |
| [media-date-check.json](media-date-check.json) | 三张自建原图拍摄／修改日期保持原值 |
| [ci-verification.json](ci-verification.json) | 测试支持提交的 CI 状态、真实报告计数及产物入口 |

## 已审阅截图

以下 25 张原始截图均逐张审阅，复制后与本机原件哈希一致。受系统横幅遮挡或不适合收录的截图不导出；字段修订采用 `08b` 重拍图。最终操作 JSON 已原样导出，原记录中未导出的截图引用在证据映射中明确说明；本目录以局部 `.gitattributes` 禁用 JSON 和 PNG 换行转换，保持校验值稳定。

| 文件 | 支持的观察 |
| --- | --- |
| [01-test-tag.png](01-test-tag.png) | 专用标签创建 |
| [02-required-errors.png](02-required-errors.png) | 标题及完整截止时间的必填提示 |
| [06-reopened-A-fields.png](06-reopened-A-fields.png) | 进程重开后的 A 字段与分类 |
| [07-unsaved-confirm.png](07-unsaved-confirm.png) | 未保存返回确认 |
| [08b-clean-edited-fields.png](08b-clean-edited-fields.png) | 保存后的字段修订与复制文本；A 恢复后信息仍在 |
| [09-source-tag-intersection.png](09-source-tag-intersection.png) | 来源／标签与关键词的交集 |
| [11-calendar-28-A.png](11-calendar-28-A.png) | 10 月 28 日仅 A |
| [12-calendar-29-B.png](12-calendar-29-B.png) | 10 月 29 日仅 B |
| [15-control-C-remains-todo.png](15-control-C-remains-todo.png) | 批量完成 A/B 后控制项 C 保留 |
| [16-batch-archive-preserves-completion.png](16-batch-archive-preserves-completion.png) | 归档保留完成状态 |
| [17-batch-delete-confirm.png](17-batch-delete-confirm.png) | 删除确认界面；此处实际执行取消分支，后续确认删除另有记录 |
| [18-gallery-three-synthetic-images.png](18-gallery-three-synthetic-images.png) | 相册三图保存后的附件列表 |
| [19-blue-preview.png](19-blue-preview.png) | 蓝色横图放大，白块在左、黄圆在右 |
| [20-orange-preview.png](20-orange-preview.png) | 橙色竖图放大，三角朝上、BOTTOM 在下 |
| [21-exif6-preview.png](21-exif6-preview.png) | EXIF6 正确方向：竖图、红条在上、箭头向下 |
| [22-gallery-after-process-reopen.png](22-gallery-after-process-reopen.png) | 保存相册附件后结束进程重开，三图仍在 |
| [23-gallery-after-removal-saved.png](23-gallery-after-removal-saved.png) | 移除旋转图并保存后保留两张 PNG |
| [24-two-clipboard-images-draft.png](24-two-clipboard-images-draft.png) | 通过独立来源应用的剪贴板一次导入两个图片 URI |
| [25-broken-image-rejected-retains-draft.png](25-broken-image-rejected-retains-draft.png) | 坏图被拒，原字段和两张已导入图仍在 |
| [26-saved-clipboard-A.png](26-saved-clipboard-A.png) | A 保存两张跨应用粘贴的图片 |
| [27-batch-delete-B-and-D-confirm.png](27-batch-delete-B-and-D-confirm.png) | 精确选中 B/D 两项的确认界面；删除成功由后续步骤及 QA 状态核对支持 |
| [28-clipboard-preview-after-provider-uninstall.png](28-clipboard-preview-after-provider-uninstall.png) | 卸载来源夹具后 A 第一张图仍能预览 |
| [29-second-clipboard-preview-after-provider-uninstall.png](29-second-clipboard-preview-after-provider-uninstall.png) | 卸载来源夹具后 A 第二张图仍能预览 |
| [30-gallery-preview-after-source-removal.png](30-gallery-preview-after-source-removal.png) | 删除公共测试原图后 C 第一张图仍能预览 |
| [31-second-gallery-preview-after-source-removal.png](31-second-gallery-preview-after-source-removal.png) | 删除公共测试原图后 C 第二张图仍能预览 |

## 相册来源与异常说明

系统选择器未列出专用 QA 目录，支盟渊协助多选后，AI 通过已核对的媒体标识／路径确认 `1000044664.png`、`1000044665.jpg`、`1000044666.png` 分别来自本轮三张合成样本。尝试仅更新这三张自建样本的 `datetaken` 被系统忽略；13:18:07 UTC 复查确认 `datetaken` 和 `date_modified` 与原值一致。未修改 mtime 或设备时钟，也没有发生需要恢复的日期变更。

实测期间修正了记录 UTF-8 读取、前台切出后的输入保护、输入法影响滚动、搜索框文本与事项标题重名时的选择器，以及日历检查漏清分类的前置。这些属于测试工具或执行环境调整。本轮产品代码没有修改，未确认产品缺陷；旧 URI 拒绝仍作为 OBS-01 保留待定位。

## 自动检查复核

[CI 40508515218](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/40508515218) 对应 `8ea56b5d77d98c599932ddd17479d9ff716b5949`，13:17:56 UTC 核对为 `completed / success`。实际 XML 为 87 个 testcase，失败、错误、跳过均为 0；Lint 0 错误、12 条警告。这是现有自动检查再验证，不增加不同方法数或实机次数；手机继续使用原本地 APK，CI 产物另行标识。
