package tech.komutech.native_scripts.support;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class ChatInputService implements Listener {
   private static ChatInputService instance;
   private final Map<UUID, Consumer<String>> pending = new ConcurrentHashMap<>();

   public static void register(Plugin var0) {
      if (instance == null) {
         instance = new ChatInputService();
         var0.getServer().getPluginManager().registerEvents(instance, var0);
      }
   }

   public static boolean request(Player var0, String var1, Consumer<String> var2) {
      if (var0 != null && var2 != null) {
         register(KomutechSupport.plugin());
         instance.pending.put(var0.getUniqueId(), var2);
         var0.sendMessage(var1);
         return true;
      } else {
         return false;
      }
   }

   public static void cancel(Player var0) {
      if (instance != null && var0 != null) {
         instance.pending.remove(var0.getUniqueId());
      }
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onChat(AsyncPlayerChatEvent var1) {
      Consumer var2 = this.pending.remove(var1.getPlayer().getUniqueId());
      if (var2 != null) {
         var1.setCancelled(true);
         String var3 = var1.getMessage();
         Plugin var4 = KomutechSupport.plugin();
         if (var4 != null) {
            Bukkit.getScheduler().runTask(var4, () -> var2.accept(var3));
         }
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent var1) {
      this.pending.remove(var1.getPlayer().getUniqueId());
   }
}
