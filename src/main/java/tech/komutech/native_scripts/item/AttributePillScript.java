package tech.komutech.native_scripts.item;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import tech.komutech.native_scripts.support.AttributeApplier;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.PlayerAttributeStore;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class AttributePillScript implements NativeScript {
   private static final Map<String, AttributePillScript.PillConfig> PILLS = new LinkedHashMap<>();

   private static void register(String var0, String var1, double var2, double var4) {
      PILLS.put(var0, new AttributePillScript.PillConfig(var1, var2, var4));
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      return !"onUse".equals(var1) ? null : UseEvents.parse(var2[0]).map(this::handleUse).orElse(null);
   }

   private Object handleUse(UseEvents.Context var1) {
      Player var2 = var1.player();
      ItemStack var3 = var1.item();
      SlimefunItem var4 = SlimefunItem.getByItem(var3);
      if (var4 == null) {
         KomutechSupport.actionBar(var2, "§c请手持丹药使用！");
         return null;
      } else {
         AttributePillScript.PillConfig var5 = PILLS.get(var4.getId());
         if (var5 == null) {
            KomutechSupport.actionBar(var2, "§c无效的丹药！");
            return null;
         } else {
            Map var6 = PlayerAttributeStore.load(var2.getName());
            if (var6 == null) {
               KomutechSupport.actionBar(var2, "§c尚未鉴灵，无法服用丹药！");
               return null;
            } else {
               double var7 = PlayerAttributeStore.getDouble(var6, var5.attribute(), 1.0);
               if (var7 >= var5.cap()) {
                  KomutechSupport.actionBar(var2, "§c当前" + var5.attribute() + "已达该品级上限(" + format(var5.cap()) + ")！");
                  return null;
               } else {
                  double var9 = round(Math.min(var7 + var5.bonus(), var5.cap()));
                  if (var9 <= var7) {
                     KomutechSupport.actionBar(var2, "§c当前" + var5.attribute() + "无法通过该丹药继续提升！");
                     return null;
                  } else {
                     var6.put(var5.attribute(), var9);
                     if ("根骨".equals(var5.attribute())) {
                        AttributeApplier.applyPlayerAttributes(var2, var6, false);
                     }

                     if (!PlayerAttributeStore.save(var2.getName(), var6)) {
                        KomutechSupport.actionBar(var2, "§c保存属性失败，请联系管理员！");
                        return null;
                     } else {
                        UseEvents.consumeOne(var1);
                        double var11 = round(var9 - var7);
                        KomutechSupport.actionBar(
                           var2, "§a" + var5.attribute() + " +" + format(var11) + " §7(当前 " + format(var9) + " / 该品级上限 " + format(var5.cap()) + ")"
                        );
                        KomutechSupport.playBell(var2);
                        return null;
                     }
                  }
               }
            }
         }
      }
   }

   private static double round(double var0) {
      return Math.round(var0 * 100.0) / 100.0;
   }

   private static String format(double var0) {
      return Math.rint(var0) == var0 ? String.valueOf((long)var0) : String.format("%.2f", var0);
   }

   static {
      register("KOMUTECH_L_DY_低品悟性丹", "悟性", 0.1, 3.0);
      register("KOMUTECH_L_DY_中品悟性丹", "悟性", 0.25, 5.0);
      register("KOMUTECH_L_DY_高品悟性丹", "悟性", 0.5, 8.0);
      register("KOMUTECH_L_DY_极品悟性丹", "悟性", 1.0, 10.0);
      register("KOMUTECH_L_DY_低品洗髓丹", "根骨", 0.1, 3.0);
      register("KOMUTECH_L_DY_中品洗髓丹", "根骨", 0.25, 5.0);
      register("KOMUTECH_L_DY_高品洗髓丹", "根骨", 0.5, 8.0);
      register("KOMUTECH_L_DY_极品洗髓丹", "根骨", 1.0, 10.0);
   }

   private record PillConfig(String attribute, double bonus, double cap) {
   }
}
