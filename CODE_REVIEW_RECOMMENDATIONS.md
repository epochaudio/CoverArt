# 稳定性与体验优化建议

## 概览
- 审查期间聚焦连接注册、网络工具与区域展示逻辑，识别可精简与潜在失效的实现。
- 目标是减少重复与遗留代码，统一关键路径行为，提升后续维护的可读性与可靠性。

## 重复的注册流程逻辑
- **位置**：`app/src/main/java/com/example/roonplayer/MainActivity.kt:3891`、`app/src/main/java/com/example/roonplayer/MainActivity.kt:4405`
- **影响**：两个几乎一致的注册流程维护成本高，字段已经开始分叉，若未来更新只改一处会造成重试路径注册失败。
- **建议**：抽象出“构建注册请求”的单一方法，接收参数控制是否包含 settings 服务，所有注册入口共享。
- **整改方案**：
  1. 新增 `buildRegisterMooMessage(includeSettings: Boolean)`，统一 token 读取、JSON 体、MOO 报文拼装。
  2. `sendRegistration()` 与 `retryRegistrationWithoutSettings()` 仅根据需要传入布尔值并调用发送。
  3. 在代码评审中加入单元/集成验证，确保两条路径生成的服务列表一致。

## 未使用的注册助手函数
- **位置**：`app/src/main/java/com/example/roonplayer/MainActivity.kt:312`
- **影响**：`createExtensionInfo` 和关联的 services 构造函数未被调用且仍返回旧版协议号 (`transport:1`)，若误用将导致与 Roon Core 不兼容。
- **建议**：删除失效工具，或更新为当前协议并为其编写调用点与测试。
- **整改方案**：
  1. 若未来需要共用构造逻辑，将上述函数迁移至新的注册构建器并更新服务版本。
  2. 否则直接清理文件中未使用的函数，减少阅读干扰。

## 遗留的发现缓存备份文件
- **位置**：`app/src/main/java/com/example/roonplayer/network/DiscoveryCache.kt.backup`
- **影响**：备份文件增加仓库噪音，容易让贡献者误判缓存仍在使用，也可能在构建脚本中被意外包含。
- **建议**：删除或移入文档目录，并在 README/贡献指南说明缓存方案的现状。
- **整改方案**：
  1. 若逻辑已经废弃，直接移除 `.backup` 文件。
  2. 如需保留示例，将其转换为 Markdown 设计文档附在 `docs/`。

## 未使用的网络工具与健康检查接口
- **位置**：`app/src/main/java/com/example/roonplayer/MainActivity.kt:346-584`、`app/src/main/java/com/example/roonplayer/network/SmartConnectionManager.kt:102`、`app/src/main/java/com/example/roonplayer/network/ConnectionHealthMonitor.kt:131`
- **影响**：`isValidIP`、`safeExecute*`、`createTcpConnection`、`createUdpSocket`、`testTcpConnection` 等函数没有调用方；`quickConnectTest` 与即时健康检查接口同样闲置。保留死代码降低可读性，并让维护者误以为仍需维护。
- **建议**：审视是否还计划使用，若无需求则删除；如未来需要，请补充清晰的调用路径与测试。
- **整改方案**：
  1. 统计调用图并确认无引用后删除上述函数。
  2. 若要保留，编写使用示例或单元测试，防止再次成为死代码。

## 区域播放信息解析重复
- **位置**：`app/src/main/java/com/example/roonplayer/MainActivity.kt:5039`、`5603`、`5896`
- **影响**：多处重复提取 `now_playing` 与 `three_line` 字段，字段名或结构变更时需多点同步修改，易遗漏。
- **建议**：抽取统一的 `parseZonePlayback(zone: JSONObject)` 或数据类封装，集中处理缺省值与格式差异。
- **整改方案**：
  1. 编写返回 `ZonePlaybackInfo` 的小函数，包含标题/艺术家/专辑/图片键等。
  2. 在所有展示与日志调用处替换为该函数，并在函数内处理空值与日志。

## 后续验证建议
- 注册流程抽象完成后，执行端到端连接测试，确认正常与降级注册均成功。
- 清理死代码后运行 `./gradlew lint testDebugUnitTest` 确认无编译和静态检查回归。
- 抽取 Zone 解析函数后，通过手工或自动测试验证多区域与艺术墙模式的显示逻辑。
