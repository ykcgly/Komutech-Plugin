package tech.komutech.native_scripts.tool;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import tech.komutech.native_scripts.support.KomutechAsyncScheduler;
import tech.komutech.native_scripts.support.KomutechJson;
import tech.komutech.native_scripts.support.KomutechMenuHandler;
import tech.komutech.native_scripts.support.KomutechMenuRouter;
import tech.komutech.native_scripts.support.KomutechPaths;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.MainThread;
import tech.komutech.native_scripts.support.MenuGuiHelper;
import tech.komutech.native_scripts.NativeLifecycleScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class NaJieScript implements NativeLifecycleScript, KomutechMenuHandler {
   private static final String TITLE = "§b纳戒";
   private static final int SIZE = 54;
   private static final long COOLDOWN_MS = 1000L;
   private final Map<UUID, Long> cooldowns = new HashMap<>();
   private final Set<Player> openPlayers = new HashSet<>();
   private Plugin plugin;

   @Override
   public void registerLifecycle(Plugin var1) {
      this.plugin = var1;
      KomutechMenuRouter.registerHandler(this);
   }

   @Override
   public boolean handles(InventoryView var1) {
      return "§b纳戒".equals(var1.getTitle());
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      return !"onUse".equals(var1) ? null : UseEvents.parse(var2[0]).map(this::handleUse).orElse(null);
   }

   private Object handleUse(UseEvents.Context var1) {
      if (var1.hand() != EquipmentSlot.HAND) {
         return null;
      } else {
         Player var2 = var1.player();
         long var3 = System.currentTimeMillis();
         long var5 = this.cooldowns.getOrDefault(var2.getUniqueId(), 0L);
         if (var3 - var5 < 1000L) {
            double var9 = Math.ceil((1000L - (var3 - var5)) / 100.0) / 10.0;
            KomutechSupport.actionBar(var2, "§c纳戒冷却中，请等待 §6" + var9 + " §c秒");
            return null;
         } else {
            this.cooldowns.put(var2.getUniqueId(), var3);
            if ("§b纳戒".equals(var2.getOpenInventory().getTitle())) {
               KomutechSupport.send(var2, "§c纳戒已打开，请先关闭当前界面再试！");
               return null;
            } else {
               List[] var7 = new List[1];
               KomutechAsyncScheduler.submit(() -> var7[0] = this.load(var2), var3x -> MainThread.run(this.plugin, () -> {
                  Inventory var3xx = this.deserialize(var7[0]);
                  var2.openInventory(var3xx);
                  this.openPlayers.add(var2);
               }));
               return null;
            }
         }
      }
   }

   @Override
   public void onClick(InventoryClickEvent var1) {
   }

   @Override
   public void onDrag(InventoryDragEvent var1) {
   }

   @Override
   public void onClose(InventoryCloseEvent var1) {
      if (var1.getPlayer() instanceof Player var2) {
         if (this.openPlayers.remove(var2) && "§b纳戒".equals(var1.getView().getTitle())) {
            this.saveAsync(var2, var1.getInventory());
         }
      }
   }

   private void saveAsync(Player var1, Inventory var2) {
      try {
         ArrayList var3 = new ArrayList();

         for (int var4 = 0; var4 < var2.getSize(); var4++) {
            ItemStack var5 = var2.getItem(var4);
            if (var5 != null && !var5.getType().isAir()) {
               var3.add(Base64.getEncoder().encodeToString(var5.serializeAsBytes()));
            } else {
               var3.add(null);
            }
         }

         Path var7 = this.file(var1);
         String var8 = KomutechJson.stringify(var3);
         KomutechAsyncScheduler.submit(() -> {
            try {
               Files.createDirectories(KomutechPaths.naJie());
               Files.writeString(var7, var8, StandardCharsets.UTF_8);
            } catch (Exception var3x) {
            }
         }, var0 -> {});
      } catch (Exception var6) {
      }
   }

   private Path file(Player var1) {
      return KomutechPaths.naJie().resolve("[" + var1.getName() + "]纳戒.json");
   }

   private List<String> load(Player var1) {
      try {
         Path var2 = this.file(var1);
         if (!Files.exists(var2)) {
            return null;
         } else {
            Object var3 = KomutechJson.parse(Files.readString(var2, StandardCharsets.UTF_8));
            return KomutechJson.asList(var3).stream().map(String::valueOf).toList();
         }
      } catch (Exception var4) {
         return null;
      }
   }

   private Inventory deserialize(List<String> var1) {
      Inventory var2 = MenuGuiHelper.create(54, "§b纳戒");
      if (var1 == null) {
         return var2;
      } else {
         for (int var3 = 0; var3 < var1.size() && var3 < 54; var3++) {
            String var4 = (String)var1.get(var3);
            if (var4 != null && !"null".equals(var4)) {
               try {
                  var2.setItem(var3, ItemStack.deserializeBytes(Base64.getDecoder().decode(var4)));
               } catch (Exception var6) {
               }
            }
         }

         return var2;
      }
   }
}
