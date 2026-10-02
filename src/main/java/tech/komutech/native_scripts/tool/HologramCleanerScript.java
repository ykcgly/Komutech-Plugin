package tech.komutech.native_scripts.tool;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import tech.komutech.native_scripts.support.AdminToolsSupport;
import tech.komutech.native_scripts.support.EntityQueries;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class HologramCleanerScript implements NativeScript {
   private static final Map<UUID, Entity> targets = new ConcurrentHashMap<>();

   @Override
   public Object invoke(String var1, Object... var2) {
      return !"onUse".equals(var1) ? null : UseEvents.parse(var2[0]).map(this::handleUse).orElse(null);
   }

   private Object handleUse(UseEvents.Context var1) {
      Player var2 = var1.player();
      if (!AdminToolsSupport.checkCooldown(var2, 500L)) {
         return null;
      } else if (!AdminToolsSupport.hasPermission(var2, var2.getLocation())) {
         KomutechSupport.send(var2, "§c你没有权限在此区域移除全息文字！");
         var2.playSound(var2.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 0.5F);
         return null;
      } else {
         Entity var3 = targets.get(var2.getUniqueId());
         if (var3 != null && var3.isValid()) {
            if (!AdminToolsSupport.hasPermission(var2, var3.getLocation())) {
               KomutechSupport.send(var2, "§c你没有权限移除这个全息文字！");
               targets.remove(var2.getUniqueId());
               return null;
            } else {
               var3.remove();
               targets.remove(var2.getUniqueId());
               KomutechSupport.actionBar(var2, "§a成功清除已选中的全息文字！");
               var2.playSound(var2.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.5F);
               return null;
            }
         } else {
            targets.remove(var2.getUniqueId());
            ArmorStand var4 = null;
            double var5 = Double.MAX_VALUE;

            for (Entity var8 : EntityQueries.entities(var2.getLocation(), 3.0)) {
               if (var8 instanceof ArmorStand var9 && var9.isInvisible() && !var9.hasGravity() && var9.isMarker()) {
                  double var10 = var2.getLocation().distanceSquared(var9.getLocation());
                  if (var10 < var5) {
                     var5 = var10;
                     var4 = var9;
                  }
               }
            }

            if (var4 == null) {
               KomutechSupport.send(var2, "§e3格范围内未找到全息文字！");
               var2.playSound(var2.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 0.5F);
               return null;
            } else {
               String var12 = var4.getCustomName();
               KomutechSupport.send(var2, "§e检测到最近的全息文字: " + (var12 == null ? "§7(无文字)" : var12));
               KomutechSupport.send(var2, "§7再次右键可清除该文字。");
               var2.playSound(var2.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5F, 1.0F);
               targets.put(var2.getUniqueId(), var4);
               return null;
            }
         }
      }
   }
}
