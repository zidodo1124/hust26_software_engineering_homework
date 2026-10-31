# 数据与信息组织需求—代码—测试追踪

初次核对日期：2026-10-30；2026-10-31 同步局部需求确认。负责人方向：付泽文的数据与信息组织。既有 CI 追踪以 `main` 合入 PR #6 后的产品源码为依据；[专项需求](data-requirements.md)更新至 v0.3。本文件为 AI 辅助整理，待成员复核。历史实机结论、需求确认和本机自动测试分别以原记录为准。

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
| MT | [SaverDatabaseMigrationTest.kt](../app/src/test/java/cn/edu/hust/ddlsaver/data/SaverDatabaseMigrationTest.kt)、[CharacterStylePersistenceTest.kt](../app/src/test/java/cn/edu/hust/ddlsaver/data/CharacterStylePersistenceTest.kt) | 真实历史 schema 构造旧库并迁移 |

## 专项需求与验收场景追踪

| 场景 / 需求 | 实现与已执行的相关自动测试 | 尚需核查或最后阶段验收 | 状态 |
| --- | --- | --- | --- |
| DA-01 / FR-14 | Repo；RT 的 `namesAreTrimmedAndDuplicateNamesCannotBypassInactiveCatalogItems` 及 `disabledSameNamedCategoriesKeepOldRelationsAcrossReopenAndCanBeRestored` 覆盖去空白、同类判重、跨类同名 | DA-01 在界面按完整输入及提示执行；空白名称及其他输入边界按需求状态确认 | 部分自动覆盖；TC-04 整例未通过 |
| DA-02 / FR-14、15 | Repo；RT 的 `renamedAndDisabledCategoriesRemainLinkedButCannotBeNewlyAssigned` 与 `disabledSameNamedCategoriesKeepOldRelationsAcrossReopenAndCanBeRestored` 覆盖改名、停用保留、阻止新增、恢复和重开 | DA-02 原样完整步骤与各状态事项的数据快照、界面检查 | 部分自动覆盖 |
| DA-03 / FR-14、18 | Repo、Query；上述两个 RT 用例覆盖停用标签、判重、双标签筛选、移除与恢复 | 停用标签在实际页面筛选与完整重名反馈；需求中额外细节待审核 | 部分自动覆盖 |
| DA-04 / FR-19 | Repo、Validation；RT 的 `sameNamedFieldsAndEmptyContentRemainIndependentAcrossEditsAndReopen` 覆盖同一事项同名、空内容、修改、移除与重开，并检查另一事项 | DA-04 的两条联系人复制和完整界面交互；空字段复制不作为该场景要求 | 部分自动覆盖；TC-07 整例未通过 |
| DA-05 / FR-19 | Validation 的 `emptyFieldNamesAndDuplicateFieldIdsAreRejected`；RT 同名字段测试覆盖移除一条；UT 的 `activityRecreationKeepsUnsavedDraftAndDiscardRequiresConfirmation` 覆盖草稿保留及放弃 | 以同名字段为样本完成 DA-05 的空字段名、取消、移除及顺序完整页面流程 | 部分自动覆盖 |
| DA-06 / FR-18 | Query；QT 的 `keywordMatchesTitleDescriptionAndCustomFieldLabelsOrValuesIgnoringCase` 和 UT 的 `categoryManagementAndCustomFieldsPersistThenFilterTogether` | A—E 原样本的空关键词、无结果、清除及完成／归档排除一整组页面结果 | 部分自动覆盖；TC-14 整例未通过 |
| DA-07 / FR-18 | Query；QT 的 `combinedFiltersRequireEveryConditionAndEverySelectedTag`；RT 分类恢复测试覆盖来源与两个标签交集 | A—E 全部单标签／双标签／日期组合和清空筛选的页面路径 | 部分自动覆盖 |
| DA-08 / FR-18 | Query；QT 的 `keywordMatchesTitleDescriptionAndCustomFieldLabelsOrValuesIgnoringCase` 有英文大小写例子；新增 `multiWordKeywordMatchesContinuouslyAndCategoriesUseTheirOwnFilters` 检查连续多词与分类筛选，已在[本次 CI](test-evidence/RUN-20261031-SEARCH-CI/summary.json)中取得独立通过节点 | 本人已确认英文大小写、完整连续文字与多词不拆分；空关键词等未涉及细节待审核，DA-08 页面整例仍需验证 | 两项相关自动用例通过；整例仍未完成 |
| DA-09 / FR-16、17、18 | Query；QT 的 `expiredItemsRemainTodoWhileArchiveTakesPrecedenceOverCompletion`；UT 的 `completedItemCanBeRecoveredFromHistoryAndDeleteCancellationPreservesIt` 和 RT 的归档标记测试 | 使用 DA-09 全样本核对归档前后两个列表，及最终设备操作 | 部分自动覆盖 |
| DA-10 / FR-03、07、08、18 | Query、`HomeScreen`、Repo；UT 的 `categoryManagementAndCustomFieldsPersistThenFilterTogether` 和 `calendarCrossesMonthAndTodayShortcutClearsDateFilter` | 按 DA-10 把编辑／取消、日期、筛选结果数与首页统计串成完整样本 | 部分自动覆盖 |
| DA-11 / FR-03、15 | Repo；RT 的 `failureDuringFieldInsertRollsBackEventRelationsAndReminderPlans`；UT 的 `staleEditShowsConflictAndKeepsDraftWithoutOverwritingNewerData` 和 `failedFieldWriteKeepsDraftAndOriginalDataUntilRetrySucceeds` | 其他失败原因及设备条件；故障注入仅代表测试中指定的字段插入错误 | 部分自动覆盖；TC-30 整例未通过 |
| DA-12 / FR-15 | DB；MT 的 `migratesV1FileAndKeepsData`、`migratesV2AndKeepsRelations`、`upgradeFromV3PreservesSettingsEventAndDeliveryLedgerWithClassicDefault` 构造 v1／v2／v3 旧库并验证各自可表示的部分数据及迁移 | 逐版本核对更多字段和异常，最终设备覆盖安装验证真实附件文件；v1 中不存在后续版本才增加的表，不应伪造 v1 数据 | 部分自动覆盖；TC-01 整例未通过 |
| DA-13 / FR-10、15、18 | Repo、Query；RT 的 `batchUsesOnlySelectedIdsPreservesIndependentFlagsAndDeletesTheirRelations`，QT 的 `filteringReturnsStableDeadlineThenIdOrderWithoutChangingInput` | DA-13 的全部其他事项、共享分类及重开快照；筛选／删除的真实页面操作 | 部分自动覆盖 |

以上均不宣称所有组合已经执行。2026-10-31 本人确认关键词完整连续匹配、多词不拆分和英文大小写不敏感，以及同截止时间稳定排序、停用分类筛选历史事项；其他未涉及的边界仍待审核。后续依优先级选择真实缺口，不为增加数量而重复实现已有测试。

## 追踪结论和后续安排

1. **代码与自动测试**：核心规则、分类维护、字段独立性、v1／v2／v3 迁移、写入失败和界面草稿已有相关自动测试证据；未因样本不完整而给整个 FR、DA 或 TC 下全面通过结论。
2. **需求与设计**：确认剩余细节后同步主需求、设计及测试预期。当前 `sourceId` 未声明 Room 外键，Repository 在保存时校验来源；应用无永久删除来源的公开操作。若以后引入其他写入路径或永久删除来源，应重新评估一致性与迁移，不将该设计差异直接登记为已发生缺陷。
3. **完整验证**：整理 DA-01—DA-13 的未覆盖步骤及测试数据；实机、覆盖升级和真实图片文件检查由最终阶段开展，不提前安排手机连接。
4. **报告与答辩**：完成后汇总事实、环境、版本、用例及缺陷，准备个人模块讲解；当前文档已有历史步骤和新 CI 摘要，最终报告仍需小组合并审阅。
