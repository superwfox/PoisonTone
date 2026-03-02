# PoisonTone

## 项目概要
PoisonTone 是一个运行在 Paper 服务端（Minecraft 1.21+）上的 QQ 群聊 AI 机器人插件。通过豆包大模型实现流式群聊对话，支持自动收集群内图片并通过识图模型将图片按情绪分类保存，具备禁言、复读、发送随机情绪表情等群管互动功能。

## 主要功能与触发方式

- **日常群聊互动**
  - 无需特定唤醒词，群内直接发言即可获得 AI 流式回复。
- **自动收集表情包**
  - **触发方式**：任意群成员发送图片。
  - **效果**：AI 自动识别图片情绪并保存，遇到已识别过的图片自动跳过。
- **动态控制人设与提示词 (Prompt)**
  - **查看当前提示词**：OP（管理员）向机器人私聊发送 `当前提示词`。
  - **修改提示词**：OP（管理员）向机器人私聊发送 `修改提示词 (新内容)`，例如：`修改提示词 你是一个傲娇的群管`，立即生效。
- **模型热切换**
  - **触发方式**：OP（管理员）在群内发送 `.使用大模型` 或 `.使用小模型`。
  - **效果**：实时切换群聊对话的语言模型版本。

## 项目结构
```text
src/main/java/sudark2/Sudark/poisonTone/
├── api/             # API 请求与 Prompt 处理
├── bot/             # OneBot 客户端消息收发
└── image/           # 图片情绪分类与缓存分发
```

## 实现细节（代码剖析）

### 1. 通信与消息分发调度
- AI 接收信息后经 `DouBaoApi.askStream()` 进行流式响应，并在 `DouBaoApi.dispatch()` 分发纯文本与内部动作指令。语句以 `+` 号分割。
- **特殊动作指令解析**：
  - 发送表情：AI 输出 `sendPicture(情绪)` 时，调用 `ImageStore.getRandom("情绪")` 转交 `bot.sendPicture(b64)` 完成发图。
  - 实施禁言：AI 输出 `setb(QQ,时长)` 时，转交 `bot.setb(qq, duration)` 发动封禁。
  - 执行复读：AI 输出 `repeat` 时，将存在 `OneBotClient.StoredMessage` 里的上一条消息直接 `bot.repeat()` 重发。

### 2. 双模型设计与热切换
- 对话通常使用 `doubao-seed-2-0-lite` 大模型，图片情绪识别固定使用 `doubao-seed-2-0-mini` 小模型以保证轻量处理。
- `OneBotClient` 群聊监听事件中拦截以 `.` 开头的管理员消息：
  - `.使用大模型`：调用 `DouBaoApi.switchModel("doubao-seed-2-0-lite-260215")`。
  - `.使用小模型`：调用 `DouBaoApi.switchModel("doubao-seed-2-0-mini-260215")`。

### 3. Prompt 热重载机制
逻辑位于 `OneBotClient.java` 私聊事件：
- 当识别为管理员发来的私聊，精准匹配 `"当前提示词"` 时调用 `DouBaoApi.getPrompt()`，以私聊 `sendP()` 返回。
- 开头匹配 `"修改提示词 "` 时，截取后半段调用 `DouBaoApi.updatePrompt(newPrompt)` 写回本地 `prompt.md` 并调用 `reloadPrompt()` 热重启内存，重置上一次的 `lastResponseId` 以刷新记忆。

### 4. 表情包去重与缓存
- `OneBotClient` 收到 `[图片]` 类型消息后提取 URL，开启一个新线程丢给 `ImageAnalyzer.analyzeAndStore(imgUrl)` 处理。
- 首先通过 `ImageStore.urlCache` (HashMap) 检查命中提升效率。若为新图片，则提交给图片模型研判，当分析出的情绪在预设的 7大类（开心/沮丧等）内，才执行网络下载。
- 图片按分类编码为 Base64 `.txt` 存入 `emojis/情绪/`，并将 URL 哈希字典固化至 `url_cache.dat` 文件用于跨重启持久化。
