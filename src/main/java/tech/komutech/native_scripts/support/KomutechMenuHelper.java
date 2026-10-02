package tech.komutech.native_scripts.support;

import org.bukkit.inventory.ItemStack;

public final class KomutechMenuHelper {
   private KomutechMenuHelper() {
   }

   public static void replaceExistingItem(Object var0, int var1, ItemStack var2) {
      if (var0 != null) {
         try {
            var0.getClass().getMethod("replaceExistingItem", int.class, ItemStack.class).invoke(var0, var1, var2);
         } catch (ReflectiveOperationException var4) {
         }
      }
   }

   public static ItemStack[] getContents(Object var0) {
      if (var0 == null) {
         return new ItemStack[0];
      } else {
         try {
            return var0.getClass().getMethod("getContents").invoke(var0) instanceof ItemStack[] var2 ? var2 : new ItemStack[0];
         } catch (ReflectiveOperationException var3) {
            return new ItemStack[0];
         }
      }
   }
}
