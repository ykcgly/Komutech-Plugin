package tech.komutech.load;

import java.util.Locale;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import tech.komutech.KT;
import tech.komutech.behavior.BlockDrops;
import tech.komutech.listeners.SingleItemRecipeGuideListener;
import tech.komutech.native_scripts.NativeScriptRegistry;
import tech.komutech.native_scripts.support.KomutechAdminPassword;

/**
 * 内容加载编排（对齐 WorldTaste 的 Setup）：
 * 分组 → 配方类型 → 预加载展示物品 → 各内容文件 → 行为监听器 → 释放加载期缓存。
 */
public final class Setup {

    private Setup() {}

    /**
     * 含「可被其它配方以 material_type:slimefun 引用」的物品的文件，需先预加载展示堆。
     *
     * <p>注意：只列 content/ 下实际存在的文件。原 addon 目录还有 foods/mob_drops/
     * generations 等文件，但本服内容包里并没有它们，写了只会多打一条「资源缺失」日志。
     */
    private static final String[] ITEM_FILES = {
        "items.yml", "machines.yml", "geo_resources.yml", "armors.yml", "mat_generators.yml",
        "recipe_machines.yml", "mb_machines.yml", "template_machines.yml", "workbenches.yml"
    };

    public static void loadAll() {
        long t = System.currentTimeMillis();
        MissingItems.reset();
        // 脚本单例必须在解析任何 script: 字段之前就绪（原实现在 onLoad 阶段 bootstrap）
        NativeScriptRegistry.bootstrap();
        GroupLoader.load();
        RecipeTypes.load();
        preloadDisplays();
        // 盔甲部件 id 是 <套id>_<部件>，与 items.yml 的顶层 id 规则不同，需单独预加载
        ArmorsLoader.preload(Yaml.loadResource(KT.plugin, "armors.yml"));
        ItemsLoader.load();
        ArmorsLoader.load();
        GeoResourcesLoader.load();
        // 菜单必须早于机器：机器按 upper(id) 从 KT.menus 取菜单
        MenusLoader.load();
        MachinesLoader.load();
        MaterialGeneratorsLoader.load();
        RecipeMachinesLoader.load();
        WorkbenchesLoader.load();
        TemplateMachinesLoader.load();
        MultiBlockMachinesLoader.load();
        BlockDrops.register(KT.plugin);
        // 单物品配方引导监听器：机器 GUI 里的「多物品输入 / 多物品输出」按钮由它接管点击，
        // 打开配方展示界面。该类在构造函数里自行 registerEvents，因此只需实例化一次。
        // 漏掉这行 = 按钮点击毫无反应（重构时整块缺失，是「多物品输入界面打不开」的根因）。
        new SingleItemRecipeGuideListener();
        // 加载期缓存释放：解析树、头颅贴图、展示堆表（运行期均不再访问）
        Yaml.clearCache();
        Read.clearSkinCache();
        MissingItems.report();
        KT.preload.clear();
        // 配置安全自检：确保 plugins/Komutech/ 已生成，未配置的高危操作密码给出告警
        KomutechAdminPassword.warnIfUnconfigured();
        // 运行期交互层注册：管理命令、菜单路由、聊天输入、异步调度器、脚本监听器。
        // 必须在内容全部注册完成之后再注册——菜单路由要按 id 从 KT.menus 取菜单；
        // 且异步调度器未启动时 submit() 会静默丢弃任务（不报错），导致删除/清空等
        // 高危操作「看似校验了密码却根本没执行」，安全形同虚设。
        NativeScriptRegistry.registerLifecycleListeners(KT.plugin);
        KT.plugin.getLogger().info("基础内容加载完成，耗时 " + (System.currentTimeMillis() - t) + "ms");
    }

    /** 第一遍：把各物品/机器的展示堆加入 KT.preload，使后续配方解析能跨文件按 id 引用。 */
    private static void preloadDisplays() {
        int n = 0;
        for (String file : ITEM_FILES) {
            YamlConfiguration y = Yaml.loadResource(KT.plugin, file);
            // 逐条 try/catch 故障隔离：单条坏展示数据只跳过该项，不会导致后续物品全部因
            // preload 查空而被跳过（插件近乎空载启用）
            for (String id : y.getKeys(false)) {
                try {
                    ConfigurationSection s = y.getConfigurationSection(id);
                    if (s == null) continue;
                    ConfigurationSection itemSec = s.getConfigurationSection("item");
                    if (itemSec == null) continue;
                    ItemStack display = Read.item(itemSec, false);
                    if (display == null) continue;
                    String effId = KT.upper(s.getString("id_alias", id));
                    KT.preload.put(effId, display);
                    n++;
                } catch (Exception e) {
                    KT.log("预加载展示物品 " + id + " 失败，跳过: " + e);
                }
            }
        }
        KT.plugin.getLogger().info("预加载展示物品: " + n);
    }

    static String lower(String v) {
        return v == null ? null : v.toLowerCase(Locale.ROOT);
    }
}
