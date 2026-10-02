package tech.komutech.native_scripts.item;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.entity.Player;
import tech.komutech.native_scripts.support.AttributeApplier;
import tech.komutech.native_scripts.support.KomutechChatInput;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.PlayerAttributeStore;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class GuiYuanZhuScript implements NativeScript {
   private static final Set<String> PENDING = new HashSet<>();

   @Override
   public Object invoke(String var1, Object... var2) {
      return !"onUse".equals(var1) ? null : UseEvents.parse(var2[0]).map(this::handleUse).orElse(null);
   }

   private Object handleUse(UseEvents.Context var1) {
      Player var2 = var1.player();
      String var3 = var2.getName();
      if (PENDING.contains(var3)) {
         return null;
      } else if (!PlayerAttributeStore.exists(var3)) {
         KomutechSupport.send(var2, "§c你没有玩家属性数据，无法使用归元珠。");
         return null;
      } else {
         PENDING.add(var3);
         AttributeApplier.clearSwitchable(var2);
         KomutechSupport.send(var2, "§c警告：此操作将永久删除你的玩家属性数据！");
         KomutechSupport.send(var2, "§c输入 \"确认重置\" 以继续，输入 cancel 取消:");
         KomutechChatInput.waitFor(var2, var2x -> {
            PENDING.remove(var3);
            if ("cancel".equalsIgnoreCase(var2x.trim())) {
               KomutechSupport.send(var2, "§a已取消。");
            } else if (!"确认重置".equals(var2x.replaceAll("§.", "").trim())) {
               KomutechSupport.send(var2, "§c输入不匹配，操作取消。");
            } else {
               PlayerAttributeStore.delete(var3);
               KomutechSupport.send(var2, "§a玩家属性已重置，请重新进行灵根鉴定。");
            }
         });
         return null;
      }
   }
}
