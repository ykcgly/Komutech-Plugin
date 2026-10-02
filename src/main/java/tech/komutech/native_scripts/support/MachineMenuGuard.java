package tech.komutech.native_scripts.support;

import java.util.List;
import java.util.function.IntPredicate;
import org.bukkit.ChatColor;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryView;
import tech.komutech.native_scripts.support.MachineScriptHelper;

public final class MachineMenuGuard {
   private MachineMenuGuard() {
   }

   public static boolean titleContains(InventoryView var0, String... var1) {
      String var2 = plainTitle(var0);
      if (var2 != null && !var2.isEmpty()) {
         for (String var6 : var1) {
            if (var6 != null && !var6.isEmpty() && var2.contains(var6)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean titleContainsAll(InventoryView var0, String... var1) {
      String var2 = plainTitle(var0);
      if (var2 != null && !var2.isEmpty() && var1 != null && var1.length != 0) {
         for (String var6 : var1) {
            if (var6 == null || var6.isEmpty() || !var2.contains(var6)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean isOwnMachine(InventoryView var0, String var1) {
      return MachineScriptHelper.hasMachineId(MachineScriptHelper.resolveBlockMachine(var0), var1);
   }

   public static String plainTitle(InventoryView var0) {
      if (var0 == null) {
         return null;
      } else {
         for (String var2 : List.of(MenuGuiHelper.legacyTitle(var0), var0.getTitle())) {
            if (var2 != null && !var2.isEmpty()) {
               String var3 = normalizePlainTitle(var2);
               if (!var3.isEmpty()) {
                  return var3;
               }
            }
         }

         return null;
      }
   }

   public static String normalizePlainTitle(String var0) {
      if (var0 == null) {
         return "";
      } else {
         String var1 = ChatColor.stripColor(var0);
         var1 = var1.replaceAll("\\{#[0-9A-Fa-f]{6}\\}", "");
         return var1.replaceAll("(?i)&[0-9a-fk-or]", "");
      }
   }

   public static boolean isUltimateSynthProcessorMenu(InventoryView var0) {
      if (isOwnMachine(var0, "KOMUTECH_L_ZJ_終極合成台")) {
         return true;
      } else {
         String var1 = plainTitle(var0);
         return var1 != null && !var1.isEmpty() ? var1.contains("終極合成台") || var1.contains("终极合成台") : false;
      }
   }

   public static boolean isSlotIn(int var0, int... var1) {
      for (int var5 : var1) {
         if (var5 == var0) {
            return true;
         }
      }

      return false;
   }

   public static void protectTopInventory(InventoryClickEvent var0, IntPredicate var1) {
      InventoryView var2 = var0.getView();
      int var3 = var2.getTopInventory().getSize();
      int var4 = var0.getRawSlot();
      if (isSwapOrHotbarAction(var0) && var4 >= 0 && var4 < var3 && !var1.test(var4)) {
         var0.setCancelled(true);
      } else if (var4 < 0 || var4 >= var3) {
         if (var0.getAction() == InventoryAction.COLLECT_TO_CURSOR
            || var0.getAction() == InventoryAction.UNKNOWN && var0.getClick() == ClickType.DOUBLE_CLICK
            || var0.getClick() == ClickType.DOUBLE_CLICK) {
            var0.setCancelled(true);
         }
      } else if (!var1.test(var4)) {
         var0.setCancelled(true);
      }
   }

   private static boolean isSwapOrHotbarAction(InventoryClickEvent var0) {
      ClickType var1 = var0.getClick();
      InventoryAction var2 = var0.getAction();
      return var1 == ClickType.NUMBER_KEY
         || var1 == ClickType.SWAP_OFFHAND
         || var2 == InventoryAction.HOTBAR_MOVE_AND_READD
         || var2 == InventoryAction.HOTBAR_SWAP;
   }

   public static void protectTopInventoryDrag(InventoryDragEvent var0, IntPredicate var1) {
      InventoryView var2 = var0.getView();
      int var3 = var2.getTopInventory().getSize();

      for (int var5 : var0.getRawSlots()) {
         if (var5 >= 0 && var5 < var3 && !var1.test(var5)) {
            var0.setCancelled(true);
            return;
         }
      }
   }
}
