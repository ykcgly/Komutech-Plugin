package tech.komutech.native_scripts.item;

import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import tech.komutech.native_scripts.combat.ScrollCombatEngine;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.PlayerAttributeStore;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class StaffScript implements NativeScript {
   private final String staffKey;

   public StaffScript(String var1) {
      this.staffKey = var1;
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("onUse".equals(var1)) {
         return UseEvents.parse(var2[0]).map(this::handleUse).orElse(null);
      } else if ("castScroll".equals(var1) && var2.length >= 3 && var2[0] instanceof Player var3) {
         Map var8 = var2[2] instanceof Map var5 ? PlayerAttributeStore.deepCopy(KomutechJsonMap(var5)) : null;
         if (var8 != null) {
            ItemStack var9 = var2[1] instanceof ItemStack var10 ? var10 : var3.getInventory().getItemInMainHand();
            String var11 = ScrollCombatEngine.getBoundSkill(var9);
            if (var11 != null) {
               ScrollCombatEngine.castScroll(var3, var9, var8, var11);
               PlayerAttributeStore.save(var3.getName(), var8);
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private Object handleUse(UseEvents.Context var1) {
      if (var1.hand() != EquipmentSlot.HAND) {
         return null;
      } else {
         Player var2 = var1.player();
         ItemStack var3 = var1.item();
         Map var4 = PlayerAttributeStore.load(var2.getName());
         if (var4 == null) {
            KomutechSupport.send(var2, "§c无法读取属性数据");
            return null;
         } else if (var2.isSneaking()) {
            String var5 = ScrollCombatEngine.getBoundSkill(var3);
            if (var5 == null) {
               KomutechSupport.send(var2, "§c未绑定卷轴");
               return null;
            } else {
               ScrollCombatEngine.castScroll(var2, var3, var4, var5);
               PlayerAttributeStore.save(var2.getName(), var4);
               return null;
            }
         } else {
            if (ScrollCombatEngine.handleStaffRightClick(var2, () -> {
               ScrollCombatEngine.staffAttack(var2, var3, var4, this.staffKey);
               PlayerAttributeStore.save(var2.getName(), var4);
            })) {
               ScrollCombatEngine.switchScroll(var2, var3);
            }

            return null;
         }
      }
   }

   @SuppressWarnings("unchecked")
   private static Map<String, Object> KomutechJsonMap(Map<?, ?> var0) {
      return (Map<String, Object>)(Map<?, ?>)var0;
   }
}
