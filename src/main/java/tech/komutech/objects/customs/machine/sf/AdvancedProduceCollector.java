package tech.komutech.objects.customs.machine.sf;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.entities.AnimalProduce;
import io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.entities.ProduceCollector;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AdvancedProduceCollector extends ProduceCollector {
   private final int speed;

   public AdvancedProduceCollector(ItemGroup var1, SlimefunItemStack var2, RecipeType var3, ItemStack[] var4, int var5) {
      super(var1, var2, var3, var4);
      this.speed = var5;
   }

   public void addProduce(@NotNull AnimalProduce var1) {
      var1.setTicks(var1.getTicks() / this.speed);
      super.addProduce(var1);
   }
}
