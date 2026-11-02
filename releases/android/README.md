# DDL Saver 手机安装包

[直接下载 0.3.7 安装压缩包](https://github.com/zidodo1124/hust26_software_engineering_homework/raw/refs/heads/main/releases/android/DDL-Saver-0.3.7-android.zip) · [查看仓库内文件](DDL-Saver-0.3.7-android.zip)

这是课程演示用的 **0.3.7（versionCode 10）debug 签名包**，适用于 **Android 8.0 及以上**。它与本轮荣耀 X50 实机检查所安装的 APK 完全一致。

## 怎么安装

1. 下载并解压 `DDL-Saver-0.3.7-android.zip`。
2. 将里面的 `DDL-Saver-0.3.7/DDL-Saver-0.3.7.apk` 发送到手机，并下载到手机本地。
3. 用手机的文件管理打开 APK；若系统询问，允许当前来源安装应用，然后点“安装”或“继续安装”。安装完成即可打开。

也可以直接在手机上下载、解压并安装。不需要电脑、USB 调试、Android Studio 或账号。压缩包内附有 `安装说明.txt`。

已装同签名旧版时可覆盖安装，保留本地数据；本轮已验证荣耀 X50 从 0.3.5 升级到 0.3.7。若提示签名不一致，请先保留旧版和数据，找队友获取同签名包，不要直接卸载。

需要提醒时，在 App 设置中按提示开启通知及相关系统权限。手机的后台限制、锁屏和重启仍可能影响送达；这份安装包不代表这些场景已经全部通过验收。

## 版本和校验

| 项目 | 值 |
| --- | --- |
| 应用包名 | `cn.edu.hust.ddlsaver` |
| 版本 | `0.3.7` / `10` |
| 数据库版本 | `5` |
| APK 大小 | `15,184,547` 字节 |
| ZIP 大小 | `14,728,839` 字节 |
| 产品源码 | [`295fb57`](https://github.com/zidodo1124/hust26_software_engineering_homework/commit/295fb579035a2efecbf914f64e0c0bb2caf263b6) |
| APK SHA-256 | `f3b263c210c20742dbb192cb847c1893b0ea9b4ff2b68a94d1c4da318ba088d9` |
| ZIP SHA-256 | `d2d4089c875208b42e682a431862ea3054c467daf1ba39c5fed49128d6dd636c` |
| 签名证书 SHA-256 | `978833bcaf0fc001248f5d8501bfa1f9d1273922474ed601f2c60a4548a652b9` |

仓库中的 [SHA256SUMS.txt](SHA256SUMS.txt) 用于核对 ZIP；解压后的同名校验文件用于核对 APK。安装包没有包含用户事项、图片、数据库、签名密钥或开发工具。

查看[项目文档](../../docs/README.md)了解功能、演示步骤和源码位置。
