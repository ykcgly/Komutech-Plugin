package tech.komutech.native_scripts.item;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import tech.komutech.native_scripts.support.AttributeApplier;
import tech.komutech.native_scripts.support.KomutechChatInput;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.PlayerAttributeStore;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class XiDianZhuScript implements NativeScript {
   public static final String ITEM_ID = "KOMUTECH_L_DJ_洗点珠";
   public static final String BASE_KEY = "属性基底";
   public static final List<String> ALLOCATABLE_ATTRS = List.of("血量", "攻击力", "防御力", "速度", "灵识", "灵气获取");
   private static final Set<String> PENDING = new HashSet();

   public Object invoke(String var1, Object... var2) {
      return !"onUse".equals(var1) ? null : UseEvents.parse(var2[0]).map(this::handleUse).orElse((Object)null);
   }

   private Object handleUse(UseEvents.Context var1) {
      Player var2 = var1.player();
      String var3 = var2.getName();
      if (PENDING.contains(var3)) {
         return null;
      } else if (!PlayerAttributeStore.exists(var3)) {
         KomutechSupport.send(var2, "§c你没有玩家属性数据，无法洗点。");
         return null;
      } else {
         Map var4 = PlayerAttributeStore.load(var3);
         if (var4 == null) {
            KomutechSupport.send(var2, "§c无法读取属性数据。");
            return null;
         } else if (!hasAllocatedAboveBase(var4)) {
            KomutechSupport.send(var2, "§e当前没有可重置的加点。");
            return null;
         } else {
            EquipmentSlot var5 = var1.hand() != null ? var1.hand() : EquipmentSlot.HAND;
            int var6 = previewRefund(var4);
            PENDING.add(var3);
            KomutechSupport.send(var2, "§d✦ 洗点珠：将清空已分配加点并返还属性点（修为/灵根不变）");
            KomutechSupport.send(var2, "§e预计返还后属性点：§a" + var6);
            KomutechSupport.send(var2, "§c输入 §f确认洗点 §c继续，输入 §fcancel §c取消：");
            KomutechChatInput.waitFor(var2, (var3x) -> {
               PENDING.remove(var3);
               if ("cancel".equalsIgnoreCase(var3x.trim())) {
                  KomutechSupport.send(var2, "§a已取消洗点。");
               } else if (!"确认洗点".equals(var3x.replaceAll("§.", "").trim())) {
                  KomutechSupport.send(var2, "§c输入不匹配，操作取消。");
               } else if (!consumeOrb(var2, var5)) {
                  KomutechSupport.send(var2, "§c未检测到洗点珠，操作取消。");
               } else {
                  Map<String, Object> var80 = PlayerAttributeStore.load(var3);
                  if (var80 == null) {
                     KomutechSupport.send(var2, "§c属性数据已丢失，无法洗点。");
                  } else {
                     int var5x = applyRespec(var80);
                     AttributeApplier.applyPlayerAttributes(var2, var80, false);
                     if (!PlayerAttributeStore.save(var3, var80)) {
                        KomutechSupport.send(var2, "§c保存失败，请联系管理员。");
                     } else {
                        KomutechSupport.send(var2, "§a✦ 洗点完成！当前可分配属性点：§e" + var5x);
                        KomutechSupport.send(var2, "§7请打开玩家属性菜单重新加点。");
                     }
                  }
               }
            });
            return null;
         }
      }
   }

   public static void captureBase(Map<String, Object> var0) {
      if (var0 != null) {
         LinkedHashMap var1 = new LinkedHashMap();

         for(String var3 : ALLOCATABLE_ATTRS) {
            var1.put(var3, PlayerAttributeStore.getInt(var0, var3, 0));
         }

         var0.put("属性基底", var1);
      }
   }

   static int applyRespec(Map<String, Object> var0) {
      Map var1 = readBase(var0);
      int var2 = PlayerAttributeStore.getInt(var0, "属性点", 0);

      for(String var4 : ALLOCATABLE_ATTRS) {
         int var5 = PlayerAttributeStore.getInt(var0, var4, 0);
         int var6 = (Integer)var1.getOrDefault(var4, 0);
         var2 += Math.max(0, var5 - var6);
         var0.put(var4, var6);
      }

      var0.put("属性点", var2);
      captureBaseFromMap(var0, var1);
      return var2;
   }

   private static void captureBaseFromMap(Map<String, Object> var0, Map<String, Integer> var1) {
      LinkedHashMap var2 = new LinkedHashMap();

      for(String var4 : ALLOCATABLE_ATTRS) {
         var2.put(var4, var1.getOrDefault(var4, 0));
      }

      var0.put("属性基底", var2);
   }

   private static boolean hasAllocatedAboveBase(Map<String, Object> var0) {
      Map var1 = readBase(var0);

      for(String var3 : ALLOCATABLE_ATTRS) {
         if (PlayerAttributeStore.getInt(var0, var3, 0) > (Integer)var1.getOrDefault(var3, 0)) {
            return true;
         }
      }

      return false;
   }

   private static int previewRefund(Map<String, Object> var0) {
      Map var1 = readBase(var0);
      int var2 = PlayerAttributeStore.getInt(var0, "属性点", 0);

      for(String var4 : ALLOCATABLE_ATTRS) {
         int var5 = PlayerAttributeStore.getInt(var0, var4, 0);
         var2 += Math.max(0, var5 - (Integer)var1.getOrDefault(var4, 0));
      }

      return var2;
   }

   private static Map<String, Integer> readBase(Map<String, Object> var0) {
      LinkedHashMap var1 = new LinkedHashMap();
      Object var2 = var0.get("属性基底");
      if (var2 instanceof Map var8) {
         for(String var5 : ALLOCATABLE_ATTRS) {
            Object var6 = var8.get(var5);
            Integer var10002;
            if (var6 instanceof Number var7) {
               var10002 = var7.intValue();
            } else {
               var10002 = 0;
            }

            var1.put(var5, var10002);
         }

         return var1;
      } else {
         for(String var4 : ALLOCATABLE_ATTRS) {
            var1.put(var4, 0);
         }

         return var1;
      }
   }

   private static boolean consumeOrb(Player var0, EquipmentSlot var1) {
      PlayerInventory var2 = var0.getInventory();
      ItemStack var3 = var1 == EquipmentSlot.OFF_HAND ? var2.getItemInOffHand() : var2.getItemInMainHand();
      if (isOrb(var3)) {
         UseEvents.consumeOne(new UseEvents.Context(var0, var3, var1));
         return true;
      } else {
         ItemStack[] var4 = var2.getContents();

         for(int var5 = 0; var5 < var4.length; ++var5) {
            ItemStack var6 = var4[var5];
            if (isOrb(var6)) {
               if (var6.getAmount() > 1) {
                  var6.setAmount(var6.getAmount() - 1);
               } else {
                  var2.setItem(var5, (ItemStack)null);
               }

               return true;
            }
         }

         return false;
      }
   }

   private static boolean isOrb(ItemStack var0) {
      if (var0 != null && !var0.getType().isAir()) {
         SlimefunItem var1 = SlimefunItem.getByItem(var0);
         return var1 != null && "KOMUTECH_L_DJ_洗点珠".equals(var1.getId());
      } else {
         return false;
      }
   }
}
