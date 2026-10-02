package tech.komutech.objects.customs.item.exts;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.RandomMobDrop;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import tech.komutech.KT;
import tech.komutech.objects.customs.item.CustomUnplaceableItem;

public class CustomMobDrop extends CustomUnplaceableItem implements RandomMobDrop {
   private final int chance;
   private final EntityType entityType;

   public CustomMobDrop(ItemGroup var1, SlimefunItemStack var2, ItemStack[] var3, int var4, EntityType var5, ItemStack var6) {
      super(var1, var2, RecipeType.MOB_DROP, var3, null, var6);
      this.chance = var4;
      this.entityType = var5;
      this.register(KT.plugin());
   }

   public int getMobDropChance() {
      return this.chance >= 100 ? 100 : Math.max(this.chance, 1);
   }

   public EntityType getEntityType() {
      return this.entityType;
   }
}
