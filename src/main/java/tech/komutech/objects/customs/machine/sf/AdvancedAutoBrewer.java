package tech.komutech.objects.customs.machine.sf;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.AutoBrewer;
import io.github.thebusybiscuit.slimefun4.libraries.dough.inventory.InvUtils;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.abstractItems.MachineRecipe;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

public class AdvancedAutoBrewer extends AutoBrewer {
   private static final Map<Material, PotionType> POTION_RECIPES = new EnumMap<>(Material.class);
   private static final Map<PotionType, PotionType> FERMENTATIONS = new EnumMap<>(PotionType.class);
   private static final Set<PotionType> EXTENDABLE = EnumSet.of(
      PotionType.NIGHT_VISION,
      PotionType.INVISIBILITY,
      PotionType.LEAPING,
      PotionType.FIRE_RESISTANCE,
      PotionType.SWIFTNESS,
      PotionType.SLOWNESS,
      PotionType.WATER_BREATHING,
      PotionType.POISON,
      PotionType.REGENERATION,
      PotionType.STRENGTH,
      PotionType.WEAKNESS,
      PotionType.TURTLE_MASTER,
      PotionType.SLOW_FALLING
   );
   private static final Set<PotionType> UPGRADEABLE = EnumSet.of(
      PotionType.LEAPING,
      PotionType.SWIFTNESS,
      PotionType.SLOWNESS,
      PotionType.POISON,
      PotionType.REGENERATION,
      PotionType.STRENGTH,
      PotionType.HEALING,
      PotionType.HARMING,
      PotionType.TURTLE_MASTER
   );
   private final int speed;

   public AdvancedAutoBrewer(ItemGroup var1, SlimefunItemStack var2, RecipeType var3, ItemStack[] var4, int var5) {
      super(var1, var2, var3, var4);
      this.speed = var5;
   }

   @Nullable
   protected MachineRecipe findNextRecipe(BlockMenu var1) {
      ItemStack var2 = var1.getItemInSlot(this.getInputSlots()[0]);
      ItemStack var3 = var1.getItemInSlot(this.getInputSlots()[1]);
      if (var2 == null || var3 == null) {
         return null;
      } else if (!this.isPotion(var2.getType()) && !this.isPotion(var3.getType())) {
         return null;
      } else {
         boolean var4 = this.isPotion(var2.getType());
         ItemStack var5 = var4 ? var3 : var2;
         if (var5.hasItemMeta()) {
            return null;
         } else {
            ItemStack var6 = var4 ? var2 : var3;
            PotionMeta var7 = (PotionMeta)var6.getItemMeta();
            ItemStack var8 = this.brew(var5.getType(), var6.getType(), var7);
            if (var8 == null) {
               return null;
            } else {
               var8.setItemMeta(var7);
               if (!InvUtils.fits(var1.toInventory(), var8, this.getOutputSlots())) {
                  return null;
               } else {
                  for (int var12 : this.getInputSlots()) {
                     var1.consumeItem(var12);
                  }

                  return new MachineRecipe(30 / this.speed, new ItemStack[]{var2, var3}, new ItemStack[]{var8});
               }
            }
         }
      }
   }

   @ParametersAreNonnullByDefault
   @Nullable
   private ItemStack brew(Material var1, Material var2, PotionMeta var3) {
      PotionData var4 = var3.getBasePotionData();
      PotionType var5 = var4.getType();
      if (var5 == PotionType.WATER) {
         if (var1 == Material.FERMENTED_SPIDER_EYE) {
            var3.setBasePotionData(new PotionData(PotionType.WEAKNESS, false, false));
            return new ItemStack(var2);
         }

         if (var1 == Material.NETHER_WART) {
            var3.setBasePotionData(new PotionData(PotionType.AWKWARD, false, false));
            return new ItemStack(var2);
         }

         if (var2 == Material.POTION && var1 == Material.GUNPOWDER) {
            return new ItemStack(Material.SPLASH_POTION);
         }

         if (var2 == Material.SPLASH_POTION && var1 == Material.DRAGON_BREATH) {
            return new ItemStack(Material.LINGERING_POTION);
         }
      } else if (var1 == Material.FERMENTED_SPIDER_EYE) {
         PotionType var6 = FERMENTATIONS.get(var5);
         if (var6 != null) {
            var3.setBasePotionData(new PotionData(var6, var4.isExtended(), var4.isUpgraded()));
            return new ItemStack(var2);
         }
      } else {
         if (var1 == Material.REDSTONE && EXTENDABLE.contains(var5) && !var4.isUpgraded()) {
            var3.setBasePotionData(new PotionData(var5, true, false));
            return new ItemStack(var2);
         }

         if (var1 == Material.GLOWSTONE_DUST && UPGRADEABLE.contains(var5) && !var4.isExtended()) {
            var3.setBasePotionData(new PotionData(var5, false, true));
            return new ItemStack(var2);
         }

         if (var5 == PotionType.AWKWARD) {
            PotionType var7 = POTION_RECIPES.get(var1);
            if (var7 != null) {
               var3.setBasePotionData(new PotionData(var7, false, false));
               return new ItemStack(var2);
            }
         }
      }

      return null;
   }

   private boolean isPotion(@Nonnull Material var1) {
      return var1 == Material.POTION || var1 == Material.SPLASH_POTION || var1 == Material.LINGERING_POTION;
   }

   static {
      POTION_RECIPES.put(Material.SUGAR, PotionType.SWIFTNESS);
      POTION_RECIPES.put(Material.RABBIT_FOOT, PotionType.LEAPING);
      POTION_RECIPES.put(Material.BLAZE_POWDER, PotionType.STRENGTH);
      POTION_RECIPES.put(Material.GLISTERING_MELON_SLICE, PotionType.HEALING);
      POTION_RECIPES.put(Material.SPIDER_EYE, PotionType.POISON);
      POTION_RECIPES.put(Material.GHAST_TEAR, PotionType.REGENERATION);
      POTION_RECIPES.put(Material.MAGMA_CREAM, PotionType.FIRE_RESISTANCE);
      POTION_RECIPES.put(Material.PUFFERFISH, PotionType.WATER_BREATHING);
      POTION_RECIPES.put(Material.GOLDEN_CARROT, PotionType.NIGHT_VISION);
      POTION_RECIPES.put(Material.TURTLE_HELMET, PotionType.TURTLE_MASTER);
      POTION_RECIPES.put(Material.PHANTOM_MEMBRANE, PotionType.SLOW_FALLING);
      FERMENTATIONS.put(PotionType.SWIFTNESS, PotionType.SLOWNESS);
      FERMENTATIONS.put(PotionType.LEAPING, PotionType.SLOWNESS);
      FERMENTATIONS.put(PotionType.HEALING, PotionType.HARMING);
      FERMENTATIONS.put(PotionType.POISON, PotionType.HARMING);
      FERMENTATIONS.put(PotionType.NIGHT_VISION, PotionType.INVISIBILITY);
   }
}
