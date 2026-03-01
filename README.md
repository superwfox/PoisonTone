# PoisonTone

一个运行在 Paper 服务端上的 QQ 群聊毒舌 AI 机器人插件。通过豆包大模型 Responses API 实现流式对话，支持表情包自动收集与情绪分发、禁言、复读等群聊互动功能。

## 主要功能

- **毒舌 AI 对话** — 群内正常聊天即可触发，流式返回，逐句实时发送
- **表情包自动收集** — 群内图片自动通过模型分析情绪，归类保存
- **表情包发送** — AI 根据语境触发 `sendPicture(情绪)`，随机发送对应类别表情
- **复读** — AI 判断发言不合语境时触发 `repeat`，复读上一条消息
- **禁言** — AI 对极端发言触发 `setb(QQ号,秒数)`
- **模型热切换** — OP 发送 `.使用大模型` / `.使用小模型` 切换
- **Prompt 热重载** — OP 发送 `.重载prompt` 重新加载人设并重置会话

## 项目结构

```
src/main/java/sudark2/Sudark/poisonTone/
├── api/
│   ├── DouBaoApi.java       # 豆包 Responses API 交互、流式解析、动作分发
│   └── HttpRequest.java     # OkHttp 封装（普通请求 + 流式请求）
├── bot/
│   └── OneBotClient.java    # OneBot WebSocket 客户端，消息收发
├── image/
│   ├── ImageAnalyzer.java   # Chat Completions API 图片情绪分类
│   └── ImageStore.java      # 7类情绪表情包存储、随机取图
└── PoisonTone.java          # 插件入口

src/main/resources/
├── config.yml               # 配置文件
├── prompt.md                # 默认 Prompt（首次运行释放到插件目录）
└── plugin.yml
```

## 前置要求

1. **Paper 1.21+** 服务端
2. **OneBot 协议端** 运行在 `ws://127.0.0.1:3001`
3. **火山方舟** 豆包 API 账号

### 火山方舟平台操作

1. 注册并登录 [火山方舟](https://console.volcengine.com/ark)
2. 创建 API Key，填入 `config.yml` 的 `API-KEY`
3. 确保以下模型已开通：
   - `doubao-seed-2-0-lite-260215`（大模型，同时用于对话和图片分析）
   - `doubao-seed-2-0-mini-260215`（小模型，可选切换）

## 配置说明

`plugins/PoisonTone/config.yml`：

```yaml
API-KEY: "你的火山方舟API密钥"
LAST-RESPONSE-ID: ""           # 自动维护，无需手动填写
MODEL: "doubao-seed-2-0-lite-260215"
GROUP: "你的群号"
SELF-QQ: "机器人QQ号"
OP-QQ: "管理员QQ号"
```

## 部署步骤

1. 构建插件 JAR 并放入 `plugins/` 目录
2. 首次启动服务器，插件自动生成：
   - `plugins/PoisonTone/config.yml` — 编辑填入你的配置
   - `plugins/PoisonTone/prompt.md` — 可自定义 AI 人设
   - `plugins/PoisonTone/emojis/` — 7 个情绪子目录（开心/满足/沮丧/悲伤/观望/不满/愤怒）
3. 表情包会从群聊图片中自动收集，也可手动将表情包放入对应情绪子目录
4. 重启服务器

## 使用方法

### 群聊对话

群内正常聊天即可触发 AI 回复，无需任何前缀：

```
群友: 你觉得我帅吗
AI:   帅是不可能帅的
AI:   [表情包-不满]
AI:   这辈子都不可能帅的
```

AI 的回复以 `+` 分割为多句，逐句流式发送。句间可穿插动作指令：
- `sendPicture(情绪)` → 从对应情绪文件夹随机发一张表情包
- `repeat` → 复读上一条群消息
- `setb(QQ号,秒数)` → 禁言指定用户
- `pass` → 跳过回复

### OP 管理命令

以下命令仅 `OP-QQ` 配置的管理员可用，以 `.` 开头：

```
.使用大模型      # 切换到 doubao-seed-2-0-lite
.使用小模型      # 切换到 doubao-seed-2-0-mini
.重载prompt      # 重新加载 prompt.md 并重置对话上下文
```

### 表情包自动收集

群内任何人发的图片都会自动通过 Responses API 独立分析（无会话上下文）。如果判定为表情包且情绪匹配 7 类之一（开心/满足/沮丧/悲伤/观望/不满/愤怒），自动下载保存到 `emojis/对应情绪/` 目录，供 AI 后续随机使用。

### 自定义 Prompt

编辑 `plugins/PoisonTone/prompt.md` 修改 AI 人设和输出规范，然后在群内发送 `.重载prompt` 即可热生效（会同时重置对话上下文）。
