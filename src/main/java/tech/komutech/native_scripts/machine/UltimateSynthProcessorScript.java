package tech.komutech.native_scripts.machine;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import tech.komutech.native_scripts.support.KomutechMenuHandler;
import tech.komutech.native_scripts.support.KomutechMenuRouter;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.MachineMenuGuard;
import tech.komutech.native_scripts.support.ParticleBudget;
import tech.komutech.native_scripts.NativeLifecycleScript;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.MachineScriptHelper;

public final class UltimateSynthProcessorScript implements NativeScript, NativeLifecycleScript, KomutechMenuHandler {
   private static final int MAIN_PHASE_TICKS = 340;
   private static final int SHRINK_STEPS = 10;
   private static final int TOTAL_TICKS = 380;
   private static final Set<String> SYNTHESIZING = new HashSet<>();
   private static final Boolean BLOCK_CLICK = Boolean.FALSE;

   @Override
   public void registerLifecycle(Plugin var1) {
      KomutechMenuRouter.registerHandler(this);
      var1.getServer().getPluginManager().registerEvents(new Listener() {
         @EventHandler
         public void onInventoryOpen(InventoryOpenEvent var1) {
            if (var1.getPlayer() instanceof Player var2) {
               if (UltimateSynthProcessorScript.this.handles(var1.getView())) {
                  KomutechMenuRouter.bindInventory(var1.getView().getTopInventory(), UltimateSynthProcessorScript.this);
               }
            }
         }
      }, var1);
   }

   @Override
   public boolean handles(InventoryView var1) {
      return MachineMenuGuard.isUltimateSynthProcessorMenu(var1);
   }

   @Override
   public Object invoke(String var1, Object... var2) {
      if ("onOpen".equals(var1) && var2[0] instanceof Player var7) {
         this.bindIfProcessorMenu(var7);
         return null;
      } else if ("onClick".equals(var1) && var2.length >= 4 && var2[0] instanceof Player var3) {
         int var9 = (Integer)var2[1];
         Object var5 = var2[3];
         Inventory var6 = var3.getOpenInventory().getTopInventory();
         if (!this.handles(var3.getOpenInventory())) {
            return null;
         } else {
            this.bindIfProcessorMenu(var3);
            if (UltimateSynthSupport.isProcessorMaterialSlot(var9)) {
               return null;
            } else {
               if (var9 == 13 && !KomutechSupport.isRightClick(var5) && !KomutechSupport.isShiftClick(var5)) {
                  this.handleClick(var3);
               }

               return BLOCK_CLICK;
            }
         }
      } else {
         return null;
      }
   }

   @Override
   public void onClick(InventoryClickEvent var1) {
      if (var1.getWhoClicked() instanceof Player var2) {
         if (this.handles(var1.getView())) {
            this.bindIfProcessorMenu(var2);
            int var5 = var1.getRawSlot();
            int var4 = var1.getView().getTopInventory().getSize();
            MachineMenuGuard.protectTopInventory(var1, UltimateSynthSupport::isProcessorMaterialSlot);
            if (var5 >= 0 && var5 < var4 && var1.isCancelled() && var5 == 13 && !KomutechSupport.isRightClick(var1) && !KomutechSupport.isShiftClick(var1)) {
               this.handleClick(var2);
            }
         }
      }
   }

   @Override
   public void onDrag(InventoryDragEvent var1) {
      if (this.handles(var1.getView())) {
         if (var1.getWhoClicked() instanceof Player var2) {
            this.bindIfProcessorMenu(var2);
         }

         MachineMenuGuard.protectTopInventoryDrag(var1, UltimateSynthSupport::isProcessorMaterialSlot);
      }
   }

   private void bindIfProcessorMenu(Player var1) {
      InventoryView var2 = var1.getOpenInventory();
      if (this.handles(var2)) {
         KomutechMenuRouter.bindInventory(var2.getTopInventory(), this);
      }
   }

   private Boolean handleClick(Player var1) {
      Location var2 = UltimateSynthSupport.getTargetProcessor(var1);
      if (var2 == null) {
         KomutechSupport.send(var1, "§c请正对处理器机器！");
         return Boolean.TRUE;
      } else {
         Location var3 = var2.clone().add(0.0, -1.0, 0.0);
         if (!"KOMUTECH_L_ZJ_終極合成台核心".equals(getSfId(var3))) {
            KomutechSupport.send(var1, "§c核心机器无效！");
            return Boolean.TRUE;
         } else if (UltimateSynthSupport.parseInt(MachineScriptHelper.getData(var3, "KOMUTECH_L_ZJ_ZJHC_zt"), 0) != 1) {
            KomutechSupport.send(var1, "§c核心未激活！");
            return Boolean.TRUE;
         } else {
            UltimateSynthSupport.StructureCheck var4 = UltimateSynthSupport.checkCraftStructure(var3);
            if (var4.valid()) {
               Object var7 = MachineScriptHelper.getMenu(var2);
               if (!UltimateSynthSupport.preCheck(var1, var2, var7)) {
                  return Boolean.TRUE;
               } else {
                  String var6 = locationKey(var2);
                  if (SYNTHESIZING.contains(var6)) {
                     KomutechSupport.send(var1, "§c机器正在合成中，请稍后再试！");
                     return Boolean.TRUE;
                  } else {
                     var1.closeInventory();
                     this.startEffect(var1, var3, var2, var6);
                     return Boolean.TRUE;
                  }
               }
            } else {
               MachineScriptHelper.setData(var3, "KOMUTECH_L_ZJ_ZJHC_zt", "0");
               KomutechSupport.send(var1, "§c结构损坏，核心已关闭！");

               for (int var5 = 0; var5 < Math.min(5, var4.errors().size()); var5++) {
                  KomutechSupport.send(var1, "§7 - " + var4.errors().get(var5));
               }

               return Boolean.TRUE;
            }
         }
      }
   }

   private void startEffect(final Player var1, final Location var2, final Location var3, final String var4) {
      SYNTHESIZING.add(var4);
      final World var5 = var2.getWorld();
      if (var5 == null) {
         SYNTHESIZING.remove(var4);
      } else {
         final long var6 = var5.getTime();
         final boolean var8 = var5.hasStorm();
         final boolean var9 = var5.isThundering();
         var5.setTime(18000L);
         var5.setStorm(true);
         var5.setThundering(true);
         final Location var10 = var3.clone().add(0.5, 0.5, 0.5);
         final List var11 = UltimateSynthSupport.lingLocations(var2);
         final List var12 = UltimateSynthSupport.beaconLocations(var2);
         final HashSet var13 = new HashSet();
         final ThreadLocalRandom var14 = ThreadLocalRandom.current();

         for (int var15 = 0; var15 < 9; var15++) {
            var13.add(var14.nextInt(340));
         }

         Plugin var16 = KomutechSupport.plugin();
         if (var16 == null) {
            this.finish(var1, var2, var3, var4, var5, var6, var8, var9);
         } else {
            (new BukkitRunnable() {
               int tick = 0;

               public void run() {
                  if (this.tick < 340) {
                     if (this.tick % 5 == 0) {
                        UltimateSynthProcessorScript.spawnLingTrails(var5, var11, var10, var14);
                     }

                     UltimateSynthProcessorScript.spawnMainTrails(var5, var10, this.tick / 20.0);
                     if (var13.contains(this.tick) && !var12.isEmpty()) {
                        Location var1x = (Location)var12.get(var14.nextInt(var12.size()));
                        var5.strikeLightningEffect(var1x);
                        var5.playSound(var1x, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0F, 1.0F);
                     }
                  } else if (this.tick < 360) {
                     int var2x = (this.tick - 340) / 2;
                     if ((this.tick - 340) % 2 == 0) {
                        UltimateSynthProcessorScript.spawnShrinkTrails(var5, var10, var2x, (340 + var2x) / 20.0);
                     }
                  } else if (this.tick >= 380) {
                     UltimateSynthProcessorScript.spawnFinishBurst(var5, var2, var14);
                     Location var3x = var2.clone().add(0.0, 9.0, 0.0);
                     var5.strikeLightningEffect(var3x);
                     var5.playSound(var3x, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 2.0F, 1.0F);
                     this.cancel();
                     UltimateSynthProcessorScript.this.finish(var1, var2, var3, var4, var5, var6, var8, var9);
                     return;
                  }

                  this.tick++;
               }
            }).runTaskTimer(var16, 0L, 1L);
         }
      }
   }

   private static void spawnLingTrails(World var0, List<Location> var1, Location var2, ThreadLocalRandom var3) {
      for (Location var5 : var1) {
         Vector var6 = var2.toVector().subtract(var5.toVector());
         if (var6.lengthSquared() > 0.0) {
            var6.normalize();
         }

         int var7 = var3.nextInt(3) + 1;
         ParticleBudget.spawnEndRod(var0, var5, var6.getX(), var6.getY(), var6.getZ(), 0.6, var7);
      }
   }

   private static void spawnMainTrails(World var0, Location var1, double var2) {
      int var4 = ParticleBudget.scaleTrailPoints(16);

      for (int var5 = 0; var5 < 3; var5++) {
         for (int var6 = 0; var6 < var4; var6++) {
            Location var7 = UltimateSynthSupport.trailPosition(var1, var5, var2 + var6 * 0.05);
            Color var8 = UltimateSynthSupport.trailColor(var5, var6);
            ParticleBudget.spawnDust(var0, var7, new DustOptions(var8, 2.4F), 1);
         }
      }
   }

   private static void spawnShrinkTrails(World var0, Location var1, int var2, double var3) {
      double var5 = var2 / 10.0;
      double var7 = 6.0 * (1.0 - var5);
      int var9 = ParticleBudget.scaleTrailPoints(16);

      for (int var10 = 0; var10 < 3; var10++) {
         for (int var11 = 0; var11 < var9; var11++) {
            double var12 = var3 + var11 * 0.05;
            double var14 = var12 * 0.5 + var10 * 2.0;
            double var16 = Math.sin(var12 * 0.8 + var10) * 2.0 * (1.0 - var5);
            Location var18 = new Location(var0, var1.getX() + var7 * Math.cos(var14), var1.getY() + 2.0 + var16, var1.getZ() + var7 * Math.sin(var14));
            Color var19 = UltimateSynthSupport.trailColor(var10, var11);
            ParticleBudget.spawnDust(var0, var18, new DustOptions(var19, 2.4F), 1);
         }
      }
   }

   private static void spawnFinishBurst(World var0, Location var1, ThreadLocalRandom var2) {
      for (int var3 = 0; var3 < 30; var3++) {
         Location var4 = var1.clone().add((var2.nextDouble() - 0.5) * 2.0, var2.nextDouble() * 4.0 + 2.5, (var2.nextDouble() - 0.5) * 2.0);
         Color var5 = UltimateSynthSupport.trailColor(var2.nextInt(3), var3);
         ParticleBudget.spawnDust(var0, var4, new DustOptions(var5, 2.4F), 1);
      }

      var0.playSound(var1, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 2.0F, 1.0F);
   }

   private void finish(Player var1, Location var2, Location var3, String var4, World var5, long var6, boolean var8, boolean var9) {
      var5.setTime(var6);
      var5.setStorm(var8);
      var5.setThundering(var9);
      if (var1.isOnline()) {
         UltimateSynthSupport.finalCraft(var1, var3, MachineScriptHelper.getMenu(var3));
      }

      SYNTHESIZING.remove(var4);
   }

   private static String getSfId(Location var0) {
      return MachineScriptHelper.getSfItem(var0) == null ? null : MachineScriptHelper.getSfItem(var0).getId();
   }

   private static String locationKey(Location var0) {
      return var0.getWorld().getName() + "," + var0.getBlockX() + "," + var0.getBlockY() + "," + var0.getBlockZ();
   }
}
