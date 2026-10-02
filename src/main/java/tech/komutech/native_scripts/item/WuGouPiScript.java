package tech.komutech.native_scripts.item;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.ArrayList;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class WuGouPiScript implements NativeScript {
   @Override
   public Object invoke(String var1, Object... var2) {
      return !"onUse".equals(var1) ? null : UseEvents.parse(var2[0]).map(this::handleUse).orElse(null);
   }

   private Object handleUse(UseEvents.Context var1) {
      Player var2 = var1.player();
      double var3 = var2.getMaxHealth();
      double var5 = var3 * 0.99;
      double var7 = var2.getHealth() - var5;
      var2.setHealth(Math.max(var7, 0.0));
      ItemStack var9 = var2.getInventory().getItemInMainHand();
      if (var9.getAmount() > 0) {
         var9.setAmount(var9.getAmount() - 1);
      }

      SlimefunItem var10 = SlimefunItem.getById("KOMUTECH_L_DJ_蕴灵身");
      if (var10 == null) {
         KomutechSupport.send(var2, "§c错误：未找到蕴灵身物品！");
         return null;
      } else {
         ItemStack var11 = var10.getItem().clone();
         ItemMeta var12 = var11.getItemMeta();
         ArrayList var13 = var12.hasLore() ? new ArrayList(var12.getLore()) : new ArrayList();
         var13.add("§b已绑定：§a" + var2.getName());
         var12.setLore(var13);
         var11.setItemMeta(var12);
         KomutechSupport.giveOrDrop(var2, var11);
         KomutechSupport.send(var2, "§a你已绑定蕴灵身！");
         KomutechSupport.send(var2, "§c已扣除最大生命值的99%！");
         if (var7 <= 0.0) {
            KomutechSupport.send(var2, "§4警告：生命值耗尽！");
         }

         return null;
      }
   }
}
