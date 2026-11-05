# 0.3.7 文案更新：本机验证

本轮只更新用户可见文案及版本号，保留现有业务规则、数据库 schema 5 和依赖。手机仍为此前的 0.3.5，本轮未覆盖安装。完整用词与参考来源见[文案清单](../../ui-copy-guide.md)。

## 代码与执行

- 产品提交：`295fb579035a2efecbf914f64e0c0bb2caf263b6`，仅在本地；构建开始时 HEAD 为 `3250cbe980527b9bd67843385e20ecd7630acb74`，另有尚未提交的文案和版本修改。产品提交与实际被测输入的核对见[输入哈希](source-inputs.json)。
- 版本：**0.3.7／versionCode 10、Room schema 5**。
- 执行窗口：`2026-11-02T10:49:43.778365+00:00` 至 `2026-11-02T10:54:46.600160+00:00`（UTC）。
- 执行一轮完整验证，没有删减测试或为取得成功重复运行。

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/gradle.ps1 --no-daemon --console=plain --stacktrace --continue --max-workers=2 :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

## 结果

| 检查 | 结果 |
| --- | --- |
| 构建 | 成功，退出码 0 |
| 测试 | 29 份 XML，197 个不同方法，236 次执行 |
| 失败／错误／跳过 | 0／0／0 |
| Lint | 0 错误／16 警告 |
| 输入稳定性 | 95 个产品输入文件在构建前后及归档时 SHA-256 一致 |
| 归档检查 | [verification.json](verification.json) 的 13 项检查通过 |
| APK | 15,184,547 字节；版本由 aapt 实际解析 |
| APK SHA-256 | `f3b263c210c20742dbb192cb847c1893b0ea9b4ff2b68a94d1c4da318ba088d9` |

执行次数包含不同 Android SDK 参数，不等于独立测试方法数。原角色状态／轮换、保存冲突、图片失败、权限提示、重复事项及周历折叠等现有测试均按本次真实报告记录；没有新增单纯照抄文案的测试。

## 页面与证据

实际 Compose 页面在本机 Robolectric 渲染，采用合成事项数据：

- [经典首页](ui-review/home-classic.png)、[Q 版首页](ui-review/home-chibi.png)。
- [320dp／150% 字体首页](ui-review/home-320dp-font150.png)。
- [经典长文本气泡](ui-review/bubble-long-text-classic.png)、[Q 版长文本气泡](ui-review/bubble-long-text-chibi.png)是既有压力测试夹具，不代表新台词长度。

本机画面不能替代实机验收。重复事项手机测试、长期后台、OBS-01／OBS-02 等原有待办仍开放。

[结果摘要](summary.json) · [构建日志](gradle.log) · [Lint](lint-results-debug.xml) · [包信息](apk-badging.txt) · [输入哈希](source-inputs.json) · [证据哈希](files-sha256.json)

测试 XML 原样保留在 `test-results/`；构建日志和 Lint 中的工作区绝对路径替换为 `WORKSPACE`，其余内容不改。以下清单覆盖除清单自身外的所有文件。
