package tech.komutech.native_scripts.tool;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.PrefabStructurePlacer;
import tech.komutech.native_scripts.support.PrefabStructureType;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class PrefabStructureScript implements NativeScript {
   private static final long COOLDOWN_MS = 3000L;
   private static final Map<UUID, Integer> DIRECTIONS = new HashMap<>();
   private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();

   @Override
   public Object invoke(String var1, Object... var2) {
      return !"onUse".equals(var1) ? null : UseEvents.parse(var2[0]).map(this::handleUse).orElse(null);
   }

   private Object handleUse(UseEvents.Context var1) {
      Player var2 = var1.player();
      if (var1.hand() != EquipmentSlot.HAND) {
         KomutechSupport.send(var2, "§c请使用主手右键放置。");
         return null;
      } else {
         SlimefunItem var3 = PrefabStructurePlacer.resolveItem(var1.item());
         PrefabStructureType var4 = PrefabStructureType.fromItem(var3);
         if (var4 == null) {
            KomutechSupport.send(var2, "§c无效的预制建筑道具。");
            return null;
         } else {
            UUID var5 = var2.getUniqueId();
            int var6 = DIRECTIONS.getOrDefault(var5, 0);
            if (var2.isSneaking()) {
               if (var4 == PrefabStructureType.JIAN_LING_SHI) {
                  var6 = (var6 + 1) % 4;
                  DIRECTIONS.put(var5, var6);
                  KomutechSupport.send(var2, "§e方向已切换为 " + PrefabStructurePlacer.directionLabel(var6));
               }

               this.showPreview(var2, var4, var6);
               return null;
            } else if (!var2.isOp() && !this.checkCooldown(var2)) {
               return null;
            } else {
               PrefabStructurePlacer.PlacementResult var7 = PrefabStructurePlacer.paste(var2, var4, var6);
               KomutechSupport.send(var2, var7.message());
               if (!var7.success()) {
                  return null;
               } else {
                  if (!var2.isOp()) {
                     COOLDOWNS.put(var5, System.currentTimeMillis());
                     UseEvents.consumeOne(var1);
                     var2.updateInventory();
                  }

                  return null;
               }
            }
         }
      }
   }

   private void showPreview(Player var1, PrefabStructureType var2, int var3) {
      PrefabStructurePlacer.PlacementSpec var4 = PrefabStructurePlacer.spec(var2);
      PrefabStructurePlacer.Dimensions var5 = PrefabStructurePlacer.dimensions(var2);
      var1.sendMessage("§8§m=========================");
      var1.sendMessage("§b预制建筑预览：§f" + var4.displayName());
      var1.sendMessage("§7方块数量：§f" + var4.blocks().size());
      var1.sendMessage("§7占地尺寸：§fX " + var5.width() + " · Y " + var5.height() + " · Z " + var5.depth());
      if (var4.rotatable()) {
         var1.sendMessage("§7当前方向：§f" + PrefabStructurePlacer.directionLabel(var3) + " §7(Shift+右键切换)");
      }

      var1.sendMessage("§7放置方式：§f看向方块 §7→ §f右键在其上方搭建");
      var1.sendMessage("§8§m=========================");
   }

   private boolean checkCooldown(Player var1) {
      long var2 = System.currentTimeMillis();
      Long var4 = COOLDOWNS.get(var1.getUniqueId());
      if (var4 != null && var2 - var4 < 3000L) {
         long var5 = (3000L - (var2 - var4)) / 1000L + 1L;
         KomutechSupport.send(var1, "§c冷却中，请等待 " + var5 + " 秒。");
         return false;
      } else {
         return true;
      }
   }
}
