package tech.komutech.native_scripts.machine;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import tech.komutech.native_scripts.support.KomutechMenuHandler;
import tech.komutech.native_scripts.support.KomutechMenuRouter;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.MenuGuiHelper;
import tech.komutech.native_scripts.support.StructureMissingHint;
import tech.komutech.native_scripts.NativeLifecycleScript;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.MachineScriptHelper;

public final class UltimateSynthCoreScript implements NativeLifecycleScript, NativeScript, KomutechMenuHandler {
   private static final String[] DIRS = new String[]{"§f北", "§f东", "§f南", "§f西"};
   private static final Boolean BLOCK_CLICK = Boolean.FALSE;
   private final Set<Player> openPlayers = ConcurrentHashMap.newKeySet();
   private final Map<Player, Location> coreMap = new ConcurrentHashMap<>();

   @Override
   public void registerLifecycle(Plugin var1) {
      KomutechMenuRouter.registerHandler(this);
   }

   @Override
   public boolean handles(InventoryView var1) {
      return MenuGuiHelper.titleEquals(var1, "§6太极合成核心");
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("onUse".equals(var1)) {
         this.handleUse(var2[0]);
      } else if ("onBreak".equals(var1)) {
         this.handleBreak(var2[0]);
      } else if ("onOpen".equals(var1) && var2[0] instanceof Player var3) {
         this.openPlayers.add(var3);
      } else if ("onClick".equals(var1) && var2.length >= 2 && var2[0] instanceof Player var4) {
         int var9 = var2[1] instanceof Number var6 ? var6.intValue() : -1;
         if (this.handles(var4.getOpenInventory()) && var9 >= 0 && var9 < 9) {
            Location var10 = this.coreMap.get(var4);
            if (var10 == null) {
               return BLOCK_CLICK;
            }

            this.handleButton(var4, var10, var4.getOpenInventory().getTopInventory(), var9);
            return BLOCK_CLICK;
         }

         return BLOCK_CLICK;
      }

      return null;
   }

   private void handleUse(Object var1) {
      try {
         Player var2 = (Player)var1.getClass().getMethod("getPlayer").invoke(var1);
         Object var3 = var1.getClass().getMethod("getClickedBlock").invoke(var1);
         if (var3 == null) {
            return;
         }

         Object var4 = var3.getClass().getMethod("get").invoke(var3);
         Location var5 = ((Block)var4).getLocation();
         String var6 = MachineScriptHelper.getSfItem(var5) == null ? null : MachineScriptHelper.getSfItem(var5).getId();
         if (!"KOMUTECH_L_ZJ_終極合成台核心".equals(var6)) {
            return;
         }

         if (MachineScriptHelper.getData(var5, "KOMUTECH_L_ZJ_ZJHC_fx") == null) {
            MachineScriptHelper.setData(var5, "KOMUTECH_L_ZJ_ZJHC_fx", "0");
         }

         if (MachineScriptHelper.getData(var5, "KOMUTECH_L_ZJ_ZJHC_zt") == null) {
            MachineScriptHelper.setData(var5, "KOMUTECH_L_ZJ_ZJHC_zt", "0");
         }

         int var7 = UltimateSynthSupport.parseInt(MachineScriptHelper.getData(var5, "KOMUTECH_L_ZJ_ZJHC_zt"), 0);
         int var8 = UltimateSynthSupport.parseInt(MachineScriptHelper.getData(var5, "KOMUTECH_L_ZJ_ZJHC_fx"), 0);
         this.coreMap.put(var2, var5);
         this.openPlayers.add(var2);
         Inventory var9 = this.buildMenu(var7, var8, var5);
         KomutechMenuRouter.bindInventory(var9, this);
         var2.openInventory(var9);
      } catch (ReflectiveOperationException var10) {
      }
   }

   private void handleBreak(Object var1) {
      try {
         Location var2 = ((Block)var1.getClass().getMethod("getBlock").invoke(var1)).getLocation();
         String var3 = MachineScriptHelper.getSfItem(var2) == null ? null : MachineScriptHelper.getSfItem(var2).getId();
         if ("KOMUTECH_L_ZJ_終極合成台核心".equals(var3)) {
            MachineScriptHelper.setData(var2, "KOMUTECH_L_ZJ_ZJHC_zt", null);
            MachineScriptHelper.setData(var2, "KOMUTECH_L_ZJ_ZJHC_fx", null);
            MachineScriptHelper.setData(var2, "KOMUTECH_L_ZJ_ZJHC_ty", null);
            StructureMissingHint.clear(var2);
            UltimateSynthProjection.clearForCoreBreak(var2);
         } else if ("KOMUTECH_L_ZJ_終極合成台".equals(var3)) {
            Location var4 = var2.clone().add(0.0, -1.0, 0.0);
            MachineScriptHelper.setData(var4, "KOMUTECH_L_ZJ_ZJHC_zt", "0");
         }
      } catch (ReflectiveOperationException var5) {
      }
   }

   @Override
   public void onClick(InventoryClickEvent var1) {
      if (var1.getWhoClicked() instanceof Player var2) {
         if (this.handles(var1.getView())) {
            var1.setCancelled(true);
            this.openPlayers.add(var2);
            if (!isHotbarSwap(var1)) {
               int var7 = var1.getRawSlot();
               int var4 = var1.getView().getTopInventory().getSize();
               if (var7 >= 0 && var7 < var4) {
                  Location var5 = this.coreMap.get(var2);
                  if (var5 != null) {
                     ItemStack var6 = var1.getCurrentItem();
                     if (var6 != null && !var6.getType().isAir()) {
                        this.handleButton(var2, var5, var1.getInventory(), var7);
                     }
                  }
               }
            }
         }
      }
   }

   private void handleButton(Player var1, Location var2, Inventory var3, int var4) {
      int var5 = UltimateSynthSupport.parseInt(MachineScriptHelper.getData(var2, "KOMUTECH_L_ZJ_ZJHC_zt"), 0);
      int var6 = UltimateSynthSupport.parseInt(MachineScriptHelper.getData(var2, "KOMUTECH_L_ZJ_ZJHC_fx"), 0);
      if (var4 == 8) {
         var1.closeInventory();
      } else if (var4 == 0) {
         UltimateSynthProjection.toggleProjection(var2, var6);
         var3.setItem(0, this.projectionButton(var2));
      } else if (var4 != 1 && (var4 != 4 || var5 == 1)) {
         if (var4 == 2 && var5 == 1) {
            MachineScriptHelper.setData(var2, "KOMUTECH_L_ZJ_ZJHC_zt", "0");
            UltimateSynthProjection.removeProjection(var2);
            StructureMissingHint.clear(var2);
            KomutechSupport.send(var1, "§c已停止");
            this.refreshMenu(var3, var2);
         } else if (var4 == 3) {
            int var7 = (var6 + 1) % 4;
            MachineScriptHelper.setData(var2, "KOMUTECH_L_ZJ_ZJHC_fx", String.valueOf(var7));
            // 方向变了，之前按旧方向生成的缺失提示已不准确，先清掉
            StructureMissingHint.clear(var2);
            if (UltimateSynthProjection.hasProjection(var2)) {
               UltimateSynthProjection.spawnProjection(var2, var7);
            }

            this.refreshMenu(var3, var2);
         }
      } else {
         this.handleActivate(var1, var2, var3);
      }
   }

   private void handleActivate(Player var1, Location var2, Inventory var3) {
      if (UltimateSynthProjection.hasProjection(var2)) {
         UltimateSynthProjection.removeProjection(var2);
      }

      UltimateSynthSupport.StructureCheck var4 = UltimateSynthSupport.checkCoreStructure(var2);
      if (var4.valid()) {
         StructureMissingHint.clear(var2);
         MachineScriptHelper.setData(var2, "KOMUTECH_L_ZJ_ZJHC_zt", "1");
         KomutechSupport.send(var1, "§a激活成功！");
      } else {
         MachineScriptHelper.setData(var2, "KOMUTECH_L_ZJ_ZJHC_zt", "0");
         // 在每一处缺失位置生成红色发光展示实体，并在聊天栏给出世界坐标 + 方块中文名；
         // 玩家放对方块后 StructureMissingHint 会自动把对应提示清掉。
         StructureMissingHint.showMissing(var1, var2, UltimateSynthSupport.collectCoreMissing(var2));
      }

      this.refreshMenu(var3, var2);
   }

   private void refreshMenu(Inventory var1, Location var2) {
      int var3 = UltimateSynthSupport.parseInt(MachineScriptHelper.getData(var2, "KOMUTECH_L_ZJ_ZJHC_zt"), 0);
      int var4 = UltimateSynthSupport.parseInt(MachineScriptHelper.getData(var2, "KOMUTECH_L_ZJ_ZJHC_fx"), 0);
      var1.setItem(0, this.projectionButton(var2));
      var1.setItem(1, MenuGuiHelper.item(Material.EMERALD_BLOCK, "§a启动", List.of("§7验证并激活")));
      var1.setItem(3, MenuGuiHelper.item(Material.COMPASS, "§b方向: " + DIRS[var4 % 4], List.of("§7点击切换")));
      var1.setItem(8, MenuGuiHelper.item(Material.BARRIER, "§c关闭", List.of()));
      if (var3 == 1) {
         var1.setItem(2, MenuGuiHelper.item(Material.REDSTONE_BLOCK, "§c停止", List.of("§7停止机器")));
         var1.setItem(4, MenuGuiHelper.item(Material.PAINTING, "§a● 已激活", List.of("§7运行中")));
      } else {
         var1.setItem(2, null);
         var1.setItem(4, MenuGuiHelper.item(Material.PAINTING, "§c○ 未激活", List.of("§7点击启动检测激活")));
      }
   }

   @Override
   public void onDrag(InventoryDragEvent var1) {
      if (this.handles(var1.getView())) {
         var1.setCancelled(true);
      }
   }

   @Override
   public void onClose(InventoryCloseEvent var1) {
      if (var1.getPlayer() instanceof Player var2) {
         if (MenuGuiHelper.titleEquals(var1.getView(), "§6太极合成核心")) {
            this.coreMap.remove(var2);
            this.openPlayers.remove(var2);
         }
      }
   }

   private static boolean isHotbarSwap(InventoryClickEvent var0) {
      ClickType var1 = var0.getClick();
      InventoryAction var2 = var0.getAction();
      return var1 == ClickType.NUMBER_KEY
         || var1 == ClickType.SWAP_OFFHAND
         || var2 == InventoryAction.HOTBAR_MOVE_AND_READD
         || var2 == InventoryAction.HOTBAR_SWAP
         || var1 == ClickType.DOUBLE_CLICK;
   }

   private ItemStack projectionButton(Location var1) {
      return UltimateSynthProjection.hasProjection(var1)
         ? MenuGuiHelper.item(Material.ENDER_PEARL, "§a投影已开启", List.of("§7点击关闭"))
         : MenuGuiHelper.item(Material.ENDER_PEARL, "§b投影", List.of("§7开启/关闭"));
   }

   private Inventory buildMenu(int var1, int var2, Location var3) {
      Inventory var4 = MenuGuiHelper.create(9, "§6太极合成核心");
      this.refreshMenu(var4, var3);
      return var4;
   }
}
