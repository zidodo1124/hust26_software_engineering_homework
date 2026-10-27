# 角色素材与许可调研记录

- 项目：HUSTer's DDL Saver
- 查阅日期：2026-10-27
- 状态：已找到有明确许可记录的候选；尚未最终选定或下载任何图片。
- 范围：角色来源、美术许可与品牌边界；本记录不开展具体技术实现调研，也不作法律保证。

## 1. 当前结论

优先考虑经核对的非官方鲸鱼娘候选，用于非商业课程项目；落地时核对具体文件、图中标识、许可版本与署名。
“蓝色大肥鱼”“DeepSeek 娘”不能唯一指向一张图片，也不能据此认定为 DeepSeek 官方角色。
用户未指定具体原始发布链接；目前按下述可追溯候选推进，具体选图待后续确认，许可结论不自动套用到其他网图、截图或表情包。

## 2. 原作者与许可证据

| 证据 | 本次实际核实内容 | 适用边界 |
| --- | --- | --- |
| [上善无形：鲸鱼娘形象动态](https://www.bilibili.com/opus/1231977657712771073) | 作者本人将该内容按 CC BY-NC-SA 4.0 发布。 | 支持该原始角色内容的许可判断，不为全网同名图片授权。 |
| [ZipZipPipe：有关大肥鱼玩偶的重要提醒](https://www.bilibili.com/opus/1247156596114456581) | 作者本人说明大肥鱼形象遵循 CC BY-NC-SA 4.0，并反对商业化。 | 结合具体素材的来源与许可记录使用，不能据此搬运其全部作品。 |
| [Small-tailqwq：上游立绘 NOTICE](https://github.com/Small-tailqwq/dsh-deep-whale/blob/main/maid-atelier/NOTICE) | 记录原角色、女仆二次设计及该项目素材的创作链，区分代码与美术许可。 | 该项目的美术素材有独立许可，不随代码的 MIT 许可处理。 |
| [JAdpp：逐文件素材 NOTICE](https://github.com/JAdpp/dsh-whale-galgame/blob/main/NOTICE.md) | 列出默认立绘、八种表情及各文件的来源、修改与许可。 | 下面候选依据其逐文件声明判断；不是对仓库所有资源的统一授权。 |

完整署名链：**上善（上善无形，原角色）→ ZipZipPipe（加入 DeepSeek 元素的女仆设计）→ Small-tailqwq / dsh-deep-whale（上游立绘）→ JAdpp / dsh-whale-galgame（新增表情）**。
默认立绘保留其对应的上游署名，使用新增表情时再保留表情制作者记录；具体写法随文件清单保存。

## 3. 四张优先候选

以下文件已在发布者的资源目录和 NOTICE 中找到；本轮未下载或进行视觉检查。

| 候选文件 | 发布者描述 | 本项目可考虑的用途 |
| --- | --- | --- |
| [maid-left.webp](https://github.com/JAdpp/dsh-whale-galgame/blob/main/assets/default/maid-left.webp) | 默认鲸鱼娘立绘 | 普通状态 |
| [whale-cheerful.webp](https://github.com/JAdpp/dsh-whale-galgame/blob/main/assets/default/whale-cheerful.webp) | 快乐表情 | 完成事件后的鼓励 |
| [whale-serious.webp](https://github.com/JAdpp/dsh-whale-galgame/blob/main/assets/default/whale-serious.webp) | 严肃表情 | 临近截止或危险期提醒 |
| [whale-angry.webp](https://github.com/JAdpp/dsh-whale-galgame/blob/main/assets/default/whale-angry.webp) | 生气表情 | 催促提醒；措辞保持友好 |

用途对应属于本项目的设计建议，不是素材作者规定的功能，也不代表已完成用户审美确认。
这些具体文件在发布者 NOTICE 中列为 CC BY-NC-SA 4.0；AI 辅助创作的许可仅及于发布者有权授权的部分。
目前不能认定图片中没有 Logo、文字、第三方标识或其他需要进一步判断的元素。

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

## 6. 需求阶段的推荐决策与后续检查

1. 需求文档暂写“优先采用经核对的非官方鲸鱼娘候选，具备普通、快乐、严肃、生气等状态”，保留具体图片待审标记。
2. 实际采用时核对文件版本、画面内容、署名链、许可与修改记录；不能用转发站的统一标签代替逐文件证据。
3. 如果用户指定另一张图，先找到它的原始发布与授权信息；不得直接套用本记录中的许可。
4. 若候选不符合期望或许可边界不适合，独立原创海洋主题角色可作备选，采用自己的名称与造型。

本轮只形成候选与许可记录；**没有下载、生成、提交入库任何角色素材，也没有联系作者。**
