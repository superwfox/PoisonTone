# PoisonTone

## 项目概要
PoisonTone 是一个运行在 Paper 服务端（Minecraft 1.21+）上的 QQ 群聊毒舌 AI 机器人插件。通过豆包大模型 Responses API 实现流式群聊对话，能够自动收集群内图片并通过轻量识图模型划分情绪类别予以存储，同时具备控制禁言、执行复读等群管互动功能。

## 主要功能
- **毒舌对话**：无需特定唤醒词，流式响应群聊并逐句发送交互。
- **表情收集与分发**：AI 自动缓存图片 URL 进行去重，使用专用模型将群图片按情绪分类存储，通过指令随机发送。
- **行为控制**：AI 能在交互中动态插入对成员特定时长的禁言（`setb`）及对上一条消息的复读（`repeat`）。
- **动态管控**：支持 OP 通过群内指令热切换语言模型，通过私聊动态读取与覆盖当前的人设规则 (Prompt)。

## 项目结构
```text
src/main/java/sudark2/Sudark/poisonTone/
├── api/
│   ├── DouBaoApi.java       # 大模型流式请求交互、响应解析及 Prompt 内存处理
│   └── HttpRequest.java     # 定制 OkHttp 请求与流式数据获取
├── bot/
│   └── OneBotClient.java    # WebSocket 通信中心，处理群聊及私聊的消息收发分拣
├── image/
│   ├── ImageAnalyzer.java   # 图片情绪识别模型调用
│   └── ImageStore.java      # URL 缓存序列化机制与情绪图片持久化保存
└── PoisonTone.java          # 插件加载卸载的生命周期主类定义

src/main/resources/
├── config.yml               # 基础设置（群号、管理员、API密钥）
└── prompt.md                # 默认设定的提示词文本模板
```

## 使用方法介绍（结合代码实现）

### 1. 对话与动作触发
AI 接收到群内信息后经由 `DouBaoApi.askStream` 调用流式响应，随后在 `DouBaoApi.dispatch()` 分发文本与动作。返回数据通过 `+` 分割后：
- **普通文本**：默认执行 `bot.sendG(s, group)` 将语句发回群聊。
- **动作 - 发送表情**：若解析出 `sendPicture(情绪)`，系统调取 `ImageStore.getRandom("情绪")` 取出 Base64 传递给 `bot.sendPicture(b64)` 完成发图。
- **动作 - 实施禁言**：解析 `setb(QQ,时长)`，系统下发至 `bot.setb(qq, duration)` 发动封禁。
- **动作 - 执行复读**：解析 `repeat`，将上回收集在 `StoredMessage` 中的消息直接 `bot.repeat()` 重发。

### 2. 模型类型热切换（群聊管控）
系统内置了双模型区分。若管理员（对应 `config.yml` 内配置的 `OP-QQ`）在群里发言以 `.` 开头：
- 发送 `.使用大模型的指令` (`.使用大模型`)：`OneBotClient.onMessage()` 会匹配后调用 `DouBaoApi.switchModel("doubao-seed-2-0-lite-260215")` 切换语言处理模型为主力大模型并发送成功提示。
- 发送 `.使用小模型的指令` (`.使用小模型`)：调用 `DouBaoApi.switchModel("doubao-seed-2-0-mini-260215")`，从而快速降级到轻量版模型进行普通对话。

### 3. 人设词热重载（私聊管控）
针对管理提示词的需求，逻辑收束在 `OneBotClient.java` 私聊事件监听内：
- **获取当前 Prompt**：由 `OP-QQ` 在私聊准确发送 `当前提示词`，代码将调用 `DouBaoApi.getPrompt()` 取回内存中暂存的文本并调用 `sendP()` 直接发给管理员。
- **更新当前 Prompt**：由 `OP-QQ` 在私聊发送包含前缀 `修改提示词 (新内容)` 的消息。截取新内容后流转至 `DouBaoApi.updatePrompt(newPrompt)`，方法内会复写本地 `prompt.md` 并在完毕后触发 `reloadPrompt()`，完成实时的重载与生效闭环。

### 4. 表情库自动收集识别
任意成员在群聊发送 `[图片]` 消息时，`OneBotClient` 会提取图片原始 URL，启动异步线程调起 `ImageAnalyzer.analyzeAndStore(imgUrl)`：
程序校验 `ImageStore.urlCache` 是否命中该 URL，未命中则走轻量模型对图片测定情绪词。如果情绪落在7大定义类内，自动由 URL 爬取原图，编码落地到 `emojis/分类名/哈希.txt` 以供发图功能后期使用，并将该状态序列化缓存持久保持。
