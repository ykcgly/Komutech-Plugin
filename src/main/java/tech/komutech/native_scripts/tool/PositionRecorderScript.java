package tech.komutech.native_scripts.tool;

import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tech.komutech.native_scripts.support.AdminToolsSupport;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class PositionRecorderScript implements NativeScript {
   @Override
   public Object invoke(String var1, Object... var2) {
      return !"onUse".equals(var1) ? null : UseEvents.parse(var2[0]).map(this::handleUse).orElse(null);
   }

   private Object handleUse(UseEvents.Context var1) {
      Player var2 = var1.player();
      ItemStack var3 = var1.item();
      Block var4 = AdminToolsSupport.rayTraceBlock(var2, 5.0);
      if (var4 != null) {
         AdminToolsSupport.BlockPos var9 = new AdminToolsSupport.BlockPos(var4.getX(), var4.getY(), var4.getZ());
         String var10 = var2.isSneaking() ? "§6§l位置二：" : "§6§l位置一：";
         String var11 = var2.isSneaking() ? "位置二" : "位置一";
         AdminToolsSupport.writePos(var3, var10, var11, var9);
         KomutechSupport.send(var2, "§a已记录" + var11 + "：X:" + var9.x() + " Y:" + var9.y() + " Z:" + var9.z());
         return null;
      } else {
         ItemMeta var5 = var3.getItemMeta();
         List var6 = var5 != null && var5.hasLore() ? var5.getLore() : null;
         AdminToolsSupport.BlockPos var7 = AdminToolsSupport.parsePos(var6, "§6§l位置一：");
         AdminToolsSupport.BlockPos var8 = AdminToolsSupport.parsePos(var6, "§6§l位置二：");
         if (var7 == null && var8 == null) {
            KomutechSupport.send(var2, "§e暂无记录的位置");
            return null;
         } else {
            KomutechSupport.send(var2, "§6=== 当前记录的位置 ===");
            if (var7 != null) {
               KomutechSupport.send(var2, "§a位置一：X:" + var7.x() + " Y:" + var7.y() + " Z:" + var7.z());
            }

            if (var8 != null) {
               KomutechSupport.send(var2, "§a位置二：X:" + var8.x() + " Y:" + var8.y() + " Z:" + var8.z());
            }

            return null;
         }
      }
   }
}
