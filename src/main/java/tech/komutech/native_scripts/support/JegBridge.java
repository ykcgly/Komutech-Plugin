package tech.komutech.native_scripts.support;

import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.GuideHistory;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideImplementation;
import java.lang.reflect.Method;
import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * JustEnoughGuides（JEG）软依赖桥接。
 *
 * <p><b>为什么需要</b>：配方展示界面里的材料/产物点击统一走
 * {@code SingleItemRecipeGuideListener.viewItem}，原实现是
 * {@code new SurvivalSlimefunGuide(false, false).displayItem(...)} ——
 * {@code SurvivalSlimefunGuide} 是 Slimefun 的<b>硬编码原生</b>实现（字节码里没有任何
 * 「查当前生效指南实现」的分支），所以即使装了 JEG，点材料弹出的仍是 Slimefun 原生界面，
 * 而不是 JEG 界面。
 *
 * <p><b>JEG 的正确入口</b>：{@code com.balugaq.jeg.utils.GuideUtil.getLastGuide(Player)}
 * 返回玩家当前实际使用的 {@link SlimefunGuideImplementation}——装了 JEG 就是 JEG 的实现，
 * 没装则返回 Slimefun 原生实现。调它的 {@code displayItem} 即可自动适配，无需分叉。
 *
 * <p><b>为什么用反射而不是直接依赖</b>：JEG 是 compileOnly 的软依赖，不同服务器装的
 * JEG 版本可能调整 API 包路径（4.13 已把大量 API 从 {@code io.github.thebusybiscuit.*}
 * 挪到 {@code me.mrCookieSlime.*}）。全部走反射 + 一次性缓存 Method，
 * 即使 JEG 缺失或 API 变动也只是退回原生行为，不会导致插件无法启动。
 */
public final class JegBridge {

   private static final String JEG_PLUGIN = "JustEnoughGuide";
   /** JEG 的 GuideUtil 全限定名。 */
   private static final String GUIDE_UTIL = "com.balugaq.jeg.utils.GuideUtil";
   /** JEG 早期版本的包路径，保留兼容。 */
   private static final String GUIDE_UTIL_LEGACY = "com.balugaq.jeg.utils.GuideUtil";

   private static volatile Boolean jegEnabled = null;
   private static volatile Method getLastGuide = null;
   private static volatile Method displayItemStack = null;
   private static volatile Method displayItemSf = null;
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
    * 打开某个物品的配方展示页。
    *
    * <p>优先走玩家当前生效的指南实现（JEG 装了就是 JEG），
    * 拿不到实现或调用异常时回退到 Slimefun 原生 {@code SurvivalSlimefunGuide}。
    *
    * @return true 表示已由 JEG 接管；false 表示调用方应回退原生
    */
   public static boolean displayItem(Player player, ItemStack item) {
      if (player == null || item == null || item.getType().isAir()) {
         return false;
      }

      if (!isAvailable()) {
         return false;
      }

      // 原版对 AIR 与 null 静默 return，这里同样直接判定为未接管
      Optional<PlayerProfile> var2 = PlayerProfile.find(player);
      if (var2.isEmpty()) {
         return false;
      }

      if (!resolveApi()) {
         return false;
      }

      try {
         Object var3 = getLastGuide.invoke(null, player);
         if (!(var3 instanceof SlimefunGuideImplementation var4)) {
            return false;
         }

         // JEG 实现（以及被它包装的原生实现）都实现 SlimefunGuideImplementation，
         // displayItem(PlayerProfile, ItemStack, int, boolean) 是接口方法，直接调即可。
         displayItemStack.invoke(var4, var2.get(), item, 0, true);
         return true;
      } catch (ReflectiveOperationException | RuntimeException var5) {
         // JEG 版本差异导致签名不匹配时静默回退原生，不打断玩家操作
         return false;
      }
   }

   /**
    * 解析并缓存 JEG 反射入口。方法只跑一次；失败后置 {@code apiBroken} 不再重试，
    * 避免每次点击都走一遍反射查找。
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
            Class<?> var0 = null;
            for (String var1 : new String[]{GUIDE_UTIL, GUIDE_UTIL_LEGACY}) {
               try {
                  var0 = Class.forName(var1);
                  break;
               } catch (ClassNotFoundException ignored) {
                  // 试下一个候选路径
               }
            }

            if (var0 == null) {
               apiBroken = true;
               return false;
            }

            Method var2 = var0.getMethod("getLastGuide", Player.class);
            getLastGuide = var2;
            displayItemStack = SlimefunGuideImplementation.class
               .getMethod("displayItem", PlayerProfile.class, ItemStack.class, int.class, boolean.class);
            displayItemSf = SlimefunGuideImplementation.class
               .getMethod("displayItem", PlayerProfile.class,
                  io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem.class, boolean.class);
            return true;
         } catch (ReflectiveOperationException | RuntimeException var4) {
            apiBroken = true;
            return false;
         }
      }
   }

   /** 供 SlimefunItem 版本重载使用；当前 viewItem 走 ItemStack 版，此方法保留备用。 */
   static boolean displaySlimefunItem(Player player, io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem item) {
      if (player == null || item == null || !isAvailable() || !resolveApi() || displayItemSf == null) {
         return false;
      }

      Optional<PlayerProfile> var1 = PlayerProfile.find(player);
      if (var1.isEmpty()) {
         return false;
      }

      try {
         Object var2 = getLastGuide.invoke(null, player);
         if (var2 instanceof SlimefunGuideImplementation var3) {
            displayItemSf.invoke(var3, var1.get(), item, true);
            return true;
         }

         return false;
      } catch (ReflectiveOperationException | RuntimeException var4) {
         return false;
      }
   }

   /** 仅供测试/热重载场景重置缓存。 */
   public static void resetCache() {
      jegEnabled = null;
      apiResolved = false;
      apiBroken = false;
      getLastGuide = null;
      displayItemStack = null;
      displayItemSf = null;
   }

   /**
    * 取玩家当前生效的指南实现（装了 JEG 就是 JEG 的实现，否则 Slimefun 原生）。
    *
    * @return null 表示 JEG 不可用或反射失败，调用方应回退原生
    */
   public static SlimefunGuideImplementation currentGuide(Player player) {
      if (player == null || !isAvailable() || !resolveApi() || getLastGuide == null) {
         return null;
      }

      try {
         Object var0 = getLastGuide.invoke(null, player);
         return var0 instanceof SlimefunGuideImplementation var1 ? var1 : null;
      } catch (ReflectiveOperationException | RuntimeException var2) {
         return null;
      }
   }

   /**
    * 配方展示界面的返回按钮：左键返回上一页、Shift+左键回主菜单。
    *
    * <p>与 {@link #displayItem} 同理，必须走当前生效的指南实现，
    * 否则玩家在 JEG 界面里点返回会被丢进 Slimefun 原生界面，history 也会对不上。
    *
    * <p>{@code GuideHistory#goBack} 与 {@code SlimefunGuideImplementation#openMainMenu}
    * 的参数类型都是接口而非 {@code SurvivalSlimefunGuide}，故 JEG 实现可直接传入。
    *
    * @return true 表示已由 JEG 接管；false 表示调用方应回退原生
    */
   public static boolean back(PlayerProfile profile, Player player, boolean shift) {
      SlimefunGuideImplementation var0 = currentGuide(player);
      if (var0 == null || profile == null) {
         return false;
      }

      try {
         GuideHistory var2 = profile.getGuideHistory();
         if (shift) {
            var0.openMainMenu(profile, var2.getMainMenuPage());
         } else {
            var2.goBack(var0);
         }

         return true;
      } catch (RuntimeException var3) {
         return false;
      }
   }
}