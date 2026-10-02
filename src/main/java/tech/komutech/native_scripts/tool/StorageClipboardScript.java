package tech.komutech.native_scripts.tool;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import tech.komutech.native_scripts.support.AdminToolsSupport;
import tech.komutech.native_scripts.support.KomutechMenuHelper;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.MachineScriptHelper;

public final class StorageClipboardScript implements NativeScript {
   private static final Map<UUID, StorageClipboardScript.Clipboard> clipboards = new ConcurrentHashMap<>();

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("onUse".equals(var1)) {
         this.handleUse(var2[0]);
      }

      return null;
   }

   private void handleUse(Object var1) {
      try {
         Player var2 = (Player)var1.getClass().getMethod("getPlayer").invoke(var1);
         if (!(AdminToolsSupport.clickedBlock(var1) instanceof Block var4)) {
            KomutechSupport.send(var2, "§c请右键机器");
            return;
         }

         Location var5 = var4.getLocation();
         if (!AdminToolsSupport.hasPermission(var2, var5)) {
            KomutechSupport.send(var2, "§c你没有权限在此区域操作机器！");
            var2.playSound(var5, Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 0.5F);
            return;
         }

         SlimefunItem var6 = MachineScriptHelper.getSfItem(var5);
         if (var6 == null) {
            KomutechSupport.send(var2, "§c不是粘液机器");
            return;
         }

         Object var7 = MachineScriptHelper.getMenu(var5);
         if (var7 == null) {
            KomutechSupport.send(var2, "§c机器无存储");
            return;
         }

         if (var2.isSneaking()) {
            copy(var2, var6, var7);
         } else {
            apply(var2, var6.getId(), var7);
         }
      } catch (ReflectiveOperationException var8) {
      }
   }

   private static void copy(Player var0, SlimefunItem var1, Object var2) {
      ArrayList var3 = new ArrayList();
      int var4 = 0;
      ItemStack[] var5 = KomutechMenuHelper.getContents(var2);

      for (int var6 = 0; var6 < var5.length; var6++) {
         ItemStack var7 = var5[var6];
         if (var7 != null && !var7.getType().isAir()) {
            var3.add(new StorageClipboardScript.SlotItem(var6, var7.clone(), var7.getAmount()));
            var4 += var7.getAmount();
         }
      }

      String var8 = var1.getItemName();
      clipboards.put(var0.getUniqueId(), new StorageClipboardScript.Clipboard(var1.getId(), var8, var3));
      updateLore(var0.getInventory().getItemInMainHand(), var8);
      KomutechSupport.send(var0, "§6已复制: §b" + var8);
      KomutechSupport.send(var0, "§7物品: " + var4 + "个 (" + var3.size() + "槽)");
      var0.playSound(var0.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5F, 1.0F);
   }

   private static void apply(Player var0, String var1, Object var2) {
      StorageClipboardScript.Clipboard var3 = clipboards.get(var0.getUniqueId());
      if (var3 == null) {
         KomutechSupport.send(var0, "§c请先复制存储");
      } else if (!var3.machineId.equals(var1)) {
         KomutechSupport.send(var0, "§c机器不匹配");
      } else {
         ArrayList<StorageClipboardScript.FillSlot> var4 = new ArrayList<>();
         int var5 = 0;

         for (StorageClipboardScript.SlotItem var7 : var3.slots) {
            MachineScriptHelper.ItemStackInMenu var8 = MachineScriptHelper.getItemInSlot(var2, var7.slot);
            int var9 = 0;
            if (var8 != null && var8.stack().isSimilar(var7.item)) {
               var9 = var8.stack().getAmount();
            } else if (var8 != null && !var8.stack().getType().isAir()) {
               continue;
            }

            int var10 = var7.amount - var9;
            if (var10 > 0) {
               var5 += var10;
               var4.add(new StorageClipboardScript.FillSlot(var7.slot, var7.item, var10));
            }
         }

         if (var4.isEmpty()) {
            KomutechSupport.send(var0, "§a存储已满足");
            var0.playSound(var0.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5F, 2.0F);
         } else {
            int var12 = 0;

            for (StorageClipboardScript.FillSlot var14 : var4) {
               int var15 = AdminToolsSupport.takeSimilar(var0, var14.item, var14.need);
               if (var15 > 0) {
                  var12 += var15;
                  MachineScriptHelper.ItemStackInMenu var16 = MachineScriptHelper.getItemInSlot(var2, var14.slot);
                  if (var16 != null) {
                     ItemStack var11 = var16.stack();
                     var11.setAmount(var11.getAmount() + var15);
                     KomutechMenuHelper.replaceExistingItem(var2, var14.slot, var11);
                  } else {
                     ItemStack var17 = var14.item.clone();
                     var17.setAmount(var15);
                     KomutechMenuHelper.replaceExistingItem(var2, var14.slot, var17);
                  }

                  if (var12 >= var5) {
                     break;
                  }
               }
            }

            pushMenu(var2);
            if (var12 > 0) {
               KomutechSupport.send(var0, "§a已补充: " + var12 + "个物品");
               var0.playSound(var0.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5F, 1.0F);
            } else {
               KomutechSupport.send(var0, "§c背包无匹配物品");
               var0.playSound(var0.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5F, 0.5F);
            }
         }
      }
   }

   private static void pushMenu(Object var0) {
      try {
         var0.getClass().getMethod("push").invoke(var0);
      } catch (ReflectiveOperationException var2) {
      }
   }

   private static void updateLore(ItemStack var0, String var1) {
      if (var0 != null) {
         ItemMeta var2 = var0.getItemMeta();
         if (var2 != null) {
            ArrayList<String> var3 = var2.hasLore() ? new ArrayList<>(var2.getLore()) : new ArrayList<>();
            var3.removeIf(var0x -> var0x != null && var0x.contains("已复制机器:"));
            var3.add("§7已复制机器: §b" + var1);
            var2.setLore(var3);
            var0.setItemMeta(var2);
         }
      }
   }

   private record Clipboard(String machineId, String machineName, List<StorageClipboardScript.SlotItem> slots) {
   }

   private record FillSlot(int slot, ItemStack item, int need) {
   }

   private record SlotItem(int slot, ItemStack item, int amount) {
   }
}
