package tech.komutech.native_scripts.support;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.lang.reflect.Method;
import java.util.Locale;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import tech.komutech.native_scripts.SlimefunBridge;

public final class MachineScriptHelper {
   private static final String[] OTHER_NATIVE_ID_PREFIXES = new String[]{"KOMUTECH_", "LENGSHANG_", "MAGIC_", "OT_"};

   private MachineScriptHelper() {
   }

   public static MachineScriptHelper.MachineContext parse(Object var0) {
      if (var0 == null) {
         return null;
      } else {
         try {
            Object var1 = var0.getClass().getMethod("machine").invoke(var0);
            return var0.getClass().getMethod("block").invoke(var0) instanceof Block var3
               ? new MachineScriptHelper.MachineContext(var1, var3, var3.getLocation())
               : null;
         } catch (ReflectiveOperationException var4) {
            return null;
         }
      }
   }

   public static String getMachineId(Object var0) {
      if (var0 == null) {
         return null;
      } else {
         try {
            Object var1 = var0.getClass().getMethod("getId").invoke(var0);
            return var1 == null ? null : var1.toString();
         } catch (ReflectiveOperationException var2) {
            return null;
         }
      }
   }

   public static float getCharge(Object var0, Location var1) {
      return invokeChargeMethod(var0, "getCharge", var1, 0.0F);
   }

   public static void removeCharge(Object var0, Location var1, float var2) {
      invokeChargeVoid(var0, "removeCharge", var1, var2);
   }

   public static void addCharge(Object var0, Location var1, float var2) {
      invokeChargeVoid(var0, "addCharge", var1, var2);
   }

   public static SlimefunItem getSfItem(Location var0) {
      try {
         Class var1 = Class.forName("com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils");
         return var1.getMethod("getSfItem", Location.class).invoke(null, var0) instanceof SlimefunItem var3 ? var3 : null;
      } catch (ReflectiveOperationException var4) {
         return null;
      }
   }

   public static Object getMenu(Location var0) {
      try {
         Class var1 = Class.forName("com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils");
         return var1.getMethod("getMenu", Location.class).invoke(null, var0);
      } catch (ReflectiveOperationException var2) {
         return null;
      }
   }

   public static String getData(Location var0, String var1) {
      try {
         Class var2 = Class.forName("com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils");
         Object var3 = var2.getMethod("getData", Location.class, String.class).invoke(null, var0, var1);
         return var3 == null ? null : var3.toString();
      } catch (ReflectiveOperationException var4) {
         return null;
      }
   }

   public static void setData(Location var0, String var1, String var2) {
      try {
         Class var3 = Class.forName("com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils");
         var3.getMethod("setData", Location.class, String.class, String.class).invoke(null, var0, var1, var2);
      } catch (ReflectiveOperationException var4) {
      }
   }

   public static void removeData(Location var0, String var1) {
      try {
         Class var2 = Class.forName("com.xzavier0722.mc.plugin.slimefun4.storage.util.StorageCacheUtils");
         var2.getMethod("removeData", Location.class, String.class).invoke(null, var0, var1);
      } catch (ReflectiveOperationException var3) {
      }
   }

   public static ItemStack[] getMenuContents(Object var0) {
      if (var0 == null) {
         return new ItemStack[0];
      } else {
         try {
            if (var0.getClass().getMethod("getContents").invoke(var0) instanceof ItemStack[] var7) {
               return var7;
            }
         } catch (ReflectiveOperationException var5) {
         }

         int var6 = getMenuSize(var0);
         ItemStack[] var2 = new ItemStack[var6];

         for (int var3 = 0; var3 < var6; var3++) {
            MachineScriptHelper.ItemStackInMenu var4 = getItemInSlot(var0, var3);
            var2[var3] = var4 == null ? null : var4.stack();
         }

         return var2;
      }
   }

   public static MachineScriptHelper.ItemStackInMenu getItemInSlot(Object var0, int var1) {
      if (var0 == null) {
         return null;
      } else {
         try {
            return var0.getClass().getMethod("getItemInSlot", int.class).invoke(var0, var1) instanceof ItemStack var3
               ? new MachineScriptHelper.ItemStackInMenu(var3)
               : null;
         } catch (ReflectiveOperationException var4) {
            return null;
         }
      }
   }

   public static void addItem(Object var0, int var1, ItemStack var2) {
      if (var0 != null && var2 != null) {
         try {
            var0.getClass().getMethod("addItem", int.class, ItemStack.class).invoke(var0, var1, var2);
         } catch (ReflectiveOperationException var4) {
         }
      }
   }

   public static void replaceExistingItem(Object var0, int var1, ItemStack var2) {
      if (var0 != null) {
         try {
            var0.getClass().getMethod("replaceExistingItem", int.class, ItemStack.class).invoke(var0, var1, var2);
         } catch (ReflectiveOperationException var5) {
            try {
               var0.getClass().getMethod("setItemInSlot", int.class, ItemStack.class).invoke(var0, var1, var2);
            } catch (ReflectiveOperationException var4) {
            }
         }
      }
   }

   public static int getMenuSize(Object var0) {
      if (var0 == null) {
         return 0;
      } else {
         try {
            return var0.getClass().getMethod("getSize").invoke(var0) instanceof Number var2 ? var2.intValue() : 0;
         } catch (ReflectiveOperationException var3) {
            return 0;
         }
      }
   }

   public static boolean pushItem(Object var0, ItemStack var1, int[] var2) {
      if (var0 == null || var1 == null || var2 == null || var1.getType().isAir()) {
         return false;
      } else if (pushViaMachineOutputHelper(var0, var1, var2)) {
         return true;
      } else {
         try {
            return !(var0.getClass().getMethod("pushItem", ItemStack.class, int[].class).invoke(var0, var1, var2) instanceof Boolean var4 && !var4);
         } catch (ReflectiveOperationException var5) {
            return false;
         }
      }
   }

   public static int[] getOutputSlots(SlimefunItem var0) {
      return invokeIntArray(var0, "getOutputSlots");
   }

   public static int[] getInputSlots(SlimefunItem var0) {
      return invokeIntArray(var0, "getInputSlots");
   }

   public static boolean isMachineInventorySlot(SlimefunItem var0, int var1) {
      if (var0 != null && var1 >= 0) {
         if (containsSlot(getInputSlots(var0), var1)) {
            return true;
         } else if (containsSlot(getOutputSlots(var0), var1)) {
            return true;
         } else {
            int var2 = invokeOptionalInt(var0, "getTemplateSlot", -1);
            return var2 >= 0 && var1 == var2 ? true : containsSlot(invokeIntArray(var0, "getWorkSlots"), var1);
         }
      } else {
         return false;
      }
   }

   public static Location resolveBlockLocation(InventoryView var0) {
      if (var0 == null) {
         return null;
      } else {
         try {
            InventoryHolder var1 = var0.getTopInventory().getHolder(false);
            if (var1 == null) {
               return null;
            } else {
               return var1.getClass().getMethod("getLocation").invoke(var1) instanceof Location var3 ? var3 : null;
            }
         } catch (ReflectiveOperationException var4) {
            return null;
         }
      }
   }

   public static SlimefunItem resolveBlockMachine(InventoryView var0) {
      Location var1 = resolveBlockLocation(var0);
      return var1 == null ? null : getSfItem(var1);
   }

   public static boolean hasMachineIdPrefix(SlimefunItem var0, String var1) {
      if (var0 != null && var1 != null && !var1.isEmpty()) {
         String var2 = var0.getId();
         return var2 != null && var2.regionMatches(true, 0, var1, 0, var1.length());
      } else {
         return false;
      }
   }

   public static boolean hasMachineId(SlimefunItem var0, String var1) {
      return var0 != null && var1 != null && var1.equalsIgnoreCase(var0.getId());
   }

   public static boolean belongsToOtherNativeAddon(SlimefunItem var0, String... var1) {
      if (var0 == null) {
         return false;
      } else {
         String var2 = var0.getId();
         if (var2 == null) {
            return false;
         } else {
            String var3 = var2.toUpperCase(Locale.ROOT);

            for (String var7 : OTHER_NATIVE_ID_PREFIXES) {
               if (!isExcludedPrefix(var7, var1) && var3.startsWith(var7)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   public static boolean matchesAddonMetadata(SlimefunItem var0, String var1, String var2, String var3) {
      if (var0 == null) {
         return false;
      } else {
         if (var0.getAddon() != null) {
            String var4 = var0.getAddon().getName();
            if (var1.equalsIgnoreCase(var4) || var2.equalsIgnoreCase(var4) || var3.equalsIgnoreCase(var4)) {
               return true;
            }
         }

         try {
            Object var6 = var0.getClass().getMethod("getProjectId").invoke(var0);
            return var1.equalsIgnoreCase(String.valueOf(var6));
         } catch (ReflectiveOperationException var5) {
            return false;
         }
      }
   }

   public static boolean isOwnAddonMachine(SlimefunItem var0, String var1, String var2, String var3, String var4) {
      if (var0 == null) {
         return false;
      } else if (hasMachineIdPrefix(var0, var1)) {
         return true;
      } else {
         return belongsToOtherNativeAddon(var0, var1) ? false : matchesAddonMetadata(var0, var2, var3, var4);
      }
   }

   private static boolean isExcludedPrefix(String var0, String... var1) {
      if (var1 == null) {
         return false;
      } else {
         for (String var5 : var1) {
            if (var5 != null && var0.equalsIgnoreCase(var5)) {
               return true;
            }
         }

         return false;
      }
   }

   private static boolean containsSlot(int[] var0, int var1) {
      if (var0 == null) {
         return false;
      } else {
         for (int var5 : var0) {
            if (var5 == var1) {
               return true;
            }
         }

         return false;
      }
   }

   public static int countSlimefunItemsInMenu(Object var0, String var1) {
      int var2 = getMenuSize(var0);
      return countSlimefunItemsInMenu(var0, var1, 0, Math.max(0, var2 - 1));
   }

   public static int countSlimefunItemsInMenu(Object var0, String var1, int var2, int var3) {
      int var4 = 0;

      for (int var5 = var2; var5 <= var3; var5++) {
         MachineScriptHelper.ItemStackInMenu var6 = getItemInSlot(var0, var5);
         if (var6 != null) {
            SlimefunItem var7 = SlimefunBridge.getSlimefunItem(var6.stack());
            if (var7 != null && var1.equals(var7.getId())) {
               var4 += var6.stack().getAmount();
            }
         }
      }

      return var4;
   }

   public static void configureSpawner(Block var0, EntityType var1) {
      if (var0.getState() instanceof CreatureSpawner var3) {
         var3.setSpawnedType(var1);
         var3.setMinSpawnDelay(800);
         var3.setMaxSpawnDelay(800);
         var3.setMaxNearbyEntities(0);
         var3.setRequiredPlayerRange(18);
         var3.setSpawnRange(0);
         var3.setSpawnCount(0);
         var3.update(false, false);
      }
   }

   private static boolean pushViaMachineOutputHelper(Object var0, ItemStack var1, int[] var2) {
      try {
         Class var3 = Class.forName("tech.komutech.util.MachineOutputHelper");
         Class var4 = Class.forName("me.mrCookieSlime.Slimefun.api.inventory.BlockMenu");
         if (!var4.isInstance(var0)) {
            return false;
         }

         if (var4.getMethod("getBlock").invoke(var0) instanceof Block var6) {
            var3.getMethod("pushOrDrop", var4, Block.class, ItemStack.class, int[].class).invoke(null, var0, var6, var1.clone(), var2);
            return true;
         }
      } catch (ReflectiveOperationException var7) {
      }

      return false;
   }

   private static int[] invokeIntArray(Object var0, String var1) {
      if (var0 == null) {
         return new int[0];
      } else {
         try {
            if (var0.getClass().getMethod(var1).invoke(var0) instanceof int[] var3) {
               return var3;
            }
         } catch (ReflectiveOperationException var4) {
         }

         return new int[0];
      }
   }

   private static int invokeOptionalInt(Object var0, String var1, int var2) {
      if (var0 == null) {
         return var2;
      } else {
         try {
            if (var0.getClass().getMethod(var1).invoke(var0) instanceof Number var4) {
               return var4.intValue();
            }
         } catch (ReflectiveOperationException var5) {
         }

         return var2;
      }
   }

   private static float invokeChargeMethod(Object var0, String var1, Location var2, float var3) {
      if (var0 == null) {
         return var3;
      } else {
         try {
            Method var4 = var0.getClass().getMethod(var1, Location.class);
            if (var4.invoke(var0, var2) instanceof Number var6) {
               return var6.floatValue();
            }
         } catch (ReflectiveOperationException var7) {
         }

         return var3;
      }
   }

   private static void invokeChargeVoid(Object var0, String var1, Location var2, float var3) {
      if (var0 != null) {
         try {
            Method var4 = var0.getClass().getMethod(var1, Location.class, float.class);
            var4.invoke(var0, var2, var3);
         } catch (ReflectiveOperationException var7) {
            try {
               Method var5 = var0.getClass().getMethod(var1, Location.class, double.class);
               var5.invoke(var0, var2, (double)var3);
            } catch (ReflectiveOperationException var6) {
            }
         }
      }
   }

   public record ItemStackInMenu(ItemStack stack) {
   }

   public record MachineContext(Object machine, Block block, Location location) {
   }
}
