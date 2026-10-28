# “蓝色大肥鱼”Q 版素材选择

查阅日期：2026-10-28。角色显示名称为**蓝色大肥鱼**。资源方案保留经典立绘，并增加可选的 Q 版；原四张图片和许可文件保持不变。下文区分本次实际入库素材与此前尚未选用的候选。

## 已入库的 Q 版

采用 [f0909172434 的 Q 版图集](https://github.com/f0909172434/dsh-deepseek-girl-pet/blob/3e7df1d76014e4f43ba9ed3c9de53c2a62ba0d14/assets/spritesheet.webp)，固定到提交 `3e7df1d76014e4f43ba9ed3c9de53c2a62ba0d14`。实际图像为蓝发、圆脸、大头短身、鲸尾女仆形象；已经查看原图，选择四个不含牌子与文字气泡的静态帧。运行时只显示对应区域，不重画、不裁改资源文件，也不引入桌宠动画或外部桌宠程序。

| 用途 | 0 基行、列 | 像素区域（左、上、宽、高） | 实际画面 |
| --- | --- | --- | --- |
| 普通 | 0、6 | 1152、0、192、208 | 正面站立 |
| 完成／快乐 | 4、1 | 192、832、192、208 | 微笑、双拳靠近脸颊 |
| 紧急／逾期 | 8、1 | 192、1664、192、208 | 皱眉思考，手靠近脸部 |
| 速通危险期 | 7、1 | 192、1456、192、208 | 皱眉站立、集中注意 |

后两者是不同姿态；上游将其对应行命名为“审查中”和“工作中”，将静态帧用于截止时间提示是本应用的展示选择。状态标题与台词继续承担明确的信息表达，不把近似表情当成独立的业务状态定义。

图集大小为 1536 × 2288，2,836,976 字节，SHA-256 为 `234F24A97C18195A00C6093DA0090773E675993C169E92E7E13A24C37B323FA2`。[逐项来源记录](../app/src/main/assets/character/chibi/sources.json)包含下载地址、尺寸、字节数、哈希与显示区域；[本项目素材说明](../app/src/main/assets/character/chibi/NOTICE.md)保留署名与使用边界。

[上游 atlas-source.json](https://github.com/f0909172434/dsh-deepseek-girl-pet/blob/3e7df1d76014e4f43ba9ed3c9de53c2a62ba0d14/atlas-source.json)明确列出该图集的 MIT 许可及原始仓库固定版本；[JAdpp 逐文件 NOTICE](https://github.com/JAdpp/dsh-whale-galgame/blob/046b848f244450e6e636335766276bf3343fe84b/NOTICE.md)也单独将同哈希图集列为 MIT。两份完整 MIT 文本随 App 保存。MIT 声明只涉及发布者有权授权的贡献，不能覆盖底层原角色和第三方标志；项目继续保留上善、ZipZipPipe 的原角色署名与既有 CC BY-NC-SA 4.0 条件，按非商业课程用途使用，不宣称得到品牌方授权。

## 此前候选与选择依据（未入库）

| 候选 | 形态与表情 | 授权核对及当前判断 |
| --- | --- | --- |
| [Small-tailqwq 三表情头像预览](https://github.com/Small-tailqwq/dsh-deep-whale/blob/dc561129f3bb3bc94c4a7c3e4007eaa274650d31/maid-atelier/assets/icons/preview.png) | 原仓库提供困困、托腮、认真三类头像及透明切图；适合头像式卡片，不能直接当成完整快乐／生气全身套图 | [图标说明](https://github.com/Small-tailqwq/dsh-deep-whale/blob/dc561129f3bb3bc94c4a7c3e4007eaa274650d31/maid-atelier/assets/icons/README.md)和独立 [美术许可](https://github.com/Small-tailqwq/dsh-deep-whale/blob/dc561129f3bb3bc94c4a7c3e4007eaa274650d31/maid-atelier/LICENSE-ARTWORK)沿用 CC BY-NC-SA 4.0，保留上善→ZipZipPipe→Small-tailqwq 署名链；可优先审核 |
| [YunYueSama 圆脸全身设定图](https://github.com/YunYueSama/codex-deepseek-pet/blob/7661c8b304c5400701f91da01b1a643a207331de/design/character-standard.png)及[表情图集](https://github.com/YunYueSama/codex-deepseek-pet/blob/7661c8b304c5400701f91da01b1a643a207331de/assets/whale/expressions.png) | 作者描述为约 2.5 头身、大头短身、圆脸、蓝紫发及鲸尾；有多个情绪与动作，形态更接近本次要求 | [自定义许可](https://github.com/YunYueSama/codex-deepseek-pet/blob/7661c8b304c5400701f91da01b1a643a207331de/LICENSE)允许其有权授权的新增贡献复制、修改、分发，须保留作者、仓库及完整许可；[素材说明](https://github.com/YunYueSama/codex-deepseek-pet/blob/7661c8b304c5400701f91da01b1a643a207331de/ASSET_LICENSE.md)明确多张社区参考原作者／授权待核实，不能据仓库许可推定所有底层内容已获授权 |
| [xiaoweigen 基础 Q 版六视图](https://github.com/xiaoweigen/deepseek-dafeiyu-assets/blob/7e7a888845877b30314d36a7f274dc3bd6f330b1/images/base-six-views.png) | 蓝发鲸尾的 Q 版设定，有基础及古风服装；不是现成情绪套图 | [逐项声明](https://github.com/xiaoweigen/deepseek-dafeiyu-assets/blob/7e7a888845877b30314d36a7f274dc3bd6f330b1/NOTICE.md)将六张图片及改编贡献按 CC BY-NC-SA 4.0 发布并保留上游许可；仍继承前项参考链，暂作设定参考 |

本表三个候选的形态依据此前查阅的作者描述、目录和许可记录；该次远程图片读取未返回可检查画面，尚未完成这三项的逐图视觉审核。它们没有加入安装包。此次实际视觉检查与入库的是前节 f0909172434 图集，不能混用验证结论。仓库代码许可与图片美术许可分别核对。

## 集成与后续维护要求

1. 经典立绘与 Q 版并存，统一显示名称“蓝色大肥鱼”；选择应可保存，升级后应保留原有数据和默认外观。
2. 保留普通、紧急／认真、危险／生气及完成／快乐的辨识度，不只依靠颜色传达状态；缺少的情绪应明确列出，不能用近似头像冒充完整表情套图。
3. 选中具体文件后保存固定来源、逐文件许可、作者链、SHA-256 及修改说明；涉及改编时遵循相应许可条件。[CC 官方说明](https://creativecommons.org/licenses/by-nc-sa/4.0/deed.zh-hans)
4. 集成后重新检查缩放、台词排版、状态映射与完成反馈。原 v0.3.0 的手机截图仅支持原立绘，不作为 Q 版验证结果；实际构建和界面测试结论以对应测试记录为准。
