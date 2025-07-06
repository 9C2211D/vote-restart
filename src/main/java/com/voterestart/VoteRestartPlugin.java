package com.voterestart;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.logging.Level;

public class VoteRestartPlugin extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    
    private boolean pluginEnabled = true;
    private boolean voteInProgress = false;
    private int requiredVotes = 3;
    private int voteTimeLimit = 60; // 秒
    private int restartDelay = 10; // 秒
    private boolean requirePermission = false;
    private boolean broadcastVotes = true;
    private boolean allowCancelVote = true;
    
    private Set<UUID> votedPlayers = new HashSet<>();
    private BukkitTask voteTask;
    private BukkitTask restartTask;
    private int remainingTime;
    
    @Override
    public void onEnable() {
        // 保存默认配置
        saveDefaultConfig();
        
        // 加载配置
        loadConfiguration();
        
        // 注册事件监听器
        Bukkit.getPluginManager().registerEvents(this, this);
        
        // 注册命令执行器
        getCommand("voterestart").setExecutor(this);
        getCommand("voterestart").setTabCompleter(this);
        
        getLogger().info("VoteRestart 插件已启用！");
        getLogger().info("需要投票数: " + requiredVotes + " 票");
        getLogger().info("投票时间限制: " + voteTimeLimit + " 秒");
    }
    
    @Override
    public void onDisable() {
        // 取消所有任务
        if (voteTask != null) {
            voteTask.cancel();
        }
        if (restartTask != null) {
            restartTask.cancel();
        }
        
        getLogger().info("VoteRestart 插件已禁用！");
    }
    
    /**
     * 加载配置文件
     */
    private void loadConfiguration() {
        reloadConfig();
        
        pluginEnabled = getConfig().getBoolean("enabled", true);
        requiredVotes = getConfig().getInt("required-votes", 3);
        voteTimeLimit = getConfig().getInt("vote-time-limit", 60);
        restartDelay = getConfig().getInt("restart-delay", 10);
        requirePermission = getConfig().getBoolean("require-permission", false);
        broadcastVotes = getConfig().getBoolean("broadcast-votes", true);
        allowCancelVote = getConfig().getBoolean("allow-cancel-vote", true);
        
        // 验证配置值
        if (requiredVotes < 1) requiredVotes = 1;
        if (voteTimeLimit < 10) voteTimeLimit = 10;
        if (restartDelay < 5) restartDelay = 5;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("voterestart")) {
            if (args.length == 0) {
                return handleVoteCommand(sender);
            }
            
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "start":
                    return handleStartVote(sender);
                case "cancel":
                    return handleCancelVote(sender);
                case "status":
                    return handleStatusCommand(sender);
                case "reload":
                    return handleReloadCommand(sender);
                case "info":
                    return handleInfoCommand(sender);
                default:
                    return handleVoteCommand(sender);
            }
        }
        
        return false;
    }
    
    /**
     * 处理投票命令
     */
    private boolean handleVoteCommand(CommandSender sender) {
        if (!pluginEnabled) {
            sender.sendMessage(ChatColor.RED + "投票重启功能已禁用！");
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以投票！");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (requirePermission && !player.hasPermission("voterestart.vote")) {
            player.sendMessage(ChatColor.RED + "你没有权限投票重启！");
            return true;
        }
        
        if (!voteInProgress) {
            return startVote(player);
        } else {
            return addVote(player);
        }
    }
    
    /**
     * 开始投票
     */
    private boolean startVote(Player initiator) {
        voteInProgress = true;
        votedPlayers.clear();
        votedPlayers.add(initiator.getUniqueId());
        remainingTime = voteTimeLimit;
        
        // 广播投票开始
        String message = ChatColor.YELLOW + "=== 服务器重启投票 ===\n" +
                        ChatColor.GREEN + initiator.getName() + " 发起了服务器重启投票！\n" +
                        ChatColor.AQUA + "需要 " + requiredVotes + " 票，当前 1 票\n" +
                        ChatColor.AQUA + "使用 /voterestart 投票支持\n" +
                        ChatColor.GRAY + "投票将在 " + voteTimeLimit + " 秒后结束";
        
        Bukkit.broadcastMessage(message);
        
        // 启动投票倒计时
        startVoteCountdown();
        
        return true;
    }
    
    /**
     * 添加投票
     */
    private boolean addVote(Player player) {
        if (votedPlayers.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "你已经投过票了！");
            return true;
        }
        
        votedPlayers.add(player.getUniqueId());
        
        if (broadcastVotes) {
            String message = ChatColor.GREEN + player.getName() + " 投票支持重启！" +
                           ChatColor.AQUA + " (" + votedPlayers.size() + "/" + requiredVotes + ")";
            Bukkit.broadcastMessage(message);
        } else {
            player.sendMessage(ChatColor.GREEN + "投票成功！当前票数: " + votedPlayers.size() + "/" + requiredVotes);
        }
        
        // 检查是否达到所需票数
        if (votedPlayers.size() >= requiredVotes) {
            executeRestart();
        }
        
        return true;
    }
    
    /**
     * 开始投票倒计时
     */
    private void startVoteCountdown() {
        voteTask = new BukkitRunnable() {
            @Override
            public void run() {
                remainingTime--;
                
                if (remainingTime <= 0) {
                    // 投票时间结束
                    voteInProgress = false;
                    votedPlayers.clear();
                    
                    Bukkit.broadcastMessage(ChatColor.RED + "投票时间结束！重启投票失败。");
                    this.cancel();
                } else if (remainingTime % 15 == 0 || remainingTime <= 10) {
                    // 定期提醒
                    String message = ChatColor.YELLOW + "重启投票进行中！" +
                                   ChatColor.AQUA + " (" + votedPlayers.size() + "/" + requiredVotes + ") " +
                                   ChatColor.GRAY + "剩余时间: " + remainingTime + " 秒";
                    Bukkit.broadcastMessage(message);
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }
    
    /**
     * 执行重启
     */
    private void executeRestart() {
        // 取消投票任务
        if (voteTask != null) {
            voteTask.cancel();
        }
        
        voteInProgress = false;
        
        Bukkit.broadcastMessage(ChatColor.GREEN + "投票通过！服务器将在 " + restartDelay + " 秒后重启！");
        
        // 开始重启倒计时
        restartTask = new BukkitRunnable() {
            int countdown = restartDelay;
            
            @Override
            public void run() {
                if (countdown <= 0) {
                    Bukkit.broadcastMessage(ChatColor.RED + "服务器正在重启...");
                    
                    // 踢出所有玩家
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.kickPlayer(ChatColor.RED + "服务器重启中，请稍后重新连接！");
                    }
                    
                    // 延迟重启服务器
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Bukkit.getServer().shutdown();
                        }
                    }.runTaskLater(VoteRestartPlugin.this, 20L);
                    
                    this.cancel();
                } else {
                    if (countdown <= 5 || countdown % 5 == 0) {
                        Bukkit.broadcastMessage(ChatColor.RED + "服务器将在 " + countdown + " 秒后重启！");
                    }
                    countdown--;
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }
    
    /**
     * 处理开始投票命令
     */
    private boolean handleStartVote(CommandSender sender) {
        if (!sender.hasPermission("voterestart.start")) {
            sender.sendMessage(ChatColor.RED + "你没有权限开始投票！");
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以开始投票！");
            return true;
        }
        
        if (voteInProgress) {
            sender.sendMessage(ChatColor.RED + "已有投票正在进行中！");
            return true;
        }
        
        return startVote((Player) sender);
    }
    
    /**
     * 处理取消投票命令
     */
    private boolean handleCancelVote(CommandSender sender) {
        if (!sender.hasPermission("voterestart.cancel")) {
            sender.sendMessage(ChatColor.RED + "你没有权限取消投票！");
            return true;
        }
        
        if (!voteInProgress) {
            sender.sendMessage(ChatColor.RED + "当前没有进行中的投票！");
            return true;
        }
        
        if (!allowCancelVote) {
            sender.sendMessage(ChatColor.RED + "投票取消功能已禁用！");
            return true;
        }
        
        // 取消投票
        voteInProgress = false;
        votedPlayers.clear();
        
        if (voteTask != null) {
            voteTask.cancel();
        }
        
        Bukkit.broadcastMessage(ChatColor.RED + sender.getName() + " 取消了重启投票！");
        
        return true;
    }
    
    /**
     * 处理状态命令
     */
    private boolean handleStatusCommand(CommandSender sender) {
        if (!sender.hasPermission("voterestart.status")) {
            sender.sendMessage(ChatColor.RED + "你没有权限查看投票状态！");
            return true;
        }
        
        if (!voteInProgress) {
            sender.sendMessage(ChatColor.YELLOW + "当前没有进行中的投票。");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "=== 投票状态 ===");
            sender.sendMessage(ChatColor.GREEN + "当前票数: " + votedPlayers.size() + "/" + requiredVotes);
            sender.sendMessage(ChatColor.GREEN + "剩余时间: " + remainingTime + " 秒");
            
            if (sender.hasPermission("voterestart.admin")) {
                sender.sendMessage(ChatColor.GRAY + "已投票玩家: ");
                for (UUID uuid : votedPlayers) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        sender.sendMessage(ChatColor.GRAY + "- " + player.getName());
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * 处理重载命令
     */
    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("voterestart.reload")) {
            sender.sendMessage(ChatColor.RED + "你没有权限重新加载配置！");
            return true;
        }
        
        loadConfiguration();
        sender.sendMessage(ChatColor.GREEN + "VoteRestart 配置已重新加载！");
        sender.sendMessage(ChatColor.GREEN + "插件状态: " + (pluginEnabled ? "启用" : "禁用"));
        
        return true;
    }
    
    /**
     * 处理信息命令
     */
    private boolean handleInfoCommand(CommandSender sender) {
        if (!sender.hasPermission("voterestart.info")) {
            sender.sendMessage(ChatColor.RED + "你没有权限查看插件信息！");
            return true;
        }
        
        sender.sendMessage(ChatColor.YELLOW + "=== VoteRestart 插件信息 ===");
        sender.sendMessage(ChatColor.GREEN + "版本: " + getDescription().getVersion());
        sender.sendMessage(ChatColor.GREEN + "状态: " + (pluginEnabled ? ChatColor.GREEN + "启用" : ChatColor.RED + "禁用"));
        sender.sendMessage(ChatColor.GREEN + "需要票数: " + requiredVotes);
        sender.sendMessage(ChatColor.GREEN + "投票时限: " + voteTimeLimit + " 秒");
        sender.sendMessage(ChatColor.GREEN + "重启延迟: " + restartDelay + " 秒");
        sender.sendMessage(ChatColor.GREEN + "需要权限: " + (requirePermission ? "是" : "否"));
        sender.sendMessage(ChatColor.GREEN + "广播投票: " + (broadcastVotes ? "是" : "否"));
        sender.sendMessage(ChatColor.GREEN + "允许取消: " + (allowCancelVote ? "是" : "否"));
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("voterestart")) {
            if (args.length == 1) {
                List<String> completions = new ArrayList<>();
                
                if (sender.hasPermission("voterestart.start")) {
                    completions.add("start");
                }
                if (sender.hasPermission("voterestart.cancel")) {
                    completions.add("cancel");
                }
                if (sender.hasPermission("voterestart.status")) {
                    completions.add("status");
                }
                if (sender.hasPermission("voterestart.reload")) {
                    completions.add("reload");
                }
                if (sender.hasPermission("voterestart.info")) {
                    completions.add("info");
                }
                
                return completions;
            }
        }
        return null;
    }
    
    /**
     * 玩家退出事件
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (voteInProgress) {
            UUID playerUUID = event.getPlayer().getUniqueId();
            if (votedPlayers.contains(playerUUID)) {
                votedPlayers.remove(playerUUID);
                
                if (broadcastVotes) {
                    String message = ChatColor.RED + event.getPlayer().getName() + " 离开了游戏，投票被移除！" +
                                   ChatColor.AQUA + " (" + votedPlayers.size() + "/" + requiredVotes + ")";
                    Bukkit.broadcastMessage(message);
                }
                
                // 如果投票数不足，检查是否需要结束投票
                if (votedPlayers.isEmpty()) {
                    voteInProgress = false;
                    if (voteTask != null) {
                        voteTask.cancel();
                    }
                    Bukkit.broadcastMessage(ChatColor.RED + "所有投票者都已离开，投票自动取消！");
                }
            }
        }
    }
    
    /**
     * 重新加载配置
     */
    public void reloadConfiguration() {
        loadConfiguration();
    }
}