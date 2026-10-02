package tech.komutech.native_scripts.item;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tech.komutech.native_scripts.cultivation.JianLingShiSupport;
import tech.komutech.native_scripts.support.AttributeApplier;
import tech.komutech.native_scripts.support.CultivationMath;
import tech.komutech.native_scripts.support.KomutechChatInput;
import tech.komutech.native_scripts.support.KomutechPaths;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.PlayerAttributeStore;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class ZeLingZhuScript implements NativeScript {
   private static final String ITEM_ID = "KOMUTECH_L_DJ_擇靈珠";
   private static final Pattern SPLIT = Pattern.compile("[、，\\s]+");

   @Override
   public Object invoke(String var1, Object... var2) {
      return !"onUse".equals(var1) ? null : UseEvents.parse(var2[0]).map(this::handleUse).orElse(null);
   }

   private Object handleUse(UseEvents.Context var1) {
      Player var2 = var1.player();
      ItemStack var3 = var1.item();
      SlimefunItem var4 = SlimefunItem.getByItem(var3);
      if (var4 == null || !"KOMUTECH_L_DJ_擇靈珠".equals(var4.getId())) {
         KomutechSupport.send(var2, "§c请手持擇靈珠使用！");
         return null;
      } else if (var2.isSneaking()) {
         KomutechSupport.send(var2, "§a请输入定向灵根（用顿号或空格分隔），输入 cancel 取消：");
         KomutechChatInput.waitFor(var2, var1x -> {
            if (var2.isOnline()) {
               if ("cancel".equalsIgnoreCase(var1x)) {
                  KomutechSupport.send(var2, "§c已取消");
               } else {
                  List var2x = parseElements(var1x);
                  if (var2x.isEmpty()) {
                     KomutechSupport.send(var2, "§c无有效元素");
                  } else {
                     ItemStack var3x = var2.getInventory().getItemInMainHand();
                     ItemMeta var4x = var3x.getItemMeta();
                     ArrayList<String> var5x = var4x.hasLore() ? new ArrayList<>(var4x.getLore()) : new ArrayList<>();
                     var5x.removeIf(var0x -> var0x.contains("定向灵根:"));
                     var5x.add("§7定向灵根: §e" + String.join("、", var2x));
                     var4x.setLore(var5x);
                     var3x.setItemMeta(var4x);
                     KomutechSupport.send(var2, "§a定向灵根已记录到物品上！");
                  }
               }
            }
         });
         return null;
      } else {
         ItemMeta var5 = var3.getItemMeta();
         if (!var5.hasLore()) {
            KomutechSupport.send(var2, "§c该物品尚未设置定向灵根，请蹲下右键设置！");
            return null;
         } else {
            List var6 = null;

            for (String var8 : var5.getLore()) {
               if (var8.contains("定向灵根:")) {
                  String var9 = var8.split(":")[1].trim().replaceAll("§.", "");
                  var6 = List.of(var9.split("、"));
                  break;
               }
            }

            if (var6 != null && !var6.isEmpty()) {
               if (Files.exists(KomutechPaths.playerAttributes().resolve("[" + var2.getName() + "].json"))) {
                  KomutechSupport.send(var2, "§c你已经鉴灵了，无法使用擇靈珠");
                  return null;
               } else {
                  ItemStack var12 = var2.getInventory().getItemInMainHand();
                  var12.setAmount(var12.getAmount() - 1);
                  if (var12.getAmount() <= 0) {
                     var2.getInventory().setItemInMainHand(null);
                  }

                  double var13 = Math.round((0.8 + Math.random() * 0.19) * 100.0) / 100.0;
                  HashMap var10 = new HashMap();
                  var10.put("定向", true);
                  var10.put("灵根", String.join("、", var6));
                  var10.put("总品质", var13);
                  Map var11 = JianLingShiSupport.rollAttributes(var10);
                  if (PlayerAttributeStore.save(var2.getName(), var11)) {
                     AttributeApplier.applyStoredAttributes(var2);
                     KomutechSupport.send(
                        var2,
                        "§a定向成功！灵根："
                           + String.join("、", var6)
                           + "，总品质："
                           + String.format("%.2f", var13)
                           + "，灵根属性："
                           + PlayerAttributeStore.getString(var11, "灵根属性", "未知")
                     );
                  } else {
                     KomutechSupport.send(var2, "§c定向失败，请联系管理员");
                  }

                  return null;
               }
            } else {
               KomutechSupport.send(var2, "§c未检测到有效的定向灵根，请蹲下右键设置！");
               return null;
            }
         }
      }
   }

   private static List<String> parseElements(String var0) {
      List<String> var1 = SPLIT.matcher(var0).find() ? List.of(SPLIT.split(var0)) : var0.codePoints().mapToObj(var0x -> String.valueOf((char)var0x)).toList();
      ArrayList<String> var2 = new ArrayList<>();

      for (String var4 : var1) {
         if (!var4.isBlank() && CultivationMath.VALID_LINGGEN.contains(var4)) {
            var2.add(var4);
         }
      }

      return var2;
   }
}
