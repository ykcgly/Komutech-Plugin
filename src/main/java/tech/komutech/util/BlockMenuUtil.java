package tech.komutech.util;

import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import tech.komutech.objects.customs.LinkedOutput;

public final class BlockMenuUtil {
   @Nullable
   public static ItemStack pushItem(@Nonnull BlockMenu var0, @Nonnull ItemStack var1, int... var2) {
      if (var1 != null && !var1.getType().isAir()) {
         int var3 = var1.getAmount();

         for (int var7 : var2) {
            if (var3 <= 0) {
               break;
            }

            ItemStack var8 = var0.getItemInSlot(var7);
            if (var8 != null && !var8.getType().isAir()) {
               int var11 = var8.getAmount();
               if (var11 < var1.getMaxStackSize() && StackUtils.itemsMatch(var1, var8, true, false)) {
                  int var12 = Math.max(0, Math.min(var1.getMaxStackSize() - var11, var3));
                  var8.setAmount(var11 + var12);
                  var1.setAmount(var3 -= var12);
               }
            } else {
               int var9 = Math.min(var3, var1.getMaxStackSize());
               ItemStack var10 = var1.clone();
               var10.setAmount(var9);
               var0.replaceExistingItem(var7, var10);
               var1.setAmount(Math.max(0, var3 -= var9));
            }
         }

         return var3 > 0 ? new CustomItemStack(var1, var3) : null;
      } else {
         return null;
      }
   }

   public static void pushOrDrop(@Nonnull BlockMenu var0, @Nonnull Block var1, @Nonnull ItemStack var2, int... var3) {
      ItemStack var4 = pushItem(var0, var2, var3);
      if (var4 != null && var4.getAmount() > 0) {
         MachineOutputHelper.dropNaturally(var1, var4);
      }
   }

   @Nonnull
   public static Map<ItemStack, Integer> pushItem(@Nonnull BlockMenu var0, @Nonnull ItemStack[] var1, int... var2) {
      if (var1 != null && var1.length != 0) {
         ArrayList<ItemStack> var3 = new ArrayList<>();

         for (ItemStack var7 : var1) {
            if (var7 != null && var7.getType() != Material.AIR) {
               var3.add(var7);
            }
         }

         return pushItem(var0, var3, var2);
      } else {
         throw new IllegalArgumentException("Cannot push null or empty array");
      }
   }

   @Nonnull
   public static Map<ItemStack, Integer> pushItem(@Nonnull BlockMenu var0, @Nonnull List<ItemStack> var1, int... var2) {
      if (var1 != null && !var1.isEmpty()) {
         HashMap<ItemStack, Integer> var3 = new HashMap<>();

         for (ItemStack var5 : var1) {
            ItemStack var6;
            if (var5 != null && var5.getType() != Material.AIR && (var6 = pushItem(var0, var5, var2)) != null) {
               var3.put(var6, var3.getOrDefault(var6, 0) + var6.getAmount());
            }
         }

         return var3;
      } else {
         throw new IllegalArgumentException("Cannot push null or empty list");
      }
   }

   public static boolean fits(@Nonnull BlockMenu var0, @Nonnull ItemStack var1, int... var2) {
      if (var1 != null && var1.getType() != Material.AIR) {
         int var3 = var1.getAmount();

         for (int var7 : var2) {
            ItemStack var8 = var0.getItemInSlot(var7);
            if (var8 == null || var8.getType() == Material.AIR) {
               var3 -= var1.getMaxStackSize();
            } else if (var8.getMaxStackSize() > var8.getAmount() && StackUtils.itemsMatch(var1, var8, true, false)) {
               var3 -= var8.getMaxStackSize() - var8.getAmount();
            }

            if (var3 <= 0) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public static boolean fits(@Nonnull BlockMenu var0, @Nonnull ItemStack[] var1, int... var2) {
      if (var1 != null && var1.length != 0) {
         ArrayList<ItemStack> var3 = new ArrayList<>();

         for (ItemStack var7 : var1) {
            if (var7 != null && var7.getType() != Material.AIR) {
               var3.add(var7.clone());
            }
         }

         return fits(var0, var3, var2);
      } else {
         return false;
      }
   }

   public static boolean fits(@Nonnull BlockMenu var0, @Nonnull List<ItemStack> var1, int... var2) {
      if (var1 != null && !var1.isEmpty()) {
         ArrayList var3 = new ArrayList();

         for (int var4 = 0; var4 < 54; var4++) {
            var3.add(null);
         }

         for (int var7 : var2) {
            ItemStack var8 = var0.getItemInSlot(var7);
            if (var8 != null && var8.getType() != Material.AIR) {
               var3.set(var7, var8.clone());
            } else {
               var3.set(var7, null);
            }
         }

         for (ItemStack var17 : var1) {
            ItemStack var18 = var17.clone();
            int var19 = var18.getAmount();

            for (int var11 : var2) {
               if (var19 <= 0) {
                  break;
               }

               ItemStack var12 = (ItemStack)var3.get(var11);
               if (var12 != null && var12.getType() != Material.AIR) {
                  int var21 = var12.getAmount();
                  if (var21 < var18.getMaxStackSize() && StackUtils.itemsMatch(var18, var12, true, false)) {
                     int var22 = Math.max(0, Math.min(var18.getMaxStackSize() - var21, var19));
                     var12.setAmount(var21 + var22);
                     var18.setAmount(var19 -= var22);
                  }
               } else {
                  int var13 = Math.min(var19, var18.getMaxStackSize());
                  ItemStack var14 = var18.clone();
                  var14.setAmount(var19);
                  var3.set(var11, var14);
                  var18.setAmount(Math.max(0, var19 -= var13));
               }
            }

            if (var19 > 0) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean fits(@Nonnull BlockMenu var0, @Nonnull LinkedOutput var1, int... var2) {
      if (var1 == null) {
         return false;
      } else {
         ArrayList var4 = new ArrayList();

         for (int var3 = 0; var3 < 54; var3++) {
            var4.add(null);
         }

         for (int var17 = 0; var17 < 54; var17++) {
            ItemStack var5 = var0.getItemInSlot(var17);
            if (var5 != null && var5.getType() != Material.AIR) {
               var4.set(var17, var5.clone());
            } else {
               var4.set(var17, null);
            }
         }

         for (int var6 : var1.linkedOutput().keySet()) {
            ItemStack var7;
            if (var6 >= 0 && var6 < 54 && (var7 = var1.linkedOutput().get(var6)) != null && var7.getType() != Material.AIR) {
               ItemStack var8 = var7.clone();
               ItemStack var9 = (ItemStack)var4.get(var6);
               if (var9 != null && var9.getType() != Material.AIR) {
                  if (!StackUtils.itemsMatch(var8, var9, true, false)) {
                     return false;
                  }

                  int var24 = var9.getAmount();
                  int var26 = Math.min(var8.getMaxStackSize() - var24, var8.getAmount());
                  if (var26 <= 0) {
                     return false;
                  }

                  var9.setAmount(var24 + var26);
                  var8.setAmount(var8.getAmount() - var26);
               } else {
                  int var10 = Math.min(var8.getAmount(), var8.getMaxStackSize());
                  if (var10 <= 0) {
                     return false;
                  }

                  ItemStack var11 = var8.clone();
                  var11.setAmount(var10);
                  var4.set(var6, var11);
                  var8.setAmount(var8.getAmount() - var10);
               }
            }
         }

         for (ItemStack var22 : var1.freeOutput()) {
            if (var22 != null && var22.getType() != Material.AIR) {
               ItemStack var23 = var22.clone();

               for (int var13 : var2) {
                  if (var23.getAmount() <= 0) {
                     break;
                  }

                  ItemStack var14 = (ItemStack)var4.get(var13);
                  if (var14 == null || var14.getType() == Material.AIR) {
                     int var28 = Math.min(var23.getAmount(), var23.getMaxStackSize());
                     ItemStack var29 = var23.clone();
                     var29.setAmount(var28);
                     var4.set(var13, var29);
                     var23.setAmount(var23.getAmount() - var28);
                  } else if (StackUtils.itemsMatch(var23, var14, true, false)) {
                     int var15 = var14.getAmount();
                     int var16 = Math.min(var23.getMaxStackSize() - var15, var23.getAmount());
                     var14.setAmount(var15 + var16);
                     var23.setAmount(var23.getAmount() - var16);
                  }
               }
            }
         }

         return true;
      }
   }

   public static void pushItem(@Nonnull BlockMenu var0, @Nonnull LinkedOutput var1, boolean var2, int... var3) {
      if (var1 != null) {
         for (int var8 : var1.linkedOutput().keySet()) {
            ItemStack var6;
            if (var8 >= 0 && var8 < 54 && (var6 = var1.linkedOutput().get(var8)) != null && var6.getType() != Material.AIR) {
               ItemStack var5 = var6.clone();
               int var4 = var1.linkedChances().get(var8);
               if (var4 <= 0 || var4 >= 100 || !(Math.random() * 100.0 > var4)) {
                  pushItem(var0, var5, var8);
                  if (var2) {
                     break;
                  }
               }
            }
         }

         ItemStack[] var12 = var1.freeOutput();

         for (int var13 = 0; var13 < var12.length; var13++) {
            ItemStack var11 = var12[var13];
            if (var11 != null && var11.getType() != Material.AIR) {
               ItemStack var10 = var11.clone();
               int var9 = var1.freeChances()[var13];
               if (var9 <= 0 || var9 >= 100 || !(Math.random() * 100.0 > var9)) {
                  pushItem(var0, var10, var3);
                  if (var2) {
                     break;
                  }
               }
            }
         }
      }
   }

   private BlockMenuUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
