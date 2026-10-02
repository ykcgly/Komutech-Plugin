package tech.komutech.objects.script.enhanced;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import tech.komutech.util.NbtBridge;

public final class NBTAPIIntegration {
   public static final NBTAPIIntegration instance = new NBTAPIIntegration();

   private NBTAPIIntegration() {
   }

   public Object readItem(ItemStack var1) {
      return NbtBridge.readItem(var1);
   }

   public Object getOrCreateCompound(Object var1, String var2) {
      return NbtBridge.getOrCreateCompound(var1, var2);
   }

   public Object readBlock(Block var1) {
      return NbtBridge.readBlock(var1);
   }

   public Object readEntity(Entity var1) {
      return NbtBridge.readEntity(var1);
   }

   public Object createCompound() {
      return NbtBridge.createCompound();
   }
}
