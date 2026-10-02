package tech.komutech.native_scripts.support;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class UseEvents {
   private UseEvents() {
   }

   public static Optional<UseEvents.Context> parse(Object var0) {
      if (!(var0 instanceof PlayerRightClickEvent var1)) {
         if (var0 instanceof PlayerInteractEvent var5) {
            EquipmentSlot var6 = var5.getHand() != null ? var5.getHand() : EquipmentSlot.HAND;
            ItemStack var7 = var6 == EquipmentSlot.OFF_HAND
               ? var5.getPlayer().getInventory().getItemInOffHand()
               : var5.getPlayer().getInventory().getItemInMainHand();
            return Optional.of(new UseEvents.Context(var5.getPlayer(), var7, var6));
         } else {
            return Optional.empty();
         }
      } else {
         Player var2 = var1.getPlayer();
         ItemStack var3 = var1.getItem();
         EquipmentSlot var4 = var1.getHand();
         if (var3 == null || var3.getType().isAir()) {
            var3 = var4 == EquipmentSlot.OFF_HAND ? var2.getInventory().getItemInOffHand() : var2.getInventory().getItemInMainHand();
         }

         return Optional.of(new UseEvents.Context(var2, var3, var4));
      }
   }

   public static Optional<PlayerInteractEvent> interactEvent(Object var0) {
      if (var0 instanceof PlayerRightClickEvent var2) {
         return Optional.ofNullable(var2.getInteractEvent());
      } else {
         return var0 instanceof PlayerInteractEvent var1 ? Optional.of(var1) : Optional.empty();
      }
   }

   public static Player getPlayer(Object var0) {
      return parse(var0).map(UseEvents.Context::player).orElse(null);
   }

   public static EquipmentSlot getHand(Object var0) {
      return parse(var0).map(UseEvents.Context::hand).orElse(EquipmentSlot.HAND);
   }

   public static boolean isMainHand(Object var0) {
      return getHand(var0) == EquipmentSlot.HAND;
   }

   public static void consumeOne(UseEvents.Context var0) {
      ItemStack var1 = var0.item();
      if (var1 != null && !var1.getType().isAir()) {
         var1.setAmount(var1.getAmount() - 1);
         if (var0.hand() == EquipmentSlot.OFF_HAND) {
            var0.player().getInventory().setItemInOffHand(var1.getAmount() <= 0 ? null : var1);
         } else {
            var0.player().getInventory().setItemInMainHand(var1.getAmount() <= 0 ? null : var1);
         }
      }
   }

   public static void decrementItem(ItemStack var0) {
      if (var0 != null && !var0.getType().isAir()) {
         if (var0.getAmount() > 1) {
            var0.setAmount(var0.getAmount() - 1);
         } else {
            var0.setAmount(0);
         }
      }
   }

   public record Context(Player player, ItemStack item, EquipmentSlot hand) {
   }
}
