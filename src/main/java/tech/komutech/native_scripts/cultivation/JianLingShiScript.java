package tech.komutech.native_scripts.cultivation;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import tech.komutech.native_scripts.support.AttributeApplier;
import tech.komutech.native_scripts.support.CultivationMath;
import tech.komutech.native_scripts.support.KomutechAsyncScheduler;
import tech.komutech.native_scripts.support.KomutechMenuHandler;
import tech.komutech.native_scripts.support.KomutechMenuRouter;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.MenuGuiHelper;
import tech.komutech.native_scripts.support.PlayerAttributeStore;
import tech.komutech.native_scripts.support.StructureMissingHint;
import tech.komutech.native_scripts.NativeLifecycleScript;
import tech.komutech.native_scripts.support.MachineScriptHelper;

public final class JianLingShiScript implements NativeLifecycleScript, KomutechMenuHandler {
   private static final String[] DIRS = new String[]{"§f北", "§f东", "§f南", "§f西"};
   private static final long COOLDOWN_MS = 10000L;
   private final Set<Player> openPlayers = ConcurrentHashMap.newKeySet();
   private final Map<Player, Location> coreMap = new ConcurrentHashMap<>();
   private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
   private final Set<String> processingCores = ConcurrentHashMap.newKeySet();
   private Plugin plugin;

   @Override
   public void registerLifecycle(Plugin var1) {
      this.plugin = var1;
      KomutechMenuRouter.registerHandler(this);
   }

   @Override
   public boolean handles(InventoryView var1) {
      return MenuGuiHelper.titleEquals(var1, "§6鉴灵石核心");
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("onUse".equals(var1)) {
         this.handleUse(var2[0]);
      } else if ("onBreak".equals(var1)) {
         this.handleBreak(var2[0]);
      }

      return null;
   }

   private void handleUse(Object var1) {
      try {
         Object var2 = var1.getClass().getMethod("getClickedBlock").invoke(var1);
         if (var2 == null) {
            return;
         }

         Block var3 = (Block)var2.getClass().getMethod("get").invoke(var2);
         Player var4 = (Player)var1.getClass().getMethod("getPlayer").invoke(var1);
         SlimefunItem var5 = MachineScriptHelper.getSfItem(var3.getLocation());
         if (var5 == null) {
            return;
         }

         if ("KOMUTECH_L_X_鉴灵石核心".equals(var5.getId())) {
            this.openCoreMenu(var4, var3.getLocation());
         } else if (JianLingShiSupport.TRIGGER_IDS.contains(var5.getId())) {
            this.handleTrigger(var4, var3.getLocation());
         }
      } catch (ReflectiveOperationException var6) {
      }
   }

   private void openCoreMenu(Player var1, Location var2) {
      if (MachineScriptHelper.getData(var2, "KOMUTECH_L_X_JLS_fx") == null) {
         MachineScriptHelper.setData(var2, "KOMUTECH_L_X_JLS_fx", "0");
      }

      if (MachineScriptHelper.getData(var2, "KOMUTECH_L_X_JLS_zt") == null) {
         MachineScriptHelper.setData(var2, "KOMUTECH_L_X_JLS_zt", "0");
      }

      int var3 = parseInt(MachineScriptHelper.getData(var2, "KOMUTECH_L_X_JLS_zt"), 0);
      int var4 = parseInt(MachineScriptHelper.getData(var2, "KOMUTECH_L_X_JLS_fx"), 0);
      this.coreMap.put(var1, var2);
      this.openPlayers.add(var1);
      var1.openInventory(this.buildMenu(var3, var4, var2));
   }

   private void handleTrigger(Player var1, Location var2) {
      Map var3 = PlayerAttributeStore.load(var1.getName());
      if (var3 != null && !PlayerAttributeStore.getBoolean(var3, "定向", false)) {
         KomutechSupport.send(var1, "§c你已经鉴别过了");
      } else {
         String var4 = var1.getUniqueId().toString();
         long var5 = System.currentTimeMillis();
         Long var7 = this.cooldowns.get(var4);
         if (var7 != null && var5 - var7 < 10000L) {
            KomutechSupport.actionBar(var1, "§c操作过快");
         } else {
            this.cooldowns.put(var4, var5);
            String var8 = MachineScriptHelper.getData(var2, "KOMUTECH_L_X_JLS_core");
            if (var8 == null) {
               KomutechSupport.send(var1, "§c未找到核心标记，请先激活鉴灵石核心");
            } else {
               Location var9 = parseLocation(var8);
               if (var9 == null) {
                  KomutechSupport.send(var1, "§c核心坐标无效");
               } else if (!"1".equals(MachineScriptHelper.getData(var9, "KOMUTECH_L_X_JLS_zt"))) {
                  KomutechSupport.send(var1, "§c核心未激活");
               } else {
                  String var10 = JianLingShiSupport.locationKey(var9);
                  if (!this.processingCores.add(var10)) {
                     KomutechSupport.send(var1, "§c核心处理中");
                  } else {
                     int var11 = parseInt(MachineScriptHelper.getData(var9, "KOMUTECH_L_X_JLS_fx"), 0);
                     JianLingShiSupport.StructureCheck var12 = JianLingShiSupport.checkStructure(var9, var11);
                     if (!var12.valid()) {
                        KomutechSupport.send(var1, "§c结构异常");
                        MachineScriptHelper.setData(var9, "KOMUTECH_L_X_JLS_zt", "0");
                        this.processingCores.remove(var10);
                     } else {
                        Map var13 = var3 != null && PlayerAttributeStore.getBoolean(var3, "定向", false) ? var3 : null;
                        Map[] var14 = new Map[1];
                        KomutechAsyncScheduler.submit(
                           () -> var14[0] = JianLingShiSupport.rollAttributes(var13),
                           var6 -> {
                              Map var7x = var14[0];
                              if (var7x == null) {
                                 this.processingCores.remove(var10);
                              } else {
                                 String var8x = PlayerAttributeStore.getString(var7x, "灵根", "");
                                 if (var13 != null) {
                                    KomutechSupport.send(var1, "§d定向标记生效，灵根锁定为：" + var8x);
                                 }

                                 JianLingShiSupport.showHologram(this.plugin, var9, var1, var8x);
                                 KomutechSupport.send(var1, "§a开始检验");
                                 JianLingShiSupport.startEffect(
                                    this.plugin, var9, JianLingShiSupport.elementsFromData(var7x), JianLingShiSupport.qualitiesFromData(var7x), () -> {
                                       JianLingShiSupport.consumeClusters(var9, var1);
                                       CultivationMath.ensureDataComplete(var1.getName(), var7x);
                                       PlayerAttributeStore.save(var1.getName(), var7x);
                                       AttributeApplier.applyStoredAttributes(var1);
                                       KomutechSupport.send(var1, "§a鉴灵完成！灵根：" + var8x + "，总品质：" + PlayerAttributeStore.getString(var7x, "总品质", ""));
                                       this.processingCores.remove(var10);
                                    }
                                 );
                              }
                           }
                        );
                     }
                  }
               }
            }
         }
      }
   }

   private void handleBreak(Object var1) {
      try {
         Location var2 = ((Block)var1.getClass().getMethod("getBlock").invoke(var1)).getLocation();
         SlimefunItem var3 = MachineScriptHelper.getSfItem(var2);
         if (var3 == null || !"KOMUTECH_L_X_鉴灵石核心".equals(var3.getId())) {
            return;
         }

         int var4 = parseInt(MachineScriptHelper.getData(var2, "KOMUTECH_L_X_JLS_fx"), 0);
         MachineScriptHelper.setData(var2, "KOMUTECH_L_X_JLS_zt", null);
         MachineScriptHelper.setData(var2, "KOMUTECH_L_X_JLS_fx", null);
         JianLingShiSupport.clearTriggers(var2, var4);
         JianLingShiProjection.clearForCoreBreak(var2);
         StructureMissingHint.clear(var2);
         this.processingCores.remove(JianLingShiSupport.locationKey(var2));
      } catch (ReflectiveOperationException var5) {
      }
   }

   @Override
   public void onClick(InventoryClickEvent var1) {
      if (var1.getWhoClicked() instanceof Player var2 && this.openPlayers.contains(var2)) {
         if (MenuGuiHelper.titleEquals(var1.getView(), "§6鉴灵石核心")) {
            var1.setCancelled(true);
            Location var10 = this.coreMap.get(var2);
            if (var10 != null) {
               ItemStack var4 = var1.getCurrentItem();
               if (var4 != null && !var4.getType().isAir()) {
                  int var5 = var1.getSlot();
                  int var6 = parseInt(MachineScriptHelper.getData(var10, "KOMUTECH_L_X_JLS_zt"), 0);
                  int var7 = parseInt(MachineScriptHelper.getData(var10, "KOMUTECH_L_X_JLS_fx"), 0);
                  Inventory var8 = var1.getInventory();
                  if (var5 == 8) {
                     var2.closeInventory();
                  } else if (var5 == 0) {
                     JianLingShiProjection.toggleProjection(var10, var7);
                     var8.setItem(0, this.projectionButton(var10));
                  } else if (var5 == 1) {
                     JianLingShiSupport.StructureCheck var9 = JianLingShiSupport.checkStructure(var10, var7);
                     if (var9.valid()) {
                        StructureMissingHint.clear(var10);
                        MachineScriptHelper.setData(var10, "KOMUTECH_L_X_JLS_zt", "1");
                        JianLingShiSupport.markTriggers(var10, var7, JianLingShiSupport.locationKey(var10));
                        KomutechSupport.send(var2, "§a激活成功！");
                     } else {
                        MachineScriptHelper.setData(var10, "KOMUTECH_L_X_JLS_zt", "0");
                        // 缺什么、缺在哪：聊天栏给世界坐标 + 方块中文名，现场给红色发光展示实体
                        StructureMissingHint.showMissing(var2, var10, JianLingShiSupport.collectMissing(var10, var7));
                     }

                     var2.closeInventory();
                  } else if (var5 == 2 && var6 == 1) {
                     MachineScriptHelper.setData(var10, "KOMUTECH_L_X_JLS_zt", "0");
                     JianLingShiSupport.clearTriggers(var10, var7);
                     JianLingShiProjection.removeProjection(var10);
                     StructureMissingHint.clear(var10);
                     KomutechSupport.send(var2, "§c已停止");
                     var2.closeInventory();
                  } else if (var5 == 3) {
                     StructureMissingHint.clear(var10);
                     int var11 = (var7 + 1) % 4;
                     MachineScriptHelper.setData(var10, "KOMUTECH_L_X_JLS_fx", String.valueOf(var11));
                     var8.setItem(3, MenuGuiHelper.item(Material.COMPASS, "§b方向: " + DIRS[var11], List.of("§7点击切换")));
                     if (JianLingShiProjection.hasProjection(var10)) {
                        JianLingShiProjection.spawnProjection(var10, var11);
                        var8.setItem(0, this.projectionButton(var10));
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public void onDrag(InventoryDragEvent var1) {
      if (var1.getWhoClicked() instanceof Player var2 && this.openPlayers.contains(var2) && MenuGuiHelper.titleEquals(var1.getView(), "§6鉴灵石核心")) {
         var1.setCancelled(true);
      }
   }

   @Override
   public void onClose(InventoryCloseEvent var1) {
      if (var1.getPlayer() instanceof Player var2) {
         if (MenuGuiHelper.titleEquals(var1.getView(), "§6鉴灵石核心")) {
            this.coreMap.remove(var2);
            this.openPlayers.remove(var2);
         }
      }
   }

   private ItemStack projectionButton(Location var1) {
      return JianLingShiProjection.hasProjection(var1)
         ? MenuGuiHelper.item(Material.ENDER_PEARL, "§a投影已开启", List.of("§7点击关闭"))
         : MenuGuiHelper.item(Material.ENDER_PEARL, "§b投影", List.of("§7开启/关闭"));
   }

   private Inventory buildMenu(int var1, int var2, Location var3) {
      Inventory var4 = MenuGuiHelper.create(9, "§6鉴灵石核心");
      if (var1 == 1) {
         var4.setItem(4, MenuGuiHelper.item(Material.PAINTING, "§a● 已激活", List.of("§7运行中")));
         var4.setItem(2, MenuGuiHelper.item(Material.REDSTONE_BLOCK, "§c停止", List.of("§7停止机器")));
      } else {
         var4.setItem(4, MenuGuiHelper.item(Material.PAINTING, "§c○ 未激活", List.of("§7点击启动检测激活")));
      }

      var4.setItem(0, this.projectionButton(var3));
      var4.setItem(1, MenuGuiHelper.item(Material.EMERALD_BLOCK, "§a启动", List.of("§7验证并激活")));
      var4.setItem(3, MenuGuiHelper.item(Material.COMPASS, "§b方向: " + DIRS[var2 % 4], List.of("§7点击切换")));
      var4.setItem(8, MenuGuiHelper.item(Material.BARRIER, "§c关闭", List.of()));
      return var4;
   }

   private static Location parseLocation(String var0) {
      String[] var1 = var0.split(",");
      if (var1.length != 4) {
         return null;
      } else {
         World var2 = Bukkit.getWorld(var1[0]);
         return var2 == null ? null : new Location(var2, Integer.parseInt(var1[1]), Integer.parseInt(var1[2]), Integer.parseInt(var1[3]));
      }
   }

   private static int parseInt(String var0, int var1) {
      if (var0 == null) {
         return var1;
      } else {
         try {
            return Integer.parseInt(var0);
         } catch (NumberFormatException var3) {
            return var1;
         }
      }
   }
}
