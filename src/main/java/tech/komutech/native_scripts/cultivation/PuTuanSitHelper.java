package tech.komutech.native_scripts.cultivation;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import tech.komutech.native_scripts.support.EntityQueries;
import tech.komutech.native_scripts.support.KomutechSupport;

final class PuTuanSitHelper {
   private static final NamespacedKey SEAT_TAG = new NamespacedKey("KomutechNative".toLowerCase(), "pu_tuan_seat");
   private static final String GSIT_API = "dev.geco.gsit.api.GSitAPI";
   private static final String GSIT_STOP_REASON = "dev.geco.gsit.model.StopReason";
   /**
    * 玩家坐姿的目标高度（相对方块底部）。
    * 1.0 = 方块顶面（脚在方块上方）。曾按「坐得靠下」调至 0.5，后按实测反馈再提高 0.5 格 → 1.0。
    *
    * <p>调整此值时的注意点：alignSeat 的闭环阈值是 0.02，SEAT_MIN/MAX_RELATIVE_Y 钳制在 ±1.5，
    * 目标高度落在钳制范围内，闭环能正常收敛。
    */
   private static final double SEAT_SURFACE_OFFSET = 1.0;
   /**
    * 玩家骑乘盔甲架时游戏自动附加的高度偏移。
    * 只在闭环已经收敛（玩家已处于目标高度）时才采样，因此这个值始终可信；
    * 它只用于"生成座位时"给一个尽量准的初值，真正的高度靠 alignSeat 闭环修正。
    */
   private static volatile double ridingOffset = 0.0;
   /** 座位相对方块的高度钳制范围，防止异常测量把盔甲架甩到天上或地底。 */
   private static final double SEAT_MIN_RELATIVE_Y = -1.5;
   private static final double SEAT_MAX_RELATIVE_Y = 1.5;
   /**
    * 挪动座位后要等几个 tick 才能重新测量。
    * 玩家被 addPassenger 吸附、以及座位 teleport 后的跟随，都要到下一 tick 才生效；
    * 读早了会拿到玩家的旧坐标，于是把误差再修一遍 —— 反复过冲，座位被甩到天上或地底。
    */
   private static final long SEAT_SYNC_WAIT = 3L;
   /** 单次坐下最多校准多少 tick，防止意外情况下校准任务一直空转。 */
   private static final int SEAT_CALIBRATE_MAX_TICKS = 40;
   /**
    * 玩家"主动下车"后的冷却窗口（毫秒）。
    * 这段时间内 maintain() 一律拒绝把人重新按回座位 —— 这是杜绝"按 Shift 又被吸回去"的最后一道保险：
    * 不管会话有没有正常结束、事件有没有按时到达，只要玩家刚下过车，就绝不再把他拉回座位。
    */
   private static final long DISMOUNT_GRACE_MS = 1500L;
   private static final Map<UUID, Long> RECENT_DISMOUNT = new ConcurrentHashMap<>();

   private PuTuanSitHelper() {
   }

   /** 记录一次"玩家主动离开修炼座位"。 */
   static void markDismount(Player var0) {
      RECENT_DISMOUNT.put(var0.getUniqueId(), System.currentTimeMillis());
   }

   /** 玩家是否刚（冷却期内）主动下过车。 */
   private static boolean isRecentDismount(Player var0) {
      Long var1 = RECENT_DISMOUNT.get(var0.getUniqueId());
      if (var1 == null) {
         return false;
      } else if (System.currentTimeMillis() - var1 > DISMOUNT_GRACE_MS) {
         RECENT_DISMOUNT.remove(var0.getUniqueId());
         return false;
      } else {
         return true;
      }
   }

   static void sit(Player var0, Location var1) {
      // 玩家主动开始修炼 → 清掉"刚下过车"的冷却，否则刚起身又立刻开新一局会落不了座。
      RECENT_DISMOUNT.remove(var0.getUniqueId());
      release(var0);
      if (!tryGSit(var0, var1)) {
         spawnArmorStandSeat(var0, var1);
      }
   }

   static void maintain(Player var0, Location var1) {
      // 玩家刚主动下过车（按 Shift 起身）：冷却期内绝不重新落座，杜绝"被蒲团吸回去"。
      if (isRecentDismount(var0)) {
         return;
      }

      // 玩家按住 Shift = 想起身，绝不能把人重新按回座位。
      // 之前的"吸回去"现象就是这里造成的：Shift 使玩家脱离座位后，
      // maintain 每秒跑一次，发现玩家没座位了又重新 sit()，把人吸回蒲团。
      if (var0.isSneaking()) {
         return;
      }

      if (!isGSitOnMat(var0, var1)) {
         Entity var2 = var0.getVehicle();
         if (var2 instanceof ArmorStand var3 && isTaggedSeat(var3)) {
            alignSeat(var0, var3, var1);
            // 座位只在"水平方向"跑偏时才强行拉回；高度一律交给 alignSeat 闭环，
            // 否则"按 ridingOffset 算出来的位置"会和闭环修正互相打架。
            Location var4 = seatLocation(var1, var0);
            double var5 = var2.getLocation().getX() - var4.getX();
            double var6 = var2.getLocation().getZ() - var4.getZ();
            if (var5 * var5 + var6 * var6 > 0.25) {
               var2.teleport(var4);
            }

            if (!var2.getPassengers().contains(var0)) {
               var2.addPassenger(var0);
            }

            var0.setPose(Pose.SITTING);
         } else {
            sit(var0, var1);
         }
      }
   }

   static void release(Player var0) {
      releaseGSit(var0);
      ejectTaggedSeat(var0);
      var0.setPose(Pose.STANDING);
   }

   static void cleanupAt(Location var0) {
      removeSeatsAt(var0);
   }

   static boolean isOnMat(Player var0, Location var1) {
      if (!var0.isOnline() || var1.getWorld() == null || !var0.getWorld().equals(var1.getWorld())) {
         return false;
      } else if (isGSitOnMat(var0, var1)) {
         return true;
      } else {
         Entity var2 = var0.getVehicle();
         if (var2 instanceof ArmorStand var3 && isTaggedSeat(var3)) {
            // 座位本身就是按蒲团位置生成的，只要水平方向没跑偏就算在蒲团上。
            // 不能再拿座位高度做门禁：座位高度是为了"坐得好看"而调的，
            // 一旦调出窄窗口就会被判成离开蒲团，导致修炼刚开始就被中断。
            return isOverMat(var2.getLocation(), var1);
         } else if (var0.getPose() == Pose.SITTING && isNearMat(var0.getLocation(), var1)) {
            return true;
         } else {
            Block var8 = var0.getLocation().getBlock().getRelative(BlockFace.DOWN);
            if (blockMatches(var8, var1)) {
               return true;
            } else {
               Location var4 = var0.getLocation();
               int var5 = var4.getBlockX();
               int var6 = var4.getBlockY();
               int var7 = var4.getBlockZ();
               return var5 == var1.getBlockX() && var7 == var1.getBlockZ() && var6 >= var1.getBlockY() && var6 <= var1.getBlockY() + 1;
            }
         }
      }
   }

   private static boolean tryGSit(Player var0, Location var1) {
      if (Bukkit.getPluginManager().getPlugin("GSit") == null) {
         return false;
      } else {
         try {
            Class var2 = Class.forName("dev.geco.gsit.api.GSitAPI");
            Object var3 = var2.getMethod("createSeat", Block.class, LivingEntity.class, boolean.class, float.class, boolean.class)
               .invoke(null, var1.getBlock(), var0, false, var0.getLocation().getYaw(), true);
            return var3 != null;
         } catch (ReflectiveOperationException var4) {
            return false;
         }
      }
   }

   private static boolean isGSitOnMat(Player var0, Location var1) {
      if (Bukkit.getPluginManager().getPlugin("GSit") == null) {
         return false;
      } else {
         try {
            Class var2 = Class.forName("dev.geco.gsit.api.GSitAPI");
            boolean var3 = (Boolean)var2.getMethod("isEntitySitting", LivingEntity.class).invoke(null, var0);
            if (!var3) {
               return false;
            } else {
               Object var4 = var2.getMethod("getSeatByEntity", LivingEntity.class).invoke(null, var0);
               if (var4 == null) {
                  return isNearMat(var0.getLocation(), var1);
               } else {
                  return var4.getClass().getMethod("getBlock").invoke(var4) instanceof Block var6 ? blockMatches(var6, var1) : false;
               }
            }
         } catch (ReflectiveOperationException var7) {
            return false;
         }
      }
   }

   private static void releaseGSit(Player var0) {
      if (Bukkit.getPluginManager().getPlugin("GSit") != null) {
         try {
            Class var1 = Class.forName("dev.geco.gsit.api.GSitAPI");
            Object var2 = var1.getMethod("getSeatByEntity", LivingEntity.class).invoke(null, var0);
            if (var2 == null) {
               return;
            }

            Class var3 = Class.forName("dev.geco.gsit.model.StopReason");
            Enum var4 = Enum.valueOf(var3, "PLUGIN");
            var1.getMethod("removeSeat", var2.getClass(), var3, boolean.class).invoke(null, var2, var4, false);
         } catch (ReflectiveOperationException var5) {
         }
      }
   }

   private static void spawnArmorStandSeat(Player var0, Location var1) {
      removeSeatsAt(var1);
      Location var2 = seatLocation(var1, var0);

      ArmorStand var3 = (ArmorStand)var1.getWorld().spawn(var2, ArmorStand.class, var1x -> {
         var1x.setInvisible(true);
         var1x.setInvulnerable(true);
         var1x.setSilent(true);
         var1x.setGravity(false);
         var1x.setAI(false);
         var1x.setCollidable(false);
         var1x.setSmall(true);
         var1x.setArms(false);
         var1x.setBasePlate(false);
         var1x.setMarker(true);
         var1x.getPersistentDataContainer().set(SEAT_TAG, PersistentDataType.STRING, PuTuanDisplayHelper.locationKey(var1));
      });
      var3.addPassenger(var0);
      var0.setPose(Pose.SITTING);
      calibrateSeat(var0, var3, var1);
   }

   /**
    * 坐下后的高度校准：每 tick 跑一次，但每次挪完座位都要等 SEAT_SYNC_WAIT 个 tick 才重新测量。
    * 收敛（误差小于阈值）或玩家离开座位时结束。
    */
   private static void calibrateSeat(Player var0, Entity var1, Location var2) {
      Plugin var3 = KomutechSupport.plugin();
      if (var3 != null) {
         new BukkitRunnable() {
            int ticks = 0;
            long nextMeasure = SEAT_SYNC_WAIT;

            public void run() {
               this.ticks++;
               if (!var0.isOnline() || var1.isDead() || !var1.getPassengers().contains(var0)) {
                  this.cancel();
               } else if ((long)this.ticks < this.nextMeasure) {
                  // 刚挪过座位，等游戏把玩家同步过去再测
               } else if (alignSeat(var0, var1, var2)) {
                  this.nextMeasure = (long)this.ticks + SEAT_SYNC_WAIT;
                  if (this.ticks >= SEAT_CALIBRATE_MAX_TICKS) {
                     this.cancel();
                  }
               } else {
                  this.cancel();
               }
            }
         }.runTaskTimer(var3, 1L, 1L);
      }
   }

   private static void ejectTaggedSeat(Player var0) {
      Entity var1 = var0.getVehicle();
      if (var1 instanceof ArmorStand var2 && isTaggedSeat(var2)) {
         var1.eject();
         if (!var2.isDead()) {
            var2.remove();
         }
      }
   }

   private static void removeSeatsAt(Location var0) {
      if (var0.getWorld() != null) {
         String var1 = PuTuanDisplayHelper.locationKey(var0);

         for (Entity var3 : EntityQueries.entities(var0, 2.0)) {
            if (var3 instanceof ArmorStand var4 && isTaggedSeat(var4)) {
               String var5 = (String)var4.getPersistentDataContainer().get(SEAT_TAG, PersistentDataType.STRING);
               if (var1.equals(var5)) {
                  var4.eject();
                  if (!var4.isDead()) {
                     var4.remove();
                  }
               }
            }
         }
      }
   }

   // 包内可见：PuTuanScript 的 VehicleExitEvent 监听需要用它判断"下的是不是修炼座位"。
   static boolean isTaggedSeat(Entity var0) {
      return var0 instanceof ArmorStand var1 && var1.getPersistentDataContainer().has(SEAT_TAG, PersistentDataType.STRING);
   }

   private static Location seatLocation(Location var0, Player var1) {
      Location var2 = var0.clone().add(0.5, SEAT_SURFACE_OFFSET - ridingOffset, 0.5);
      var2.setYaw(var1.getLocation().getYaw());
      var2.setPitch(0.0F);
      return var2;
   }

   /**
    * 闭环修正座位高度：直接看"玩家现在离目标高度差多少"，把座位挪同样的量。
    * 玩家与座位是刚性跟随的，所以挪多少玩家就跟多少，与骑乘偏移是多少无关。
    *
    * @return true = 本次挪动了座位（调用方需要等几个 tick 让游戏同步后再测）
    */
   private static boolean alignSeat(Player var0, Entity var1, Location var2) {
      if (var0.isOnline() && !var1.isDead() && var1.getPassengers().contains(var0)) {
         double var3 = var2.getBlockY() + SEAT_SURFACE_OFFSET;
         double var4 = var0.getLocation().getY();
         double var5 = var3 - var4;
         if (Math.abs(var5) > 0.02) {
            Location var6 = var1.getLocation();
            double var7 = var6.getY() + var5;
            double var8 = (double)var2.getBlockY() + SEAT_MIN_RELATIVE_Y;
            double var9 = (double)var2.getBlockY() + SEAT_MAX_RELATIVE_Y;
            if (var7 < var8) {
               var7 = var8;
            }

            if (var7 > var9) {
               var7 = var9;
            }

            var6.setY(var7);
            var1.teleport(var6);
            return true;
         } else {
            // 已经收敛了，此时"玩家 Y - 座位 Y"才是真实的骑乘偏移，
            // 记下来只作为下次生成座位的初值，减少开局那一下的偏差。
            double var6 = var4 - var1.getLocation().getY();
            if (var6 >= -0.5 && var6 <= 2.0) {
               ridingOffset = var6;
            }

            return false;
         }
      } else {
         return false;
      }
   }

   /** 只校验水平位置：座位始终生成在蒲团上，高度由 alignSeat 负责，不该参与在/不在蒲团的判定。 */
   private static boolean isOverMat(Location var0, Location var1) {
      if (var0.getWorld() != null && var0.getWorld().equals(var1.getWorld())) {
         double var2 = var0.getX() - (var1.getBlockX() + 0.5);
         double var4 = var0.getZ() - (var1.getBlockZ() + 0.5);
         return var2 * var2 + var4 * var4 <= 1.2;
      } else {
         return false;
      }
   }

   private static boolean isNearMat(Location var0, Location var1) {
      if (var0.getWorld() != null && var0.getWorld().equals(var1.getWorld())) {
         double var2 = var0.getX() - (var1.getBlockX() + 0.5);
         double var4 = var0.getZ() - (var1.getBlockZ() + 0.5);
         return var2 * var2 + var4 * var4 <= 1.2 && var0.getY() >= var1.getY() - 0.6 && var0.getY() <= var1.getBlockY() + 1.5;
      } else {
         return false;
      }
   }

   private static boolean blockMatches(Block var0, Location var1) {
      return var0.getWorld().equals(var1.getWorld()) && var0.getX() == var1.getBlockX() && var0.getY() == var1.getBlockY() && var0.getZ() == var1.getBlockZ();
   }
}
