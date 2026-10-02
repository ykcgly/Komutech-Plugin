package tech.komutech.objects.customs.item;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.ProtectionType;
import io.github.thebusybiscuit.slimefun4.core.attributes.ProtectiveArmor;
import io.github.thebusybiscuit.slimefun4.implementation.items.armor.SlimefunArmorPiece;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tech.komutech.KT;

public class CustomArmorPiece extends SlimefunArmorPiece implements ProtectiveArmor {
   private final String armorKey;
   private final boolean fullSet;
   private final ProtectionType[] protectionTypes;
   private final String projectId;

   public CustomArmorPiece(
      ItemGroup var1,
      SlimefunItemStack var2,
      RecipeType var3,
      ItemStack[] var4,
      @Nullable PotionEffect[] var5,
      boolean var6,
      String var7,
      ProtectionType[] var8,
      String var9
   ) {
      super(var1, var2, var3, var4, var5);
      this.armorKey = var7;
      this.fullSet = var6;
      this.protectionTypes = var8;
      this.projectId = var9;
      this.register(KT.plugin());
   }

   @NotNull
   public ProtectionType[] getProtectionTypes() {
      return this.protectionTypes;
   }

   public boolean isFullSetRequired() {
      return this.fullSet;
   }

   @Nullable
   public NamespacedKey getArmorSetId() {
      return new NamespacedKey(KT.plugin(), this.projectId + "_" + this.armorKey);
   }

   @NotNull
   public String getProjectId() {
      return this.projectId;
   }
}
