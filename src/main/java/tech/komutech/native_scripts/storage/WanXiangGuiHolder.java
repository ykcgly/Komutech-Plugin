package tech.komutech.native_scripts.storage;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class WanXiangGuiHolder implements InventoryHolder {
   public final String storageName;
   public final String page;
   public final String mode;

   public WanXiangGuiHolder(String var1, String var2, String var3) {
      this.storageName = var1 == null ? "" : var1;
      this.page = var2 == null ? "1" : var2;
      this.mode = var3 == null ? "normal" : var3;
   }

   public Inventory getInventory() {
      return null;
   }
}
