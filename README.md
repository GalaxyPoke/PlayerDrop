# PlayerDropGuard

一个使用 PacketEvents 实现私有掉落物显示的 Spigot 插件，防止玩家误丢贵重物品。

## 功能特性

- **私有掉落物显示** - 丢弃的物品只有自己能看到，其他玩家完全看不到
- **虚拟悬浮效果** - 虚拟物品悬浮在玩家面前，带发光效果便于识别
- **确认机制** - Shift+Q 确认丢弃，生成真实掉落物，所有人可见
- **撤回机制** - 站立状态按 Q 撤回物品到背包
- **超时自动撤回** - 可配置超时时间，默认 5 秒
- **设置界面** - 双击 Q 打开设置 GUI，可开关保护功能
- **音效提示** - 创建/确认/撤回/超时时播放音效
- **日志记录** - 记录所有丢弃行为到文件
- **MySQL 支持** - 大型服务器可使用数据库存储
- **PlaceholderAPI** - 提供占位符供计分板等使用

## 操作说明

| 操作 | 效果 |
|------|------|
| Q键 | 创建虚拟掉落物 / 撤回已有虚拟物品 |
| Shift+Q | 确认丢弃，生成真实掉落物 |
| 双击Q (300ms内) | 打开设置界面 |

## 依赖

- Spigot/Paper 1.21+
- [PacketEvents](https://github.com/retrooper/packetevents) 2.4.0+
- Java 21
- (可选) PlaceholderAPI

## 安装

1. 下载并安装 [PacketEvents](https://github.com/retrooper/packetevents/releases) 插件
2. 将 `PlayerDropGuard.jar` 放入 `plugins` 文件夹
3. 重启服务器

## 命令

| 命令 | 说明 | 权限 |
|------|------|------|
| `/pdg reload` | 重载配置 | dropguard.admin |
| `/pdg toggle <玩家>` | 切换玩家保护状态 | dropguard.admin |
| `/pdg status [玩家]` | 查看保护状态 | - |
| `/pdg stats` | 查看统计信息 | dropguard.admin |

## 权限

| 权限节点 | 说明 | 默认 |
|----------|------|------|
| dropguard.use | 使用丢弃保护功能 | true |
| dropguard.toggle | 允许开关保护设置 | true |
| dropguard.bypass | 绕过丢弃保护 | op |
| dropguard.admin | 管理员权限 | op |

## PlaceholderAPI 占位符

| 占位符 | 说明 |
|--------|------|
| `%dropguard_enabled%` | 是否启用保护 (是/否) |
| `%dropguard_enabled_raw%` | 是否启用保护 (true/false) |
| `%dropguard_pending%` | 是否有待确认物品 |
| `%dropguard_pending_count%` | 待确认物品数量 |
| `%dropguard_status%` | 状态文本 (带颜色) |
| `%dropguard_timeout%` | 超时时间 (秒) |

## 配置文件

```yaml
# 确认超时时间（秒）
confirm-timeout: 5

# 双击检测间隔（毫秒）
double-click-interval: 300

# 发光颜色
glow-color: YELLOW

# 新玩家默认启用保护
default-enabled: true

# 声音设置
sounds:
  enabled: true
  create: BLOCK_NOTE_BLOCK_PLING
  confirm: ENTITY_EXPERIENCE_ORB_PICKUP
  recall: ENTITY_ITEM_PICKUP
  timeout: BLOCK_NOTE_BLOCK_BASS

# 粒子效果
particles:
  enabled: true
  type: END_ROD
  count: 3

# 日志记录
logging:
  enabled: true
  file: "drops.log"

# 数据库设置（可选）
database:
  enabled: false
  host: localhost
  port: 3306
  database: minecraft
  username: root
  password: ""
  table-prefix: "dropguard_"
```

## 构建

```bash
./gradlew build
```

构建后的 jar 文件位于 `build/libs/PlayerDropGuard-1.0.0.jar`

## 开源协议

MIT License
