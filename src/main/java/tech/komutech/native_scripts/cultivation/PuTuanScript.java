package tech.komutech.native_scripts.cultivation;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import tech.komutech.native_scripts.support.AttributeApplier;
import tech.komutech.native_scripts.support.AttributePointLimits;
import tech.komutech.native_scripts.support.CultivationMath;
import tech.komutech.native_scripts.support.DisplayReflectionHelper;
import tech.komutech.native_scripts.support.KomutechJson;
import tech.komutech.native_scripts.support.KomutechMenuHandler;
import tech.komutech.native_scripts.support.KomutechMenuRouter;
import tech.komutech.native_scripts.support.KomutechPaths;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.MenuGuiHelper;
import tech.komutech.native_scripts.support.PlayerAttributeStore;
import tech.komutech.native_scripts.NativeLifecycleScript;
import tech.komutech.native_scripts.support.MachineScriptHelper;

public final class PuTuanScript implements NativeLifecycleScript, KomutechMenuHandler {
   private static final String GUI_TITLE = "§d修炼蒲团";
   private static final String STATE_KEY = "meditation_state";
   private static final String PLAYER_UUID_KEY = "meditation_player_uuid";
   static final String HOLO_UUID_KEY = "meditation_holo_uuid";
   private static final String PRAYER_MAT_KEY = "prayer_mat_id";
   private static final String PROJ_UUID_KEY = "meditation_proj_uuid";
   private static final int BREAK_DURATION_TICKS = 300;
   private static final long MONITOR_INTERVAL_TICKS = 20L;
   private static final int SPIRIT_SETTLE_EVERY_N_TICKS = 3;
   /**
    * "玩家还在不在蒲团上 / 座位要不要维护"这类检测，每多少轮监控做一次。
    * 蒲团本身被破坏已由原版方块事件（BlockBreak/Explode/Piston/Burn/Fade）即时处理，
    * 这里的轮询只兜底"玩家被传送、掉出世界、被插件挪走"等事件覆盖不到的场景，
    * 所以不需要每秒都做：6 轮 × 20 tick = 每 6 秒一次，开销降到原来的 1/6。
    */
   private static final int SEAT_CHECK_EVERY_N_TICKS = 6;
   private static volatile Map<String, Object> cachedPuTuanConfig = Map.of();
   private static volatile long cachedPuTuanConfigAt = 0L;
   private static final long PU_TUAN_CONFIG_CACHE_MS = 60000L;
   private static final Map<String, Double> DEFAULT_BASE_GAIN = Map.of(
      "KOMUTECH_L_X_蒲团", 1.0, "KOMUTECH_L_X_玉坐", 3.0, "KOMUTECH_L_X_莲台", 9.0, "KOMUTECH_L_X_云榻", 16.0, "KOMUTECH_L_X_空蒲", 32.0
   );
   private static final Map<String, Long> DEFAULT_MIN_SPIRIT = Map.of("KOMUTECH_L_X_莲台", 1000L, "KOMUTECH_L_X_云榻", 10000L, "KOMUTECH_L_X_空蒲", 1000000L);
   private static final Map<Long, Integer> TRIBULATION_THRESHOLDS = Map.of(
      100000L, 9, 1000000L, 16, 10000000L, 21, 100000000L, 36, 1000000000L, 49, 10000000000L, 81, 100000000000L, 99
   );
   private final Map<Player, Location> openPlayers = new HashMap<>();
   private final Map<UUID, Location> playerActiveMat = new HashMap<>();
   private final Map<UUID, Map<String, Object>> sessionData = new HashMap<>();
   private final Set<Location> monitorLocs = new HashSet<>();
   private final Map<String, Integer> breakTasks = new HashMap<>();
   private final Map<String, Integer> tribulationTasks = new HashMap<>();
   private final Map<UUID, Integer> monitorSpiritTicks = new HashMap<>();
   private int seatCheckCounter = 0;
   private BukkitTask monitorTask;
   private Plugin plugin;

   @Override
   public void registerLifecycle(final Plugin var1) {
      this.plugin = var1;
      KomutechMenuRouter.registerHandler(this);
      var1.getServer().getPluginManager().registerEvents(new Listener() {
         @EventHandler
         public void onQuit(PlayerQuitEvent var1x) {
            PuTuanScript.this.handlePlayerQuit(var1x.getPlayer());
         }

         @EventHandler
         public void onSneak(PlayerToggleSneakEvent var1x) {
            PuTuanScript.this.handlePlayerSneak(var1x.getPlayer());
         }

         // 骑乘状态下按 Shift，客户端走的是"下车"流程，不会触发 PlayerToggleSneakEvent
         // （潜行状态在载具上不切换）。
         //
         // 注意不能用 VehicleExitEvent：ArmorStand 只实现 LivingEntity，**不是** org.bukkit.entity.Vehicle，
         // 而 VehicleExitEvent 的构造签名强制要求 Vehicle 参数（CraftBukkit 只在 CraftVehicle 里触发它），
         // 所以监听盔甲架座位时该事件永远不会被触发 —— 这就是之前"按 Shift 没反应"的真正原因。
         // 正确事件是 EntityDismountEvent：它对任意实体生效，且不要求实现 Vehicle。
         @EventHandler
         public void onDismount(EntityDismountEvent var1x) {
            if (var1x.getEntity() instanceof Player) {
               PuTuanScript.this.handlePlayerDismount((Player)var1x.getEntity(), var1x.getDismounted());
            }
         }

         // —— 修炼台方块本身的变化：改由原版方块事件驱动，不再靠每秒轮询去问"蒲团还在不在" ——

         @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
         public void onBlockBreak(BlockBreakEvent var1x) {
            PuTuanScript.this.onMatChanged(var1x.getBlock().getLocation());
         }

         @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
         public void onBlockBurn(BlockBurnEvent var1x) {
            PuTuanScript.this.onMatChanged(var1x.getBlock().getLocation());
         }

         @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
         public void onBlockFade(BlockFadeEvent var1x) {
            PuTuanScript.this.onMatChanged(var1x.getBlock().getLocation());
         }

         @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
         public void onBlockExplode(BlockExplodeEvent var1x) {
            for (Block var3 : var1x.blockList()) {
               PuTuanScript.this.onMatChanged(var3.getLocation());
            }
         }

         @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
         public void onEntityExplode(EntityExplodeEvent var1x) {
            for (Block var3 : var1x.blockList()) {
               PuTuanScript.this.onMatChanged(var3.getLocation());
            }
         }

         @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
         public void onPistonExtend(BlockPistonExtendEvent var1x) {
            for (Block var3 : var1x.getBlocks()) {
               PuTuanScript.this.onMatChanged(var3.getLocation());
            }
         }

         @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
         public void onPistonRetract(BlockPistonRetractEvent var1x) {
            for (Block var3 : var1x.getBlocks()) {
               PuTuanScript.this.onMatChanged(var3.getLocation());
            }
         }

         @EventHandler
         public void onChunkLoad(ChunkLoadEvent var1x) {
            PuTuanScript.this.sweepChunkOrphans(var1x.getChunk());
         }

         @EventHandler
         public void onPluginDisable(PluginDisableEvent var1x) {
            if (var1x.getPlugin() == var1) {
               for (Location var3 : new HashSet<>(PuTuanScript.this.monitorLocs)) {
                  PuTuanScript.this.forceRelease(var3);
               }

               PuTuanScript.this.monitorLocs.clear();
               PuTuanScript.this.playerActiveMat.clear();
               PuTuanScript.this.sessionData.clear();
               if (PuTuanScript.this.monitorTask != null) {
                  PuTuanScript.this.monitorTask.cancel();
                  PuTuanScript.this.monitorTask = null;
               }
            }
         }
      }, var1);
      Bukkit.getScheduler().runTaskLater(var1, this::sweepAllLoadedOrphans, 40L);
      Bukkit.getScheduler().runTaskLater(var1, this::sweepAllLoadedOrphans, 300L);
   }

   private void sweepChunkOrphans(Chunk var1) {
      PuTuanDisplayHelper.sweepChunk(var1, (var1x, var2) -> {
         Location var3 = PuTuanDisplayHelper.parseLocationKey(var2);
         return var3 == null ? true : this.shouldPurgeTaggedDisplay(var3, var1x.getUniqueId());
      });
   }

   private void sweepAllLoadedOrphans() {
      int var1 = PuTuanDisplayHelper.sweepLoadedWorlds((var1x, var2) -> {
         Location var3 = PuTuanDisplayHelper.parseLocationKey(var2);
         return var3 == null ? true : this.shouldPurgeTaggedDisplay(var3, var1x.getUniqueId());
      });
      if (var1 > 0 && this.plugin != null) {
         this.plugin.getLogger().info("[蒲团] 已清理崩溃/残留修炼显示实体: " + var1);
      }
   }

   private boolean shouldPurgeTaggedDisplay(Location var1, UUID var2) {
      int var3 = getState(var1);
      String var4 = MachineScriptHelper.getData(var1, "meditation_holo_uuid");
      String var5 = MachineScriptHelper.getData(var1, "meditation_proj_uuid");
      String var6 = var2.toString();
      boolean var7 = var6.equals(var4) || var6.equals(var5);
      if (var3 != 0) {
         String var12 = MachineScriptHelper.getData(var1, "meditation_player_uuid");
         if (var12 == null) {
            this.forceRelease(var1);
            return true;
         } else {
            Player var13;
            try {
               var13 = Bukkit.getPlayer(UUID.fromString(var12));
            } catch (IllegalArgumentException var11) {
               this.forceRelease(var1);
               return true;
            }

            if (var13 == null || !var13.isOnline()) {
               this.forceRelease(var1);
               return true;
            } else if (!var7) {
               return true;
            } else if (!isOnMat(var13, var1)) {
               this.forceRelease(var1);
               return true;
            } else {
               return false;
            }
         }
      } else if (!var7) {
         return true;
      } else {
         if ((var1.getWorld() == null ? null : var1.getWorld().getEntity(var2)) instanceof TextDisplay var9) {
            String var10 = plainText(var9);
            if (var10.contains("修炼中") || var10.contains("突破中")) {
               MachineScriptHelper.removeData(var1, "meditation_holo_uuid");
               return true;
            }
         }

         return false;
      }
   }

   private static String plainText(TextDisplay var0) {
      try {
         String var1 = var0.getText();
         return var1 == null ? "" : var1.toString();
      } catch (Throwable var2) {
         return "";
      }
   }

   private void handlePlayerQuit(Player var1) {
      this.openPlayers.remove(var1);
      this.sessionData.remove(var1.getUniqueId());
      this.monitorSpiritTicks.remove(var1.getUniqueId());
      Location var2 = this.playerActiveMat.remove(var1.getUniqueId());
      if (var2 != null) {
         this.endSession(var2, var1, null);
         this.removeMonitorLoc(var2);
      }
   }

   /**
    * 按下 Shift = 玩家想起身：立即结束修炼。
    * 之前的行为是"Shift 让玩家脱离座位 → maintain 每秒发现没座位又把人重新 sit 回去"，
    * 表现为"被蒲团吸回去继续修炼"。现在 Shift 一按直接结束。
    */
   private void handlePlayerSneak(Player var1) {
      if (!var1.isSneaking()) {
         return;
      }

      Location var2 = this.playerActiveMat.get(var1.getUniqueId());
      if (var2 == null) {
         return;
      }

      int var3 = getState(var2);
      if (var3 != 1 && var3 != 2) {
         return;
      }

      this.endSession(var2, var1, "§e你已起身，修炼中断。");
   }

   /**
    * 玩家从修炼座位上下来（EntityDismountEvent，通常是按 Shift 下车）→ 立即结束修炼。
    * 这是比 PlayerToggleSneakEvent 可靠的"想起身"信号：骑乘时按 Shift 不触发潜行事件。
    * endSession → cleanup 会把 playerActiveMat 移除并 release 玩家，重复触发是安全的（幂等）。
    */
   private void handlePlayerDismount(Player var1, Entity var2) {
      if (var2 == null || !PuTuanSitHelper.isTaggedSeat(var2)) {
         return;
      }

      // 无论这次下车最终有没有结束到会话，都先打上"主动下车"标记：
      // 冷却期内 maintain() 不会再把这个玩家按回座位，彻底杜绝"被蒲团吸回去"。
      PuTuanSitHelper.markDismount(var1);
      Location var3 = this.playerActiveMat.get(var1.getUniqueId());
      if (var3 == null) {
         return;
      }

      int var4 = getState(var3);
      if (var4 != 1 && var4 != 2) {
         return;
      }

      this.endSession(var3, var1, "§e你已起身，修炼中断。");
   }

   /**
    * 修炼台方块本身发生了变化（被破坏 / 烧毁 / 消退 / 炸掉 / 被活塞推走）→ 立刻结束该处的修炼。
    * 有了这些原版方块事件，"蒲团还在不在"就不需要每秒轮询了，轮询只留作兜底并已大幅降频。
    */
   private void onMatChanged(Location var1) {
      if (var1 == null) {
         return;
      }

      for (Entry<UUID, Location> var3 : new HashSet<>(this.playerActiveMat.entrySet())) {
         if (sameBlock(var3.getValue(), var1)) {
            this.endSession(var3.getValue(), Bukkit.getPlayer(var3.getKey()), "§c修炼台已被破坏，修炼中断。");
         }
      }

      this.monitorLocs.removeIf(var2 -> sameBlock(var2, var1));
   }

   @Override
   public boolean handles(InventoryView var1) {
      return "§d修炼蒲团".equals(var1.getTitle());
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("onUse".equals(var1)) {
         try {
            Object var3 = var2[0];
            Player var4 = (Player)var3.getClass().getMethod("getPlayer").invoke(var3);
            Object var5 = var3.getClass().getMethod("getClickedBlock").invoke(var3);
            if (var5 == null) {
               return null;
            }

            Object var6 = var5.getClass().getMethod("get").invoke(var5);
            Location var7 = ((Block)var6).getLocation();
            this.openMenu(var4, var7);
         } catch (ReflectiveOperationException var9) {
         }
      } else if ("onBreak".equals(var1)) {
         try {
            Object var10 = var2[0];
            Location var11 = ((Block)var10.getClass().getMethod("getBlock").invoke(var10)).getLocation();
            this.cancelBreakTasks(var11);
            this.cleanup(var11, null);
            setState(var11, 0);
         } catch (ReflectiveOperationException var8) {
         }
      }

      return null;
   }

   @Override
   public void onClick(InventoryClickEvent var1) {
      if (var1.getWhoClicked() instanceof Player var2) {
         if ("§d修炼蒲团".equals(var1.getView().getTitle())) {
            var1.setCancelled(true);
            Location var6 = this.openPlayers.get(var2);
            if (var6 == null) {
               var2.closeInventory();
            } else {
               int var4 = var1.getSlot();
               int var5 = getState(var6);
               if (var4 == 3) {
                  if (var5 == 0) {
                     this.startMeditation(var2, var6);
                  } else if (var5 == 1) {
                     this.stopMeditation(var2, var6);
                  } else {
                     KomutechSupport.send(var2, "§c请先停止突破。");
                  }

                  var2.closeInventory();
               } else if (var4 == 5) {
                  if (var5 == 0) {
                     this.startBreakthrough(var2, var6);
                  } else if (var5 == 2) {
                     this.cancelBreakTasks(var6);
                     this.stopBreakthrough(var2, var6);
                  } else {
                     KomutechSupport.send(var2, "§c请先停止修炼。");
                  }

                  var2.closeInventory();
               } else if (var4 == 8) {
                  this.cancelBreakTasks(var6);
                  this.cleanup(var6, var2);
                  setState(var6, 0);
                  this.removeMonitorLoc(var6);
                  var2.closeInventory();
               }
            }
         }
      }
   }

   @Override
   public void onClose(InventoryCloseEvent var1) {
      if (var1.getPlayer() instanceof Player var2) {
         this.openPlayers.remove(var2);
      }
   }

   private void openMenu(Player var1, Location var2) {
      this.reconcileStaleSession(var2);
      if (checkMatSpiritRequirement(var1, var2)) {
         Inventory var3 = MenuGuiHelper.create(9, "§d修炼蒲团");
         int var4 = getState(var2);
         String var5 = var4 == 0 ? "§7○ 空闲" : (var4 == 1 ? "§a● 修炼中" : "§d● 突破中");
         var3.setItem(4, MenuGuiHelper.item(Material.BOOK, var5, List.of("§7当前状态")));
         var3.setItem(3, MenuGuiHelper.item(Material.LIME_DYE, var4 == 1 ? "§e停止修炼" : "§a开始修炼", List.of("§7点击开始/停止修炼")));
         var3.setItem(5, MenuGuiHelper.item(Material.REDSTONE, var4 == 2 ? "§e停止突破" : "§c开始突破", List.of("§7灵气满时可尝试突破")));
         var3.setItem(8, MenuGuiHelper.item(Material.BARRIER, "§c强制停止", List.of()));
         this.openPlayers.put(var1, var2);
         var1.openInventory(var3);
      }
   }

   private void startMeditation(Player var1, Location var2) {
      if (checkMatSpiritRequirement(var1, var2)) {
         Map var3 = PlayerAttributeStore.load(var1.getName());
         if (var3 == null) {
            KomutechSupport.send(var1, "§c无法读取玩家数据");
         } else if (this.tryBeginSession(var1, var2, 1, var3)) {
            KomutechSupport.send(var1, "§a你开始修炼了。");
         }
      }
   }

   private void stopMeditation(Player var1, Location var2) {
      this.cleanup(var2, var1);
      setState(var2, 0);
      this.removeMonitorLoc(var2);
      KomutechSupport.send(var1, "§e你停止了修炼。");
   }

   private void startBreakthrough(Player var1, Location var2) {
      if (checkMatSpiritRequirement(var1, var2)) {
         Map var3 = PlayerAttributeStore.load(var1.getName());
         if (var3 == null) {
            KomutechSupport.send(var1, "§c无法读取玩家数据");
         } else {
            PlayerAttributeStore.Lingli var4 = PlayerAttributeStore.parseLingli(var3.get("灵气"));
            if (var4.isUnlimitedMax()) {
               KomutechSupport.send(var1, "§c仙人境界灵气无上限，无法通过蒲团突破");
            } else if (var4.current() < var4.max()) {
               KomutechSupport.send(var1, "§c灵气未满，无法突破");
            } else if (this.tryBeginSession(var1, var2, 2, var3)) {
               KomutechSupport.send(var1, "§a你开始突破了！");
               this.startBreakSequence(var2, var1, var3);
            }
         }
      }
   }

   private void startBreakSequence(final Location var1, final Player var2, final Map<String, Object> var3) {
      this.cancelBreakTasks(var1);
      Map var4 = PlayerAttributeStore.getMap(var3, "根基");
      int var5 = PlayerAttributeStore.getInt(var4, "稳固值", 0);
      int var6 = PlayerAttributeStore.getInt(var4, "破镜药力", 0);
      int var7 = Math.min(25 + var5 + var6, 100);
      final boolean var8 = ThreadLocalRandom.current().nextInt(100) < var7;
      KomutechSupport.send(var2, "§6突破中... 成功率: §e" + var7 + "%");
      final String var9 = locationKey(var1);
      int var10 = Bukkit.getScheduler().runTaskTimer(this.plugin, new Runnable() {
         int tick = 0;

         @Override
         public void run() {
            this.tick++;
            if (this.tick >= 15) {
               Integer var1x = PuTuanScript.this.breakTasks.remove(var9);
               if (var1x != null) {
                  Bukkit.getScheduler().cancelTask(var1x);
               }

               PuTuanScript.this.finishBreakthrough(var1, var2, var3, var8);
            }
         }
      }, 0L, 20L).getTaskId();
      this.breakTasks.put(var9, var10);
   }

   private void stopBreakthrough(Player var1, Location var2) {
      this.cleanup(var2, var1);
      setState(var2, 0);
      this.removeMonitorLoc(var2);
      KomutechSupport.send(var1, "§e你停止了突破。");
   }

   private void finishBreakthrough(Location var1, Player var2, Map<String, Object> var3, boolean var4) {
      if (!var4) {
         this.failBreakthrough(var1, var2, var3);
      } else {
         PlayerAttributeStore.Lingli var5 = PlayerAttributeStore.parseLingli(var3.get("灵气"));
         double var6 = var5.max() * 10.0;
         if (var6 > 1.0E14) {
            var6 = 1.0E14;
         }

         int var8 = tribulationCount(var6);
         if (var8 > 0) {
            KomutechSupport.send(var2, "§c§l雷劫降临！需承受 " + var8 + " 道天雷！");
            this.startTribulation(var1, var2, var3, var6, var8, 1, var5.current());
         } else {
            this.applyBreakSuccess(var1, var2, var3, var6);
         }
      }
   }

   private void failBreakthrough(Location var1, Player var2, Map<String, Object> var3) {
      PlayerAttributeStore.Lingli var4 = PlayerAttributeStore.parseLingli(var3.get("灵气"));
      Map var5 = PlayerAttributeStore.getMap(var3, "根基");
      double var6 = 0.15 + ThreadLocalRandom.current().nextDouble(0.15);
      double var8 = PlayerAttributeStore.getInt(var5, "保阶药力", 0) / 100.0;
      double var10 = Math.max(0.0, var6 - var8);
      double var12 = var4.current() * (1.0 - var10);
      var3.put("灵气", PlayerAttributeStore.formatSpiritAmount(var12, var4.max()));
      double var14 = Math.floor(PlayerAttributeStore.getDouble(var3, "根骨", 1.0) / 10.0 * var4.max() * 100.0) / 100.0;
      var5.put("溢出的灵气", String.format("0.00/%.2f", var14));
      var5.put("稳固值", 0);
      var5.put("破镜药力", 0);
      var5.put("保阶药力", 0);
      var3.put("根基", var5);
      var3.put("突破失败", true);
      PlayerAttributeStore.save(var2.getName(), var3);
      PuTuanParticles.breakthroughFailure(var2);
      KomutechSupport.send(var2, "§c突破失败... 灵气损失了 " + (int)(var10 * 100.0) + "%");
      this.cleanup(var1, var2);
      setState(var1, 0);
      this.removeMonitorLoc(var1);
   }

   private void startTribulation(Location var1, Player var2, Map<String, Object> var3, double var4, int var6, int var7, double var8) {
      if (!var2.isOnline() || var2.isDead()) {
         KomutechSupport.send(var2, "§c渡劫失败...");
         this.failTribulation(var1, var2, var3);
      } else if (var7 > var6) {
         KomutechSupport.send(var2, "§a§l雷劫渡过！突破成功！");
         this.applyBreakSuccess(var1, var2, var3, var4);
      } else {
         KomutechSupport.send(var2, "§e第 " + var7 + "/" + var6 + " 道雷劫");
         PuTuanParticles.tribulationStrike(var2);
         int var10 = PlayerAttributeStore.getInt(var3, "功德", 0);
         double var11 = this.calculateTribulationDamage(var2, var3, var7);
         int var13 = PlayerAttributeStore.getInt(var3, "功德", 0);
         int var14 = var10 - var13;
         var2.damage(var11);
         KomutechSupport.send(var2, "§c受到了 " + (int)var11 + " 点雷劫伤害");
         if (var14 > 0) {
            KomutechSupport.send(var2, "§a功德护体：消耗 §6" + var14 + " §a点功德，减免雷劫伤害");
         }

         if (var2.isDead()) {
            KomutechSupport.send(var2, "§c你未能承受雷劫...");
            this.failTribulation(var1, var2, var3);
         } else {
            PlayerAttributeStore.save(var2.getName(), var3);
            String var15 = locationKey(var1) + "_trib";
            int var16 = Bukkit.getScheduler()
               .runTaskLater(this.plugin, () -> this.startTribulation(var1, var2, var3, var4, var6, var7 + 1, var8), 60L)
               .getTaskId();
            this.tribulationTasks.put(var15, var16);
         }
      }
   }

   private double calculateTribulationDamage(Player var1, Map<String, Object> var2, int var3) {
      double var4 = PlayerAttributeStore.getInt(var2, "血量", 0) * 3.0;
      double var6 = CultivationMath.getRealmCoefficient(var2);
      List var8 = CultivationMath.splitLinggen(PlayerAttributeStore.getString(var2, "灵根", ""));
      double var9 = PlayerAttributeStore.getDouble(var2, "总品质", 0.5);
      double var11 = CultivationMath.getLinggenFinalCoefficient(var8, var9, "灵力");
      double var13 = 1.0 + PlayerAttributeStore.getInt(var2, "煞气", 0) / 10000.0;
      double var15 = Math.min(PlayerAttributeStore.getInt(var2, "根骨", 0) * 0.03, 0.8);
      double var17 = Math.pow(1.05, var3 - 1);
      double var19 = var4 * var6 * var11 * var13 * (1.0 - var15) * var17;
      double var21 = PlayerAttributeStore.getDouble(var2, "防御力_实际", 0.0) * PlayerAttributeStore.getInt(var2, "根骨", 0);
      var19 = Math.max(1.0, Math.floor(var19 - var21));
      int var23 = (int)Math.floor(PlayerAttributeStore.getInt(var2, "功德", 0) / 100.0);
      int var24 = (int)Math.min(var19, (double)var23);
      var2.put("功德", PlayerAttributeStore.getInt(var2, "功德", 0) - var24 * 100);
      return Math.max(1.0, Math.floor(var19 - var24));
   }

   private void failTribulation(Location var1, Player var2, Map<String, Object> var3) {
      this.failBreakthrough(var1, var2, var3);
   }

   private void applyBreakSuccess(Location var1, Player var2, Map<String, Object> var3, double var4) {
      double var6 = var4 / 10.0 + 1.0;
      if (var6 > var4) {
         var6 = var4;
      }

      var3.put("灵气", PlayerAttributeStore.formatSpiritAmount(var6, var4));
      var3.put("修为", CultivationMath.getCultivationInfo((long)Math.floor(var6)).stage());
      CultivationMath.ensureLingliFormat(var3);
      Map var8 = PlayerAttributeStore.getMap(var3, "根基");
      double var9 = Math.floor(PlayerAttributeStore.getDouble(var3, "根骨", 1.0) / 10.0 * var4 * 100.0) / 100.0;
      var3.put("突破失败", false);
      boolean var11 = PlayerAttributeStore.getInt(var8, "破镜药力", 0) > 0 || PlayerAttributeStore.getInt(var8, "保阶药力", 0) > 0;
      if (var11) {
         var3.put("属性点", PlayerAttributeStore.getInt(var3, "属性点", 0) + ThreadLocalRandom.current().nextInt(4, 8));
      } else {
         var3.put("属性点", PlayerAttributeStore.getInt(var3, "属性点", 0) + 6);
      }

      var8.put("稳固值", 0);
      var8.put("溢出的灵气", String.format("0.00/%.2f", var9));
      var8.put("破镜药力", 0);
      var8.put("保阶药力", 0);
      var3.put("根基", var8);
      var2.playSound(var2.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);
      PuTuanParticles.breakthroughSuccess(var2);
      KomutechSupport.send(var2, "§a§l恭喜！突破成功！修为提升至 " + PlayerAttributeStore.getString(var3, "修为", "") + "，获得属性点奖励！");
      AttributeApplier.applyPlayerAttributes(var2, var3, false);
      PlayerAttributeStore.save(var2.getName(), var3);
      this.cleanup(var1, var2);
      setState(var1, 0);
      this.removeMonitorLoc(var1);
   }

   private void ensureMonitor() {
      if (this.monitorTask == null) {
         this.monitorTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            if (this.monitorLocs.isEmpty()) {
               if (this.monitorTask != null) {
                  this.monitorTask.cancel();
                  this.monitorTask = null;
               }
            } else {
               // 灵力结算 / 粒子仍按每 20 tick 跑；"在不在蒲团上 + 座位维护"降频到每 6 秒一次。
               boolean var1 = ++this.seatCheckCounter % SEAT_CHECK_EVERY_N_TICKS == 0;
               this.monitorLocs.removeIf(var2 -> this.processMonitorTick(var2, var1));
            }
         }, 0L, MONITOR_INTERVAL_TICKS);
      }
   }

   /**
    * @param var1 修炼台位置
    * @param var2 本轮是否做"在不在蒲团上 + 座位维护"的兜底检测（已降频，见 SEAT_CHECK_EVERY_N_TICKS）
    */
   private boolean processMonitorTick(Location var1, boolean var2) {
      int var3 = getState(var1);
      if (var3 != 1 && var3 != 2) {
         return true;
      }

      String var4 = MachineScriptHelper.getData(var1, "meditation_player_uuid");
      if (var4 == null) {
         setState(var1, 0);
         return true;
      }

      Player var5;
      try {
         var5 = Bukkit.getPlayer(UUID.fromString(var4));
      } catch (IllegalArgumentException var10) {
         this.endSession(var1, null, "§e修炼中断。");
         return true;
      }

      if (var5 == null || !var5.isOnline()) {
         this.endSession(var1, null, null);
         return true;
      }

      // 兜底检测（已降频到每 6 秒一次）：玩家是否还登记在这座修炼台上、是否还坐在蒲团上、座位要不要维护。
      // 蒲团本身被破坏已由原版方块事件即时处理，这里只覆盖"被传送 / 掉出世界"等事件看不到的情况。
      if (var2) {
         Location var6 = this.playerActiveMat.get(var5.getUniqueId());
         if (var6 == null || !sameBlock(var6, var1)) {
            this.endSession(var1, var5, "§e你已离开修炼台，修炼中断。");
            return true;
         }

         if (!isOnMat(var5, var1)) {
            this.endSession(var1, var5, "§e你已离开修炼台，修炼中断。");
            return true;
         }

         maintainSit(var5, var1);
      }

      // 灵力 / 突破进度结算：业务推进，保持每轮都跑（不受降频影响）。
      if (var3 == 1) {
         PuTuanParticles.meditation(var5);
         int var7 = this.monitorSpiritTicks.merge(var5.getUniqueId(), 1, Integer::sum);
         if (var7 % SPIRIT_SETTLE_EVERY_N_TICKS != 0) {
            return false;
         }

         Map var8 = this.sessionData.get(var5.getUniqueId());
         if (var8 == null) {
            var8 = PlayerAttributeStore.load(var5.getName());
            if (var8 != null) {
               this.sessionData.put(var5.getUniqueId(), var8);
            }
         }

         if (var8 != null) {
            boolean var9 = this.addSpirit(var5, var1, var8);
            if (var9) {
               KomutechSupport.send(var5, "§6灵气与溢出的灵气均已满，修炼自动结束。");
               this.endSession(var1, var5, null);
               return true;
            }
         }
      } else if (var3 == 2) {
         PuTuanParticles.breakthroughProgress(var5);
      }

      return false;
   }

   private void endSession(Location var1, Player var2, String var3) {
      this.cancelBreakTasks(var1);
      this.cleanup(var1, var2);
      setState(var1, 0);
      PuTuanDisplayHelper.updateHologram(var1, statusText(0));
      if (var2 != null && var3 != null) {
         KomutechSupport.send(var2, var3);
      }
   }

   private boolean addSpirit(Player var1, Location var2, Map<String, Object> var3) {
      Map var4 = PlayerAttributeStore.load(var1.getName());
      if (var4 != null) {
         var3 = var4;
         this.sessionData.put(var1.getUniqueId(), var4);
      }

      String var5 = PlayerAttributeStore.getString(var3, "修为", "");
      PlayerAttributeStore.Lingli var6 = PlayerAttributeStore.parseLingli(var3.get("灵气"));
      double var7 = PlayerAttributeStore.getDouble(var3, "悟性", 1.0);
      double var9 = PlayerAttributeStore.getDouble(var3, "总品质", 0.5);
      String var11 = PlayerAttributeStore.getString(var3, "灵根属性", "");
      double var12 = attrCoeff(var11) * realmCoeff(var3);
      double var14 = loadBaseGain(var2);
      int var16 = PlayerAttributeStore.getInt(var3, "灵气获取", 0);
      double var17 = AttributePointLimits.spiritGainPercent(var3, var16) / 100.0;
      double var19 = Math.max(0.01, (var7 * var9 * var12 + Math.max(0.01, var14)) * Math.max(0.01, var17));
      Map var21 = PlayerAttributeStore.getMap(var3, "根基");
      if (!var3.containsKey("根基")) {
         var3.put("根基", var21);
      }

      double var22 = var6.current();
      double var24 = var6.max();
      double var26 = parseOverflowCurrent(var21);
      double var28;
      if (!var6.isUnlimitedMax() && Double.isFinite(var24) && !(var24 <= 0.0)) {
         var28 = Math.floor(PlayerAttributeStore.getDouble(var3, "根骨", 1.0) / 10.0 * var24 * 100.0) / 100.0;
      } else {
         var28 = Double.POSITIVE_INFINITY;
      }

      if (!var6.isUnlimitedMax() && var22 < var24) {
         var22 = Math.min(var22 + var19, var24);
         var3.put("灵气", PlayerAttributeStore.formatSpiritAmount(var22, var24));
      } else if (var6.isUnlimitedMax()) {
         var22 += var19;
         var3.put("灵气", PlayerAttributeStore.formatSpiritAmount(var22, var24));
      }

      if (!var6.isUnlimitedMax() && var22 >= var24) {
         double var30 = var22 + var19 - var24;
         var26 = Math.min(var26 + var30, var28);
         var21.put("稳固值", (int)Math.floor(var26 / (var24 * 0.01)));
         var21.put("溢出的灵气", String.format("%.2f/%.2f", var26, var28));
         var3.put("根基", var21);
      }

      long var34 = (long)Math.floor(Math.min(var22, 9.223372E18F));
      CultivationMath.CultivationInfo var32 = CultivationMath.getCultivationInfo(var34);
      var3.put("修为", var32.stage());
      if (!var5.isBlank() && !var5.equals(var32.stage()) && !PlayerAttributeStore.getBoolean(var3, "突破失败", false)) {
         var3.put("属性点", PlayerAttributeStore.getInt(var3, "属性点", 0) + ThreadLocalRandom.current().nextInt(2, 4));
         KomutechSupport.send(var1, "§a恭喜！修为突破小阶段，获得属性点奖励！");
         AttributeApplier.applyPlayerAttributes(var1, var3, false);
      }

      CultivationMath.ensureLingliFormat(var3);
      PlayerAttributeStore.save(var1.getName(), var3);
      PlayerAttributeStore.Lingli var33 = PlayerAttributeStore.parseLingli(var3.get("灵气"));
      if (var33.isUnlimitedMax()) {
         KomutechSupport.actionBar(var1, "§b灵气: §6" + PlayerAttributeStore.formatAmount(var33.current()) + " §7/ §6無");
         return false;
      } else {
         if (var33.current() >= var33.max()) {
            KomutechSupport.actionBar(var1, "§b溢出的灵气: §6" + String.format("%.2f", var26) + " §7/ §6" + String.format("%.2f", var28));
         } else {
            KomutechSupport.actionBar(var1, "§b灵气: §6" + String.format("%.2f", var33.current()) + " §7/ §6" + trim(var33.max()));
         }

         return var26 >= var28 && var33.current() >= var33.max();
      }
   }

   private static double parseOverflowCurrent(Map<String, Object> var0) {
      String var1 = PlayerAttributeStore.getString(var0, "溢出的灵气", "0/0");
      Matcher var2 = Pattern.compile("^(\\d+(?:\\.\\d+)?)").matcher(var1.trim());
      if (!var2.find()) {
         return 0.0;
      } else {
         try {
            return Double.parseDouble(var2.group(1));
         } catch (NumberFormatException var4) {
            return 0.0;
         }
      }
   }

   private static Map<String, Object> puTuanConfig() {
      long var0 = System.currentTimeMillis();
      if (var0 - cachedPuTuanConfigAt < 60000L && cachedPuTuanConfig != null) {
         return cachedPuTuanConfig;
      } else {
         try {
            if (!Files.exists(KomutechPaths.puTuanConfig())) {
               cachedPuTuanConfig = Map.of();
            } else {
               cachedPuTuanConfig = KomutechJson.asMap(KomutechJson.parse(Files.readString(KomutechPaths.puTuanConfig(), StandardCharsets.UTF_8)));
            }
         } catch (Exception var3) {
            cachedPuTuanConfig = Map.of();
         }

         cachedPuTuanConfigAt = var0;
         return cachedPuTuanConfig;
      }
   }

   private static String matIdAt(Location var0) {
      String var1 = MachineScriptHelper.getData(var0, "prayer_mat_id");
      if (var1 != null) {
         return var1;
      } else {
         SlimefunItem var2 = MachineScriptHelper.getSfItem(var0);
         return var2 == null ? "KOMUTECH_L_X_蒲团" : var2.getId();
      }
   }

   private static long loadMinSpirit(String var0) {
      try {
         Map var1 = KomutechJson.asMap(puTuanConfig().get(var0));
         if (!var1.isEmpty()) {
            return (long)KomutechJson.getDouble(var1, "最低灵气", KomutechJson.getDouble(var1, "minSpirit", DEFAULT_MIN_SPIRIT.getOrDefault(var0, 0L).longValue()));
         }
      } catch (Exception var2) {
      }

      return DEFAULT_MIN_SPIRIT.getOrDefault(var0, 0L);
   }

   private static boolean checkMatSpiritRequirement(Player var0, Location var1) {
      long var2 = loadMinSpirit(matIdAt(var1));
      if (var2 <= 0L) {
         return true;
      } else {
         Map var4 = PlayerAttributeStore.load(var0.getName());
         if (var4 == null) {
            KomutechSupport.send(var0, "§c无法读取玩家数据");
            return false;
         } else if (CultivationMath.spiritValue(var4) < var2) {
            CultivationMath.CultivationInfo var5 = CultivationMath.getCultivationInfo(var2);
            KomutechSupport.send(var0, "§c修为不足！至少需要 §6" + var5.stage() + " §c方可使用此修炼台");
            return false;
         } else {
            return true;
         }
      }
   }

   private static double loadBaseGain(Location var0) {
      String var1 = matIdAt(var0);
      double var2 = DEFAULT_BASE_GAIN.getOrDefault(var1, 1.0);

      try {
         Map var4 = puTuanConfig();
         if (var4.isEmpty()) {
            return var2;
         } else {
            Map var5 = KomutechJson.asMap(var4.get(var1));
            if (var5.isEmpty()) {
               return var2;
            } else {
               double var6 = KomutechJson.getDouble(var5, "基础修炼速度", KomutechJson.getDouble(var5, "baseGain", var2));
               return var6 > 0.0 ? var6 : var2;
            }
         }
      } catch (Exception var8) {
         return var2;
      }
   }

   private void rememberPrayerMat(Location var1) {
      SlimefunItem var2 = MachineScriptHelper.getSfItem(var1);
      MachineScriptHelper.setData(var1, "prayer_mat_id", var2 == null ? "KOMUTECH_L_X_蒲团" : var2.getId());
   }

   private boolean tryBeginSession(Player var1, Location var2, int var3, Map<String, Object> var4) {
      this.reconcileStaleSession(var2);
      UUID var5 = var1.getUniqueId();
      Location var6 = this.playerActiveMat.get(var5);
      if (var6 != null && !sameBlock(var6, var2)) {
         KomutechSupport.send(var1, "§c你已在其他修炼台上修炼，请先停止。");
         return false;
      } else {
         int var7 = getState(var2);
         if (var7 != 0 && var7 != var3) {
            KomutechSupport.send(var1, "§c该修炼台正在使用中。");
            return false;
         } else {
            String var8 = MachineScriptHelper.getData(var2, "meditation_player_uuid");
            if (var8 != null && !var8.equals(var5.toString())) {
               KomutechSupport.send(var1, "§c该修炼台已被其他玩家占用。");
               return false;
            } else {
               this.beginSession(var1, var2, var3, var4);
               return true;
            }
         }
      }
   }

   private void reconcileStaleSession(Location var1) {
      String var2 = MachineScriptHelper.getData(var1, "meditation_player_uuid");
      int var3 = getState(var1);
      if (var3 != 0) {
         if (var2 == null) {
            setState(var1, 0);
            PuTuanDisplayHelper.updateHologram(var1, statusText(0));
            this.removeMonitorLoc(var1);
         } else {
            Player var4;
            try {
               var4 = Bukkit.getPlayer(UUID.fromString(var2));
            } catch (IllegalArgumentException var7) {
               this.forceRelease(var1);
               return;
            }

            if (var4 != null && var4.isOnline()) {
               Location var5 = this.playerActiveMat.get(var4.getUniqueId());
               if (var5 != null && sameBlock(var5, var1)) {
                  if (!isOnMat(var4, var1)) {
                     this.endSession(var1, var4, null);
                  } else {
                     this.addMonitorLoc(var1);
                     this.ensureMonitor();
                  }
               } else if (isOnMat(var4, var1)) {
                  this.playerActiveMat.put(var4.getUniqueId(), var1);
                  Map var6 = PlayerAttributeStore.load(var4.getName());
                  if (var6 != null) {
                     this.sessionData.put(var4.getUniqueId(), var6);
                  }

                  this.addMonitorLoc(var1);
                  this.ensureMonitor();
               } else {
                  this.forceRelease(var1);
               }
            } else {
               this.forceRelease(var1);
            }
         }
      } else {
         if (var2 != null || this.containsMonitorLoc(var1)) {
            this.forceRelease(var1);
         }
      }
   }

   private void forceRelease(Location var1) {
      this.cancelBreakTasks(var1);
      this.cleanup(var1, null);
      setState(var1, 0);
      this.removeMonitorLoc(var1);
      PuTuanDisplayHelper.updateHologram(var1, statusText(0));
   }

   public int adminCleanupAt(Location var1) {
      this.forceRelease(var1);
      int var2 = PuTuanDisplayHelper.removeTaggedAt(var1);
      PuTuanSitHelper.cleanupAt(var1);
      return var2;
   }

   public int adminCleanupNearby(Location var1, double var2) {
      int var4 = 0;
      HashSet var5 = new HashSet();

      for (Display var7 : PuTuanDisplayHelper.findTaggedNear(var1, var2)) {
         String var8 = (String)var7.getPersistentDataContainer().get(PuTuanDisplayHelper.tagKey(), PersistentDataType.STRING);
         if (var8 != null && !var8.isEmpty()) {
            if (var5.add(var8)) {
               Location var9 = PuTuanDisplayHelper.parseLocationKey(var8);
               if (var9 != null) {
                  this.forceRelease(var9);
                  var4++;
               } else if (!var7.isDead()) {
                  var7.remove();
               }
            }
         } else if (!var7.isDead()) {
            var7.remove();
         }
      }

      return var4;
   }

   public int adminSweepAll() {
      int[] var1 = new int[]{0};
      var1[0] += PuTuanDisplayHelper.sweepLoadedWorlds((var1x, var2) -> {
         Location var3 = PuTuanDisplayHelper.parseLocationKey(var2);
         return var3 == null ? true : this.shouldPurgeTaggedDisplay(var3, var1x.getUniqueId());
      });
      if (var1[0] > 0 && this.plugin != null) {
         this.plugin.getLogger().info("[蒲团] 管理指令清理残留修炼显示实体: " + var1[0]);
      }

      return var1[0];
   }

   private void beginSession(Player var1, Location var2, int var3, Map<String, Object> var4) {
      this.rememberPrayerMat(var2);
      setState(var2, var3);
      MachineScriptHelper.setData(var2, "meditation_player_uuid", var1.getUniqueId().toString());
      this.playerActiveMat.put(var1.getUniqueId(), var2);
      if (var4 != null) {
         this.sessionData.put(var1.getUniqueId(), var4);
      } else {
         Map var5 = PlayerAttributeStore.load(var1.getName());
         if (var5 != null) {
            this.sessionData.put(var1.getUniqueId(), var5);
         }
      }

      maintainSit(var1, var2);
      PuTuanDisplayHelper.updateHologram(var2, statusText(var3));
      this.spawnProjection(var2);
      this.addMonitorLoc(var2);
      this.ensureMonitor();
   }

   private static void maintainSit(Player var0, Location var1) {
      PuTuanSitHelper.maintain(var0, var1);
   }

   private static String statusText(int var0) {
      return switch (var0) {
         case 1 -> "§a✦ 修炼中 ✦";
         case 2 -> "§d✦ 突破中 ✦";
         default -> "§7○ 空闲 ○";
      };
   }

   private void spawnProjection(Location var1) {
      String var2 = MachineScriptHelper.getData(var1, "meditation_proj_uuid");
      Entity var3 = var2 == null ? null : PuTuanDisplayHelper.findEntity(var1, var2);
      if (var3 == null || var3.isDead()) {
         PuTuanDisplayHelper.removeTaggedProjectionsAt(var1, null);
         BlockDisplay var4 = DisplayReflectionHelper.spawnBlockDisplay(
            var1.clone().add(-0.5, 1.0, -0.5), DisplayReflectionHelper.parseBlockData(loadProjMaterial(var1))
         );
         PuTuanDisplayHelper.tag(var4, var1);
         MachineScriptHelper.setData(var1, "meditation_proj_uuid", var4.getUniqueId().toString());
      }
   }

   private void cleanup(Location var1, Player var2) {
      this.cancelBreakTasks(var1);
      String var3 = MachineScriptHelper.getData(var1, "meditation_player_uuid");
      PuTuanDisplayHelper.removeStoredOrNearby(var1, "meditation_proj_uuid");
      PuTuanDisplayHelper.removeStoredOrNearby(var1, "meditation_holo_uuid");
      PuTuanDisplayHelper.removeTaggedAt(var1);
      MachineScriptHelper.removeData(var1, "meditation_player_uuid");
      MachineScriptHelper.removeData(var1, "meditation_proj_uuid");
      MachineScriptHelper.removeData(var1, "meditation_holo_uuid");
      MachineScriptHelper.removeData(var1, "prayer_mat_id");
      if (var2 != null) {
         this.playerActiveMat.remove(var2.getUniqueId());
         this.sessionData.remove(var2.getUniqueId());
         this.monitorSpiritTicks.remove(var2.getUniqueId());
      } else if (var3 != null) {
         try {
            UUID var4 = UUID.fromString(var3);
            this.playerActiveMat.remove(var4);
            this.sessionData.remove(var4);
            this.monitorSpiritTicks.remove(var4);
         } catch (IllegalArgumentException var5) {
         }
      }

      if (var2 != null) {
         PuTuanSitHelper.release(var2);
      }

      PuTuanSitHelper.cleanupAt(var1);
   }

   private void cancelBreakTasks(Location var1) {
      String var2 = locationKey(var1);
      Integer var3 = this.breakTasks.remove(var2);
      if (var3 != null) {
         Bukkit.getScheduler().cancelTask(var3);
      }

      Integer var4 = this.tribulationTasks.remove(var2 + "_trib");
      if (var4 != null) {
         Bukkit.getScheduler().cancelTask(var4);
      }
   }

   private static String loadProjMaterial(Location var0) {
      try {
         Map var1 = puTuanConfig();
         if (var1.isEmpty()) {
            return "minecraft:horn_coral_fan";
         } else {
            String var2 = MachineScriptHelper.getData(var0, "prayer_mat_id");
            if (var2 == null) {
               SlimefunItem var3 = MachineScriptHelper.getSfItem(var0);
               var2 = var3 == null ? "KOMUTECH_L_X_蒲团" : var3.getId();
            }

            Map var6 = KomutechJson.asMap(var1.get(var2));
            String var4 = KomutechJson.getString(var6, "投影材质", null);
            if (var4 == null) {
               var4 = KomutechJson.getString(var6, "projMaterial", "minecraft:horn_coral_fan");
            }

            return var4;
         }
      } catch (Exception var5) {
         return "minecraft:horn_coral_fan";
      }
   }

   private static int tribulationCount(double var0) {
      int var2 = 0;

      for (Entry var4 : TRIBULATION_THRESHOLDS.entrySet()) {
         if (var0 >= ((Long)var4.getKey()).longValue()) {
            var2 = (Integer)var4.getValue();
         }
      }

      return var2;
   }

   private static boolean isOnMat(Player var0, Location var1) {
      return PuTuanSitHelper.isOnMat(var0, var1);
   }

   private static boolean sameBlock(Location var0, Location var1) {
      return var0.getWorld().equals(var1.getWorld())
         && var0.getBlockX() == var1.getBlockX()
         && var0.getBlockY() == var1.getBlockY()
         && var0.getBlockZ() == var1.getBlockZ();
   }

   private boolean containsMonitorLoc(Location var1) {
      for (Location var3 : this.monitorLocs) {
         if (sameBlock(var3, var1)) {
            return true;
         }
      }

      return false;
   }

   private void addMonitorLoc(Location var1) {
      this.removeMonitorLoc(var1);
      this.monitorLocs.add(var1);
   }

   private void removeMonitorLoc(Location var1) {
      this.monitorLocs.removeIf(var1x -> sameBlock(var1x, var1));
   }

   private static String locationKey(Location var0) {
      return var0.getWorld().getName() + "," + var0.getBlockX() + "," + var0.getBlockY() + "," + var0.getBlockZ();
   }

   private static int getState(Location var0) {
      try {
         String var1 = MachineScriptHelper.getData(var0, "meditation_state");
         return var1 == null ? 0 : Integer.parseInt(var1);
      } catch (NumberFormatException var2) {
         return 0;
      }
   }

   private static void setState(Location var0, int var1) {
      MachineScriptHelper.setData(var0, "meditation_state", String.valueOf(var1));
   }

   private static double realmCoeff(Map<String, Object> var0) {
      return CultivationMath.getRealmCoefficient(var0);
   }

   private static double attrCoeff(String var0) {
      return switch (var0) {
         case "杂灵根" -> 0.1;
         case "四灵根" -> 0.3;
         case "三灵根" -> 0.5;
         case "双灵根" -> 0.8;
         case "单灵根" -> 1.0;
         case "天灵根" -> 1.2;
         case "变异灵根" -> 1.3;
         default -> 1.0;
      };
   }

   private static String trim(double var0) {
      if (Double.isInfinite(var0)) {
         return "無";
      } else {
         return Math.floor(var0) == var0 ? String.valueOf((long)var0) : String.valueOf(var0);
      }
   }
}
