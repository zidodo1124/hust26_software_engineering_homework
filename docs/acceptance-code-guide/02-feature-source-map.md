# 演示功能与源码逐项对照

按[演示脚本](../acceptance-demo/02-demo-script.md)的 D01—D09 编号组织。全部行号以 `demo-v0.3.5` 固定版本为准；在 IDE 中打开仓库相对路径后跳到对应行即可。方括号中的“固定代码”链接直接定位 GitHub 同一版本，避免最新 main 或 PR 分支改动导致错行。代码块均为实际源码的短节选，不是另写的伪实现；片段中涉及的变量可在同一函数前文找到。

**前 5 分钟只展示 D01—D06 的软件操作，不展开本页源码。** 本页用于提前理解，以及演示结束后老师问到时查阅。主线依次是首页、编辑预建 A、查看已有图片、完整标题搜索、月／周／日历、完成 B；D07 只在首页顺带指认现有角色，形象切换及 D08／D09 留作老师要求时追加。

## D01 首页与定位

**演示对应（00:00—00:20）：** 进入首页，用一句话说明截止事项管理用途，指认待办列表、统计及现有角色；不进入设置、不解释实现。样本日 S 可以在未来，“今日 DDL”不一定等于样本日的 2 条。

| 源文件地址 | 函数／位置 | 负责的行为 |
| --- | --- | --- |
| [app/src/main/java/cn/edu/hust/ddlsaver/MainActivity.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/MainActivity.kt)，L17—38 | `onCreate`；[固定代码](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/MainActivity.kt#L17-L38) | 建立 ViewModel，加载 `SaverTheme` 和 `SaverApp` |
| [app/src/main/java/cn/edu/hust/ddlsaver/ui/SaverApp.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/ui/SaverApp.kt)，L86—100 | `SaverApp`；[固定代码](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/ui/SaverApp.kt#L86-L100) | 收集状态，用页面状态决定显示首页／详情／编辑等 |
| [app/src/main/java/cn/edu/hust/ddlsaver/ui/HomeScreen.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/ui/HomeScreen.kt)，L54—57 | 首页统计；[固定代码](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/ui/HomeScreen.kt#L54-L57) | 从全部有效事项计算今日和危险数量 |
| [app/src/main/java/cn/edu/hust/ddlsaver/domain/UrgencyRules.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/domain/UrgencyRules.kt)，L7—28 | `classify`／`isActive`／`isToday`；[固定代码](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/domain/UrgencyRules.kt#L7-L28) | 统一紧急度、有效状态和北京时间日期规则 |

`HomeScreen.kt` L54—57：

```kotlin
val active = state.events.filter(UrgencyRules::isActive)
val today = state.now.atZone(BeijingZone).toLocalDate()
val todayCount = active.count { UrgencyRules.isToday(it, state.now) }
val dangerCount = active.count { UrgencyRules.classify(it, state.now, state.settings.urgentMinutes) == Urgency.DANGER }
```

输入是 ViewModel 中的全部事项和当前时间；先去掉已完成／归档事项，再计算两类数量。搜索、单日选择只缩小下面的结果列表，不会改这段全局统计。因此“搜索只剩一条，今日 DDL 仍为两条”可能是正确行为，不能把两个范围混为一谈。

**老师问到时可答：** “首页统计直接来自全局业务状态；筛选只是选择查看范围，不修改事项，也不改全局风险判断。”

**对应回归：** [SaverAppFlowTest.kt](../../app/src/test/java/cn/edu/hust/ddlsaver/ui/SaverAppFlowTest.kt) L347 的 `combinedSourceTagsKeywordAndDateFilterTheVisibleListWithoutChangingTodayCount`。本轮新实机重点验证列表与日期格，未重新验证所有全局统计边界。

## D02 事项编辑、分类字段与保存

**演示对应（00:20—01:20）：** 打开预建 A，只编辑一行描述并保存；来源、标签和自定义字段直接展示已有内容，不现场重建分类或完整事项。

| 源文件地址 | 函数／位置 | 负责的行为 |
| --- | --- | --- |
| [app/src/main/java/cn/edu/hust/ddlsaver/ui/EventEditor.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/ui/EventEditor.kt)，L78—123、L207—225 | `EventEditor`；[草稿与修改判断](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/ui/EventEditor.kt#L78-L123)、[保存入口](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/ui/EventEditor.kt#L207-L225) | 以 session 保存草稿，保留打开时 revision；把本地日期时间转成北京时间对应的时间戳，创建 `EventDraft` |
| [app/src/main/java/cn/edu/hust/ddlsaver/ui/EditorExtras.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/ui/EditorExtras.kt)，L50—99 | `EventCategories`／`CustomFieldEditor`；[固定代码](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/ui/EditorExtras.kt#L50-L99) | 选择来源／标签、添加移除字段、更新编辑草稿 |
| [app/src/main/java/cn/edu/hust/ddlsaver/domain/EventValidation.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/domain/EventValidation.kt)，L7—54 | `normalize`；[固定代码](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/domain/EventValidation.kt#L7-L54) | 去标题首尾空白、校验 revision／分钟数／字段／附件，归一化截止时间 |
| [app/src/main/java/cn/edu/hust/ddlsaver/ui/SaverViewModel.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/ui/SaverViewModel.kt)，L178—184、L231—254 | `save`／`perform`；[保存](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/ui/SaverViewModel.kt#L178-L184)、[操作反馈](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/ui/SaverViewModel.kt#L231-L254) | 异步保存、防止重复提交、返回保存 ID／revision、显示错误而保留草稿 |
| [app/src/main/java/cn/edu/hust/ddlsaver/data/RoomEventRepository.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/data/RoomEventRepository.kt)，L34—94、L240—244 | `save`／`validateRelations`／`reminderTransaction`；[保存事务](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/data/RoomEventRepository.kt#L34-L94)、[事务与提醒](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/data/RoomEventRepository.kt#L240-L244) | 插入或更新主表，保存关联字段，校验目录和图片归属，同事务重建提醒计划 |
| [app/src/main/java/cn/edu/hust/ddlsaver/ui/SaverApp.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/ui/SaverApp.kt)，L141—176 | `LaunchedEffect(state.actionResult, state.events)`；[固定代码](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/ui/SaverApp.kt#L141-L176) | 等待观察结果包含新 revision，然后进入详情并提示“事项已保存” |

讲解顺序是：**控件输入 → 草稿 → 校验 → Repository 事务 → Room Flow → ViewModel 状态 → 新详情**。输入时修改的是草稿；点击“保存事项”才进入持久化流程。返回时若有修改，页面先询问是否放弃，不能把自动保留页面草稿解释为数据库已保存。

`RoomEventRepository.kt` L38—41 的冲突检查：

```kotlin
val previous = normalized.id?.let { dao.findEvent(it) ?: throw EventNotFoundException() }
if (previous != null && normalized.expectedRevision != previous.revision) throw EventConflictException()
validateRelations(id, normalized, previous)
```

新建时生成 UUID；修改时必须仍存在并且 revision 匹配。校验通过后更新 `events`，重写该事项的标签关系、字段和附件元数据。若中途失败，外层事务回滚，不会留下“标题更新了但字段只写一半”的状态。

同文件 L240—244：

```kotlin
private suspend fun <T> reminderTransaction(action: suspend () -> T): T = database.withTransaction {
    val result = action()
    ReminderPlanStore.rebuild(dao, clock.millis())
    result
}
```

这里把业务变更与提醒计划生成作为一个数据库提交单元；真正向 Android 排 Alarm／发通知由后续校对负责。UI 保存成功和系统提醒暂时失败可以分别显示，不能让系统权限错误伪装成“事项没保存”。

来源／标签使用目录 ID 建立关联；同一事项可同时有多个标签，自定义字段按事项独立保存。字段值既用于详情展示，也能用于 D04 搜索。停用分类会限制新关联，但旧关联可保留，具体逻辑在 `validateRelations`；这不等于删除所有使用该分类的事项。

**老师问到时可答：** “保存是一个经过校验的事务；revision 防止旧草稿覆盖新数据，Flow 让页面跟随提交后的结果更新。”

**对应回归：** `SaverAppFlowTest.kt` L122 必填校验与编辑、L157 旧草稿冲突、L182 写入失败后重试、L283 分类和字段持久化。该页中的正向分类操作是可预演扩展，本轮真机来源验证仅覆盖空交集。

## D03 图片附件与详情

**演示对应（01:20—01:45）：** 在 A 的详情打开提前保存好的合成图片，查看后返回；不在这 5 分钟内重新选择、导入或删除图片。

| 源文件地址 | 函数／位置 | 负责的行为 |
| --- | --- | --- |
| [app/src/main/java/cn/edu/hust/ddlsaver/ui/AttachmentUi.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/ui/AttachmentUi.kt)，L58—104、L107 起 | `AttachmentEditor`／`AttachmentGallery`；[导入入口](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/ui/AttachmentUi.kt#L58-L104) | 调系统图片选择器或读取主动粘贴的图片 URI；展示附件和预览入口 |
| [app/src/main/java/cn/edu/hust/ddlsaver/platform/AttachmentStore.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/platform/AttachmentStore.kt)，L27—65、L135—208 | `importImages`／`importOne`；[批量导入与回滚](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/platform/AttachmentStore.kt#L27-L65)、[私有副本](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/platform/AttachmentStore.kt#L135-L208) | IO 线程读取、大小／像素／解码验证、临时文件转正式副本、失败回滚 |
| 同 `AttachmentStore.kt`，L102—132、L240—280 | `loadPreview`／`checkedFile`／`rollback`；[预览](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/platform/AttachmentStore.kt#L102-L132)、[路径及回滚](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/platform/AttachmentStore.kt#L240-L280) | 采样预览、校验自有路径、清理本次创建的失败副本 |
| [app/src/main/java/cn/edu/hust/ddlsaver/ui/SaverViewModel.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/ui/SaverViewModel.kt)，L119—169、L178—183、L224—230 | 草稿附件生命周期；[固定代码](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/ui/SaverViewModel.kt#L119-L183) | 保留正在编辑的候选文件；保存／取消后只清理不再被数据库引用的自有文件 |

`AttachmentStore.kt` L149—152 附近创建自己的文件标识：

```kotlin
val id = UUID.randomUUID().toString()
val temporary = checkedFile("$id.part", allowTemporary = true)
val destination = checkedFile("$id.img")
```

照片选择器给的是外部 URI，它的访问许可可能是临时的，所以不能只把 URI 记在数据库就认为永远能打开。这里通过 `ContentResolver` 读入，复制到 `files/attachments`；先验证 `.part` 文件，成功后改名为 `.img`，返回 `AttachmentRef`。文件扩展名是应用自己的管理命名，实际图片类型仍由解码结果记录。

每张图片最多 20 MiB、1 亿像素；不是只检查 MIME 声明，还会实际做小尺寸解码验证。预览按最大边 1600 进行采样并处理 EXIF 方向；文件名必须符合自有 UUID 命名，规范路径必须仍在私有附件目录内，拒绝符号链接越界。

**“已导入草稿”与“已保存到事项”是两步。** 导入成功后先显示草稿缩略图，点击保存才写附件元数据。取消编辑不会删除原有附件；保存移除附件的修改或删除事项后，按数据库引用清理私有副本，相册原图不受影响。多个图片中途导入失败会回滚本次已创建文件，保留原草稿与既有附件。

**老师问到时可答：** “我们保存的是经过验证的私有副本，数据库存它的元数据；草稿文件和已保存文件的生命周期分别管理。”

**边界与证据：** 图片网址文字不是图片附件；OBS-01 的旧来源许可失效仍开放。应在演示前从相册导入合成图并保存，主线只打开已有附件。源码测试见 [AttachmentStoreTest.kt](../../app/src/test/java/cn/edu/hust/ddlsaver/platform/AttachmentStoreTest.kt)、[AttachmentClipboardFlowTest.kt](../../app/src/test/java/cn/edu/hust/ddlsaver/ui/AttachmentClipboardFlowTest.kt)，页面取消／移除见 `SaverAppFlowTest.kt` L554、L572。0.3.4 的图片实机分项不被本轮日历测试重新记为新覆盖。

## D04 关键词与分类组合筛选

**演示对应（01:45—02:15）：** 在样本前缀 P 的 3 条结果中，把搜索框内容替换为 A 的完整连续标题 `P + " 软件工程报告"`，看到 3→1；再恢复为前缀 P，看到 1→3。字段搜索、来源和多标签组合仅在老师要求且已提前预演时追加。

入口是 [app/src/main/java/cn/edu/hust/ddlsaver/domain/EventQuery.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/domain/EventQuery.kt) L18—43 的 `select`，[固定代码](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/domain/EventQuery.kt#L18-L43)。首页控件修改 `query`、`sourceId`、标签 ID 集合，再调用它；控件位于 [HomeScreen.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/ui/HomeScreen.kt) L121—159。

核心条件节选：

```kotlin
(filter.sourceId == null || event.source?.id == filter.sourceId) &&
event.tags.map { it.id }.toSet().containsAll(filter.tagIds) &&
(filter.date == null || Instant.ofEpochMilli(event.deadlineAt)
    .atZone(UrgencyRules.BEIJING_ZONE).toLocalDate() == filter.date) &&
(!filter.dangerOnly || UrgencyRules.classify(event, now, settings.urgentMinutes) == Urgency.DANGER) &&
(keyword.isEmpty() || event.title.contains(keyword, ignoreCase = true) ||
    event.description.contains(keyword, ignoreCase = true) ||
    event.customFields.any {
        it.label.contains(keyword, ignoreCase = true) || it.value.contains(keyword, ignoreCase = true)
    })
```

状态、来源、所选标签、日期、危险期、关键词这些条件总体上取交集；关键词内部的标题／描述／字段名／字段值取“任一命中”。多标签采用 `containsAll`，所以选了两个标签必须同时包含两个，不是任意一个。最终按截止时间升序、相同截止按 ID 排序。

**搜索是去首尾空白后的整段、不区分大小写的连续子串搜索。** 输入“软件 报告”要求某个可搜索文本内存在这段连续内容；代码没有把空格分词后做 AND，也没有做模糊搜索。来源／标签名称不自动纳入关键词搜索，它们有单独筛选控件。主线使用 A 的完整标题，以免误把两个不连续的词当作 AND；若老师追问字段搜索，可用准备好的唯一字段 token 追加验证。

**老师问到时可答：** “这是可组合的内存查询：不同筛选维度相与，文本搜索域相或，多标签要求全部包含；结果不改变原数据。”

**对应回归：** [EventQueryTest.kt](../../app/src/test/java/cn/edu/hust/ddlsaver/domain/EventQueryTest.kt) L42、L51、L88 分别覆盖文本域、连续多词及稳定排序；页面组合用例见 `SaverAppFlowTest.kt` L347。实机本批有关键词细化／恢复和来源空交集 4→0→4，不宣称正向多标签实机组合已覆盖。

## D05 月周日历、计数与折叠

**演示对应（02:15—03:40）：** 保留前缀 P，月格核对样本日 S 为 2、S+1 为 1；选 S 显示 2 条，再切周视图，折叠／展开时范围保持；点 S 回到日视图的 2 条，最后回月视图并保留 S。S 为周日时次日属于下一周，当前周应为 2 条，其余同周情形为 3 条；只要求折叠前后相同。“今天”、跨年和其他导航不加入主线，老师要求时再追加。

| 源文件地址 | 函数／位置 | 负责的行为 |
| --- | --- | --- |
| [app/src/main/java/cn/edu/hust/ddlsaver/ui/HomeScreen.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/ui/HomeScreen.kt)，L34—44 | `rememberSaveable` 的筛选状态；[固定代码](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/ui/HomeScreen.kt#L34-L44) | 保存列表模式、查询、面板展开、月／周／日模式、锚点和单日选择 |
| 同文件 L59—74 | 列表日期范围与日期格分布；[固定代码](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/ui/HomeScreen.kt#L59-L74) | 周范围过滤、按日查询、独立计算日期格数量 |
| 同文件 L160—188、L249—280 | 模式按钮及 `CalendarPeriod`；[模式切换](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/ui/HomeScreen.kt#L160-L188)、[周日组件](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/ui/HomeScreen.kt#L249-L280) | 周一为起点，每次导航 7 天／1 天，点击某天进入日视图，“今天”重置锚点 |
| 同文件 L295—336 | `MonthCalendar`／`calendarWeeks`；[固定代码](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/ui/HomeScreen.kt#L295-L336) | 生成月份网格，首周留空，补齐 7 列，展示每个日期的数量 |

列表的周范围在 L61—68：

```kotlin
val inWeekView = calendarView == "week" && quickFilter == "all"
val dateFilter = if (quickFilter == "today") today else if (inWeekView) null else selectedDate?.let(LocalDate::parse)
val filtered = EventQuery.select(state.events, EventFilter(mode = mode, keyword = query, sourceId = sourceId,
    tagIds = tagIds, date = dateFilter, dangerOnly = quickFilter == "danger"), state.now, state.settings)
    .filter { event ->
        val date = java.time.Instant.ofEpochMilli(event.deadlineAt).atZone(BeijingZone).toLocalDate()
        !inWeekView || (date >= weekStart && date < weekStart.plusWeeks(1))
    }
```

周视图先不要把查询限定为某一个单日，再保留 `[周一, 下周一)` 范围；使用 `LocalDate` 加周／加日，跨月和跨年不需要自己拼日期字符串。不是所有逾期事项无条件加入当前周：先按条件过滤，再把符合结果的逾期待办单独分组显示。

日期格用另一份查询，L70—74：

```kotlin
val calendarEvents = EventQuery.select(state.events, EventFilter(mode = mode, keyword = query,
    sourceId = sourceId, tagIds = tagIds, dangerOnly = quickFilter == "danger"), state.now, state.settings)
val calendarCounts = calendarEvents.groupingBy {
    java.time.Instant.ofEpochMilli(it.deadlineAt).atZone(BeijingZone).toLocalDate()
}.eachCount()
```

这里保留关键词、来源、标签、列表类型和危险期条件，但没有传入当前单日 `date`。否则点了今天，明天的格子就会错误变成 0。`groupingBy(...).eachCount()` 再按北京时间日期统计数量，月份／周组件只读取对应日期的结果。

**本轮修复的核心：** 原判断把 `calendarOpen` 也放进 `inWeekView`；收起面板会导致周条件失效，并落回锚点单日。本版 L61 移除这个依赖，让“是否显示面板”只影响展示，“当前查询周期”独立保持。这是状态职责分离的具体例子，不是靠保存一份额外事项列表修补。

**老师问到时可答：** “日历是对已有事项的不同投影。选日列表、整月日期分布与全局今日统计分别计算；周历的查询语义不随面板收起而变化。”

**对应证据：** [SaverAppFlowTest.kt L423—468](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/test/java/cn/edu/hust/ddlsaver/ui/SaverAppFlowTest.kt#L423-L468) 的新回归先在旧条件失败、修复后通过；[CalendarGridTest.kt](../../app/src/test/java/cn/edu/hust/ddlsaver/ui/CalendarGridTest.kt) 覆盖网格。真机已验证周 3→收起 3→直接展开 3、同日 2／次日 1、跨年周和向前日导航；未据此宣称反向跨年、1 月月份网格或独立冷启动日历偏好全部通过。

## D06 完成、恢复与归档

**演示对应（03:40—04:30）：** 打开 B 标记完成，返回月视图核对 S 的待办从 2 变 1；切到“已完成”找到 B。保留前缀 P 和日期 S 时摘要可能仍是“S 的事项 · 1”，以“已完成”选中和 B 出现为准。主线让 B 保持已完成，不立即恢复；04:30—04:40 用一句功能总结结束，余下 20 秒留作缓冲。恢复在会后重置样本时进行，归档／恢复展示仅按老师要求追加。

按钮在 [app/src/main/java/cn/edu/hust/ddlsaver/ui/SaverApp.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/ui/SaverApp.kt) 的 `DetailScreen` L432—439，[固定代码](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/ui/SaverApp.kt#L432-L439)。回调经 [SaverViewModel.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/ui/SaverViewModel.kt) L185—190，到 [RoomEventRepository.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/data/RoomEventRepository.kt) L96—120 的 `setCompleted`／`setArchived`，[固定代码](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/data/RoomEventRepository.kt#L96-L120)。

完成状态的更新节选：

```kotlin
if ((row.completedAt != null) != completed) {
    val now = clock.millis()
    updateExisting(row.copy(
        completedAt = if (completed) now else null,
        updatedAt = now, revision = Math.addExact(row.revision, 1L),
    ))
}
```

完成用非空 `completedAt` 表达，恢复则置空，不删除事项。归档另用 `archivedAt`；归档一个已完成事项不会把完成状态清空，恢复归档后它仍属于已完成列表。`EventQuery` 中归档列表独立判断，已完成列表排除归档记录。

状态改变在 `reminderTransaction` 内执行，所以对应截止前计划随业务状态失效；`ReminderPlanStore.rebuild` L28—34 设置 `active=false` 和 `cancelPending=true`，随后 `ReminderEngine.cancelInvalidNotifications` L86—103 撤销不再需要的系统通知。取消完成后历史计划可以仍是 `PENDING` 或 `SENT`，但 inactive；**“历史行还在”不等于还会提醒**。稳定历史键用于去重和取消审计，不应该为了清理演示数据直接删账本。

恢复未完成会重新按当前时间／设置评估计划；已经发过且属于同一稳定业务键的记录仍遵守去重，不承诺恢复后立即重复弹一条通知。

**老师问到时可答：** “完成、归档和删除是三种不同操作。完成／归档是可恢复状态，删除才是移除记录；状态变化和提醒计划在同一数据库事务中联动。”

**对应回归：** `SaverAppFlowTest.kt` L219 的完成恢复与删除取消、L523 的归档保留完成标记；[ReminderEngineTest.kt](../../app/src/test/java/cn/edu/hust/ddlsaver/reminders/ReminderEngineTest.kt) L130、L143、L155 的已送达通知失效。本轮手机实际验证 B 完成后今日待办 2→1、已完成 1；恢复／归档流程应在演示前预演，不能冒充本轮新实测。

## D07 角色形象与本地反馈

**演示对应（追加）：** 前 5 分钟仅在 D01 顺带指认首页现有角色，不进入设置。老师要求时，再预览经典／Q 版、保存后回首页，并按实际状态说明短暂完成反馈；不要为等待反馈额外占用主线时间。

| 源文件地址 | 函数／位置 | 负责的行为 |
| --- | --- | --- |
| [app/src/main/java/cn/edu/hust/ddlsaver/ui/ReminderSettingsPanel.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/ui/ReminderSettingsPanel.kt)，L55—90、L129—141 | `SettingsScreen`；[草稿与预览](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/ui/ReminderSettingsPanel.kt#L55-L90)、[保存](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/ui/ReminderSettingsPanel.kt#L129-L141) | 预览只是设置草稿，点击“保存设置”才提交样式 |
| [app/src/main/java/cn/edu/hust/ddlsaver/domain/CharacterStyle.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/domain/CharacterStyle.kt)，L4—11 | `CharacterStyle`；[固定代码](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/domain/CharacterStyle.kt#L4-L11) | CLASSIC／CHIBI 枚举；未知持久值回退 CLASSIC |
| [app/src/main/java/cn/edu/hust/ddlsaver/domain/CharacterFeedback.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/domain/CharacterFeedback.kt)，L18—63 | `selectCharacterFeedback`；[固定代码](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/domain/CharacterFeedback.kt#L18-L63) | 从全局有效事项选择焦点、状态和本地台词 |
| [app/src/main/java/cn/edu/hust/ddlsaver/ui/CharacterPanel.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/ui/CharacterPanel.kt)，L45—75、L121—146 | `characterArtwork`／`CharacterArtwork`／`CharacterPanel`；[素材映射](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/ui/CharacterPanel.kt#L45-L75)、[绘制](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/ui/CharacterPanel.kt#L121-L146) | 把状态和样式映射到本地图片／精灵图区域并显示 |

角色焦点按危险、逾期、临近、正常优先级选择；同类取最早截止，再按 ID 稳定排序。注意这段“多个事项之间挑一个焦点”的优先级，与 `UrgencyRules.classify` 判断单个事项状态是两个层次。

本地台词的选择在 `CharacterFeedback.kt` L47—50：

```kotlin
val bucket = Math.floorDiv(now.epochSecond, 600L)
val seed = (if (justCompleted) "completed" else focus?.first?.id ?: "empty").hashCode().toLong()
val line = lines[Math.floorMod(bucket + seed, lines.size.toLong()).toInt()]
```

它用十分钟时间段和事项 ID 从预设列表里稳定选句，没有网络请求或语言模型。页面传入的是 `state.events`，因此用户搜索到空列表时角色仍可能提醒全局其他事项。

`SaverViewModel.perform` L236—242 在成功完成后把 `justCompleted` 短暂设为 true，约 5 秒后恢复；不会因为数据库里有历史已完成事项就一直欢呼。角色样式是 `settings.characterStyle`，与业务状态分开；换图片不会改变截止时间、重要程度或提醒规则。

**老师问到时可答：** “形象是可保存的显示偏好，表情和台词由本地业务状态规则驱动，二者独立。它是状态反馈，不是在线 AI 聊天。”

**对应回归：** `SaverAppFlowTest.kt` L631 的预览／保存／重建后切回；[CharacterStylePersistenceTest.kt](../../app/src/test/java/cn/edu/hust/ddlsaver/data/CharacterStylePersistenceTest.kt) L27、L76 覆盖迁移与保存重开；[CharacterFeedbackTest.kt](../../app/src/test/java/cn/edu/hust/ddlsaver/domain/CharacterFeedbackTest.kt) 验证选择规则。素材来源与许可见[角色素材说明](../character-assets.md)，不要把外部许可素材声称为原创绘制。

## D08 通知提醒（可选）

**演示对应（追加）：** 仅在老师要求且提前预演成功时，正常打开 App、检查通知能力，为独立 `DEMO-NOTIFY` 样本安排将来的一次提醒并观察通知／点击进入事项。不加入前 5 分钟，不与主要日历样本混用，不依赖重启自动恢复。

| 源文件地址 | 函数／位置 | 负责的行为 |
| --- | --- | --- |
| [app/src/main/java/cn/edu/hust/ddlsaver/reminders/ReminderModels.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/reminders/ReminderModels.kt)，L67—118 | `ReminderRules.eventPlans`／`dailyPlan`／`nextWake`；[固定代码](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/reminders/ReminderModels.kt#L67-L118) | 每日汇总、速通危险、临近截止三种规则；稳定键及最早唤醒时间 |
| [app/src/main/java/cn/edu/hust/ddlsaver/reminders/ReminderPlanStore.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/reminders/ReminderPlanStore.kt)，L11—58 | `rebuild`；[固定代码](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/reminders/ReminderPlanStore.kt#L11-L58) | 从当前事项／设置重新计算计划，失效取消，保留已发送账本 |
| [app/src/main/java/cn/edu/hust/ddlsaver/reminders/ReminderEngine.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/reminders/ReminderEngine.kt)，L33—78、L105—136 | `reconcile`／`deliver`；[校对](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/reminders/ReminderEngine.kt#L33-L78)、[发送状态](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/reminders/ReminderEngine.kt#L105-L136) | 串行取消失效通知、处理到期计划、持久化发送状态并安排下一次 Alarm |
| [app/src/main/java/cn/edu/hust/ddlsaver/reminders/AndroidReminderPlatform.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/reminders/AndroidReminderPlatform.kt)，L28—34、L46—88 | `capabilities`／`scheduleAlarm`／`post`；[权限](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/reminders/AndroidReminderPlatform.kt#L28-L34)、[系统调用](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/reminders/AndroidReminderPlatform.kt#L46-L88) | 实际检查权限／通道，精确或近似安排，创建通知和点击 Intent |
| [app/src/main/java/cn/edu/hust/ddlsaver/reminders/ReminderRuntime.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/reminders/ReminderRuntime.kt)，L22—46、L70—115 | `requestReconcile`、两个 Receiver；[应用级运行器](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/reminders/ReminderRuntime.kt#L22-L46)、[回调](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/reminders/ReminderRuntime.kt#L70-L115) | 合并校对请求，接收系统唤醒／恢复广播，执行有时限的异步校对 |

事项提醒键的构造在 `ReminderModels.kt` L84—93：

```kotlin
private fun eventPlan(event: DdlEvent, kind: ReminderKind, minutes: Long): ReminderPlanEntity {
    val duration = Math.multiplyExact(minutes, 60_000L)
    val trigger = try { Math.subtractExact(event.deadlineAt, duration) }
    catch (_: ArithmeticException) { Long.MIN_VALUE }
    return ReminderPlanEntity(
        key = "${kind.name}:${event.id}:${event.deadlineAt}", kind = kind.name,
        eventId = event.id, deadlineAt = event.deadlineAt, eventRevision = event.revision,
        triggerAt = trigger, day = null, notificationTag = "event:${event.id}:${event.deadlineAt}",
    )
}
```

解释这段时先区分：速通危险提醒时间＝截止减速通分钟；临近截止提醒时间＝截止减设置的提前分钟。界面普通“临近”颜色使用 `urgentMinutes`，不等于通知用的 `deadlineLeadMinutes`。同一事项／截止／触发时刻的两个原因可合成一条通知，但保留两个计划键。

校对流程是：先从数据库重建有效计划 → 取消已失效通知 → 若可通知则处理到期计划 → 再计算最早未处理时间 → 用一个固定身份的 PendingIntent 安排 Alarm。权限关闭时业务事项和计划仍存在，但系统唤醒会暂停；恢复能力后再校对，而不是不断唤醒却无法显示通知。

`canPost` 同时检查 Android 13+ 通知权限、App 总开关和提醒通道；精确 Alarm 能力单独检查。无精确能力时降级 `setAndAllowWhileIdle`，不能承诺精确到秒。系统调用后记录实际模式／错误，必要时重试。

点击通知的链路是 `AndroidReminderPlatform.post` L67—74 的 `OPEN_REMINDER` Intent → `MainActivity.handleReminderIntent` L58—63 → `SaverApp.openReminder` L102—118。按当前 ID 找最新事项，已删除则回首页解释；事项编辑、设置或分类管理页面会保护当前输入，先显示“查看通知中的事项？”确认，允许稍后查看或放弃当前编辑，见 `SaverApp` L119—124 与 L304—318。演示若要观察点击通知直接进入详情，应先回 App 正常首页，再按系统 Home 键切到后台。

**为什么不能宣称严格一次送达：** `deliver` 先独立持久化 CLAIMED，再复核当前数据、调用系统、写 SENT；系统通知与数据库提交之间仍有中断窗口。稳定 tag、`setOnlyAlertOnce`、尝试时间与重试减少重复影响，但不能把本地事务说成覆盖 Android 通知服务的分布式事务。并且空的每日汇总可以被标为已处理而不调用通知，SENT 不应在所有上下文中简单等同于“用户已看到弹窗”。

**老师问到时可答：** “系统 Alarm 只是唤醒信号，持久化的事项和账本才决定这条提醒现在是否有效；因此旧回调来了，也必须重查数据库。”

**对应回归与限制：** `ReminderEngineTest.kt` L52 同事务回滚、L82 同刻合并、L110 失效事项、L321 权限恢复、L338 系统发送后提交中断；[ReminderColdLaunchTest.kt](../../app/src/test/java/cn/edu/hust/ddlsaver/ui/ReminderColdLaunchTest.kt) 覆盖通知冷启动路由。真实锁屏、自然 Doze、重启和 OEM 调度要分别实测；OBS-02 仍开放，不能用这些自动测试代替其结论。

## D09 批量操作（可选）

**演示对应（追加）：** 仅在老师要求时，对独立演示样本勾选批量完成／归档；不加入前 5 分钟。若展示删除，先展示“取消”保持数据，真正删除留到收尾并核对选择。

三个入口：

- [app/src/main/java/cn/edu/hust/ddlsaver/ui/HomeScreen.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/ui/HomeScreen.kt) L199—216、L238—245：管理选中 ID、批量按钮、冻结删除确认；[固定代码](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/ui/HomeScreen.kt#L199-L245)。
- [app/src/main/java/cn/edu/hust/ddlsaver/ui/SaverViewModel.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/ui/SaverViewModel.kt) L196—205：`applyBatch` 拷贝 ID 集合、保存附件候选，调用 Repository；[固定代码](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/ui/SaverViewModel.kt#L196-L205)。
- [app/src/main/java/cn/edu/hust/ddlsaver/data/RoomEventRepository.kt](../../app/src/main/java/cn/edu/hust/ddlsaver/data/RoomEventRepository.kt) L128—148：`applyBatch` 先验证完整选择，再在事务中变更；[固定代码](https://github.com/zidodo1124/hust26_software_engineering_homework/blob/demo-v0.3.5/app/src/main/java/cn/edu/hust/ddlsaver/data/RoomEventRepository.kt#L128-L148)。

Repository 的关键检查：

```kotlin
val selected = ids.toSet()
if (selected.isEmpty()) return
reminderTransaction {
    // Resolve the entire frozen selection before the first mutation; stale selections roll back.
    val rows = dao.findEvents(selected)
    if (rows.size != selected.size) throw EventNotFoundException()
```

用户点按钮时先冻结所选集合；事务中先确定全部事项仍存在，再逐一变更。某条已消失或写入失败时不能出现一部分完成、一部分没完成却提示整体成功。筛选切换不会自动清空已选集合，所以演示时必须读“已选 N 件”，不要以当前可见几条来猜实际选择范围。

批量操作成功后的退出是另一条明确的状态链：`SaverViewModel.applyBatch` L200 使用 `perform("batch")` 发出操作结果，`SaverApp` L150 更新 `batchToken`，`HomeScreen` L49—50 的 `LaunchedEffect(batchToken)` 自动退出批量模式并清空选择。切换／清除筛选和批量提交成功是不同触发，前者保留选择，后者清空选择。

批量永久删除会删除事项及级联业务关联，但保留提醒历史来完成取消与去重。图片副本由 ViewModel 在提交后按数据库引用清理；系统相册原图不删除。

**老师问到时可答：** “批量操作冻结用户选择，并在一个事务中验证和提交。界面上的筛选范围与已选择集合是两个状态，删除确认必须使用冻结的集合。”

**对应回归：** `SaverAppFlowTest.kt` L499 的批量删除取消与确认；[RoundTwoRepositoryTest.kt](../../app/src/test/java/cn/edu/hust/ddlsaver/data/RoundTwoRepositoryTest.kt) 包含批量失效／回滚等存储用例。本轮日历实机收尾是逐条删除 5 个 QA，不是新执行了一轮批量删除验收。

## 老师追问总体实现时，可用这段回答

“演示时所有按钮最终都落在明确的数据或状态操作上。编辑先形成草稿，保存进入 Repository 事务；数据库 Flow 更新 ViewModel，Compose 再按新状态显示。搜索和日历只改变查看范围；完成／归档会改变业务记录和提醒计划。图片使用独立的私有副本生命周期，系统通知则由持久账本校对。这样我们能分别测试纯规则、存储事务、页面交互和真实手机条件，也能说明每一种测试没有覆盖什么。”
