package tech.komutech.objects.customs.machine.sf;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.AutoAnvil;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class AdvancedAutoAnvil extends AutoAnvil {
   private final int repairFactor;
   private final int speed;

   public AdvancedAutoAnvil(ItemGroup var1, int var2, SlimefunItemStack var3, RecipeType var4, ItemStack[] var5, int var6) {
      super(var1, var2, var3, var4, var5);
      this.repairFactor = var2;
      this.speed = var6;
   }

   protected MachineRecipe findNextRecipe(BlockMenu var1) {
      for (int var5 : this.getInputSlots()) {
         ItemStack var6 = var1.getItemInSlot(var5 == this.getInputSlots()[0] ? this.getInputSlots()[1] : this.getInputSlots()[0]);
         ItemStack var7 = var1.getItemInSlot(var5);
         if (var7 != null && var7.getType().getMaxDurability() > 0 && ((Damageable)var7.getItemMeta()).getDamage() > 0) {
            if (SlimefunUtils.isItemSimilar(var6, SlimefunItems.DUCT_TAPE, true, false)) {
               ItemStack var8 = this.repair(var7);
               if (!var1.fits(var8, this.getOutputSlots())) {
                  return null;
               }

               for (int var12 : this.getInputSlots()) {
                  var1.consumeItem(var12);
               }

               return new MachineRecipe(30 / this.speed, new ItemStack[]{var6, var7}, new ItemStack[]{var8});
            }
            break;
         }
      }

      return null;
   }

   private ItemStack repair(ItemStack var1) {
      ItemStack var2 = var1.clone();
      ItemMeta var3 = var2.getItemMeta();
      short var4 = var1.getType().getMaxDurability();
      int var5 = 100 / this.repairFactor;
      short var6 = (short)(((Damageable)var3).getDamage() - var4 / var5);
      if (var6 < 0) {
         var6 = 0;
      }

      ((Damageable)var3).setDamage(var6);
      var2.setItemMeta(var3);
      return var2;
   }
}
