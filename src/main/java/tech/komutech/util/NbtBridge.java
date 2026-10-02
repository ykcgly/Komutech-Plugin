package tech.komutech.util;

import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

public final class NbtBridge {
   private static final String NBT_CLASS = "de.tr7zw.nbtapi.NBT";
   private static final String NBT_BLOCK_CLASS = "de.tr7zw.nbtapi.NBTBlock";

   private NbtBridge() {
   }

   public static boolean isAvailable() {
      return Bukkit.getPluginManager().isPluginEnabled("NBTAPI");
   }

   public static void modifyItem(ItemStack var0, Consumer<Object> var1) {
      if (var0 != null && isAvailable()) {
         try {
            Class var2 = Class.forName("de.tr7zw.nbtapi.NBT");
            var2.getMethod("modify", ItemStack.class, Consumer.class).invoke(null, var0, var1);
         } catch (ReflectiveOperationException var3) {
            ExceptionHandler.handleWarning("NBT-API 写入物品 NBT 失败: " + var3.getMessage());
         }
      }
   }

   public static void setInteger(Object var0, String var1, int var2) {
      invokeCompound(var0, "setInteger", var1, var2);
   }

   public static void setFloat(Object var0, String var1, float var2) {
      invokeCompound(var0, "setFloat", var1, var2);
   }

   public static void setBoolean(Object var0, String var1, boolean var2) {
      invokeCompound(var0, "setBoolean", var1, var2);
   }

   public static Object readItem(ItemStack var0) {
      if (var0 != null && isAvailable()) {
         try {
            Class var1 = Class.forName("de.tr7zw.nbtapi.NBT");
            return var1.getMethod("itemStackToNBT", ItemStack.class).invoke(null, var0);
         } catch (ReflectiveOperationException var2) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static Object getOrCreateCompound(Object var0, String var1) {
      if (var0 == null) {
         return null;
      } else {
         try {
            return var0.getClass().getMethod("getOrCreateCompound", String.class).invoke(var0, var1);
         } catch (ReflectiveOperationException var3) {
            return null;
         }
      }
   }

   public static Object readBlock(Block var0) {
      if (var0 != null && isAvailable()) {
         try {
            Object var1 = Class.forName("de.tr7zw.nbtapi.NBTBlock").getConstructor(Block.class).newInstance(var0);
            return var1.getClass().getMethod("getData").invoke(var1);
         } catch (ReflectiveOperationException var2) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static Object readEntity(Entity var0) {
      if (var0 != null && isAvailable()) {
         try {
            Class var1 = Class.forName("de.tr7zw.nbtapi.NBT");
            Object var2 = var1.getMethod("createNBTObject").invoke(null);
            Consumer var3 = var1x -> {
               try {
                  var2.getClass().getMethod("mergeCompound", var1x.getClass().getInterfaces()[0]).invoke(var2, var1x);
               } catch (ReflectiveOperationException var8) {
                  try {
                     Class[] var3x = var1x.getClass().getInterfaces();
                     int var4x = var3x.length;
                     byte var5 = 0;
                     if (var5 < var4x) {
                        Class var6 = var3x[var5];
                        var2.getClass().getMethod("mergeCompound", var6).invoke(var2, var1x);
                        return;
                     }
                  } catch (ReflectiveOperationException var7) {
                  }
               }
            };
            var1.getMethod("get", Entity.class, Consumer.class).invoke(null, var0, var3);
            return var2;
         } catch (ReflectiveOperationException var4) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static Object createCompound() {
      if (!isAvailable()) {
         return null;
      } else {
         try {
            Class var0 = Class.forName("de.tr7zw.nbtapi.NBT");
            return var0.getMethod("createNBTObject").invoke(null);
         } catch (ReflectiveOperationException var1) {
            return null;
         }
      }
   }

   private static void invokeCompound(Object var0, String var1, String var2, Object var3) {
      if (var0 != null) {
         try {
            if (var3 instanceof Integer var4) {
               var0.getClass().getMethod(var1, String.class, Integer.class).invoke(var0, var2, var4);
            } else if (var3 instanceof Float var5) {
               var0.getClass().getMethod(var1, String.class, Float.class).invoke(var0, var2, var5);
            } else if (var3 instanceof Boolean var6) {
               var0.getClass().getMethod(var1, String.class, Boolean.class).invoke(var0, var2, var6);
            }
         } catch (ReflectiveOperationException var7) {
            ExceptionHandler.handleWarning("NBT-API 字段写入失败: " + var2);
         }
      }
   }
}
