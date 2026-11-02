# 数据与信息组织需求—代码—测试追踪

初次核对日期：2026-10-30；2026-10-31 同步局部需求确认，2026-11-01 补充独立本机界面回归。负责人方向：付泽文的数据与信息组织；本次独立补测不登记为其个人新增贡献。既有 CI 追踪以 `main` 合入 PR #6 后的产品源码为依据；[专项需求](data-requirements.md)更新至 v0.3。本文件为 AI 辅助整理，待成员复核。历史实机结论、需求确认和本机自动测试分别以原记录为准。

2026-11-01 本轮补记：PR #10 的[规划建议](planning-extension-requirements.md)已合入讨论稿，当时 7 项提案尚未实现，不加入下表“已执行”覆盖。合并后的本机基线 194 次执行通过；历史远程 CI #45 的 169 次执行有 1 项角色页面失败，两者见[当前稳定性批次](test-evidence/RUN-20261101-DEMO-STABILITY/README.md)。此前界面测试现已保存为 `778429e`；本批已测 0.3.4 源码 `bdcd76f` 的完整构建、179 方法／218 次执行通过，Lint 0／16，随后[0.3.4 设备批次](test-evidence/RUN-20261101-DEVICE-034/README.md)已完成同签名覆盖、原数据保持及本样本图片提示／重试／回滚分项；本批设备分项及17项收尾已完成，原六表／附件／设置／原12条SENT保持；自动恢复失败与历史数据测试结果保留，不扩大完整DA覆盖。

## 本次核对的自动化证据

复核[PR #6 的 Android 工作流 41066985947](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/41066985947)：运行事件为 `pull_request`，PR 分支头是 `5ee1128d1a1f77c1d6dedb39590fe12b4b606214`；实际报告产物对应 GitHub 生成的合并提交 `73245d6d9e5889004544e5787298a06894fa1834`，其父提交依次是 `453cee3a17c97a0033ddcedfab9dd21504057583` 和上述分支头。该合并提交用于 PR 检查，与后续合入 main 的提交 `9d3e3c69d0d7f910cbd3d630f60303ca429a6adb` 不是同一个 SHA。

工作流的构建、`testDebugUnitTest` 与 `lintDebug` 步骤成功。实际下载报告 ZIP 并解析：22 组 JUnit XML、137 个不同方法、156 次配置执行，失败／错误／跳过各 0；Lint XML 17 条 Warning、0 条 Error。22 组 XML 声明数与 156 个 testcase 节点相符。报告 ZIP 哈希、产物 ID、六个相关测试节点和复核范围见[机器可读摘要](test-evidence/RUN-20261030-DATA-CI/summary.json)。[报告产物](https://github.com/zidodo1124/hust26_software_engineering_homework/actions/runs/41066985947/artifacts/12984425651)需要仓库访问权限，按工作流配置保留 14 天，可能过期。

JUnit XML 中六个本轮新增或扩展测试方法均显示通过：同事项同名／空字段、v3→v4 关联、分类停用恢复、写入失败回滚、旧编辑冲突界面、写入失败后界面重试。该结果只证明本次 CI 的自动化检查；不把它换算成 13 个 DA 或 30 个 TC 整例通过，也不等同手机验收。

## 核查方式

“部分自动覆盖”表示已经有相关测试执行证据，但未证明该行全部步骤、全部边界或真实设备条件；“待确认”表示规则尚未由需求审核方明确批准。测试中的样本可以与 DA 表述不同，因此通过的是测试中的实际断言，不是将测试方法名直接替换成整个 DA 的通过结论。[测试计划](test-plan.md) TC 的历史状态保留，用户要求最终阶段再安排实机。

代码与测试入口简称：

| 简称 | 文件 | 职责 |
| --- | --- | --- |
| Repo | [RoomEventRepository.kt](../app/src/main/java/cn/edu/hust/ddlsaver/data/RoomEventRepository.kt) | 分类维护、事项和关联事务、修订号 |
| Query | [EventQuery.kt](../app/src/main/java/cn/edu/hust/ddlsaver/domain/EventQuery.kt) | 搜索、组合过滤、时间排序 |
| Validation | [EventValidation.kt](../app/src/main/java/cn/edu/hust/ddlsaver/domain/EventValidation.kt) | 字段和输入校验 |
| DB | [SaverDatabase.kt](../app/src/main/java/cn/edu/hust/ddlsaver/data/SaverDatabase.kt)、[Entities.kt](../app/src/main/java/cn/edu/hust/ddlsaver/data/Entities.kt) | 本地数据关系与 1→2→3→4 迁移 |
| RT | [RoundTwoRepositoryTest.kt](../app/src/test/java/cn/edu/hust/ddlsaver/data/RoundTwoRepositoryTest.kt) | 真实 Room 仓库、事务和重开测试 |
| QT | [EventQueryTest.kt](../app/src/test/java/cn/edu/hust/ddlsaver/domain/EventQueryTest.kt) | 搜索、筛选和排序规则测试 |
| UT | [SaverAppFlowTest.kt](../app/src/test/java/cn/edu/hust/ddlsaver/ui/SaverAppFlowTest.kt) | Robolectric 下 Activity／Compose／Room 页面流程 |
| CT | [ContentClipboardFlowTest.kt](../app/src/test/java/cn/edu/hust/ddlsaver/ui/ContentClipboardFlowTest.kt) | 本机真实页面的长描述复制、同名字段编辑／复制及取消／移除隔离 |
| MT | [SaverDatabaseMigrationTest.kt](../app/src/test/java/cn/edu/hust/ddlsaver/data/SaverDatabaseMigrationTest.kt)、[CharacterStylePersistenceTest.kt](../app/src/test/java/cn/edu/hust/ddlsaver/data/CharacterStylePersistenceTest.kt) | 真实历史 schema 构造旧库并迁移 |

## 专项需求与验收场景追踪

| 场景 / 需求 | 实现与已执行的相关自动测试 | 尚需核查或最后阶段验收 | 状态 |
| --- | --- | --- | --- |
| DA-01 / FR-14 | Repo；RT 的 `namesAreTrimmedAndDuplicateNamesCannotBypassInactiveCatalogItems` 及 `disabledSameNamedCategoriesKeepOldRelationsAcrossReopenAndCanBeRestored` 覆盖去空白、同类判重、跨类同名 | DA-01 在界面按完整输入及提示执行；空白名称及其他输入边界按需求状态确认 | 部分自动覆盖；TC-04 整例未通过 |
| DA-02 / FR-14、15 | Repo；RT 的 `renamedAndDisabledCategoriesRemainLinkedButCannotBeNewlyAssigned` 与 `disabledSameNamedCategoriesKeepOldRelationsAcrossReopenAndCanBeRestored` 覆盖改名、停用保留、阻止新增、恢复和重开 | DA-02 原样完整步骤与各状态事项的数据快照、界面检查 | 部分自动覆盖 |
| DA-03 / FR-14、18 | Repo、Query；上述两个 RT 用例覆盖停用标签、判重、双标签筛选、移除与恢复 | 停用标签在实际页面筛选与完整重名反馈；需求中额外细节待审核 | 部分自动覆盖 |
| DA-04 / FR-19 | Repo、Validation；RT 覆盖同名／空内容字段存储；CT 的 `sameNamedFieldsEditAndCopyIndependentlyWhileEmptyValueAndOtherEventRemain` 已通过本机页面验证：两条同名联系人分别复制，仅修改第二条，第一条、空内容字段及另一事项保持 | 原 DA 样本的最终设备完整流程；本机“重开”为返回列表再进详情，未验证系统杀进程；空字段复制不作为该场景要求 | 相关存储与界面子场景通过；TC-07 整例未通过 |
| DA-05 / FR-19 | Validation 覆盖空字段名及重复 ID；RT 覆盖移除；CT 的 `cancellingFieldEditsThenRemovingOneDuplicatePreservesRemainingOrderAndOtherEvent` 已验证编辑后放弃、移除单个同名字段、剩余 ID／顺序与另一事项保持 | 空白字段名的完整页面提示、原 DA 样本及设备步骤；需求中未确认的边界继续待审 | 取消与移除的本机界面子场景通过；整例仍未完成 |
| DA-06 / FR-18 | Query；QT 的 `keywordMatchesTitleDescriptionAndCustomFieldLabelsOrValuesIgnoringCase` 和 UT 的 `categoryManagementAndCustomFieldsPersistThenFilterTogether` | A—E 原样本的空关键词、无结果、清除及完成／归档排除一整组页面结果 | 部分自动覆盖；TC-14 整例未通过 |
| DA-07 / FR-18 | Query；QT 的 `combinedFiltersRequireEveryConditionAndEverySelectedTag`；RT 分类恢复测试覆盖来源与两个标签交集 | A—E 全部单标签／双标签／日期组合和清空筛选的页面路径 | 部分自动覆盖 |
| DA-08 / FR-18 | Query；QT 的 `keywordMatchesTitleDescriptionAndCustomFieldLabelsOrValuesIgnoringCase` 有英文大小写例子；新增 `multiWordKeywordMatchesContinuouslyAndCategoriesUseTheirOwnFilters` 检查连续多词与分类筛选，已在[本次 CI](test-evidence/RUN-20261031-SEARCH-CI/summary.json)中取得独立通过节点 | 本人已确认英文大小写、完整连续文字与多词不拆分；空关键词等未涉及细节待审核，DA-08 页面整例仍需验证 | 两项相关自动用例通过；整例仍未完成 |
| DA-09 / FR-16、17、18 | Query；QT 的 `expiredItemsRemainTodoWhileArchiveTakesPrecedenceOverCompletion`；UT 的 `completedItemCanBeRecoveredFromHistoryAndDeleteCancellationPreservesIt` 和 RT 的归档标记测试 | 使用 DA-09 全样本核对归档前后两个列表，及最终设备操作 | 部分自动覆盖 |
| DA-10 / FR-03、07、08、18 | Query、`HomeScreen`、Repo；UT 的 `categoryManagementAndCustomFieldsPersistThenFilterTogether` 和 `calendarCrossesMonthAndTodayShortcutClearsDateFilter` | 按 DA-10 把编辑／取消、日期、筛选结果数与首页统计串成完整样本 | 部分自动覆盖 |
| DA-11 / FR-03、15 | Repo；RT 的 `failureDuringFieldInsertRollsBackEventRelationsAndReminderPlans`；UT 的 `staleEditShowsConflictAndKeepsDraftWithoutOverwritingNewerData` 和 `failedFieldWriteKeepsDraftAndOriginalDataUntilRetrySucceeds` | 其他失败原因及设备条件；故障注入仅代表测试中指定的字段插入错误 | 部分自动覆盖；TC-30 整例未通过 |
| DA-12 / FR-15 | DB；MT 的 `migratesV1FileAndKeepsData`、`migratesV2AndKeepsRelations`、`upgradeFromV3PreservesSettingsEventAndDeliveryLedgerWithClassicDefault` 构造 v1／v2／v3 旧库并验证各自可表示的部分数据及迁移 | 逐版本核对更多字段和异常，最终设备覆盖安装验证真实附件文件；v1 中不存在后续版本才增加的表，不应伪造 v1 数据 | 部分自动覆盖；TC-01 整例未通过 |
| DA-13 / FR-10、15、18 | Repo、Query；RT 的 `batchUsesOnlySelectedIdsPreservesIndependentFlagsAndDeletesTheirRelations`，QT 的 `filteringReturnsStableDeadlineThenIdOrderWithoutChangingInput` | DA-13 的全部其他事项、共享分类及重开快照；筛选／删除的真实页面操作 | 部分自动覆盖 |

后续新增 `SaverAppFlowTest.combinedSourceTagsKeywordAndDateFilterTheVisibleListWithoutChangingTodayCount`，使用独立样本检查来源、两个标签、关键词和跨月日期组合、已完成／归档排除、清除筛选及首页今日计数的本地界面流程；其实际 CI 结果以对应提交的运行记录为准，不替代 DA-07／10 的全部预设步骤或实机验收。

以上均不宣称所有组合已经执行。2026-10-31 本人确认关键词完整连续匹配、多词不拆分和英文大小写不敏感，以及同截止时间稳定排序、停用分类筛选历史事项；其他未涉及的边界仍待审核。后续依优先级选择真实缺口，不为增加数量而重复实现已有测试。

## 追踪结论和后续安排

2026-11-01 的 CT 证据来自 [RUN-20261101-INDEPENDENT-UI](test-evidence/RUN-20261101-INDEPENDENT-UI/README.md)：基线 `ac1d978` 加未提交新增测试，生产代码未改。该本机批次与历史 PR CI 分开记录，不把当前远程 CI 的 169 次执行当作新增测试已在 CI 运行，也不把页面自动化替代最终实机验收。

1. **代码与自动测试**：核心规则、分类维护、字段独立性、v1／v2／v3 迁移、写入失败和界面草稿已有相关自动测试证据；未因样本不完整而给整个 FR、DA 或 TC 下全面通过结论。
2. **需求与设计**：确认剩余细节后同步主需求、设计及测试预期。当前 `sourceId` 未声明 Room 外键，Repository 在保存时校验来源；应用无永久删除来源的公开操作。若以后引入其他写入路径或永久删除来源，应重新评估一致性与迁移，不将该设计差异直接登记为已发生缺陷。
3. **完整验证**：整理 DA-01—DA-13 的未覆盖步骤及测试数据；实机、覆盖升级和真实图片文件检查由最终阶段开展，不提前安排手机连接。
4. **报告与答辩**：完成后汇总事实、环境、版本、用例及缺陷，准备个人模块讲解；当前文档已有历史步骤和新 CI 摘要，最终报告仍需小组合并审阅。

## 规划扩展的后续状态

截至 2026-11-02 06:58:10 UTC，远程 main 为 [99490e6](https://github.com/zidodo1124/hust26_software_engineering_homework/commit/99490e692b67f3d4f46548aa8e97a0a56d864221)。付泽文 [PR #11](https://github.com/zidodo1124/hust26_software_engineering_homework/pull/11) 的月历日期格数量增量已合入，沿用既有月历，数量遵循列表状态、关键词、来源、标签及危险期筛选，不随所选单日缩减；随后队友已合入 [PR #12](https://github.com/zidodo1124/hust26_software_engineering_homework/pull/12) 的周／日周期界面与筛选，开放 PR 为 0。本批未替队友执行此次审阅／合并。PX-01／PX-02 的完整验收仍待完成，PX-03—PX-07 未由这些增量实现。 月历数量新增的 3 条既有测试断言属于 PR #11 的远程增量，不扩写为下表 DA 完整步骤或 PA-01 整例通过。本批手机已测试 `bdcd76f` 的 0.3.4／7，APK SHA-256 `690626883dd90684128959a53737b54c326352e3decb0dd46a707ac3d962ab90`；远程增量版本号也为 0.3.4／7，应以源码提交和 APK 哈希区分。本批本机与 CI #46 的 218 次执行，以及本批手机结果，均不覆盖上述远程新增代码。
