# 图片剪贴板实机测试夹具

本目录是独立测试工具，不属于 HUSTer's DDL Saver 发布 App，也不参与根 Gradle 构建。包名为 `cn.edu.hust.ddlsaver.qa.clipboard`，用于在真实 Android 设备上，通过外部应用的 `content://` 图片剪贴板验证产品的显式粘贴及跨 UID 读取授权。应优先使用设备实际提供的文件应用复制入口；使用本夹具的结果不能扩展为兼容所有相册和剪贴板应用。

## 内容与数据范围

| 文件／目录 | 用途 |
| --- | --- |
| [AndroidManifest.xml](AndroidManifest.xml) | 最低 API 26、目标 API 35；独立 Activity 和私有 Provider |
| [ClipboardFixtureActivity.java](src/cn/edu/hust/ddlsaver/qa/clipboard/ClipboardFixtureActivity.java) | 六个明确的复制／清空按钮 |
| [FixtureProvider.java](src/cn/edu/hust/ddlsaver/qa/clipboard/FixtureProvider.java) | 只读提供四个固定白名单文件及其 MIME、显示名称、大小 |
| [assets/](assets/)与[清单](assets-manifest.json) | 两张 PNG、一张 EXIF 方向 6 的 JPEG、一个有意损坏的 PNG；均为合成测试文件，清单记录尺寸、字节数与 SHA-256 |
| [build_fixture.py](build_fixture.py) | 使用既有工具离线构建与核验，不下载、不安装、不调用 ADB |
| [generate_fixtures.py](generate_fixtures.py) | 用 Pillow 重新生成合成文件及清单；正常构建无需执行此脚本或安装 Pillow |

工具不申请网络、存储或其他敏感权限，不读取原有剪贴板、公共存储、产品数据库或个人图片。四个自带文件仅复制到工具自身缓存；Provider 设置 `exported=false`、`grantUriPermissions=true`，拒绝任意路径、额外路径段、查询参数、片段及写入。复制使用 `ClipData.newUri` 与 `addItem`，**不另行显式授权给产品**，因此粘贴通过依赖 Android 正常剪贴板 URI 授权流程。

## Windows 构建复现

准备 Python 3、JDK 17 或更高版本，以及已安装 Platform 35 和 Build Tools 35.0.0 的 Android SDK。脚本依次使用 `javac` 编译、`d8` 生成 DEX、`aapt2` 打包、`zipalign` 对齐及 `apksigner` 签名与核验，不改产品依赖或版本。实际验证环境为 Windows、JDK 17、Build Tools 35.0.0；脚本兼容非 Windows 工具命名，但尚未据此声明其他系统验证通过。

在仓库根目录执行：

```powershell
python test-tools/clipboard-fixture/build_fixture.py
```

SDK 优先采用 `--sdk`、`ANDROID_HOME`／`ANDROID_SDK_ROOT`，否则使用本仓库 `.local-tools/android-sdk`；JDK 优先采用 `--jdk`、`JAVA_HOME`，否则查找本仓库 `.local-tools/jdk-17*`。现有 `JAVA_HOME` 若指向旧版 Java，明确指定 JDK 17：

```powershell
python test-tools/clipboard-fixture/build_fixture.py --jdk 'C:\path\to\jdk-17' --sdk 'C:\path\to\Android\Sdk'
```

仓库根目录通过父目录中的 `settings.gradle.kts` 定位。每次输出到 `.local-tools/phone-functional/clipboard-fixture/repro-builds/<UTC时间>/`，包含 APK、核验 JSON 和中间产物；独立临时调试密钥位于同一忽略树中的 `qa-debug.keystore`。新构建不会覆盖此前用于手机测试的 APK。APK、签名密钥和构建输出不得提交；只提交本目录审阅过的源码、合成资源和说明。不同构建的 APK 哈希应分别记录，不能用新构建哈希替代已经执行的设备证据。

构建会核对四个资源的清单哈希、私有 Provider 声明、APK 包名和 API、无请求权限、v2 签名，以及 APK 内资源与源文件一致。它不会证明手机功能已经通过，仍需执行下面的真实界面操作。

如需重新生成合成文件，使用已安装 Pillow 的 Python 执行以下命令，并审查资源及清单变化：

```powershell
python test-tools/clipboard-fixture/generate_fixtures.py
```

## 实机使用与清理

由负责实机测试的人安装实际选定并记录哈希的夹具 APK，启动 `.ClipboardFixtureActivity`。按钮只在点击时替换系统剪贴板，不会读取或备份原内容。测试时仅操作事先约定的测试数据。

| 按钮 | 验证场景 |
| --- | --- |
| `Copy blue`／`Copy orange` | 标准单个图片 URI；返回产品编辑器点击“粘贴图片” |
| `Copy two images` | 一个 `ClipData` 中的两个 URI；产品应导入两张独立副本 |
| `Copy broken image` | 错误提示、原有表单输入和已存附件保留 |
| `Copy text` | 复制纯文本图片网址，产品应提示没有可读取的图片，而非当作图片导入 |
| `Clear clipboard` | API 28+ 使用标准清空接口；API 26／27 回退为空文本；检查已导入附件仍可读取 |

EXIF JPEG 已列入 Provider 白名单，主要供真实相册选择和方向检查使用：编码尺寸 480×240，正确显示尺寸 240×480，红条在顶端、箭头向下。图片选择、放大预览、编辑取消、保存和进程重开等应在产品真实界面操作，记录实际采用的入口与结果；不能只写入数据库代替 UI 测试。

测试结束后，仅卸载独立夹具包 `cn.edu.hust.ddlsaver.qa.clipboard`，**不得卸载或清空产品包 `cn.edu.hust.ddlsaver`**。卸载夹具会清除它自身的缓存和来源 Provider；随后重新打开产品中保存的测试事项，验证私有副本仍能预览。公共目录中另行放置的合成图片按测试清单单独处理，不删除其他文件。夹具没有恢复旧剪贴板的功能，因为它从未读取原内容。

参考：[Android 剪贴板与 URI 文档](https://developer.android.com/develop/ui/views/touch-and-input/copy-paste)、[AOSP DocumentsUI 文件复制接口](https://android.googlesource.com/platform/packages/apps/DocumentsUI/+/master/src/com/android/documentsui/clipping/DocumentClipper.java)。厂商系统是否提供相同文件复制入口，应以实际观察为准。
