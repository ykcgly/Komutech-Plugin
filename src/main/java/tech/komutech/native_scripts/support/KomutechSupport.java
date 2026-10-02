package tech.komutech.native_scripts.support;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import tech.komutech.native_scripts.NativePluginHelper;
import tech.komutech.native_scripts.support.ChatColors;
import tech.komutech.native_scripts.support.InventoryHelper;

public final class KomutechSupport {
   private static final String FULL_INVENTORY_MESSAGE = "&e背包已满，物品已掉落在地面上";

   private KomutechSupport() {
   }

   public static void send(Player var0, String var1) {
      ChatColors.send(var0, var1);
   }

   public static void actionBar(Player var0, String var1) {
      var0.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(var1));
   }

   public static void giveOrDrop(Player var0, ItemStack var1) {
      InventoryHelper.giveOrDrop(var0, var1, "&e背包已满，物品已掉落在地面上");
   }

   public static void giveOrDrop(Player var0, String var1) {
      InventoryHelper.giveOrDrop(var0, var1, "&e背包已满，物品已掉落在地面上");
   }

   public static void playBell(Player var0) {
      Location var1 = var0.getLocation();
      var0.getWorld().playSound(var1, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0F, 1.0F);
   }

   public static Plugin plugin() {
      Plugin var0 = NativePluginHelper.getPlugin();
      return var0 != null ? var0 : Bukkit.getPluginManager().getPlugin("Komutech");
   }

   public static boolean isRightClick(Object var0) {
      if (var0 == null) {
         return false;
      } else {
         try {
            return Boolean.TRUE.equals(var0.getClass().getMethod("isRightClicked").invoke(var0));
         } catch (ReflectiveOperationException var2) {
            return false;
         }
      }
   }

   public static boolean isShiftClick(Object var0) {
      if (var0 == null) {
         return false;
      } else {
         try {
            return Boolean.TRUE.equals(var0.getClass().getMethod("isShiftClicked").invoke(var0));
         } catch (ReflectiveOperationException var2) {
            return false;
         }
      }
   }
}
