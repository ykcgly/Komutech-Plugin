package tech.komutech;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import tech.komutech.commands.KmCommand;
import tech.komutech.load.Setup;
import tech.komutech.native_scripts.NativeScriptRegistry;
import tech.komutech.util.Store;

/**
 * 口木科技 Komutech —— 独立 Slimefun 附属插件主类（对齐 WorldTaste 的单插件自包含形态）。
 *
 * <p>与旧版 KomutechNative 的区别：不再解压 addons/ 目录、不再扫描/加载多个附属文件夹、
 * 不再有 Graal/JS 脚本层与依赖轮询等待。内容 YAML 直接打进 jar 根目录，
 * 启动即由 {@link Setup#loadAll()} 一次性注册。</p>
 */
public final class KomutechPlugin extends JavaPlugin implements SlimefunAddon {

    private static KomutechPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        KT.plugin = this;
        getLogger().info("口木科技 开始加载（独立版）...");
        if (!checkSlimefun()) return;
        try {
            Setup.loadAll();
            registerCommands();
            getLogger().info("口木科技 加载成功");
        } catch (Throwable e) {
            getLogger().severe("口木科技 加载过程中出现异常: " + e);
            e.printStackTrace();
        }
    }

    /** Slimefun 为硬依赖：缺失则卸载自身，避免后续注册全部失败。 */
    private boolean checkSlimefun() {
        if (Bukkit.getPluginManager().getPlugin("Slimefun") == null) {
            getLogger().severe("缺少 Slimefun（粘液科技），口木科技已自动卸载！");
            Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }
        return true;
    }

    /** 注册 {@code /km}（数据管理，含从 RSC 导入）。 */
    private void registerCommands() {
        try {
            KmCommand km = new KmCommand();
            org.bukkit.command.PluginCommand cmd = getCommand("km");
            if (cmd == null) {
                getLogger().warning("km 指令未在 plugin.yml 中声明，无法注册");
                return;
            }
            cmd.setExecutor(km);
            cmd.setTabCompleter(km);
        } catch (Throwable e) {
            getLogger().warning("注册 km 指令时出现异常: " + e);
        }
    }

    @Override
    public void onDisable() {
        // 停掉异步调度器，避免插件卸载后线程仍在跑
        try {
            NativeScriptRegistry.shutdown();
        } catch (Throwable var1) {
            getLogger().warning("关闭异步调度器时出现异常: " + var1);
        }
        // 先把待写文档落库再关连接，否则停服会丢最近 5 分钟的进度
        try {
            Store.shutdown();
        } catch (Throwable e) {
            getLogger().warning("关闭 SQLite 存储时出现异常: " + e);
        }
        getLogger().info("口木科技 已卸载");
    }

    public static KomutechPlugin getInstance() {
        return instance;
    }

    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Override
    public String getBugTrackerURL() {
        return "";
    }
}
