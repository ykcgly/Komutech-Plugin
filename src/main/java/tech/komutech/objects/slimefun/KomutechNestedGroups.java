package tech.komutech.objects.slimefun;

import io.github.thebusybiscuit.slimefun4.api.items.groups.NestedItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.SubItemGroup;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tech.komutech.util.ExceptionHandler;

/**
 * 嵌套物品组工厂。
 *
 * <p><b>为什么必须用「匿名直接子类」而不是具名子类</b>：JEG（JustEnoughGuide）
 * 在 {@code openItemGroup} 里对分组类型做硬性判定（字节码已核对）：
 *
 * <pre>
 * if (group instanceof NestedItemGroup) {
 *     Class&lt;?&gt; c = group.getClass();
 *     if (c == NestedItemGroup.class
 *         || (c.getSuperclass() == NestedItemGroup.class &amp;&amp; c.isAnonymousClass())) {
 *         // → JEG 完整增强渲染（收藏栏、实时搜索、翻页…）
 *     }
 *     // 具名子类落到此处：JEG 完全不接管，页面近乎空白
 * }
 * </pre>
 *
 * 也就是说判定条件是「<b>恰好是 NestedItemGroup 本身，或它的匿名直接子类</b>」。
 * 具名子类 {@code AdvancedNestedItemGroup extends NestedItemGroup} 会被 JEG 跳过，
 * 这正是「JEG 里翻不到本附属物品 / 物品组打不开近乎空白页」��根因。
 *
 * <p>本工厂把原 {@code AdvancedNestedItemGroup} 的全部能力搬进匿名子类：
 * 子组顺序记录、按钮组点击分发、翻页渲染。JEG 接管时走它自己的渲染，
 * 未接管（未装 JEG）时虚分派到 {@link #openVanilla} 渲染原版样式。
 *
 * <p><b>按钮组识别</b>：按钮组原本靠父组类型判定能否挂载。匿名子类无法被具名类
 * {@code instanceof} 命中（Java 也不允许匿名类带 {@code implements}），
 * 故改为按「父组是 NestedItemGroup 且已登记子组」判定——
 * 本附属的根组全部由本工厂创建，判定等价。
 */
public final class KomutechNestedGroups {

   private KomutechNestedGroups() {
   }

   /**
    * 创建嵌套组（匿名直接子类——JEG 以
    * {@code getSuperclass() == NestedItemGroup && isAnonymousClass()} 识别并接管渲染）。
    *
    * @param var1 物品组键
    * @param var2 图标
    * @param var3 层级
    */
   public static NestedItemGroup create(NamespacedKey var1, ItemStack var2, int var3) {
      return new NestedItemGroup(var1, var2, var3) {
         private final List<SubItemGroup> subGroups = new ArrayList<>();

         @Override
         public void addSubGroup(@Nonnull SubItemGroup var4x) {
            super.addSubGroup(var4x);
            this.subGroups.add(var4x);
         }

         @Override
         public void removeSubGroup(@Nonnull SubItemGroup var4x) {
            super.removeSubGroup(var4x);
            this.subGroups.remove(var4x);
         }

         @Override
         public void open(Player var4x, PlayerProfile var5x, SlimefunGuideMode var6x) {
            // 只有原版 Slimefun 指南会走到这里；
            // 装了 JEG 时 JEG 的 openItemGroup 自己渲染，不会调用本方法
            ExceptionHandler.debugLog("打开嵌套物品组(原版路径): " + this.getKey());
            openVanilla(var4x, var5x, var6x, this, this.subGroups, 1);
         }
      };
   }

   /**
    * 原版指南路径的嵌套菜单渲染。
    *
    * <p>这是原 {@code AdvancedNestedItemGroup.setup} 的等价实现，保留其行为：
    * 子组从槽位 9 起每页 36 个、按钮组走 {@link ItemGroupButton#run}、
    * 其余子组走 {@code SlimefunGuide.openItemGroup}、46/52 为翻页。
    */
   private static void openVanilla(Player player, PlayerProfile profile, SlimefunGuideMode mode,
                                   NestedItemGroup group, List<SubItemGroup> subgroups, int page) {
      if (profile.getPlayer() == null) {
         return;
      }

      if (mode == SlimefunGuideMode.SURVIVAL_MODE) {
         profile.getGuideHistory().add(group, page);
      }

      ChestMenu menu = new ChestMenu(Slimefun.getLocalization().getMessage(player, "guide.title.main"));
      menu.setEmptySlotsClickable(false);
      menu.addMenuOpeningHandler(opener -> SoundEffect.GUIDE_BUTTON_CLICK_SOUND.playFor(opener));

      // createHeader 不在 SlimefunGuideImplementation 接口上，只在 Survival/CheatSheet
      // 与 JEG 的实现上，按「当前生效实现 → 跳过」顺序反射调用。
      // JEG 未接管时这里走原生实现，顶部栏正常渲染。
      Object guide = Slimefun.getRegistry().getSlimefunGuide(mode);
      if (guide != null) {
         try {
            guide.getClass()
               .getMethod("createHeader", Player.class, PlayerProfile.class, ChestMenu.class)
               .invoke(guide, player, profile, menu);
         } catch (ReflectiveOperationException | RuntimeException ex) {
            ExceptionHandler.handleWarning("嵌套物品组顶部栏渲染失败（当前指南实现: "
               + guide.getClass().getSimpleName() + "），已跳过顶部栏");
         }
      }

      menu.addItem(1, new CustomItemStack(ChestMenuUtils.getBackButton(player,
         new String[]{"", ChatColor.GRAY + Slimefun.getLocalization().getMessage(player, "guide.back.guide")})));
      menu.addMenuClickHandler(1, (clicker, slot, item, action) -> {
         SlimefunGuide.openMainMenu(profile, mode, profile.getGuideHistory().getMainMenuPage());
         return false;
      });

      // 子组从槽位 9 起，每页 36 个
      int slot = 9;
      for (SubItemGroup sub : subgroups) {
         if (slot >= 45) {
            break;
         }

         if (!sub.isVisibleInNested(player)) {
            continue;
         }

         menu.addItem(slot, sub.getItem(player));
         menu.addMenuClickHandler(slot, (clicker, clickedSlot, clicked, action) -> {
            if (sub instanceof ItemGroupButton button) {
               button.run(player, clickedSlot, clicked, action, mode);
            } else {
               SlimefunGuide.openItemGroup(profile, sub, mode, 1);
            }

            return false;
         });
         slot++;
      }

      int totalPages = (subgroups.size() - 1) / 36 + 1;
      if (page > 1) {
         menu.addItem(46, ChestMenuUtils.getPreviousButton(player, page, totalPages));
         menu.addMenuClickHandler(46, (clicker, clickedSlot, item, action) -> {
            if (page - 1 > 0) {
               openVanilla(player, profile, mode, group, subgroups, page - 1);
            }

            return false;
         });
      }

      if (page < totalPages) {
         menu.addItem(52, ChestMenuUtils.getNextButton(player, page, totalPages));
         menu.addMenuClickHandler(52, (clicker, clickedSlot, item, action) -> {
            if (page + 1 <= totalPages) {
               openVanilla(player, profile, mode, group, subgroups, page + 1);
            }

            return false;
         });
      }

      menu.open(player);
   }
}
