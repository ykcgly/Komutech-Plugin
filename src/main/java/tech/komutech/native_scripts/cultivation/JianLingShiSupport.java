package tech.komutech.native_scripts.cultivation;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import tech.komutech.native_scripts.item.XiDianZhuScript;
import tech.komutech.native_scripts.support.CultivationMath;
import tech.komutech.native_scripts.support.DisplayReflectionHelper;
import tech.komutech.native_scripts.support.ParticleBudget;
import tech.komutech.native_scripts.support.PlayerAttributeStore;
import tech.komutech.native_scripts.support.StructureMissingHint;
import tech.komutech.native_scripts.support.MachineScriptHelper;

public final class JianLingShiSupport {
   public static final String PREFIX = "KOMUTECH_L_X_JLS_";
   public static final String CORE_ID = "KOMUTECH_L_X_鉴灵石核心";
   public static final String GUI_TITLE = "§6鉴灵石核心";
   public static final List<String> TRIGGER_IDS = List.of("KOMUTECH_L_X_鉴灵石框架", "KOMUTECH_L_X_鉴灵石框架板");
   private static final String TRIGGER_FRAME = "KOMUTECH_L_X_鉴灵石框架";
   private static final String TRIGGER_FRAME_PLATE = "KOMUTECH_L_X_鉴灵石框架板";
   public static final List<String> ALLOWED_CLUSTERS = List.of("KOMUTECH_L_DJ_JPLJ", "KOMUTECH_L_DJ_XPLJ", "KOMUTECH_L_DJ_ZPLJ", "KOMUTECH_L_DJ_SPLJ");
   private static final List<String> ALL_ELEMENTS = List.of("金", "木", "水", "火", "土");
   private static final double MIN_QUALITY = 0.01;
   private static final int TOTAL_TICKS = 200;
   private static final int TRAIL_POINTS = 16;
   private static final double TRAIL_INIT_RADIUS = (double)3.0F;
   private static final double TRAIL_RADIUS_AMP = (double)0.5F;
   private static final double TRAIL_HEIGHT_AMP = (double)1.5F;
   private static final double ROTATION_SPEED = (double)2.5F;
   private static final double HEIGHT_OSCILLATION_SPEED = 0.8;
   private static final double RADIUS_OSCILLATION_SPEED = 0.3;
   private static final double CENTER_Y_OFFSET = (double)4.0F;
   private static final float PARTICLE_BASE_SIZE = 1.0F;
   private static final float PARTICLE_SIZE_MULTIPLIER = 3.0F;
   private static final int PARTICLE_BASE_COUNT = 2;
   private static final int PARTICLE_COUNT_MULTIPLIER = 6;
   private static final Map<String, Color> BASE_COLORS = Map.of("金", Color.fromRGB(255, 215, 0), "木", Color.fromRGB(0, 255, 0), "水", Color.fromRGB(0, 150, 255), "火", Color.fromRGB(255, 0, 0), "土", Color.fromRGB(139, 69, 19));
   private static final Map<String, Color> MUTATED_COLORS = Map.of("雷", Color.fromRGB(170, 0, 255), "风", Color.fromRGB(0, 255, 255), "冰", Color.fromRGB(0, 191, 255), "光", Color.fromRGB(255, 255, 255), "暗", Color.fromRGB(0, 0, 0));
   private static final Map<String, String> MUTATION_MAP = Map.of("雷", "金", "风", "木", "冰", "水", "光", "火", "暗", "土");
   private static final int[][] CLUSTER_OFFSETS = new int[][]{{-3, 2, -3}, {-3, 2, 3}, {3, 2, -3}, {3, 2, 3}};
   private static final StructureBlock[] STRUCTURE = buildStructure();

   private JianLingShiSupport() {
   }

   public static Rotated rot(int var0, int var1, int var2) {
      Rotated var10000;
      switch (var2) {
         case 1 -> var10000 = new Rotated(-var1, var0);
         case 2 -> var10000 = new Rotated(-var0, -var1);
         case 3 -> var10000 = new Rotated(var1, -var0);
         default -> var10000 = new Rotated(var0, var1);
      }

      return var10000;
   }

   public static StructureBlock[] structure() {
      return STRUCTURE;
   }

   public static StructureCheck checkStructure(Location var0, int var1) {
      World var2 = var0.getWorld();
      ArrayList var3 = new ArrayList();
      ArrayList var4 = new ArrayList();

      for(StructureBlock var8 : STRUCTURE) {
         Rotated var9 = rot(var8.x(), var8.z(), var1);
         int var10 = var0.getBlockX() + var9.x();
         int var11 = var0.getBlockY() + var8.y();
         int var12 = var0.getBlockZ() + var9.z();
         Block var13 = var2.getBlockAt(var10, var11, var12);
         ResolvedBlock var14 = new ResolvedBlock(var10, var11, var12, var8.material(), var8.sfId());
         var4.add(var14);
         if (var8.dynamicCluster()) {
            SlimefunItem var15 = MachineScriptHelper.getSfItem(var13.getLocation());
            if (var15 == null || !ALLOWED_CLUSTERS.contains(var15.getId())) {
               int var10001 = var10 - var0.getBlockX();
               var3.add("位置 (" + var10001 + "," + (var11 - var0.getBlockY()) + "," + (var12 - var0.getBlockZ()) + ") 需要聚灵簇");
               return new StructureCheck(false, var3, var4);
            }

            var14 = new ResolvedBlock(var10, var11, var12, var8.material(), var15.getId());
            var4.set(var4.size() - 1, var14);
         } else {
            if (!var13.getType().name().equalsIgnoreCase(var8.material())) {
               int var19 = var10 - var0.getBlockX();
               var3.add("位置 (" + var19 + "," + (var11 - var0.getBlockY()) + "," + (var12 - var0.getBlockZ()) + ") 需要 " + var8.material());
               return new StructureCheck(false, var3, var4);
            }

            if (var8.sfId() != null) {
               SlimefunItem var17 = MachineScriptHelper.getSfItem(var13.getLocation());
               if (var17 == null || !var8.sfId().equals(var17.getId())) {
                  int var18 = var10 - var0.getBlockX();
                  var3.add("位置 (" + var18 + "," + (var11 - var0.getBlockY()) + "," + (var12 - var0.getBlockZ()) + ") 需要 " + var8.sfId());
                  return new StructureCheck(false, var3, var4);
               }
            }
         }
      }

      return new StructureCheck(var3.isEmpty(), var3, var4);
   }

   /**
    * 收集全部缺失/放错的方块（世界绝对坐标），用于给玩家展示红色提示实体。
    * 与 checkStructure 的区别：checkStructure 只报第一处错误，这里把全部都列出来。
    */
   public static List<StructureMissingHint.MissingBlock> collectMissing(Location var0, int var1) {
      World var2 = var0.getWorld();
      ArrayList var3 = new ArrayList();
      if (var2 == null) {
         return var3;
      }

      for (StructureBlock var7 : STRUCTURE) {
         Rotated var8 = rot(var7.x(), var7.z(), var1);
         int var9 = var0.getBlockX() + var8.x();
         int var10 = var0.getBlockY() + var7.y();
         int var11 = var0.getBlockZ() + var8.z();
         Location var12 = new Location(var2, (double)var9, (double)var10, (double)var11);
         Material var13 = Material.matchMaterial(var7.material());
         if (var13 != null) {
            Block var14 = var2.getBlockAt(var9, var10, var11);
            if (var7.dynamicCluster()) {
               SlimefunItem var15 = MachineScriptHelper.getSfItem(var14.getLocation());
               if (var15 == null || !ALLOWED_CLUSTERS.contains(var15.getId())) {
                  var3.add(new StructureMissingHint.MissingBlock(var12, var13, ALLOWED_CLUSTERS.get(0)));
               }
            } else if (var14.getType() != var13) {
               var3.add(new StructureMissingHint.MissingBlock(var12, var13, (String)null));
            } else if (var7.sfId() != null) {
               SlimefunItem var15 = MachineScriptHelper.getSfItem(var14.getLocation());
               String var16 = var15 == null ? null : var15.getId();
               if (var16 == null || !var7.sfId().equals(var16)) {
                  var3.add(new StructureMissingHint.MissingBlock(var12, var13, var7.sfId()));
               }
            }
         }
      }

      return var3;
   }

   public static void markTriggers(Location var0, int var1, String var2) {
      StructureCheck var3 = checkStructure(var0, var1);

      for(ResolvedBlock var5 : var3.blocks()) {
         if (var5.sfId() != null && TRIGGER_IDS.contains(var5.sfId())) {
            MachineScriptHelper.setData(new Location(var0.getWorld(), (double)var5.x(), (double)var5.y(), (double)var5.z()), "KOMUTECH_L_X_JLS_core", var2);
         }
      }

   }

   public static void clearTriggers(Location var0, int var1) {
      StructureCheck var2 = checkStructure(var0, var1);

      for(ResolvedBlock var4 : var2.blocks()) {
         if (var4.sfId() != null && TRIGGER_IDS.contains(var4.sfId())) {
            MachineScriptHelper.setData(new Location(var0.getWorld(), (double)var4.x(), (double)var4.y(), (double)var4.z()), "KOMUTECH_L_X_JLS_core", (String)null);
         }
      }

   }

   public static void consumeClusters(Location var0, Player var1) {
      World var2 = var0.getWorld();

      for(int[] var6 : CLUSTER_OFFSETS) {
         Block var7 = var2.getBlockAt(var0.getBlockX() + var6[0], var0.getBlockY() + var6[1], var0.getBlockZ() + var6[2]);
         SlimefunItem var8 = MachineScriptHelper.getSfItem(var7.getLocation());
         if (var8 != null && ALLOWED_CLUSTERS.contains(var8.getId())) {
            boolean var10000;
            switch (var8.getId()) {
               case "KOMUTECH_L_DJ_XPLJ" -> var10000 = true;
               case "KOMUTECH_L_DJ_ZPLJ" -> var10000 = Math.random() < 0.6;
               case "KOMUTECH_L_DJ_SPLJ" -> var10000 = Math.random() < 0.3;
               default -> var10000 = false;
            }

            boolean var9 = var10000;
            if (var9) {
               var7.setType(Material.AIR);
            }
         }
      }

   }

   public static Map<String, Object> rollAttributes(Map<String, Object> var0) {
      Object var1;
      if (var0 != null && PlayerAttributeStore.getBoolean(var0, "定向", false)) {
         var1 = PlayerAttributeStore.deepCopy(var0);
         ((Map)var1).remove("定向");
         completeLinggenFields((Map)var1);
      } else {
         var1 = new LinkedHashMap();
         double var3 = Math.random();
         byte var2;
         if (var3 < 0.2) {
            var2 = 1;
         } else if (var3 < 0.4) {
            var2 = 2;
         } else if (var3 < 0.6) {
            var2 = 3;
         } else if (var3 < 0.8) {
            var2 = 4;
         } else {
            var2 = 5;
         }

         ArrayList var5 = new ArrayList();

         while(var5.size() < var2) {
            String var6 = (String)ALL_ELEMENTS.get((int)(Math.random() * (double)ALL_ELEMENTS.size()));
            if (!var5.contains(var6)) {
               var5.add(var6);
            }
         }

         List var10001 = ALL_ELEMENTS;
         Objects.requireNonNull(var10001);
         var5.sort(Comparator.comparingInt(var10001::indexOf));
         double var11 = (double)Math.round((0.1 + Math.random() * 0.9) * (double)100.0F) / (double)100.0F;
         List var8 = genQuality(var5, var11);
         LinkedHashMap var9 = new LinkedHashMap();

         for(int var10 = 0; var10 < var5.size(); ++var10) {
            var9.put((String)var5.get(var10), var8.get(var10));
         }

         ((Map)var1).put("灵根", String.join("、", var5));
         ((Map)var1).put("灵根品质", var9);
         ((Map)var1).put("总品质", var11);
         ((Map)var1).put("灵根属性", CultivationMath.computeLinggenAttr(var5, var11));
      }

      ((Map)var1).put("修为", "『引气入体』");
      ((Map)var1).put("灵气", "0/100");
      ((Map)var1).put("灵力", "100/100+0");
      ((Map)var1).put("功德", 0);
      ((Map)var1).put("煞气", 0);
      ((Map)var1).put("悟性", (double)Math.round(((double)1.0F + Math.random() * (double)4.0F) * (double)100.0F) / (double)100.0F);
      ((Map)var1).put("根骨", (int)(Math.floor(Math.random() * (double)10.0F) + (double)1.0F));
      ((Map)var1).put("血量", (int)(Math.floor(Math.random() * (double)10.0F) + (double)1.0F));
      ((Map)var1).put("攻击力", (int)(Math.floor(Math.random() * (double)10.0F) + (double)1.0F));
      ((Map)var1).put("防御力", (int)(Math.floor(Math.random() * (double)10.0F) + (double)1.0F));
      ((Map)var1).put("速度", (int)(Math.floor(Math.random() * (double)10.0F) + (double)1.0F));
      ((Map)var1).put("灵识", 0);
      ((Map)var1).put("灵气获取", 0);
      ((Map)var1).put("灵气获取_实际", (double)100.0F);
      ((Map)var1).put("属性点", 3);
      XiDianZhuScript.captureBase((Map)var1);
      return (Map<String, Object>)var1;
   }

   public static void completeLinggenFields(Map<String, Object> var0) {
      List var1 = CultivationMath.splitLinggen(PlayerAttributeStore.getString(var0, "灵根", ""));
      if (!var1.isEmpty()) {
         double var2 = PlayerAttributeStore.getDouble(var0, "总品质", 0.01);
         if (!var0.containsKey("灵根品质")) {
            LinkedHashMap var4 = new LinkedHashMap();
            List var5 = genQuality(var1, var2);

            for(int var6 = 0; var6 < var1.size(); ++var6) {
               var4.put((String)var1.get(var6), var5.get(var6));
            }

            var0.put("灵根品质", var4);
         }

         String var7 = PlayerAttributeStore.getString(var0, "灵根属性", "");
         if (var7.isBlank() || "未知".equals(var7)) {
            var0.put("灵根属性", CultivationMath.computeLinggenAttr(var1, var2));
         }

      }
   }

   public static boolean completeLinggenFieldsIfNeeded(Map<String, Object> var0) {
      if (!var0.containsKey("灵根")) {
         return false;
      } else {
         boolean var1 = !var0.containsKey("灵根品质") || !var0.containsKey("灵根属性") || "未知".equals(PlayerAttributeStore.getString(var0, "灵根属性", ""));
         if (!var1) {
            return false;
         } else {
            completeLinggenFields(var0);
            return true;
         }
      }
   }

   public static List<String> elementsFromData(Map<String, Object> var0) {
      String var1 = PlayerAttributeStore.getString(var0, "灵根", "");
      return var1.isBlank() ? List.of() : List.of(var1.split("、"));
   }

   public static List<Double> qualitiesFromData(Map<String, Object> var0) {
      List<String> var1 = elementsFromData(var0);
      if (var1.isEmpty()) {
         return List.of();
      } else {
         Object var2 = var0.get("灵根品质");
         if (var2 instanceof Map) {
            Map<String, Object> var3 = (Map<String, Object>)var2;
            ArrayList var4 = new ArrayList();

            for(String var6 : var1) {
               Object var7 = var3.get(var6);
               Double var10001;
               if (var7 instanceof Number) {
                  Number var8 = (Number)var7;
                  var10001 = var8.doubleValue();
               } else {
                  var10001 = 0.01;
               }

               var4.add(var10001);
            }

            return var4;
         } else {
            return var1.stream().map((var0x) -> 0.01).toList();
         }
      }
   }

   public static void startEffect(Plugin var0, final Location var1, List<String> var2, List<Double> var3, final Runnable var4) {
      final World var5 = var1.getWorld();
      if (var5 == null) {
         var4.run();
      } else {
         LinkedHashMap<String, Double> var6 = new LinkedHashMap<>();

         for(int var7 = 0; var7 < var2.size(); ++var7) {
            var6.put((String)var2.get(var7), (Double)var3.get(var7));
         }

         final ArrayList<ElementTrail> var20 = new ArrayList<>();

         for(int var8 = 0; var8 < ALL_ELEMENTS.size(); ++var8) {
            String var9 = (String)ALL_ELEMENTS.get(var8);
            String var10 = findMutatedElement(var2, var9);
            boolean var11 = var2.contains(var9);
            double var12 = var11 ? (Double)var6.getOrDefault(var9, (double)0.0F) : (var10 != null ? (Double)var6.getOrDefault(var10, (double)0.0F) : (double)0.0F);
            float var14 = 1.0F + 3.0F * (float)var12;
            int var15 = Math.max(1, (int)Math.floor((double)2.0F + (double)6.0F * var12 + (double)0.5F));
            double var16 = (double)(var8 * 2) * Math.PI / (double)ALL_ELEMENTS.size();
            Color var18 = (Color)BASE_COLORS.get(var9);
            Color var19 = var10 != null ? (Color)MUTATED_COLORS.get(var10) : null;
            var20.add(new ElementTrail(var11 || var10 != null, var14, var15, var16, var18, var19, var12));
         }

         var5.playSound(var1.clone().add((double)0.0F, (double)4.0F, (double)0.0F), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 1.0F);
         final Location var21 = var1.clone().add((double)0.5F, (double)0.5F, (double)0.5F);
         (new BukkitRunnable() {
            int tick = 0;

            public void run() {
               if (this.tick > 200) {
                  var5.playSound(var1.clone().add((double)0.0F, (double)4.0F, (double)0.0F), Sound.BLOCK_BEACON_ACTIVATE, 1.0F, 1.0F);
                  this.cancel();
                  var4.run();
               } else if (!ParticleBudget.canRunHeavyEffect(var5)) {
                  ++this.tick;
               } else {
                  double var1x = (double)this.tick / (double)200.0F;
                  double var3 = (double)this.tick / (double)20.0F;

                  for(ElementTrail var6 : var20) {
                     double var7 = (double)3.0F * ((double)1.0F - var1x);
                     float var9 = var6.size();
                     if (var6.effectiveQuality() == (double)0.0F) {
                        var7 *= (double)1.0F - var1x;
                     }

                     if (!(var7 < 0.05)) {
                        double var10 = (double)4.0F + Math.sin(var3 * 0.8 + var6.angleOffset()) * (double)1.5F * ((double)1.0F - var1x);
                        Color var12 = var6.baseColor();
                        if (var6.mutatedColor() != null) {
                           double var13 = var1x > 0.1 ? Math.min((double)1.0F, (var1x - 0.1) / 0.15) : (double)0.0F;
                           var12 = JianLingShiSupport.interpolateColor(var6.baseColor(), var6.mutatedColor(), var13);
                        }

                        int var31 = ParticleBudget.scaleTrailPoints(16);

                        for(int var14 = 0; var14 < var31; ++var14) {
                           double var15 = (double)var14 * 0.05;
                           double var17 = var3 + var15;
                           double var19 = var7 + Math.sin(var17 * 0.3) * (double)0.5F * ((double)1.0F - var1x);
                           double var21x = var10 + Math.sin(var17 * 0.8) * (double)1.5F * ((double)1.0F - var1x) * (double)0.5F;
                           double var23 = var17 * (double)2.5F + var6.angleOffset();
                           Location var25 = new Location(var5, var21.getX() + var19 * Math.cos(var23), var1.getY() + var21x, var21.getZ() + var19 * Math.sin(var23));
                           int var26 = var6.count();
                           if (var6.effectiveQuality() == (double)0.0F) {
                              var26 = Math.max(1, (int)Math.floor((double)var26 * ((double)1.0F - var1x)));
                           }

                           Particle.DustOptions var27 = new Particle.DustOptions(var12, var9);
                           ParticleBudget.spawnDust(var5, var25, var27, var26);
                        }
                     }
                  }

                  int var28 = 3 + (int)Math.floor(Math.random() * (double)4.0F);

                  for(int var29 = 0; var29 < var28; ++var29) {
                     Location var30 = var21.clone().add((Math.random() - (double)0.5F) * (double)8.0F, Math.random() * (double)5.0F, (Math.random() - (double)0.5F) * (double)8.0F);
                     ParticleBudget.spawnEndRod(var5, var30, (Math.random() - (double)0.5F) * 0.2, 0.1 + Math.random() * 0.4, (Math.random() - (double)0.5F) * 0.2, (double)0.5F, 1);
                  }

                  ++this.tick;
               }
            }
         }).runTaskTimer(var0, 0L, 1L);
      }
   }

   private static String findMutatedElement(List<String> var0, String var1) {
      for(String var3 : var0) {
         if (var1.equals(MUTATION_MAP.get(var3))) {
            return var3;
         }
      }

      return null;
   }

   private static List<Double> genQuality(List<String> var0, double var1) {
      double var3 = 0.01 * (double)var0.size();
      if (var1 < var3) {
         var1 = var3;
      }

      double var5 = var1 - var3;
      double[] var7 = new double[var0.size()];
      double var8 = (double)0.0F;

      for(int var10 = 0; var10 < var0.size(); ++var10) {
         var7[var10] = Math.random();
         var8 += var7[var10];
      }

      ArrayList var14 = new ArrayList();

      for(int var11 = 0; var11 < var0.size(); ++var11) {
         double var12 = 0.01 + var7[var11] / var8 * var5;
         var14.add((double)Math.round(var12 * (double)100.0F) / (double)100.0F);
      }

      return var14;
   }

   private static Color interpolateColor(Color var0, Color var1, double var2) {
      return Color.fromRGB((int)Math.floor((double)var0.getRed() + (double)(var1.getRed() - var0.getRed()) * var2), (int)Math.floor((double)var0.getGreen() + (double)(var1.getGreen() - var0.getGreen()) * var2), (int)Math.floor((double)var0.getBlue() + (double)(var1.getBlue() - var0.getBlue()) * var2));
   }

   public static void showHologram(final Plugin var0, Location var1, Player var2, String var3) {
      World var4 = var1.getWorld();
      if (var4 != null) {
         Location var5 = var2.getLocation();
         double var6 = var5.getX() - var1.getX();
         double var8 = var5.getZ() - var1.getZ();
         double var10 = (double)0.0F;
         double var12 = (double)0.0F;
         if (Math.abs(var6) >= Math.abs(var8)) {
            var10 = var6 >= (double)0.0F ? (double)3.0F : (double)-3.0F;
         } else {
            var12 = var8 >= (double)0.0F ? (double)3.0F : (double)-3.0F;
         }

         Location var14 = var1.clone().add(var10, (double)4.0F, var12);
         final TextDisplay var15 = DisplayReflectionHelper.spawnTextDisplay(var14, "§f§l你的灵根为：", 2.0F);
         final String var16 = "§f§l你的灵根为：";
         final String var17 = "§6§l" + var3;
         (new BukkitRunnable() {
            int index = 0;

            public void run() {
               if (var15.isDead()) {
                  this.cancel();
               } else if (this.index >= var17.length()) {
                  this.cancel();
                  var0.getServer().getScheduler().runTaskLater(var0, () -> {
                     if (!var15.isDead()) {
                        var15.remove();
                     }

                  }, 200L);
               } else {
                  String var10001 = var16;
                  var15.setText(var10001 + var17.substring(0, this.index + 1));
                  ++this.index;
               }
            }
         }).runTaskTimer(var0, 0L, 3L);
      }
   }

   public static String locationKey(Location var0) {
      String var10000 = var0.getWorld().getName();
      return var10000 + "," + var0.getBlockX() + "," + var0.getBlockY() + "," + var0.getBlockZ();
   }

   private static StructureBlock[] buildStructure() {
      ArrayList var0 = new ArrayList();
      var0.add(new StructureBlock(0, 0, 0, "SHROOMLIGHT", "KOMUTECH_L_X_鉴灵石核心", false));

      for(int[] var4 : CLUSTER_OFFSETS) {
         var0.add(new StructureBlock(var4[0], var4[1], var4[2], "AIR", (String)null, true));
      }

      for(int var5 = 3; var5 <= 5; ++var5) {
         for(int var6 = -1; var6 <= 1; ++var6) {
            for(int var7 = -1; var7 <= 1; ++var7) {
               if (var6 == 0 && var7 == 0) {
                  var0.add(new StructureBlock(0, var5, 0, "BLUE_STAINED_GLASS", (String)null, false));
               } else if (Math.abs(var6) == 1 && Math.abs(var7) == 1) {
                  var0.add(new StructureBlock(var6, var5, var7, "LIGHT_BLUE_STAINED_GLASS_PANE", "KOMUTECH_L_X_鉴灵石框架板", false));
               } else {
                  var0.add(new StructureBlock(var6, var5, var7, "LIGHT_BLUE_STAINED_GLASS", "KOMUTECH_L_X_鉴灵石框架", false));
               }
            }
         }
      }

      addPlatformRow(var0, -3, new String[]{"ANDESITE_WALL", "POLISHED_ANDESITE_SLAB", "POLISHED_ANDESITE_STAIRS", "POLISHED_ANDESITE_STAIRS", "POLISHED_ANDESITE_STAIRS", "POLISHED_ANDESITE_SLAB", "ANDESITE_WALL"});
      addPlatformRow(var0, -2, new String[]{"POLISHED_ANDESITE_SLAB", "DIORITE", "ANDESITE", "POLISHED_ANDESITE", "ANDESITE", "DIORITE", "POLISHED_ANDESITE_SLAB"});
      addPlatformRow(var0, -1, new String[]{"POLISHED_ANDESITE_STAIRS", "ANDESITE", "ANDESITE_SLAB", "POLISHED_ANDESITE_STAIRS", "ANDESITE_SLAB", "ANDESITE", "POLISHED_ANDESITE_STAIRS"});
      addPlatformRow(var0, 0, new String[]{"POLISHED_ANDESITE_STAIRS", "POLISHED_ANDESITE", "POLISHED_ANDESITE_STAIRS", null, "POLISHED_ANDESITE_STAIRS", "POLISHED_ANDESITE", "POLISHED_ANDESITE_STAIRS"});
      addPlatformRow(var0, 1, new String[]{"POLISHED_ANDESITE_STAIRS", "ANDESITE", "ANDESITE_SLAB", "POLISHED_ANDESITE_STAIRS", "ANDESITE_SLAB", "ANDESITE", "POLISHED_ANDESITE_STAIRS"});
      addPlatformRow(var0, 2, new String[]{"POLISHED_ANDESITE_SLAB", "DIORITE", "ANDESITE", "POLISHED_ANDESITE", "ANDESITE", "DIORITE", "POLISHED_ANDESITE_SLAB"});
      addPlatformRow(var0, 3, new String[]{"ANDESITE_WALL", "POLISHED_ANDESITE_SLAB", "POLISHED_ANDESITE_STAIRS", "POLISHED_ANDESITE_STAIRS", "POLISHED_ANDESITE_STAIRS", "POLISHED_ANDESITE_SLAB", "ANDESITE_WALL"});
      var0.add(new StructureBlock(-3, 1, -3, "CHISELED_TUFF", (String)null, false));
      var0.add(new StructureBlock(-3, 1, 3, "CHISELED_TUFF", (String)null, false));
      var0.add(new StructureBlock(3, 1, -3, "CHISELED_TUFF", (String)null, false));
      var0.add(new StructureBlock(3, 1, 3, "CHISELED_TUFF", (String)null, false));
      addLanternRing(var0, 2);
      addLanternRing(var0, 6);
      var0.add(new StructureBlock(0, 7, 0, "GRINDSTONE", (String)null, false));
      var0.add(new StructureBlock(0, 8, 0, "END_ROD", (String)null, false));
      return (StructureBlock[])var0.toArray((var0x) -> new StructureBlock[var0x]);
   }

   private static void addPlatformRow(List<StructureBlock> var0, int var1, String[] var2) {
      int[] var3 = new int[]{-3, -2, -1, 0, 1, 2, 3};

      for(int var4 = 0; var4 < var3.length; ++var4) {
         if (var2[var4] != null) {
            var0.add(new StructureBlock(var3[var4], 0, var1, var2[var4], (String)null, false));
         }
      }

   }

   private static void addLanternRing(List<StructureBlock> var0, int var1) {
      for(int var2 = -1; var2 <= 1; ++var2) {
         for(int var3 = -1; var3 <= 1; ++var3) {
            if (var2 == 0 && var3 == 0) {
               var0.add(new StructureBlock(0, var1, 0, "SEA_LANTERN", (String)null, false));
            } else {
               var0.add(new StructureBlock(var2, var1, var3, "POLISHED_ANDESITE_STAIRS", (String)null, false));
            }
         }
      }

   }

   public static record StructureBlock(int x, int y, int z, String material, String sfId, boolean dynamicCluster) {
   }

   public static record ResolvedBlock(int x, int y, int z, String material, String sfId) {
   }

   public static record StructureCheck(boolean valid, List<String> errors, List<ResolvedBlock> blocks) {
   }

   public static record Rotated(int x, int z) {
   }

   private static record ElementTrail(boolean active, float size, int count, double angleOffset, Color baseColor, Color mutatedColor, double effectiveQuality) {
   }
}
