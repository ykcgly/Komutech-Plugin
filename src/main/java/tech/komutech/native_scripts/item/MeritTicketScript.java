package tech.komutech.native_scripts.item;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.PlayerAttributeStore;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class MeritTicketScript implements NativeScript {
   private static final String ITEM_ID = "KOMUTECH_L_DJ_功德券";
   private static final int PER_TICKET = 1000;

   @Override
   public Object invoke(String var1, Object... var2) {
      return !"onUse".equals(var1) ? null : UseEvents.parse(var2[0]).map(this::handleUse).orElse(null);
   }

   private Object handleUse(UseEvents.Context var1) {
      Player var2 = var1.player();
      MeritTicketScript.TicketSlot var3 = findTicket(var2);
      if (var3 == null) {
         KomutechSupport.actionBar(var2, "§c请手持功德券使用！");
         return null;
      } else {
         Map var4 = PlayerAttributeStore.load(var2.getName());
         if (var4 == null) {
            KomutechSupport.actionBar(var2, "§c无法读取玩家属性数据！");
            return null;
         } else {
            int var5 = PlayerAttributeStore.getInt(var4, "功德", 0);
            boolean var6 = var2.isSneaking();
            int var7 = var6 ? var3.amount() : 1;
            if (var7 < 1) {
               KomutechSupport.actionBar(var2, "§c没有足够的功德券！");
               return null;
            } else {
               int var8 = var7 * 1000;
               var4.put("功德", var5 + var8);
               if (!PlayerAttributeStore.save(var2.getName(), var4)) {
                  KomutechSupport.actionBar(var2, "§c保存数据失败！");
                  return null;
               } else {
                  int var9 = consumeTicket(var2, var3, var6);
                  KomutechSupport.actionBar(var2, "§a功德 +" + var8 + " §7(消耗" + var9 + "张) §7| §f当前 " + (var5 + var8));
                  KomutechSupport.playBell(var2);
                  return null;
               }
            }
         }
      }
   }

   private static MeritTicketScript.TicketSlot findTicket(Player var0) {
      ItemStack var1 = var0.getInventory().getItemInMainHand();
      if (isTicket(var1)) {
         return new MeritTicketScript.TicketSlot("main", var1, var1.getAmount());
      } else {
         ItemStack var2 = var0.getInventory().getItemInOffHand();
         return isTicket(var2) ? new MeritTicketScript.TicketSlot("off", var2, var2.getAmount()) : null;
      }
   }

   private static boolean isTicket(ItemStack var0) {
      if (var0 != null && !var0.getType().isAir()) {
         SlimefunItem var1 = SlimefunItem.getByItem(var0);
         return var1 != null && "KOMUTECH_L_DJ_功德券".equals(var1.getId());
      } else {
         return false;
      }
   }

   private static int consumeTicket(Player var0, MeritTicketScript.TicketSlot var1, boolean var2) {
      PlayerInventory var3 = var0.getInventory();
      ItemStack var4 = "main".equals(var1.slot()) ? var3.getItemInMainHand() : var3.getItemInOffHand();
      int var5 = var2 ? var4.getAmount() : 1;
      if (var4.getAmount() > var5) {
         var4.setAmount(var4.getAmount() - var5);
      } else if ("main".equals(var1.slot())) {
         var3.setItemInMainHand(null);
      } else {
         var3.setItemInOffHand(null);
      }

      var0.updateInventory();
      return var5;
   }

   private record TicketSlot(String slot, ItemStack item, int amount) {
   }
}
