package tech.komutech.objects.customs.item;

import io.github.thebusybiscuit.slimefun4.api.geo.GEOResource;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.UnplaceableBlock;
import java.util.function.BiFunction;
import org.bukkit.NamespacedKey;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import tech.komutech.KT;

public class CustomGeoResource extends UnplaceableBlock implements GEOResource {
   private final BiFunction<Environment, Biome, Integer> supply;
   private final int maxDeviation;
   private final boolean obtainableFromGEOMiner;
   private final String name;

   public CustomGeoResource(
      ItemGroup var1,
      SlimefunItemStack var2,
      RecipeType var3,
      ItemStack[] var4,
      BiFunction<Environment, Biome, Integer> var5,
      int var6,
      boolean var7,
      String var8
   ) {
      super(var1, var2, var3, var4);
      this.supply = var5;
      this.maxDeviation = var6;
      this.obtainableFromGEOMiner = var7;
      this.name = var8;
      this.register();
      this.register(KT.plugin());
   }

   public int getDefaultSupply(@NotNull Environment var1, @NotNull Biome var2) {
      return this.supply.apply(var1, var2);
   }

   public int getMaxDeviation() {
      return this.maxDeviation;
   }

   @NotNull
   public String getName() {
      return this.name;
   }

   public boolean isObtainableFromGEOMiner() {
      return this.obtainableFromGEOMiner;
   }

   @NotNull
   public NamespacedKey getKey() {
      return new NamespacedKey(KT.plugin(), this.getId());
   }
}
