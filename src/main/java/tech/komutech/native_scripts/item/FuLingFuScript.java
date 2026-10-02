package tech.komutech.native_scripts.item;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.ParticleBudget;
import tech.komutech.native_scripts.support.PlayerAttributeStore;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.support.UseEvents;

public final class FuLingFuScript implements NativeScript {
   private static final int SPIRIT_COST = 5;
   private static final long COOLDOWN_MS = 1000L;
   private static final int RANGE = 8;
   private static final double DETECTION_RADIUS = 1.0;
   private static final int ANIMATION_DURATION = 60;
   private static final int SPIRAL_COUNT = 3;
   private static final int PARTICLES_PER_SPIRAL = 8;
   private static final double SPIRAL_RADIUS = 1.2;
   private static final double SPIRAL_SPEED = 0.2;
   private static final String SPECIAL_ID = "KOMUTECH_L_F_灵墟缚灵符";
   private final Map<UUID, Long> cooldowns = new HashMap<>();
   private final Map<UUID, FuLingFuScript.CaptureAnimation> captureAnimations = new HashMap<>();
   private final Map<UUID, BukkitTask> animationTasks = new HashMap<>();

   @Override
   public Object invoke(String var1, Object... var2) {
      return !"onUse".equals(var1) ? null : UseEvents.parse(var2[0]).map(this::handleUse).orElse(null);
   }

   private Object handleUse(UseEvents.Context var1) {
      Player var2 = var1.player();
      UUID var3 = var2.getUniqueId();
      long var4 = System.currentTimeMillis();
      if (var4 - this.cooldowns.getOrDefault(var3, 0L) < 1000L) {
         KomutechSupport.actionBar(var2, "§c冷却中");
         return null;
      } else if (this.captureAnimations.containsKey(var3)) {
         KomutechSupport.actionBar(var2, "§c你正在进行捕捉！");
         return null;
      } else {
         this.cooldowns.put(var3, var4);
         ItemStack var6 = var1.item();
         boolean var7 = false;
         SlimefunItem var8 = SlimefunItem.getByItem(var6);
         if (var8 != null && "KOMUTECH_L_F_灵墟缚灵符".equals(var8.getId())) {
            var7 = true;
         }

         LivingEntity var9 = getTargetEntity(var2);
         shootRayParticles(var2, var9);
         if (var9 == null) {
            KomutechSupport.actionBar(var2, "§c未命中生物！");
            return null;
         } else {
            Map var10 = PlayerAttributeStore.load(var2.getName());
            if (var10 == null) {
               KomutechSupport.actionBar(var2, "§c无玩家属性数据");
               return null;
            } else {
               PlayerAttributeStore.Lingli var11 = PlayerAttributeStore.parseLingli(var10.get("灵力"));
               if (var11.current() < 5.0) {
                  KomutechSupport.actionBar(var2, "§c灵力不足！需要 5");
                  return null;
               } else {
                  var11 = new PlayerAttributeStore.Lingli(var11.current() - 5.0, var11.max(), var11.bonus());
                  var10.put("灵力", PlayerAttributeStore.formatLingli(var11));
                  PlayerAttributeStore.save(var2.getName(), var10);
                  double var12 = PlayerAttributeStore.getDouble(var10, "血量_实际", 0.0) + 20.0;
                  double var14 = var9.getHealth();
                  double var16 = var7
                     ? Math.min(100.0, var14 > var12 ? 25.0 : 25.0 + (var12 - var14) / var12 * 100.0)
                     : (var14 >= var12 ? 0.0 : Math.min(50.0, (var12 - var14) / var12 * 50.0));
                  int var18 = (int)Math.floor(var16);
                  KomutechSupport.actionBar(var2, "§e捕捉概率: " + var18 + "%");
                  if (Math.random() * 100.0 >= var16) {
                     this.handleCaptureFailure(var2, var1, var18, PlayerAttributeStore.getDouble(var10, "灵识", 0.0), var16);
                     return null;
                  } else if (!consumeOne(var2)) {
                     KomutechSupport.actionBar(var2, "§c物品已消失，取消捕捉");
                     return null;
                  } else {
                     KomutechSupport.actionBar(var2, "§a捕捉成功！正在束缚... (" + var18 + "%)");
                     this.startCaptureAnimation(var2, var9, var18, var7);
                     return null;
                  }
               }
            }
         }
      }
   }

   private void handleCaptureFailure(Player var1, UseEvents.Context var2, int var3, double var4, double var6) {
      double var8 = Math.min(100.0, Math.max(0.0, 100.0 - var6 - var4));
      if (Math.random() * 100.0 < var8) {
         if (consumeOne(var1)) {
            KomutechSupport.actionBar(var1, "§c捕捉失败！符箓已损坏 (" + var3 + "%)");
         } else {
            KomutechSupport.actionBar(var1, "§c捕捉失败！");
         }
      } else {
         KomutechSupport.actionBar(var1, "§c捕捉失败！但符箓完好无损 (" + var3 + "%)");
      }
   }

   private void startCaptureAnimation(Player var1, LivingEntity var2, int var3, boolean var4) {
      UUID var5 = var1.getUniqueId();
      this.cancelAnimation(var5);
      PotionEffectType var6 = slownessEffect();
      if (var6 != null) {
         var2.addPotionEffect(new PotionEffect(var6, 60, 255, true, false));
      }

      FuLingFuScript.CaptureAnimation var7 = new FuLingFuScript.CaptureAnimation(var1, var2, var3, var4);
      this.captureAnimations.put(var5, var7);
      BukkitTask var8 = KomutechSupport.plugin().getServer().getScheduler().runTaskTimer(KomutechSupport.plugin(), () -> {
         FuLingFuScript.CaptureAnimation var2x = this.captureAnimations.get(var5);
         if (var2x == null) {
            this.cancelAnimation(var5);
         } else if (var2x.tick >= 60) {
            this.finishCaptureAnimation(var5, true);
         } else if (!var2x.entity.isDead() && var2x.entity.isValid() && var2x.player.isOnline()) {
            generateSpiralParticles(var2x.entity, var2x.tick);
            var2x.tick++;
         } else {
            this.finishCaptureAnimation(var5, false);
         }
      }, 0L, 2L);
      this.animationTasks.put(var5, var8);
   }

   private void finishCaptureAnimation(UUID var1, boolean var2) {
      FuLingFuScript.CaptureAnimation var3 = this.captureAnimations.remove(var1);
      this.cancelAnimation(var1);
      if (var3 != null) {
         PotionEffectType var4 = slownessEffect();
         if (var4 != null && var3.entity.isValid()) {
            var3.entity.removePotionEffect(var4);
         }

         if (var2 && var3.entity.isValid() && !var3.entity.isDead()) {
            giveSpawnEgg(var3.player, var3.entity, var3.catchPercent, var3.special);
         } else if (var3.player.isOnline()) {
            KomutechSupport.actionBar(var3.player, "§c捕捉失败！");
         }
      }
   }

   private void cancelAnimation(UUID var1) {
      BukkitTask var2 = this.animationTasks.remove(var1);
      if (var2 != null) {
         var2.cancel();
      }
   }

   private static void giveSpawnEgg(Player var0, LivingEntity var1, int var2, boolean var3) {
      Material var4;
      try {
         var4 = Material.valueOf(var1.getType().name() + "_SPAWN_EGG");
      } catch (IllegalArgumentException var8) {
         var4 = Material.EGG;
      }

      Location var5 = var1.getLocation();
      var1.remove();
      ItemStack var6 = new ItemStack(var4, 1);
      HashMap var7 = var0.getInventory().addItem(new ItemStack[]{var6});
      if (!var7.isEmpty()) {
         var0.getWorld().dropItem(var5, var6);
         KomutechSupport.actionBar(var0, "§e背包已满，刷怪蛋已掉落！");
      }

      KomutechSupport.actionBar(var0, (var3 ? "§6灵墟§a" : "§a") + "捕捉成功！ (" + var2 + "%)");
   }

   private static void generateSpiralParticles(LivingEntity var0, int var1) {
      if (ParticleBudget.canRunHeavyEffect(var0.getWorld())) {
         Location var2 = var0.getLocation();
         double var3 = var0.getHeight();
         int var5 = ParticleBudget.request(var0.getWorld(), 24);
         int var6 = 0;

         for (int var7 = 0; var7 < 3 && var6 < var5; var7++) {
            double var8 = var7 * (Math.PI * 2.0 / 3.0);
            double var10 = var3 * (var7 / 3.0);

            for (int var12 = 0; var12 < 8 && var6 < var5; var12++) {
               double var13 = (var12 * (Math.PI / 4) + var1 * 0.2 + var8) % (Math.PI * 2);
               double var15 = Math.cos(var13) * 1.2;
               double var17 = Math.sin(var13) * 1.2;
               Location var19 = var2.clone().add(var15, var10 + 0.1, var17);
               ParticleBudget.spawnDust(var0.getWorld(), var19, new DustOptions(Color.fromRGB(255, 105, 180), 0.5F), 1);
               if (++var6 < var5 && var12 % 2 == 0) {
                  ParticleBudget.spawnDust(var0.getWorld(), var19, new DustOptions(Color.fromRGB(0, 255, 255), 0.4F), 1);
                  var6++;
               }
            }
         }
      }
   }

   private static void shootRayParticles(Player var0, LivingEntity var1) {
      Location var2 = var0.getEyeLocation();
      Location var3;
      if (var1 != null && var1.isValid()) {
         var3 = var1.getLocation().add(0.0, var1.getHeight() / 2.0, 0.0);
      } else {
         Vector var4 = var2.getDirection().normalize();
         var3 = var2.clone().add(var4.multiply(8));
      }

      Vector var15 = var3.toVector().subtract(var2.toVector());
      double var5 = Math.min(var15.length(), 8.0);
      if (!(var5 < 0.5)) {
         var15.normalize();

         for (double var7 = 0.0; var7 <= var5; var7 += 0.25) {
            double var9 = var7 / var5;
            int var11 = (int)Math.round(0.0 + 255.0 * var9);
            int var12 = (int)Math.round(255.0 + -150.0 * var9);
            int var13 = (int)Math.round(255.0 + -75.0 * var9);
            Location var14 = var2.clone().add(var15.clone().multiply(var7));
            var0.getWorld().spawnParticle(Particle.DUST, var14, 1, 0.0, 0.0, 0.0, 0.0, new DustOptions(Color.fromRGB(var11, var12, var13), 0.6F));
         }
      }
   }

   private static LivingEntity getTargetEntity(Player var0) {
      RayTraceResult var1 = var0.getWorld()
         .rayTraceEntities(
            var0.getEyeLocation(),
            var0.getEyeLocation().getDirection(),
            8.0,
            1.0,
            var1x -> var1x instanceof LivingEntity var2x && !(var2x instanceof Player) && var2x != var0
         );
      return var1 != null && var1.getHitEntity() instanceof LivingEntity var2 ? var2 : null;
   }

   private static boolean consumeOne(Player var0) {
      ItemStack var1 = var0.getInventory().getItemInMainHand();
      if (var1 != null && !var1.getType().isAir()) {
         if (var1.getAmount() > 1) {
            var1.setAmount(var1.getAmount() - 1);
         } else {
            var0.getInventory().setItemInMainHand(null);
         }

         return true;
      } else {
         return false;
      }
   }

   private static PotionEffectType slownessEffect() {
      PotionEffectType var0 = PotionEffectType.getByName("SLOWNESS");
      return var0 != null ? var0 : PotionEffectType.getByName("SLOW");
   }

   private static final class CaptureAnimation {
      final Player player;
      final LivingEntity entity;
      final int catchPercent;
      final boolean special;
      int tick;

      CaptureAnimation(Player var1, LivingEntity var2, int var3, boolean var4) {
         this.player = var1;
         this.entity = var2;
         this.catchPercent = var3;
         this.special = var4;
      }
   }
}
