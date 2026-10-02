package tech.komutech.native_scripts.item;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.PlayerAttributeStore;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class LingStoneScript implements NativeScript {
   private static final Map<String, LingStoneScript.StoneConfig> STONES = Map.of(
      "KOMUTECH_L_DJ_XPLS",
      new LingStoneScript.StoneConfig(1, "下品灵石"),
      "KOMUTECH_L_DJ_ZPLS",
      new LingStoneScript.StoneConfig(100, "中品灵石"),
      "KOMUTECH_L_DJ_SPLS",
      new LingStoneScript.StoneConfig(10000, "上品灵石"),
      "KOMUTECH_L_DJ_JPLS",
      new LingStoneScript.StoneConfig(1000000, "极品灵石")
   );

   @Override
   public Object invoke(String var1, Object... var2) {
      return !"onUse".equals(var1) ? null : UseEvents.parse(var2[0]).map(this::handleUse).orElse(null);
   }

   private Object handleUse(UseEvents.Context var1) {
      if (var1.hand() != EquipmentSlot.HAND) {
         return null;
      } else {
         Player var2 = var1.player();
         ItemStack var3 = var2.getInventory().getItemInMainHand();
         if (var3 != null && !var3.getType().isAir() && var3.getAmount() >= 1) {
            LingStoneScript.StoneConfig var4 = getStoneConfig(var3);
            if (var4 == null) {
               KomutechSupport.send(var2, "&c主手物品不是有效的灵石！");
               return null;
            } else {
               Map var5 = PlayerAttributeStore.load(var2.getName());
               if (var5 == null) {
                  KomutechSupport.send(var2, "&c你还没有玩家属性数据，无法充能！");
                  return null;
               } else {
                  if (!var5.containsKey("灵力")) {
                     var5.put("灵力", "100/100+0");
                  }

                  PlayerAttributeStore.Lingli var6 = PlayerAttributeStore.parseLingli(var5.get("灵力"));
                  double var7 = var6.totalMax();
                  if (var6.current() >= var7) {
                     KomutechSupport.send(var2, "&e灵力已满，无需充能！");
                     return null;
                  } else {
                     double var9 = var7 - var6.current();
                     int var11 = 1;
                     if (var2.isSneaking()) {
                        var11 = Math.min(var3.getAmount(), Math.max(1, (int)Math.floor(var9 / var4.energy)));
                     }

                     double var12 = Math.min((double)var11 * var4.energy, var9);
                     int var14 = (int)Math.floor(var12 / var4.energy);
                     if (var14 < 1) {
                        var14 = 1;
                     }

                     double var15 = Math.min(var6.current() + var12, var7);
                     var5.put("灵力", PlayerAttributeStore.formatLingli(new PlayerAttributeStore.Lingli(var15, var6.max(), var6.bonus())));
                     if (!PlayerAttributeStore.save(var2.getName(), var5)) {
                        KomutechSupport.send(var2, "&c保存数据失败！");
                        return null;
                     } else {
                        var3.setAmount(var3.getAmount() - var14);
                        KomutechSupport.send(var2, "&a[" + var4.name + "] 充能成功！");
                        KomutechSupport.send(var2, "&b当前灵力：&6" + String.format("%.2f", var15) + " &7/ &6" + var7 + " &7（消耗&6" + var14 + "&7颗）");
                        KomutechSupport.playBell(var2);
                        return null;
                     }
                  }
               }
            }
         } else {
            KomutechSupport.send(var2, "&c主手物品不足！");
            return null;
         }
      }
   }

   private static LingStoneScript.StoneConfig getStoneConfig(ItemStack var0) {
      SlimefunItem var1 = SlimefunItem.getByItem(var0);
      return var1 == null ? null : STONES.get(var1.getId());
   }

   private record StoneConfig(int energy, String name) {
   }
}
