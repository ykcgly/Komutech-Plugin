package tech.komutech.objects.slimefun;

import io.github.thebusybiscuit.slimefun4.api.items.groups.NestedItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.SubItemGroup;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.core.guide.GuideHistory;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide;
import io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.guide.SurvivalSlimefunGuide;
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

public class AdvancedNestedItemGroup extends NestedItemGroup {
   private final List<SubItemGroup> subGroups;

   public AdvancedNestedItemGroup(NamespacedKey var1, ItemStack var2, int var3) {
      super(var1, var2, var3);
      ExceptionHandler.debugLog("创建物品组: " + var1);
      this.subGroups = new ArrayList<>();
   }

   public void open(Player var1, PlayerProfile var2, SlimefunGuideMode var3) {
      this.setup(var1, var2, var3, 1);
   }

   public void addSubGroup(@Nonnull SubItemGroup var1) {
      super.addSubGroup(var1);
      this.subGroups.add(var1);
   }

   public void removeSubGroup(@Nonnull SubItemGroup var1) {
      super.removeSubGroup(var1);
      this.subGroups.remove(var1);
   }

   /**
    * 画菜单顶部栏（返回按钮 + 翻页 + 关闭等）。
    *
    * <p>{@code createHeader} 不在 {@link SlimefunGuideImplementation} 接口上，
    * 只存在于 {@code SurvivalSlimefunGuide} / {@code CheatSheetSlimefunGuide}
    * 与 JEG 的 {@code JEGSlimefunGuideImplementation}。这里按「当前生效实现 → 原生」顺序
    * 反射查找，找不到就跳过（只是少一个返回按钮，不影响菜单其余功能）。
    */
   private void createHeader(Player var1, PlayerProfile var2, SlimefunGuideMode var3, ChestMenu var4) {
      Object var5 = Slimefun.getRegistry().getSlimefunGuide(var3);
      if (var5 != null) {
         try {
            var5.getClass()
               .getMethod("createHeader", Player.class, PlayerProfile.class, ChestMenu.class)
               .invoke(var5, var1, var2, var4);
            return;
         } catch (ReflectiveOperationException | RuntimeException var6) {
            ExceptionHandler.handleWarning("嵌套物品组顶部栏渲染失败（当前指南实现: "
               + var5.getClass().getSimpleName() + "），已回退原生实现");
         }
      }

      new SurvivalSlimefunGuide(false, false).createHeader(var1, var2, var4);
   }

   private void setup(Player var1, PlayerProfile var2, SlimefunGuideMode var3, int var4) {
      GuideHistory var5 = var2.getGuideHistory();
      if (var3 == SlimefunGuideMode.SURVIVAL_MODE) {
         var5.add(this, var4);
      }

      ChestMenu var6 = new ChestMenu(Slimefun.getLocalization().getMessage(var1, "guide.title.main"));
      // 不能强转成 SurvivalSlimefunGuide：装了 JEG 后 getSlimefunGuide 返回的是
      // JEG 自己的实现，强转会抛 ClassCastException 导致嵌套物品组打不开。
      //
      // createHeader 不在 SlimefunGuideImplementation 接口上，只在 Survival/CheatSheet 两个
      // 原生实现与 JEG 的 JEGSlimefunGuideImplementation 上，故用反射调用：
      // 有则用当前生效实现（JEG 界面），无则回退 SurvivalSlimefunGuide。
      this.createHeader(var1, var2, var3, var6);
      var6.setEmptySlotsClickable(false);
      SoundEffect var8 = SoundEffect.GUIDE_BUTTON_CLICK_SOUND;
      var6.addMenuOpeningHandler(var1x -> var8.playFor(var1x));
      var6.addItem(
         1,
         new CustomItemStack(
            ChestMenuUtils.getBackButton(var1, new String[]{"", ChatColor.GRAY + Slimefun.getLocalization().getMessage(var1, "guide.back.guide")})
         )
      );
      var6.addMenuClickHandler(1, (var3x, var4x, var5x, var6x) -> {
         SlimefunGuide.openMainMenu(var2, var3, var5.getMainMenuPage());
         return false;
      });
      int var9 = 9;
      int var10 = 36 * (var4 - 1) - 1;

      while (var10 < this.subGroups.size() - 1 && var9 < 45) {
         SubItemGroup var11;
         if ((var11 = this.subGroups.get(++var10)).isVisibleInNested(var1)) {
            var6.addItem(var9, var11.getItem(var1));
            var6.addMenuClickHandler(var9, (var4x, var5x, var6x, var7x) -> {
               if (var11 instanceof ItemGroupButton var8x) {
                  var8x.run(var1, var5x, var6x, var7x, var3);
                  return false;
               } else {
                  SlimefunGuide.openItemGroup(var2, var11, var3, 1);
                  return false;
               }
            });
            var9++;
         }
      }

      int var12 = var10 == this.subGroups.size() - 1 ? var4 : (this.subGroups.size() - 1) / 36 + 1;
      var6.addItem(46, ChestMenuUtils.getPreviousButton(var1, var4, var12));
      var6.addMenuClickHandler(46, (var5x, var6x, var7x, var8x) -> {
         int var9x = var4 - 1;
         if (var9x > 0) {
            this.setup(var1, var2, var3, var9x);
         }

         return false;
      });
      var6.addItem(52, ChestMenuUtils.getNextButton(var1, var4, var12));
      var6.addMenuClickHandler(52, (var6x, var7x, var8x, var9x) -> {
         int var10x = var4 + 1;
         if (var10x <= var12) {
            this.setup(var1, var2, var3, var10x);
         }

         return false;
      });
      var6.open(new Player[]{var1});
   }
}
