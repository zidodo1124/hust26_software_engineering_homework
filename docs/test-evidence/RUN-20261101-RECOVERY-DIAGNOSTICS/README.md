# RUN-20261101-RECOVERY-DIAGNOSTICS

2026-11-01 本机检查，基线 `ac1d9785ac9ab74dbd34fc00bc8459eb923002bc` 加未提交源码／测试增量。版本仍为 0.3.3／6、schema 4；本次增加仅 debuggable 启用的图片导入失败诊断，并补真实恢复广播与每日汇总 PendingIntent 的本机集成测试。没有手机安装或设备操作，不登记为任何队友的个人新增贡献。

| 阶段 | 实际结果 |
| --- | --- |
| `targeted-01` | 3 份 JUnit XML，13 个不同方法／21 次执行，8 失败，错误／跳过各 0；BUILD FAILED，2 分 18 秒 |
| `targeted-02` | 相同三类定向检查，13 个方法／21 次执行，失败／错误／跳过各 0；BUILD SUCCESSFUL，1 分 15 秒 |
| `full-01` | Debug 构建、测试及 Lint 成功，3 分 11 秒；27 份 JUnit XML，167 个不同方法／194 次执行，失败／错误／跳过各 0；Lint 0 错误／16 警告 |

本批新增 12 个方法、20 次执行：诊断 8 个方法在 API 28／35 执行 16 次，恢复 3 个方法在 API 35 执行 3 次，每日汇总冷启动新增 1 次。定向运行还包含原有单事项冷启动 1 次，故为 21 次；全部均包含在全量 194 次中，不累加不同阶段。前一批 [174 次界面回归](../RUN-20261101-INDEPENDENT-UI/README.md)独立保留，也不累加为新方法。

## 命令与被测状态

两轮定向使用同一命令，分别重定向到对应本机日志：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/gradle.ps1 --no-daemon --console=plain --stacktrace --continue --max-workers=2 :app:testDebugUnitTest --tests cn.edu.hust.ddlsaver.platform.AttachmentImportDiagnosticsTest --tests cn.edu.hust.ddlsaver.reminders.ReminderRestoreIntegrationTest --tests cn.edu.hust.ddlsaver.ui.ReminderColdLaunchTest
```

全量命令：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/gradle.ps1 --no-daemon --console=plain --stacktrace --continue --max-workers=2 :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

[summary.json](summary.json)保存命令、逐套件计数、方法去重规则、Lint 分组、日志哈希、APK 与证书哈希，以及相对基线全部 7 份变动源码的 SHA-256：两份生产图片文件、三份本轮新增／修改测试及前一批两份未提交页面测试。源码哈希标识最终第二轮定向及全量的状态，不能拿它代表第一次失败时的测试版本。

最终 Debug APK 为 `cn.edu.hust.ddlsaver`、0.3.3／6、15,490,535 字节，SHA-256 `6f30078bf0dfa95731afe9fae0c08f06dd19d048eb815fd2236022c90e19e65b`。证书 SHA-256 `978833bcaf0fc001248f5d8501bfa1f9d1273922474ed601f2c60a4548a652b9`，与此前设备使用的本机包一致；本次未安装。远程 `ac1d978` CI 仍为 169 次执行，不覆盖本批未提交工作。

## 首轮失败、修正与边界

首轮 8 次失败来自测试前提／观察点：6 次要求协程恢复后的直接 cause 与原异常引用相同，修正为限定深度 cause 链查找；1 次对未知 action 使用 manifest 查询，修正为直接验证生产接收器守卫；1 次重建后等待已滚出视口的标题，修正为等待稳定列表容器再滚动检查。原消息、隐私、回滚及不重放断言保留，没有通过这些测试失败修改生产行为或增加等待时间。完整原因见[执行记录第 19 节](../../test-records.md#19-run-20261101-recovery-diagnostics恢复集成与导入诊断)。

支持的 5 种恢复广播仍使用真实 manifest 派发，经过生产 Receiver／Runtime／Room／Alarm；系统 Alarm 丢失和权限能力是本机受控条件，不证明真机重启、锁屏、Doze 或厂商权限表现。真实汇总 PendingIntent 冷启动验证今日有效范围；用户主动清除今日筛选后重建仍保持全部待办 3 项、不重新应用通知路由。该冷启动无初始旧筛选，已有热启动清旧筛选的历史检查见[执行记录第 12 节](../../test-records.md#12-run-20261027-005第三轮提醒与角色本机验证)。手机真实通知点击仍待执行。图片异常是合成故障，不能关闭 OBS-01 或认定其真实根因。

## 报告副本与脱敏

- 首轮失败：[JUnit XML](targeted-01-results/)、[日志](targeted-01.log)、[原始／副本哈希](targeted-01-results-hashes.json)。
- 第二轮定向：[JUnit XML](targeted-02-results/)、[日志](targeted-02.log)、[原始／副本哈希](targeted-02-results-hashes.json)。
- 全量：[JUnit 与 Lint XML](full-results/)、[日志](full-01.log)、[原始／副本哈希](full-results-hashes.json)。

这些是脱敏副本：移除 XML 的 `hostname` 属性，将工作区和用户目录绝对路径前缀替换为 `[workspace]`／`[user-home]`。测试节点、结果和诊断输出保留，未删除 `system-out`／`system-err`；失败断言中的 private-authority、secret-token 等是故意构造的合成样本，不是真实用户图片来源。原始日志和报告留在忽略目录 `.local-tools/recovery-diagnostics/`。损坏图片解码等诊断输出按实际保留，测试通过不表示日志从未产生任何诊断。
