package tech.komutech.native_scripts.support;

import java.util.List;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tech.komutech.native_scripts.storage.WanXiangGuiHolder;

public final class MenuGuiHelper {
   private MenuGuiHelper() {
   }

   public static ItemStack item(Material var0, String var1, List<String> var2) {
      ItemStack var3 = new ItemStack(var0);
      ItemMeta var4 = var3.getItemMeta();
      if (var1 != null) {
         var4.setDisplayName(ChatColor.translateAlternateColorCodes('&', var1));
      }

      if (var2 != null) {
         var4.setLore(var2.stream().map(var0x -> ChatColor.translateAlternateColorCodes('&', var0x)).toList());
      }

      var3.setItemMeta(var4);
      return var3;
   }

   public static ItemStack item(String var0, String var1, List<String> var2) {
      Material var3 = Material.matchMaterial(var0);
      if (var3 == null) {
         var3 = Material.PAPER;
      }

      return item(var3, var1, var2);
   }

   public static ItemStack border() {
      return item(Material.WHITE_STAINED_GLASS_PANE, " ", null);
   }

   public static Inventory create(int var0, String var1) {
      return Bukkit.createInventory(null, var0, ChatColor.translateAlternateColorCodes('&', var1));
   }

   public static String legacyTitle(InventoryView var0) {
      try {
         return LegacyComponentSerializer.legacySection().serialize(var0.title());
      } catch (Throwable var2) {
         return var0.getTitle();
      }
   }

   public static boolean titleEquals(InventoryView var0, String var1) {
      return var1.equals(legacyTitle(var0));
   }

   public static boolean titleStartsWith(InventoryView var0, String var1) {
      return legacyTitle(var0).startsWith(var1);
   }

   public static boolean isWanXiangGuiTitle(InventoryView var0) {
      if (var0 == null) {
         return false;
      } else if (var0.getTopInventory().getHolder() instanceof WanXiangGuiHolder) {
         return true;
      } else {
         for (String var2 : List.of(legacyTitle(var0), var0.getTitle())) {
            if (var2 != null && !var2.isEmpty()) {
               String var3 = ChatColor.stripColor(var2);
               if (containsWanXiangMarker(var3)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private static boolean containsWanXiangMarker(String var0) {
      if (var0 != null && !var0.isEmpty()) {
         boolean var1 = var0.contains("萬象") || var0.contains("万象");
         if (!var1) {
            return false;
         } else {
            boolean var2 = var0.contains("匱") || var0.contains("匣");
            boolean var3 = var0.contains("第") && (var0.contains("页") || var0.contains("頁"));
            return var2 || var3;
         }
      } else {
         return false;
      }
   }

   public static void applyBorder(Inventory var0, int... var1) {
      ItemStack var2 = border();

      for (int var6 : var1) {
         var0.setItem(var6, var2.clone());
      }
   }
}
