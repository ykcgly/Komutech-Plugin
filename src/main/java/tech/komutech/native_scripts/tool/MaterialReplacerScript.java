package tech.komutech.native_scripts.tool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import tech.komutech.native_scripts.support.AdminToolsSupport;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class MaterialReplacerScript implements NativeScript {
   private static final Map<String, Material> stored = new ConcurrentHashMap<>();

   @Override
   public Object invoke(String var1, Object... var2) {
      return !"onUse".equals(var1) ? null : UseEvents.parse(var2[0]).map(this::handleUse).orElse(null);
   }

   private Object handleUse(UseEvents.Context var1) {
      Player var2 = var1.player();
      Block var3 = var2.getTargetBlockExact(3);
      if (var3 == null || var3.getType().isAir()) {
         KomutechSupport.actionBar(var2, "§c❌ 未瞄准有效方块！");
         return null;
      } else if (!AdminToolsSupport.hasPermission(var2, var3.getLocation())) {
         KomutechSupport.send(var2, "§c你没有权限在此区域操作方块！");
         var2.playSound(var3.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 0.5F);
         return null;
      } else if (var2.isSneaking()) {
         stored.put(var2.getName(), var3.getType());
         KomutechSupport.actionBar(var2, "§a✅ 存储：§e" + var3.getType().name().replace('_', ' '));
         var2.playSound(var3.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
         return null;
      } else {
         Material var4 = stored.get(var2.getName());
         if (var4 == null) {
            KomutechSupport.actionBar(var2, "§c❌ 请先用Shift+右键存储材质！");
            return null;
         } else if (var3.getType() == var4) {
            KomutechSupport.actionBar(var2, "§c❌ 目标方块已经是该材质，无需替换");
            return null;
         } else {
            var3.setType(var4);
            KomutechSupport.actionBar(var2, "§a✅ 替换为：§e" + var4.name().replace('_', ' '));
            var2.playSound(var3.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.5F);
            return null;
         }
      }
   }
}
