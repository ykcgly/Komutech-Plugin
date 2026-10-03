package tech.komutech.native_scripts.support;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.GuideHistory;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tech.komutech.KT;

/**
 * JustEnoughGuides（JEG）软依赖桥接。
 *
 * <p><b>核心结论：这里不需要任何反射去"模拟"指南路由。</b>
 *
 * <p>Slimefun 自己就提供了会正确路由的静态入口。{@code SlimefunGuide.displayItem}
 * 的字节码（javap 核对）：
 *
 * <pre>
 * Slimefun.getRegistry()
 *           .getSlimefunGuide(SURVIVAL_MODE)      // ← 查当前生效的实现
 *           .displayItem(profile, sfItem, addToHistory);
 * </pre>
 *
 * JEG 在启动时把自己的实现注册进 {@code SlimefunRegistry.guides}
 * （见 JEG 的 {@code GroupSetup}），所以这个静态方法<b>装了 JEG 就是 JEG 实现，
 * 没装就是 Slimefun 原生</b>。前一版手写 {@code asJEG} + 反射 + Method 缓存
 * 是在重复实现 Slimefun 已有的能力，且因此踩了三个坑（选错 displayItem 重载、
 * 漏转换、异常被吞）。现已全部删除。
 *
 * <p><b>为什么不能 {@code new SurvivalSlimefunGuide(...)}</b>：
 * 那是硬编码的原生实现，字节码里没有「查当前生效实现」的分支，装了 JEG 也弹原生页。
 *
 * <p><b>为什么必须走 SlimefunItem 版重载</b>：
 * 物品组里的材料/产物要先用 {@code SlimefunItem.getByItem(ItemStack)} 还原成
 * SlimefunItem 才有归属信息（属于哪个 ItemGroup、对应什么配方）。
 * 直接传 ItemStack 会走 default 分支，附属物品尤其容易取不到归属而静默失败。
 *
 * <p><b>仍需反射的一处</b>：{@code openMainMenu} 与 {@code goBack} 在 JEG
 * 与原生实现上行为不同（要保持玩家停在 JEG 界面内），但 Slimefun 的
 * {@code SlimefunGuide.openMainMenu} 静态方法同样走注册表，直接用即可 ——
 * {@link #back} 因此也不需要反射。
 */
public final class JegBridge {

   private static final String JEG_PLUGIN = "JustEnoughGuide";

   private static volatile Boolean jegInstalled = null;

   /** 诊断开关：true 时跳转链路的每次进出都打 INFO 日志，日常运行保持 false。 */
   public static boolean DEBUG = false;

   private JegBridge() {
   }

   /** JEG 是否已安装并启用。仅用于日志与调试，玩家可见行为无需依赖它。 */
   public static boolean isAvailable() {
      Boolean cached = jegInstalled;
      if (cached == null) {
         cached = Bukkit.getPluginManager().getPlugin(JEG_PLUGIN) != null
            && Bukkit.getPluginManager().isPluginEnabled(JEG_PLUGIN);
         jegInstalled = cached;
      }

      return cached;
   }

   /**
    * 打开某个物品的配方展示页。
    *
    * <p>装 JEG 时进 JEG 的配方页，没装则进 Slimefun 原生页 —— 由 Slimefun 注册表决定，
    * 本类不做任何分支。之所以还要保留返回值，是为了让调用方能在
    * 「PlayerProfile 取不到」时决定是否回退。
    *
    * <p><b>原版材料同样支持</b>：无 Slimefun 身份的材料（末影之眼、钻石等）经
    * {@link SlimefunGuide#displayItem(PlayerProfile, ItemStack, boolean)} 的 ItemStack 重载
    * 路由，JEG 会展示其原版合成配方页（该物品没有任何原版配方时 JEG 静默，属正常现象）。
    * 之前直接对原版材料返回 false，是「点材料毫无反应」的根因之一。
    *
    * @return true 表示已成功打开
    */
   public static boolean displayItem(Player player, ItemStack item) {
      if (player == null || item == null || item.getType().isAir()) {
         return false;
      }

      // 还原成 SlimefunItem：附属物品必须靠它才能定位 ItemGroup 与配方
      SlimefunItem sfItem = SlimefunItem.getByItem(item);

      Optional<PlayerProfile> found = PlayerProfile.find(player);
      if (found.isEmpty()) {
         debug("跳转放弃: 玩家无 PlayerProfile, item=" + item.getType());
         return false;
      }

      try {
         if (sfItem != null) {
            // 关键：走 Slimefun 的静态入口，它内部查注册表 → 装了 JEG 自动是 JEG 实现
            SlimefunGuide.displayItem(found.get(), sfItem, true);
            debug("跳转(粘液物品): " + sfItem.getId());
         } else {
            // 原版材料：同样走静态入口，JEG 的 ItemStack 路径展示原版配方页
            SlimefunGuide.displayItem(found.get(), item, true);
            debug("跳转(原版物品): " + item.getType() + "（该物品无原版配方时 JEG 静默）");
         }

         return true;
      } catch (RuntimeException ex) {
         // 不静默吞：兼容层出问题必须留日志，否则线上只表现为「点了没反应」
         warn("打开物品配方页失败: " + (sfItem != null ? sfItem.getId() : item.getType()), ex);
         return false;
      }
   }

   private static void debug(String msg) {
      if (DEBUG) {
         Bukkit.getLogger().info("[JEG-诊断] " + msg);
      }
   }

   /**
    * 配方展示界面的返回按钮：左键返回上一页、Shift+左键回主菜单。
    *
    * <p>同样走注册表：{@code SlimefunGuide.openMainMenu} 静态方法内部取当前生效实现，
    * JEG 环境下返回会留在 JEG 界面内，history 也不会错位。
    */
   public static boolean back(PlayerProfile profile, Player player, boolean shift) {
      if (profile == null || player == null) {
         return false;
      }

      try {
         GuideHistory history = profile.getGuideHistory();
         if (shift) {
            // openMainMenu 需要 guideMode，这里用玩家当前模式；
            // 未记录时按 SURVIVAL_MODE，与 JEG GuideUtil 的兜底一致
            SlimefunGuide.openMainMenu(profile, SlimefunGuideMode.SURVIVAL_MODE, history.getMainMenuPage());
         } else {
            // goBack 需要传「当前生效的实现」对象，让 JEG 与原生各自处理
            io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideImplementation guideImpl =
               Slimefun.getRegistry().getSlimefunGuide(SlimefunGuideMode.SURVIVAL_MODE);
            if (guideImpl == null) {
               return false;
            }

            history.goBack(guideImpl);
         }

         return true;
      } catch (RuntimeException ex) {
         warn("返回按钮失败（shift=" + shift + "）", ex);
         return false;
      }
   }

   /**
    * 打一条 JEG 相关警告。
    *
    * <p>前一版把异常全静默吞掉，结果是「点了没反应」且毫无线索 ——
    * 这类兼容层必须留日志，否则线上无从排查。
    */
   private static void warn(String msg, Throwable t) {
      try {
         if (KT.plugin() != null) {
            KT.plugin().getLogger().log(Level.WARNING, "[JEG] " + msg, t);
         } else {
            Bukkit.getLogger().log(Level.WARNING, "[JEG] " + msg, t);
         }
      } catch (RuntimeException ignored) {
         // 日志本身失败不应影响游戏运行
      }
   }

   /** 仅供测试/热重载场景重置缓存。 */
   public static void resetCache() {
      jegInstalled = null;
   }
}
