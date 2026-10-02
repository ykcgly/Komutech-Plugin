package tech.komutech.util;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.AxolotlBucketMeta;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.KnowledgeBookMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.SuspiciousStewMeta;
import org.bukkit.inventory.meta.TropicalFishBucketMeta;
import org.bukkit.map.MapView;

public final class StackUtils {
   public static boolean itemsMatch(@Nonnull ItemStack var0, @Nullable ItemStack var1) {
      return itemsMatch(var0, var1, false, false, false);
   }

   public static boolean itemsMatch(@Nonnull ItemStack var0, @Nullable ItemStack var1, boolean var2) {
      return itemsMatch(var0, var1, var2, false, false);
   }

   public static boolean itemsMatch(@Nonnull ItemStack var0, @Nullable ItemStack var1, boolean var2, boolean var3) {
      return itemsMatch(var0, var1, var2, var3, false);
   }

   public static boolean itemsMatch(@Nullable ItemStack var0, @Nullable ItemStack var1, boolean var2, boolean var3, boolean var4) {
      if (var0 != null && var1 != null) {
         if (var1.getType() != var0.getType()) {
            return false;
         } else if (Tag.SHULKER_BOXES.isTagged(var1.getType())) {
            return false;
         } else if (var1.getType() == Material.BUNDLE) {
            return false;
         } else if (var3 && var1.getAmount() > var0.getAmount()) {
            return false;
         } else if (var1.hasItemMeta() && var0.hasItemMeta()) {
            ItemMeta var5 = var1.getItemMeta();
            ItemMeta var6 = var0.getItemMeta();
            if (var5 != null && var6 != null) {
               if (!var5.getClass().equals(var6.getClass())) {
                  return false;
               } else if (canQuickEscapeMetaVariant(var5, var6)) {
                  return false;
               } else if (var5.hasDisplayName() != var6.hasDisplayName()) {
                  return false;
               } else {
                  if (var4) {
                     boolean var7 = var5.hasCustomModelData();
                     boolean var8 = var6.hasCustomModelData();
                     if (var7 ? !var8 || var5.getCustomModelData() != var6.getCustomModelData() : var8) {
                        return false;
                     }
                  }

                  if (!var5.getPersistentDataContainer().equals(var6.getPersistentDataContainer())) {
                     return false;
                  } else if (!var5.getEnchants().equals(var6.getEnchants())) {
                     return false;
                  } else if (!var5.getItemFlags().equals(var6.getItemFlags())) {
                     return false;
                  } else {
                     boolean var11 = var5.hasAttributeModifiers();
                     boolean var12 = var6.hasAttributeModifiers();
                     if (var11 ? !var12 || !Objects.equals(var5.getAttributeModifiers(), var6.getAttributeModifiers()) : var12) {
                        return false;
                     } else if (!var2 && var1.getType() != Material.PLAYER_HEAD && var1.getType() != Material.SPAWNER && var1.getType() != Material.SUGAR
                        || (var5.hasLore() && var6.hasLore() ? Objects.equals(var5.getLore(), var6.getLore()) : var5.hasLore() == var6.hasLore())) {
                        Optional<String> var9 = Slimefun.getItemDataService().getItemData(var5);
                        Optional<String> var10 = Slimefun.getItemDataService().getItemData(var6);
                        return var9.isPresent() != var10.isPresent()
                           ? false
                           : var9.<Boolean>map(var1x -> var1x.equals(var10.get()))
                              .orElseGet(() -> !var5.hasDisplayName() || Objects.equals(var5.getDisplayName(), var6.getDisplayName()));
                     } else {
                        return false;
                     }
                  }
               }
            } else {
               return var5 == var6;
            }
         } else {
            return var1.hasItemMeta() == var0.hasItemMeta();
         }
      } else {
         return var1 == null;
      }
   }

   public static boolean canQuickEscapeMetaVariant(@Nonnull ItemMeta var0, @Nonnull ItemMeta var1) {
      if (var0 instanceof Damageable var2 && var1 instanceof Damageable var3) {
         if (var2.hasDamage() != var3.hasDamage()) {
            return true;
         }

         if (var2.getDamage() != var3.getDamage()) {
            return true;
         }
      }

      if (var0 instanceof Repairable var6 && var1 instanceof Repairable var25) {
         if (var6.hasRepairCost() != var25.hasRepairCost()) {
            return true;
         }

         if (var6.getRepairCost() != var25.getRepairCost()) {
            return true;
         }
      }

      if (var0 instanceof AxolotlBucketMeta var7 && var1 instanceof AxolotlBucketMeta var26) {
         if (var7.hasVariant() != var26.hasVariant()) {
            return true;
         }

         if (!var7.hasVariant() || !var26.hasVariant()) {
            return true;
         }

         if (var7.getVariant() != var26.getVariant()) {
            return true;
         }
      }

      if (var0 instanceof BannerMeta var8 && var1 instanceof BannerMeta var27) {
         if (var8.numberOfPatterns() != var27.numberOfPatterns()) {
            return true;
         }

         if (!var8.getPatterns().equals(var27.getPatterns())) {
            return true;
         }
      }

      if (var0 instanceof BlockDataMeta var9 && var1 instanceof BlockDataMeta var28 && var9.hasBlockData() != var28.hasBlockData()) {
         return true;
      } else {
         if (var0 instanceof BlockStateMeta var10 && var1 instanceof BlockStateMeta var29) {
            if (var10.hasBlockState() != var29.hasBlockState()) {
               return true;
            }

            if (!var10.getBlockState().equals(var29.getBlockState())) {
               return true;
            }
         }

         if (var0 instanceof BookMeta var11 && var1 instanceof BookMeta var30) {
            if (var11.getPageCount() != var30.getPageCount()) {
               return true;
            }

            if (!Objects.equals(var11.getAuthor(), var30.getAuthor())) {
               return true;
            }

            if (!Objects.equals(var11.getTitle(), var30.getTitle())) {
               return true;
            }

            if (!Objects.equals(var11.getGeneration(), var30.getGeneration())) {
               return true;
            }
         }

         if (var0 instanceof BundleMeta var12 && var1 instanceof BundleMeta var31) {
            if (var12.hasItems() != var31.hasItems()) {
               return true;
            }

            if (!var12.getItems().equals(var31.getItems())) {
               return true;
            }
         }

         if (var0 instanceof CompassMeta var13 && var1 instanceof CompassMeta var32) {
            if (var13.isLodestoneTracked() != var32.isLodestoneTracked()) {
               return true;
            }

            if (!Objects.equals(var13.getLodestone(), var32.getLodestone())) {
               return true;
            }
         }

         if (var0 instanceof CrossbowMeta var14 && var1 instanceof CrossbowMeta var33) {
            if (var14.hasChargedProjectiles() != var33.hasChargedProjectiles()) {
               return true;
            }

            if (!var14.getChargedProjectiles().equals(var33.getChargedProjectiles())) {
               return true;
            }
         }

         if (var0 instanceof EnchantmentStorageMeta var15 && var1 instanceof EnchantmentStorageMeta var34) {
            if (var15.hasStoredEnchants() != var34.hasStoredEnchants()) {
               return true;
            }

            if (!var15.getStoredEnchants().equals(var34.getStoredEnchants())) {
               return true;
            }
         }

         if (var0 instanceof FireworkEffectMeta var16 && var1 instanceof FireworkEffectMeta var35 && !Objects.equals(var16.getEffect(), var35.getEffect())) {
            return true;
         } else {
            if (var0 instanceof FireworkMeta var17 && var1 instanceof FireworkMeta var36) {
               if (var17.getPower() != var36.getPower()) {
                  return true;
               }

               if (!var17.getEffects().equals(var36.getEffects())) {
                  return true;
               }
            }

            if (var0 instanceof LeatherArmorMeta var18 && var1 instanceof LeatherArmorMeta var37 && !var18.getColor().equals(var37.getColor())) {
               return true;
            } else {
               if (var0 instanceof MapMeta var19 && var1 instanceof MapMeta var38) {
                  boolean var4 = var19.hasMapView();
                  boolean var5 = var38.hasMapView();
                  if (var4 != var5) {
                     return true;
                  }

                  if (var4 && var5 && !Objects.equals(safeMapView(var19), safeMapView(var38))) {
                     return true;
                  }

                  if (var19.hasLocationName() != var38.hasLocationName()) {
                     return true;
                  }

                  if (var19.hasColor() != var38.hasColor()) {
                     return true;
                  }

                  if (var19.hasLocationName() && var38.hasLocationName() && !Objects.equals(var19.getLocationName(), var38.getLocationName())) {
                     return true;
                  }

                  if (var19.hasColor() && var38.hasColor() && !Objects.equals(var19.getColor(), var38.getColor())) {
                     return true;
                  }
               }

               if (var0 instanceof PotionMeta var20 && var1 instanceof PotionMeta var39) {
                  if (!Objects.equals(var20.getBasePotionData(), var39.getBasePotionData())) {
                     return true;
                  }

                  if (var20.hasCustomEffects() != var39.hasCustomEffects()) {
                     return true;
                  }

                  if (var20.hasColor() != var39.hasColor()) {
                     return true;
                  }

                  if (!Objects.equals(var20.getColor(), var39.getColor())) {
                     return true;
                  }

                  if (!var20.getCustomEffects().equals(var39.getCustomEffects())) {
                     return true;
                  }
               }

               if (var0 instanceof SkullMeta var21 && var1 instanceof SkullMeta var40) {
                  if (var21.hasOwner() != var40.hasOwner()) {
                     return true;
                  }

                  if (!Objects.equals(var21.getOwningPlayer(), var40.getOwningPlayer())) {
                     return true;
                  }
               }

               if (var0 instanceof SuspiciousStewMeta var22 && var1 instanceof SuspiciousStewMeta var41) {
                  if (var22.hasCustomEffects() != var41.hasCustomEffects()) {
                     return true;
                  }

                  if (!Objects.equals(var22.getCustomEffects(), var41.getCustomEffects())) {
                     return true;
                  }
               }

               if (var0 instanceof TropicalFishBucketMeta var23 && var1 instanceof TropicalFishBucketMeta var42) {
                  if (var23.hasVariant() != var42.hasVariant()) {
                     return true;
                  }

                  if (!var23.getPattern().equals(var42.getPattern())) {
                     return true;
                  }

                  if (!var23.getBodyColor().equals(var42.getBodyColor())) {
                     return true;
                  }

                  if (!var23.getPatternColor().equals(var42.getPatternColor())) {
                     return true;
                  }
               }

               if (!(var0 instanceof KnowledgeBookMeta var24 && var1 instanceof KnowledgeBookMeta var43)) {
                  return false;
               } else {
                  return var24.hasRecipes() != var43.hasRecipes() ? true : !Objects.equals(var24.getRecipes(), var43.getRecipes());
               }
            }
         }
      }
   }

   @Nullable
   private static MapView safeMapView(@Nonnull MapMeta var0) {
      if (!var0.hasMapView()) {
         return null;
      } else {
         try {
            return var0.getMapView();
         } catch (IllegalStateException var2) {
            return null;
         }
      }
   }

   private StackUtils() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
