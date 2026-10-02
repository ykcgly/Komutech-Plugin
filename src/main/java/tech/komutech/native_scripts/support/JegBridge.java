package tech.komutech.native_scripts.support;

import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.GuideHistory;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideImplementation;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import java.lang.reflect.InvocationTargetException;
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
 * <p><b>要解决什么</b>：配方展示界面（点机器 GUI 的「多物品输入/输出」打开的那个）
 * 里点击材料，应直达 JEG 的配方页，而不是 Slimefun 原生页。
 *
 * <p><b>为什么不能直接 {@code new SurvivalSlimefunGuide(...)}</b>：
 * 那是 Slimefun 的硬编码原生实现，字节码里没有任何「查当前生效指南实现」的分支，
 * 装了 JEG 也只会弹原生界面。
 *
 * <p><b>JEG 的正确调用链</b>（用 javap 核对字节码得出，不要凭直觉改）：
 * <ol>
 *    <li>{@code Slimefun.getRegistry().getSlimefunGuide(mode)} 拿到的是
 *        {@code SlimefunGuideImplementation}，JEG 已把自己的实现注册进去了；</li>
 *    <li>但要调用 JEG 增强过的方法，必须先经
 *        {@code GuideUtil.asJEG(SlimefunGuideImplementation)} 转成
 *        {@code JEGSlimefunGuideImplementation}；</li>
 *    <li>再调它的 {@code displayItem(PlayerProfile, SlimefunItem, boolean)}。
 *        这是 JEG 内部 {@code OnClick$Item$Normal} 点击物品时实际调用的方法
 *        （见该类字节码），保证与 JEG 自己的点击行为完全一致。</li>
 * </ol>
 *
 * <p><b>为什么 SlimefunItem 版优先于 ItemStack 版</b>：
 * JEG 的 {@code displayItem(profile, ItemStack, int, boolean)} 是接口 default 方法，
 * 转调 {@code displayItem0}；而它内部需要 SlimefunItem 才能定位配方来源。
 * 直接给 ItemStack 版在部分 JEG 版本上会走到没有内部状态的分支而静默失败 ——
 * 这正是上一版「点击后没有任何反应」的原因。
 *
 * <p><b>为什么全用反射</b>：JEG 是 compileOnly 软依赖，各服务器版本可能调整包路径
 * （Slimefun 4.13 已把大量 API 从 {@code io.github.thebusybiscuit.*} 挪到
 * {@code me.mrCookieSlime.*}）。反射保证 JEG 缺失或 API 变动时只是退回原生行为，
 * 不会让插件无法启动。首次失败会打一条 WARN 便于排查，之后不再刷屏。
 */
public final class JegBridge {

   private static final String JEG_PLUGIN = "JustEnoughGuide";
   private static final String GUIDE_UTIL = "com.balugaq.jeg.utils.GuideUtil";

   private static volatile Boolean jegEnabled = null;
   private static volatile Method asJeg = null;
   private static volatile Method displaySfItem = null;
   private static volatile Method displayStackItem = null;
   private static volatile Method openMainMenu = null;
   private static volatile boolean apiResolved = false;
   private static volatile boolean apiBroken = false;

   private JegBridge() {
   }

   /** JEG 是否已安装并启用。 */
   public static boolean isAvailable() {
      Boolean var0 = jegEnabled;
      if (var0 == null) {
         var0 = Bukkit.getPluginManager().getPlugin(JEG_PLUGIN) != null
            && Bukkit.getPluginManager().isPluginEnabled(JEG_PLUGIN);
         jegEnabled = var0;
      }

      return var0;
   }

   /**
    * 打开某个物品的配方展示页（JEG 版）。
    *
    * @return true 表示已由 JEG 接管；false 表示调用方应回退 Slimefun 原生
    */
   public static boolean displayItem(Player player, ItemStack item) {
      if (player == null || item == null || item.getType().isAir()) {
         return false;
      }

      if (!isAvailable() || !resolveApi()) {
         return false;
      }

      Optional<PlayerProfile> var1 = PlayerProfile.find(player);
      if (var1.isEmpty()) {
         return false;
      }

      // 取得 JEG 实现对象；拿不到就回退原生
      Object var2 = jegGuide(player);
      if (var2 == null) {
         return false;
      }

      try {
         // 优先 SlimefunItem 版：与 JEG 内部点击物品走的是同一个方法，行为完全一致。
         // 附属物品必须走这条——JEG 靠 SlimefunItem 反查所属 ItemGroup 与配方来源，
         // 只给 ItemStack 会丢失归属信息。
         Object var3 = io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem.getByItem(item);
         if (var3 != null && displaySfItem != null) {
            displaySfItem.invoke(var2, var1.get(), var3, true);
            return true;
         }

         if (displayStackItem != null) {
            displayStackItem.invoke(var2, var1.get(), item, 0, true);
            return true;
         }

         return false;
      } catch (InvocationTargetException var4) {
         // JEG 内部抛异常：打一条日志便于排查，不把异常吞成"点了没反应"
         warn("JEG 打开物品配方页失败: " + item.getType(), var4.getTargetException());
         return false;
      } catch (ReflectiveOperationException | RuntimeException var5) {
         warn("JEG 反射调用失败（API 可能已变更）", var5);
         return false;
      }
   }

   /**
    * 配方展示界面的返回按钮：左键返回上一页、Shift+左键回主菜单。
    *
    * <p>{@code GuideHistory#goBack} 与 {@code SlimefunGuideImplementation#openMainMenu}
    * 的参数类型都是接口，JEG 实现可直接传入，无需额外适配。
    *
    * @return true 表示已由 JEG 接管；false 表示调用方应回退原生
    */
   public static boolean back(PlayerProfile profile, Player player, boolean shift) {
      if (profile == null || player == null || !isAvailable() || !resolveApi()) {
         return false;
      }

      Object var0 = jegGuide(player);
      if (var0 == null) {
         return false;
      }

      try {
         GuideHistory var1 = profile.getGuideHistory();
         if (shift) {
            if (openMainMenu == null) {
               return false;
            }

            openMainMenu.invoke(var0, profile, var1.getMainMenuPage());
         } else {
            var1.goBack((SlimefunGuideImplementation)var0);
         }

         return true;
      } catch (InvocationTargetException var2) {
         warn("JEG 返回按钮失败（shift=" + shift + "）", var2.getTargetException());
         return false;
      } catch (ReflectiveOperationException | RuntimeException var3) {
         warn("JEG 返回按钮反射调用失败", var3);
         return false;
      }
   }

   /**
    * 取玩家当前生效的 JEG 指南实现对象。
    *
    * @return null 表示拿不到（未装 JEG / 非 JEG 实现 / API 变更），调用方应回退原生
    */
   private static Object jegGuide(Player player) {
      try {
         SlimefunGuideImplementation var0 = io.github.thebusybiscuit.slimefun4.implementation.Slimefun
            .getRegistry()
            .getSlimefunGuide(modeOf(player));
         if (var0 == null) {
            return null;
         }

         // 已是 JEG 实现则直接用；否则经 GuideUtil.asJEG 转换。
         // JEG 的 asJEG 对原生实现会返回其内部的包装实现，对 JEG 实现原样返回。
         Object var1 = asJeg.invoke(null, var0);
         return var1 != null ? var1 : var0;
      } catch (InvocationTargetException var2) {
         warn("GuideUtil.asJEG 调用失败", var2.getTargetException());
         return null;
      } catch (ReflectiveOperationException | RuntimeException var3) {
         warn("获取 JEG 指南实现失败", var3);
         return null;
      }
   }

   /** 玩家当前指南模式；未记录时按 SURVIVAL_MODE，与 JEG GuideUtil 的兜底一致。 */
   private static SlimefunGuideMode modeOf(Player player) {
      try {
         Method var0 = getLastGuideMode();
         if (var0 != null) {
            Object var1 = var0.invoke(null, player);
            if (var1 instanceof SlimefunGuideMode var2) {
               return var2;
            }
         }
      } catch (ReflectiveOperationException | RuntimeException ignored) {
         // 取不到就用默认模式
      }

      return SlimefunGuideMode.SURVIVAL_MODE;
   }

   private static Method getLastGuideMode() {
      try {
         Class<?> var0 = Class.forName(GUIDE_UTIL);
         return var0.getMethod("getLastGuideMode", Player.class);
      } catch (ReflectiveOperationException | RuntimeException var1) {
         return null;
      }
   }

   /**
    * 解析并缓存 JEG 反射入口。只跑一次；失败后置 {@code apiBroken} 不再重试，
    * 避免每次点击都走一遍反射查找。首次失败打一条 WARN。
    */
   private static boolean resolveApi() {
      if (apiResolved) {
         return !apiBroken;
      }

      synchronized (JegBridge.class) {
         if (apiResolved) {
            return !apiBroken;
         }

         apiResolved = true;
         try {
            Class<?> var0 = Class.forName(GUIDE_UTIL);
            Class<?> var1 = Class.forName("com.balugaq.jeg.api.interfaces.JEGSlimefunGuideImplementation");
            Class<?> var2 = io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem.class;

            asJeg = var0.getMethod("asJEG", SlimefunGuideImplementation.class);
            // JEG 接口上的两个 displayItem 重载，按 JEG 内部点击流程选用
            displaySfItem = var1.getMethod("displayItem", PlayerProfile.class, var2, boolean.class);
            displayStackItem = var1.getMethod(
               "displayItem", PlayerProfile.class, ItemStack.class, int.class, boolean.class);
            openMainMenu = var1.getMethod("openMainMenu", PlayerProfile.class, int.class);
            return true;
         } catch (ReflectiveOperationException | RuntimeException var3) {
            apiBroken = true;
            warn("解析 JEG API 失败，本次及后续点击将回退 Slimefun 原生指南", var3);
            return false;
         }
      }
   }

   /**
    * 打一条 JEG 相关警告。
    *
    * <p>上一版把异常全静默吞掉，结果是「点了没反应」且毫无线索——
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
      jegEnabled = null;
      apiResolved = false;
      apiBroken = false;
      asJeg = null;
      displaySfItem = null;
      displayStackItem = null;
      openMainMenu = null;
   }
}