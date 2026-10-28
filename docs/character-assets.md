# 角色素材与许可调研记录

- 项目：HUSTer's DDL Saver
- 查阅日期：2026-10-27；Q 版资源补充核对：2026-10-28
- 状态：保留第三轮四张非官方鲸鱼娘立绘，新增一张可选 Q 版原始图集，完成逐文件来源、校验与视觉检查；实际构建和界面验证另见测试记录。
- 范围：角色来源、美术许可、品牌边界及本项目的状态与台词映射。

## 1. 当前结论

**当前资源方案：** 角色显示名称为“蓝色大肥鱼”，经典立绘与 Q 版作为两种外观并存。[Q 版素材选择](character-q-candidates.md)记录本次新增图集，第 8 节记录其许可与显示方式。下文第 2—7 节保留 v0.3.0 经典立绘的来源与历史核对结果；v0.3.0 真机测试截图不作为新增 Q 版的验证依据。

本项目的非商业课程 App 采用经核对的四张非官方鲸鱼娘插画，全部固定到上游提交 `046b848f244450e6e636335766276bf3343fe84b`。图片按原字节复制，未裁切、重绘、重新压缩或移除元数据；普通、快乐、严肃和生气四种表情用于本地状态反馈。
“蓝色大肥鱼”“DeepSeek 娘”不能唯一指向一张图片，也不能据此认定为 DeepSeek 官方角色。
用户未指定具体原始发布链接，并允许在核对可用后采用鲸鱼娘；第三轮实施选择下述可追溯文件，用户仍可审核实际画面和台词风格。许可结论不自动套用到其他网图、截图或表情包。

## 2. 原作者与许可证据

| 证据 | 本次实际核实内容 | 适用边界 |
| --- | --- | --- |
| [上善无形：鲸鱼娘形象动态](https://www.bilibili.com/opus/1231977657712771073) | 作者本人将该内容按 CC BY-NC-SA 4.0 发布。 | 支持该原始角色内容的许可判断，不为全网同名图片授权。 |
| [ZipZipPipe：有关大肥鱼玩偶的重要提醒](https://www.bilibili.com/opus/1247156596114456581) | 作者本人说明大肥鱼形象遵循 CC BY-NC-SA 4.0，并反对商业化。 | 结合具体素材的来源与许可记录使用，不能据此搬运其全部作品。 |
| [Small-tailqwq：上游立绘 NOTICE](https://github.com/Small-tailqwq/dsh-deep-whale/blob/main/maid-atelier/NOTICE) | 记录原角色、女仆二次设计及该项目素材的创作链，区分代码与美术许可。 | 该项目的美术素材有独立许可，不随代码的 MIT 许可处理。 |
| [JAdpp：逐文件素材 NOTICE](https://github.com/JAdpp/dsh-whale-galgame/blob/main/NOTICE.md) | 列出默认立绘、八种表情及各文件的来源、修改与许可。 | 下面候选依据其逐文件声明判断；不是对仓库所有资源的统一授权。 |

完整署名链：**上善（上善无形，原角色）→ ZipZipPipe（加入 DeepSeek 元素的女仆设计）→ Small-tailqwq / dsh-deep-whale（上游立绘）→ JAdpp / dsh-whale-galgame（新增表情）**。
默认立绘保留其对应的上游署名，使用新增表情时再保留表情制作者记录；具体写法随文件清单保存。

## 3. 实际采用的四张图片

以下四个文件已下载并逐张视觉检查。固定版本的逐文件来源、字节数和 SHA-256 见[资源清单](../app/src/main/assets/character/sources.json)，本项目署名见[素材 NOTICE](../app/src/main/assets/character/NOTICE.md)。原发布者的[完整声明](../app/src/main/assets/character/upstream-NOTICE.md)、[上游声明](../app/src/main/assets/character/dsh-deep-whale-NOTICE.txt)和[CC 完整许可](../app/src/main/assets/character/dsh-deep-whale-LICENSE.txt)随仓库及安装包保留。

| 候选文件 | 发布者描述 | 本项目可考虑的用途 |
| --- | --- | --- |
| [maid-left.webp](https://github.com/JAdpp/dsh-whale-galgame/blob/main/assets/default/maid-left.webp) | 默认鲸鱼娘立绘 | 普通状态 |
| [whale-cheerful.webp](https://github.com/JAdpp/dsh-whale-galgame/blob/main/assets/default/whale-cheerful.webp) | 快乐表情 | 完成事件后的鼓励 |
| [whale-serious.webp](https://github.com/JAdpp/dsh-whale-galgame/blob/main/assets/default/whale-serious.webp) | 严肃表情 | 临近截止或危险期提醒 |
| [whale-angry.webp](https://github.com/JAdpp/dsh-whale-galgame/blob/main/assets/default/whale-angry.webp) | 生气表情 | 催促提醒；措辞保持友好 |

用途对应是本项目的实现选择，不是素材作者规定的功能，也不代表已完成用户审美确认。
这些具体文件在发布者 NOTICE 中列为 CC BY-NC-SA 4.0；AI 辅助创作的许可仅及于发布者有权授权的部分。
实际视觉检查显示四图均为蓝发、鲸尾的女仆角色立绘，普通、快乐、严肃和生气表情明确；未见文字，围裙上均有鲸鱼徽记。该徽记属于原同人文件保留的 DeepSeek 元素，不据此宣称没有第三方标识，也不宣称已获 DeepSeek 官方授权。图片仅用于应用内角色插画，不作为本应用图标或产品品牌；角色卡片的“素材与署名”说明非官方关系、完整创作链和许可。

## 4. 课堂演示与公开分发分别判断

依据 [CC BY-NC-SA 4.0 中文法律文本](https://creativecommons.org/licenses/by-nc-sa/4.0/legalcode.zh-hans)，许可包含符合条件的非商业复制、分享和改编。
“非商业”只是条件之一；还需要满足署名、修改说明、许可保留及相同方式共享等适用要求。

| 使用情形 | 当前判断 | 采用前需要落实 |
| --- | --- | --- |
| 非商业课程 App、课堂演示 | 上述具体候选具有条件适用的许可依据。 | 核实所用文件确属相应许可范围，保留署名与许可，并保持实际用途非商业。 |
| 资源随 App 在公开 GitHub 仓库分发 | 这是独立的复制与向公众分享行为；CC 条款允许符合条件的非商业分享，并非只能课堂查看。 | 在仓库保留资源来源、对应许可、署名和修改记录；改编素材按相同或兼容许可发布，不把资源统称为 MIT 或“无版权”。 |
| 商业化、广告推广、收费或其他商业用途 | 不应沿用本记录的非商业适用结论。 | 依据实际用途及权利范围重新判断，必要时另取得相应许可。 |

公开放在 GitHub 上不自动等于商业使用，也不自动获得素材使用权；应判断具体用途与每个资源的许可。
已存在明确公共许可且用途在其范围内时，不必仅因公开分发就额外要求作者逐项书面同意。
代码与素材分别标注许可；不要以代码开源为由覆盖美术素材的原有条件。
CC 法律文本第二条明确不授予商标权，也不代表作者或品牌方对本项目背书。

## 5. ChatGPT 娘、Claude 娘作为次选

同一素材项目还列有 [GPT 启发立绘](https://github.com/JAdpp/dsh-whale-galgame/blob/main/assets/default/gpt-recursive-weaver-v7.webp) 和 [Claude 启发立绘](https://github.com/JAdpp/dsh-whale-galgame/blob/main/assets/default/claude-amber-manuscript-mediator-v5.webp)。
其 NOTICE 将两者列为非官方 AI 辅助作品并声明 CC BY-NC-SA 4.0；本项目尚未选择，也未核实到对应多表情套图。
鲸鱼娘的上游角色许可不能推广到任意 ChatGPT 娘或 Claude 娘；每张同人图仍需核对自己的作者与许可。

- [OpenAI Design Guidelines](https://openai.com/brand/)：规范名称与标识使用，限制修改标识、融入自己的品牌及暗示官方背书等行为。
- [Anthropic Trademark Guidelines](https://www.anthropic.com/legal/trademark-guidelines)：要求商标按其具体许可及获批材料使用，不得擅自改动或暗示官方关系。

上述品牌指南不能代替第三方插画作者的授权；插画许可也不能代替品牌权利方的许可。
本项目不据此宣称存在官方“ChatGPT 娘”“Claude 娘”，也不宣称已获得品牌合作或背书。

## 6. 本地状态、表情与台词

状态复用 `UrgencyRules`，排除已完成和归档事项。在全局有效事项中依次显示“危险期 → 逾期 → 普通紧急 → 普通”；同类事项按截止时间及 ID 稳定选取，优先提示仍可处理的危险期事项。成功完成操作可由页面明确触发约 5 秒快乐反馈，随后回到实际状态；历史中已有完成记录不会自行触发庆祝。

| 状态 | 表情文件 | 本项目台词方向 |
| --- | --- | --- |
| 无待办／普通 | `maid-left.webp` | 休息、拆分任务、预留检查时间 |
| 普通紧急 | `whale-serious.webp` | 收尾、检查提交要求和附件 |
| 速通危险期 | `whale-angry.webp` | 友好催促、优先完成核心要求 |
| 已逾期 | `whale-serious.webp` | 核实补交、联系相关人员和整理事项 |
| 成功完成 | `whale-cheerful.webp` | 肯定实际进展、适当休息 |

共 36 条本项目编写的中文预设台词：无待办、普通、紧急、危险、逾期、完成各 6 条。台词随状态、事项 ID 与稳定的 10 分钟时间段选择，不使用随机数、联网请求或生成式对话；同一快照反复重组不会改变台词。角色图片在 IO 线程采样解码；资源不可读时仍显示状态和台词，信息不只依赖颜色或表情。图片使用、状态边界和离线解码的实际检查结论以第三轮测试记录为准。

## 7. 后续审核与边界

1. 用户审核这四张具体图片在手机上的效果、台词语气和卡片布局；不把代码实现写成用户已确认审美。
2. 后续替换图片时继续核对具体文件版本、画面内容、署名链、许可与修改记录；不能用转发站统一标签代替逐文件证据。
3. 如果用户指定另一张图，先找到它的原始发布与授权信息；不得直接套用本记录中的许可。
4. 若候选不符合期望或许可边界不适合，独立原创海洋主题角色可作备选，采用自己的名称与造型。

第三轮实际下载并原字节入库上述四张图片及许可材料，未生成新角色图片，也未联系作者。CC 许可按素材单独保留，不被应用代码的许可覆盖；未将 ChatGPT 娘或 Claude 娘加入安装包。

## 8. 可选 Q 版资源（2026-10-28）

新增 `character/chibi/spritesheet.webp`，来源为 [f0909172434/dsh-deepseek-girl-pet 固定提交](https://github.com/f0909172434/dsh-deepseek-girl-pet/tree/3e7df1d76014e4f43ba9ed3c9de53c2a62ba0d14)。图集为 8 × 11 格、1536 × 2288 像素、透明背景的 Q 版蓝发鲸尾女仆。实际查看后选用四个无牌子／无气泡文字的静态区域：普通正面、快乐、皱眉托腮、皱眉站立。每个区域为 192 × 208 像素，完整坐标见[资源清单](../app/src/main/assets/character/chibi/sources.json)。上游图片原字节保留；应用在运行时选择区域并执行下述透明排除，没有生成或分发修改后的图片资源。

2026-10-28 的 0.3.2 真机检查发现，认真表情格内部包含左侧残片；进一步逐帧检查确认，危险表情和快乐表情也分别含左侧尾部残片、左下细点。0.3.3 为这三个区域增加经逐图审阅的固定透明排除范围，普通表情不处理。排除位置以格内像素坐标登记在资源清单的 `runtimeExcludedRects`，右边界和下边界均不包含；危险表情采用分段窄区域，避免整条左裁伤及相邻头发。输出仍为 192 × 208，人物位置、缩放及所有排除区之外的原像素保持不变。此处理不补画原图内容，也不使用自动连通域删除。

对应检查逐像素比较保留区与原图，确认排除区透明，并使用原图中实际非透明残片及相邻头发作为回归点；API 28／35、构建与后续真机复验结果以[测试记录](test-records.md)为准。原图及上游许可的字节数、SHA-256 和创作链不变。

该仓库的逐图 [atlas-source.json](https://github.com/f0909172434/dsh-deepseek-girl-pet/blob/3e7df1d76014e4f43ba9ed3c9de53c2a62ba0d14/atlas-source.json)指定图集为 MIT，并追溯到同作者的原始仓库 `deepseek-girl-codex-pet`、提交 `2572709632b0e81401b9e92994f2c3786c186397`。所用图集哈希与记录一致，也与现有 JAdpp 逐文件 NOTICE 的对应项一致。完整许可、上游逐图记录及本项目[署名与边界](../app/src/main/assets/character/chibi/NOTICE.md)均随应用保存。

两套素材的许可分开处理：原四图继续按 CC BY-NC-SA 4.0 保留；Q 版作者对其有权授权的图集贡献声明 MIT。后者不覆盖底层原角色、第三方标志或其它图片，项目保留上善、ZipZipPipe 的角色署名与原有 CC 条件，不借 MIT 声明改变课程非商业使用范围。没有将整个角色、全部美术或应用统称为 MIT。

本次未运行任何桌宠程序、安装脚本或第三方代码，未将这些程序加入安装包，也未生成新插画。经典四图及其原始清单保持原字节不变。Q 版实际界面缩放、离线解码、外观选择与状态反馈的测试结果另见测试记录。
