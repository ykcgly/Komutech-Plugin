package tech.komutech.jeg;

import com.balugaq.jeg.api.objects.events.GuideEvents;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import tech.komutech.listeners.BigRecipeMenu;
import tech.komutech.listeners.SingleItemRecipeGuideListener;
import tech.komutech.native_scripts.support.JegBridge;
import tech.komutech.objects.slimefun.ItemGroupButton;
import tech.komutech.objects.customs.machine.CustomLinkedRecipeMachine;
import tech.komutech.objects.customs.machine.CustomRecipeMachine;
import tech.komutech.objects.customs.machine.CustomTemplateMachine;
import tech.komutech.objects.customs.machine.CustomWorkbench;

/**
 * JEG 指南事件拦截（对齐尘世百味 {@code JegGuideListener}）：
 *
 * <p>JEG 在指南中点击任何物品前都会触发 {@link GuideEvents.ItemButtonClickEvent}
 * （包括机器物品页配方预览区的「多物品输入/输出」面板——JEG 对无 Slimefun 身份的
 * 展示物品也会挂它的点击处理器并发事件），取消事件即拦下 JEG 的默认行为。本监听器借此补上
 * 两处 JEG 默认页做不到的事：
 * <ol>
 *   <li><b>点「多物品输入/输出」面板</b>：JEG 默认处理对无 Slimefun 身份的面板是空操作
 *       （点了没反应）。这里根据面板 PDC 标记直接打开对应配方展示页，机器从当前
 *       指南菜单里按「标记类型匹配 + 槽位 16 优先」还原（JEG 布局是配置驱动的，
 *       不能硬编码槽位）；</li>
 *   <li><b>点大型配方机器</b>：JEG 默认页只放得下 9 个输入格，联动配方的多余材料
 *       全部不可见。这里改开 {@link BigRecipeMenu} 的机器级完整视图
 *       （合成配方页 → 产物列表页 → 配方页）。普通机器保留 JEG 默认展示——
 *       那边有搜索、收藏、配方树等完整功能。</li>
 * </ol>
 *
 * <p><b>兜底路径</b>：部分 JEG 构建/点击路径下 {@code ItemButtonClickEvent} 可能
 * 不派发（实测有「监听器注册成功但点击无反应」的反馈）。因此另挂一条
 * {@link InventoryClickEvent} 兜底：仅当点击发生在「裸 {@code ChestMenu} 界面」
 * （JEG/原版指南页都是；Slimefun 机器 GUI 是 {@link DirtyChestMenu} 子类，被排除，
 * 不影响机器内既有交互）时才处理，且与事件路径共用去重标记，不会重复打开。</p>
 *
 * <p>仅当 JEG 存在时由 {@link #register()} 注册本类（本类直接引用 JEG API，
 * 类加载必须晚于可用性检查）。</p>
 */
public final class JegGuideListener implements Listener {

    /**
     * 诊断开关：true 时每次指南点击/兜底命中都打 INFO 日志。
     * 排查「事件是否到达、卡在哪一环」用，日常运行保持 false。
     */
    public static boolean DEBUG = false;

    private JegGuideListener() {}

    /** 仅在 JEG 可用时调用（见 {@link JegHook#available()}）。 */
    public static void register() {
        if (JegHook.available()) {
            Bukkit.getPluginManager().registerEvents(new JegGuideListener(), tech.komutech.KT.plugin());
            tech.komutech.KT.plugin().getLogger().info("JEG 集成：指南点击拦截已启用（多物品输入面板 + 大型配方机器）");
        }
    }

    private static void debug(String msg) {
        if (DEBUG) {
            tech.komutech.KT.plugin().getLogger().info("[JEG-诊断] " + msg);
        }
    }

    // ------------------------------------------------------------------
    // 主路径：JEG 指南事件
    // ------------------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onItemClick(GuideEvents.ItemButtonClickEvent e) {
        try {
            ItemStack clicked = e.getClickedItem();
            debug("事件到达: item=" + name(clicked) + ", mode=" + mode(e.getGuide().getMode()));
            // 作弊模式点击 = 领取物品，不拦截
            if (e.getGuide().getMode() != SlimefunGuideMode.SURVIVAL_MODE) {
                debug("跳过：非生存模式（作弊模式点击=领取物品）");
                return;
            }

            if (clicked == null || clicked.getType().isAir()) {
                return;
            }

            // 1) 「多物品输入/输出」面板：按 PDC 标记打开对应配方展示页
            if (SingleItemRecipeGuideListener.isTaggedRecipeItem(clicked)) {
                SlimefunItem machine = resolveMachine(e.getMenu(), SingleItemRecipeGuideListener.tagKind(clicked));
                debug("命中标记面板, kind=" + SingleItemRecipeGuideListener.tagKind(clicked)
                    + ", 机器=" + (machine == null ? "未找到" : machine.getId()));
                if (machine != null && SingleItemRecipeGuideListener.openTaggedRecipe(e.getPlayer(), clicked, machine)) {
                    // 取消后 JEG 的 EventBuilder.ifSuccess 视为「点击已处理」，默认页不再打开
                    e.setCancelled(true);
                    debug("已打开标记配方页并取消 JEG 默认行为");
                }
                return;
            }

            // 2) 大型配方机器：改开机器级完整配方视图
            SlimefunItem sf = SlimefunItem.getByItem(clicked);
            if (sf instanceof CustomLinkedRecipeMachine machine && BigRecipeMenu.isLargeRecipeMachine(machine)) {
                if (SingleItemRecipeGuideListener.markMachineOpen(e.getPlayer(), machine)) {
                    e.setCancelled(true);
                    debug("命中大型配方机器: " + machine.getId() + "，改开完整配方视图");
                    BigRecipeMenu.openMachine(e.getPlayer(), machine, 0, null);
                }
                return;
            }

            if (sf != null) {
                debug("普通粘液物品，保留 JEG 默认页: " + sf.getId()
                    + (sf instanceof CustomLinkedRecipeMachine ? "（联动机器但无>9槽配方）" : ""));
                return;
            }

            // 3) JEG 页面里的原版材料（如常规 3x3 配方里的末影之眼）：JEG 默认不跳页，
            //    这里左键跳转到该物品的原版配方页（经 JegBridge 的 ItemStack 路由）。
            //    不取消事件——JEG 若有自己的处理仍会执行，不会互相顶掉。
            //    hasItemMeta 过滤 JEG 的界面按钮（翻页箭头/搜索/收藏等都带名字或 lore），
            //    右键留给 JEG 自己的「搜索物品作用」动作。
            if (!clicked.hasItemMeta() && !e.getClickAction().isRightClicked()) {
                debug("原版材料跳转: " + clicked.getType());
                JegBridge.displayItem(e.getPlayer(), clicked);
            }
        } catch (Throwable t) {
            debug("事件处理异常: " + t);
        }
    }

    /**
     * JEG 指南中点击物品组按钮（37 个 button 组：下载链接 / 控制台命令 / 跳转组 /
     * 展示物品 / 脚本）。按钮组的 {@link ItemGroupButton#run} 原本只挂在
     * vanilla 指南的渲染路径上，JEG 接管嵌套组渲染后点击走的是 JEG 自己的
     * 组打开逻辑（打开空组），actions 永远不执行——这里补上 JEG 路径的分发。
     */
    @EventHandler(ignoreCancelled = true)
    public void onGroupButtonClick(GuideEvents.ItemGroupButtonClickEvent e) {
        try {
            if (e.getGuide().getMode() != SlimefunGuideMode.SURVIVAL_MODE) {
                return;
            }

            ItemStack clicked = e.getClickedItem();
            if (clicked == null || clicked.getType().isAir()) {
                return;
            }

            ItemGroupButton button = ItemGroupButton.getByDisplayItem(clicked);
            if (button == null) {
                return;
            }

            // 取消后 JEG 视为「点击已处理」，不再打开按钮组背后的空物品组
            e.setCancelled(true);
            debug("命中物品组按钮: " + button.getKey().getKey());
            button.run(e.getPlayer(), e.getClickedSlot(), clicked, e.getClickAction(), SlimefunGuideMode.SURVIVAL_MODE);
        } catch (Throwable t) {
            debug("物品组按钮事件处理异常: " + t);
        }
    }

    // ------------------------------------------------------------------
    // 兜底路径：InventoryClickEvent（JEG 事件未派发时仍可拦截）
    // ------------------------------------------------------------------
    /**
     * 兜底拦截：仅处理「裸 ChestMenu 界面」里的点击。
     *
     * <p>JEG 指南页与原版指南页的 Inventory holder 都是裸 {@link ChestMenu}
     * （ChestMenu 构造时把自己传给 createInventory）；Slimefun 机器 GUI 的 holder 是
     * {@link DirtyChestMenu} 子类——排除掉，机器 GUI 内的既有交互（包括
     * {@link SingleItemRecipeGuideListener} 对「多物品输入」按钮的原生接管）完全不受影响。</p>
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInvClickFallback(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }

        // 只看顶栏容器里的点击（玩家自身物品栏的点击一律不碰）
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) {
            return;
        }

        InventoryHolder holder = e.getView().getTopInventory().getHolder();
        if (!(holder instanceof ChestMenu menu) || holder instanceof DirtyChestMenu) {
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        // 1) 「多物品输入/输出」面板（无 JEG 版本差异风险；机器 GUI 已被 DirtyChestMenu 排除）
        if (SingleItemRecipeGuideListener.isTaggedRecipeItem(clicked)) {
            int kind = SingleItemRecipeGuideListener.tagKind(clicked);
            SlimefunItem machine = resolveMachine(menu, kind);
            debug("兜底命中标记面板, kind=" + kind + ", 机器=" + (machine == null ? "未找到" : machine.getId()));
            if (machine != null) {
                SingleItemRecipeGuideListener.openTaggedRecipe(player, clicked, machine);
            }
            return;
        }

        // 2) 大型配方机器：仅在 JEG 可用且玩家当前处于 JEG 生存指南时接管，
        //    且要求界面标题是指南标题——避免把聊天/其他插件菜单里的机器误当指南点击
        SlimefunItem sf = SlimefunItem.getByItem(clicked);
        if (!(sf instanceof CustomLinkedRecipeMachine machine) || !BigRecipeMenu.isLargeRecipeMachine(machine)) {
            return;
        }

        if (!JegHook.available()) {
            return;
        }

        try {
            Object lastGuide = com.balugaq.jeg.utils.GuideUtil.getLastGuide(player);
            if (!(lastGuide instanceof io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideImplementation impl)
                || impl.getMode() != SlimefunGuideMode.SURVIVAL_MODE) {
                return;
            }
        } catch (Throwable t) {
            return; // JEG API 变更：兜底路径静默放弃，不影响正常游戏
        }

        // 标题判据：JEG 指南页标题 = GuideUtil.getGuideTitle(生存模式)（JEG create0 同源）。
        // 双方都去色后比较，&/§ 颜色码差异不影响匹配；其它插件的裸 ChestMenu 菜单
        // （如展示菜单里摆着机器本体）不会命中，避免劫持它们的交互。
        try {
            String expected = com.balugaq.jeg.utils.GuideUtil.getGuideTitle(SlimefunGuideMode.SURVIVAL_MODE);
            String actualTitle = e.getView().getTitle();
            if (expected == null || actualTitle == null) {
                return;
            }

            String strippedExpected = org.bukkit.ChatColor.stripColor(
                org.bukkit.ChatColor.translateAlternateColorCodes('&', expected));
            String strippedActual = org.bukkit.ChatColor.stripColor(actualTitle);
            if (strippedExpected == null || strippedActual == null
                || !strippedActual.contains(strippedExpected)) {
                return;
            }
        } catch (Throwable t) {
            return;
        }

        if (SingleItemRecipeGuideListener.markMachineOpen(player, machine)) {
            debug("兜底命中大型配方机器: " + machine.getId() + "，改开完整配方视图");
            BigRecipeMenu.openMachine(player, machine, 0, null);
        }
    }

    // ------------------------------------------------------------------
    // 工具
    // ------------------------------------------------------------------

    /**
     * 从指南菜单中还原「当前正在查看的机器」。
     *
     * <p>标记只携带配方下标不携带机器 id，只能从页面反查。优先槽位 16
     * （Slimefun 原生物品页的被查看物品位置，JEG 默认布局沿用），
     * 其次按槽位顺序取第一个<b>类型与标记一致</b>的口木机器——物品页的合成材料区
     * 也可能摆着另一台机器，类型匹配能把误配概率压到最低。</p>
     */
    private static SlimefunItem resolveMachine(ChestMenu menu, int tagKind) {
        if (menu == null) {
            return null;
        }

        Class<? extends SlimefunItem> expected = switch (tagKind) {
            case 1 -> CustomRecipeMachine.class;
            case 2 -> CustomTemplateMachine.class;
            case 3 -> CustomLinkedRecipeMachine.class;
            case 4 -> CustomWorkbench.class;
            default -> null;
        };
        if (expected == null) {
            return null;
        }

        SlimefunItem fallback = null;
        for (int slot = 0; slot < 54; slot++) {
            ItemStack it = menu.getItemInSlot(slot);
            if (it == null || it.getType().isAir()) {
                continue;
            }

            SlimefunItem sf = SlimefunItem.getByItem(it);
            if (sf == null || !expected.isInstance(sf)) {
                continue;
            }

            if (slot == 16) {
                return sf;
            }
            if (fallback == null) {
                fallback = sf;
            }
        }

        return fallback;
    }

    private static String name(ItemStack it) {
        if (it == null) {
            return "null";
        }

        SlimefunItem sf = SlimefunItem.getByItem(it);
        return it.getType() + (sf != null ? "/" + sf.getId() : "");
    }

    private static String mode(SlimefunGuideMode m) {
        return m == null ? "null" : m.name();
    }
}
