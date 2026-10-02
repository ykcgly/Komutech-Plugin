package tech.komutech.native_scripts.support;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.Plugin;

public final class KomutechMenuRouter implements Listener {
   private static final List<KomutechMenuHandler> HANDLERS = new CopyOnWriteArrayList<>();
   private static final Map<Inventory, KomutechMenuHandler> INVENTORY_HANDLERS = new ConcurrentHashMap<>();
   private static KomutechMenuRouter instance;

   private KomutechMenuRouter() {
   }

   public static void register(Plugin var0) {
      if (instance == null) {
         instance = new KomutechMenuRouter();
         var0.getServer().getPluginManager().registerEvents(instance, var0);
      }
   }

   public static void registerHandler(KomutechMenuHandler var0) {
      if (var0 != null && !HANDLERS.contains(var0)) {
         HANDLERS.add(var0);
      }
   }

   public static void bindInventory(Inventory var0, KomutechMenuHandler var1) {
      if (var0 != null && var1 != null) {
         INVENTORY_HANDLERS.put(var0, var1);
      }
   }

   public static void unbindInventory(Inventory var0) {
      if (var0 != null) {
         INVENTORY_HANDLERS.remove(var0);
      }
   }

   public static boolean isHandlerFor(Inventory var0, KomutechMenuHandler var1) {
      return var0 != null && var1 != null && var1 == INVENTORY_HANDLERS.get(var0);
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = false
   )
   public void onInventoryClick(InventoryClickEvent var1) {
      KomutechMenuHandler var2 = resolve(var1.getView().getTopInventory(), var1.getView());
      if (var2 != null) {
         var2.onClick(var1);
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST,
      ignoreCancelled = false
   )
   public void onInventoryDrag(InventoryDragEvent var1) {
      KomutechMenuHandler var2 = resolve(var1.getView().getTopInventory(), var1.getView());
      if (var2 != null) {
         var2.onDrag(var1);
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onInventoryClose(InventoryCloseEvent var1) {
      Inventory var2 = var1.getView().getTopInventory();
      KomutechMenuHandler var3 = INVENTORY_HANDLERS.remove(var2);
      if (var3 != null) {
         var3.onClose(var1);
      } else {
         KomutechMenuHandler var4 = resolve(var2, var1.getView());
         if (var4 != null) {
            var4.onClose(var1);
         }
      }
   }

   private static KomutechMenuHandler resolve(Inventory var0, InventoryView var1) {
      KomutechMenuHandler var2 = INVENTORY_HANDLERS.get(var0);
      if (var2 != null) {
         return var2;
      } else {
         for (KomutechMenuHandler var4 : HANDLERS) {
            if (var4.handles(var1)) {
               return var4;
            }
         }

         return null;
      }
   }
}
