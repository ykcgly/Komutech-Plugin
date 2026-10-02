package tech.komutech.native_scripts.support;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryView;

public interface KomutechMenuHandler {
   boolean handles(InventoryView var1);

   void onClick(InventoryClickEvent var1);

   default void onDrag(InventoryDragEvent var1) {
   }

   default void onClose(InventoryCloseEvent var1) {
   }
}
