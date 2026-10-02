package tech.komutech.native_scripts.machine;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import tech.komutech.native_scripts.support.KomutechMenuHandler;
import tech.komutech.native_scripts.support.KomutechMenuRouter;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.MachineMenuGuard;
import tech.komutech.native_scripts.NativeLifecycleScript;
import tech.komutech.native_scripts.NativeScript;

public final class SuPeiTaiScript implements NativeScript, NativeLifecycleScript, KomutechMenuHandler {
   private static final Map<Integer, String> RECIPE = Map.of(
      12,
      "KOMUTECH_L_DJ_JPLS",
      13,
      "KOMUTECH_L_DJ_蕴灵身",
      14,
      "KOMUTECH_L_DJ_JPLS",
      21,
      "KOMUTECH_L_JCWP_SPLNHX",
      22,
      "KOMUTECH_L_WD_WDS",
      23,
      "KOMUTECH_L_JCWP_SPLNHX",
      30,
      "KOMUTECH_L_DJ_JPLS",
      31,
      "KOMUTECH_L_JQ_FZCZQ",
      32,
      "KOMUTECH_L_DJ_JPLS"
   );
   private static final String OUTPUT_ITEM = "KOMUTECH_L_JQ_万相悟道仪";
   private static final int WORK_SLOT = 41;
   private static final int OUTPUT_SLOT = 49;
   private static final int[] RECIPE_SLOTS = new int[]{12, 13, 14, 21, 22, 23, 30, 31, 32};

   @Override
   public void registerLifecycle(Plugin var1) {
      KomutechMenuRouter.registerHandler(this);
   }

   @Override
   public boolean handles(InventoryView var1) {
      return MachineMenuGuard.isOwnMachine(var1, "KOMUTECH_L_WD_WXZZY") ? true : MachineMenuGuard.titleContainsAll(var1, "万相", "制造");
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("onOpen".equals(var1) && var2[0] instanceof Player var6) {
         Inventory var9 = var6.getOpenInventory().getTopInventory();
         if (this.handles(var6.getOpenInventory())) {
            KomutechMenuRouter.bindInventory(var9, this);
         }

         return null;
      } else if ("onClick".equals(var1) && var2.length >= 4 && var2[0] instanceof Player var3) {
         int var8 = (Integer)var2[1];
         Object var5 = var2[3];
         if (this.isAllowedSlot(var8)) {
            return null;
         } else {
            if (var8 == 41 && !KomutechSupport.isRightClick(var5) && !KomutechSupport.isShiftClick(var5)) {
               this.tryCraft(var3);
            }

            return Boolean.FALSE;
         }
      } else {
         return null;
      }
   }

   @Override
   public void onClick(InventoryClickEvent var1) {
      if (var1.getWhoClicked() instanceof Player var2) {
         if (this.handles(var1.getView())) {
            int var4 = var1.getRawSlot();
            MachineMenuGuard.protectTopInventory(var1, this::isAllowedSlot);
            if (var1.isCancelled() && var4 == 41 && !KomutechSupport.isRightClick(var1) && !KomutechSupport.isShiftClick(var1)) {
               this.tryCraft(var2);
            }
         }
      }
   }

   @Override
   public void onDrag(InventoryDragEvent var1) {
      if (this.handles(var1.getView())) {
         MachineMenuGuard.protectTopInventoryDrag(var1, this::isAllowedSlot);
      }
   }

   private boolean isAllowedSlot(int var1) {
      return MachineMenuGuard.isSlotIn(var1, RECIPE_SLOTS) || var1 == 49;
   }

   private void tryCraft(Player var1) {
      try {
         Inventory var2 = var1.getOpenInventory().getTopInventory();
         ItemStack var3 = var2.getItem(49);
         if (var3 != null && var3.getAmount() >= 64) {
            KomutechSupport.send(var1, "§c输出槽位已满！");
            return;
         }

         Iterator var4 = RECIPE.entrySet().iterator();

         while (true) {
            if (var4.hasNext()) {
               Entry var12 = (Entry)var4.next();
               ItemStack var13 = var2.getItem((Integer)var12.getKey());
               SlimefunItem var14 = var13 == null ? null : SlimefunItem.getByItem(var13);
               if (var14 != null && ((String)var12.getValue()).equals(var14.getId())) {
                  if ((Integer)var12.getKey() == 13 && !isItemBoundToPlayer(var13, var1)) {
                     KomutechSupport.send(var1, "§c大胆！何人竟敢行苟且之事！");
                     return;
                  }
                  continue;
               }

               KomutechSupport.send(var1, "§c材料不正确！");
               return;
            }

            SlimefunItem var9 = SlimefunItem.getById("KOMUTECH_L_JQ_万相悟道仪");
            if (var9 == null) {
               KomutechSupport.send(var1, "§c合成失败！");
               return;
            }

            if (var3 != null) {
               SlimefunItem var5 = SlimefunItem.getByItem(var3);
               if (var5 == null || !"KOMUTECH_L_JQ_万相悟道仪".equals(var5.getId())) {
                  KomutechSupport.send(var1, "§c输出槽位已有其他物品！");
                  return;
               }
            }

            for (Entry var6 : RECIPE.entrySet()) {
               if ((Integer)var6.getKey() != 13) {
                  ItemStack var7 = var2.getItem((Integer)var6.getKey());
                  if (var7.getAmount() == 1) {
                     var2.setItem((Integer)var6.getKey(), null);
                  } else {
                     var7.setAmount(var7.getAmount() - 1);
                  }
               }
            }

            ItemStack var11 = var9.getItem().clone();
            if (var3 == null) {
               var2.setItem(49, var11);
            } else {
               var3.setAmount(var3.getAmount() + 1);
            }

            KomutechSupport.send(var1, "§a合成成功！");
            break;
         }
      } catch (RuntimeException var8) {
         KomutechSupport.send(var1, "§c合成失败！");
      }
   }

   private static boolean isItemBoundToPlayer(ItemStack var0, Player var1) {
      if (var0 != null && var0.hasItemMeta()) {
         ItemMeta var2 = var0.getItemMeta();
         if (var2 != null && var2.hasLore()) {
            String var3 = "§b已绑定：§a" + var1.getName();

            for (String var5 : var2.getLore()) {
               if (var5 != null && var5.contains(var3)) {
                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }
}
