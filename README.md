# VoteRestart - 投票重启插件

一个功能完整的Minecraft Spigot插件，允许玩家通过投票系统来重启服务器。

## 功能特点

- 🗳️ **民主投票**: 玩家可以发起和参与服务器重启投票
- ⏰ **时间限制**: 可配置的投票时间限制，防止无效投票
- 🔢 **票数控制**: 可自定义所需的最少投票数
- 📢 **实时广播**: 实时显示投票进度和剩余时间
- 🛡️ **权限控制**: 完整的权限节点系统
- ⚙️ **配置灵活**: 丰富的配置选项
- 🔄 **热重载**: 支持配置文件热重载
- 🚪 **智能处理**: 玩家离线时自动移除其投票
- ⏱️ **安全重启**: 重启前的倒计时和玩家踢出机制

## 系统要求

- Minecraft 1.16+
- Spigot/Paper 服务器
- Java 8+

## 安装方法

1. 下载 `VoteRestart.jar` 文件
2. 将文件放入服务器的 `plugins` 文件夹
3. 重启服务器或使用 `/reload` 命令
4. 插件会自动生成配置文件

## 配置文件

配置文件位于 `plugins/VoteRestart/config.yml`：

```yaml
# 基础设置
enabled: true                    # 是否启用插件功能
require-permission: false        # 是否需要权限才能投票

# 投票设置
required-votes: 3               # 重启所需的投票数
vote-time-limit: 60             # 投票时间限制（秒）
allow-cancel-vote: true         # 是否允许取消投票

# 重启设置
restart-delay: 10               # 投票通过后的重启延迟（秒）

# 显示设置
broadcast-votes: true           # 是否广播每次投票
```

## 命令

| 命令 | 描述 | 权限 |
|------|------|------|
| `/voterestart` | 投票支持重启（无投票时开始投票） | `voterestart.vote` |
| `/voterestart start` | 开始新的投票 | `voterestart.start` |
| `/voterestart cancel` | 取消当前投票 | `voterestart.cancel` |
| `/voterestart status` | 查看投票状态 | `voterestart.status` |
| `/voterestart reload` | 重新加载配置 | `voterestart.reload` |
| `/voterestart info` | 显示插件信息 | `voterestart.info` |

### 命令别名

- `/vr` - `/voterestart` 的简短别名
- `/restart` - `/voterestart` 的别名

## 权限节点

| 权限 | 描述 | 默认 |
|------|------|------|
| `voterestart.vote` | 投票重启服务器 | true |
| `voterestart.start` | 开始投票重启 | true |
| `voterestart.cancel` | 取消投票重启 | op |
| `voterestart.status` | 查看投票状态 | true |
| `voterestart.reload` | 重新加载配置 | op |
| `voterestart.info` | 查看插件信息 | true |
| `voterestart.admin` | 管理员权限 | op |
| `voterestart.*` | 所有权限 | op |

## 使用流程

### 1. 发起投票
玩家使用 `/voterestart` 命令发起重启投票：
```
=== 服务器重启投票 ===
PlayerName 发起了服务器重启投票！
需要 3 票，当前 1 票
使用 /voterestart 投票支持
投票将在 60 秒后结束
```

### 2. 参与投票
其他玩家使用 `/voterestart` 命令投票支持：
```
PlayerName2 投票支持重启！ (2/3)
```

### 3. 投票通过
当达到所需票数时：
```
投票通过！服务器将在 10 秒后重启！
服务器将在 5 秒后重启！
服务器正在重启...
```

### 4. 投票失败
如果时间超时：
```
投票时间结束！重启投票失败。
```

## 特殊功能

### 智能投票管理
- **防重复投票**: 每个玩家只能投一票
- **离线处理**: 玩家离开服务器时自动移除其投票
- **自动取消**: 所有投票者离开时自动取消投票

### 实时提醒
- 每15秒提醒投票进度
- 最后10秒每秒提醒
- 重启倒计时提醒

### 安全重启
- 重启前踢出所有玩家
- 显示友好的踢出消息
- 延迟执行确保安全关闭

## 配置示例

### 快速投票模式
```yaml
required-votes: 2
vote-time-limit: 30
restart-delay: 5
```

### 严格投票模式
```yaml
required-votes: 5
vote-time-limit: 120
restart-delay: 30
require-permission: true
```

### 静默模式
```yaml
broadcast-votes: false
allow-cancel-vote: false
```

## 开发信息

- **版本**: 1.0.0
- **作者**: YourName
- **API版本**: 1.16-1.20
- **开源协议**: MIT License

## 更新日志

### v1.0.0
- 初始版本发布
- 完整的投票系统
- 权限控制和配置系统
- 智能投票管理
- 安全重启机制

## 故障排除

### 常见问题

**Q: 投票无法开始？**
A: 检查 `enabled` 配置是否为 `true`，确认玩家有 `voterestart.vote` 权限。

**Q: 服务器没有重启？**
A: 确认服务器有重启脚本，或者使用支持自动重启的服务器管理工具。

**Q: 权限不生效？**
A: 检查权限插件配置，确认权限节点正确设置。

### 调试模式
在 `config.yml` 中启用调试：
```yaml
debug: true
```

## 支持与反馈

如果您遇到问题或有建议，请：

1. 检查配置文件是否正确
2. 确认权限节点设置
3. 查看服务器日志中的错误信息
4. 联系插件作者获取支持

## 许可证

本插件采用 MIT 许可证开源。您可以自由使用、修改和分发。