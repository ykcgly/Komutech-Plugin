package tech.komutech.native_scripts.combat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import tech.komutech.native_scripts.support.KomutechConfigMerge;
import tech.komutech.native_scripts.support.KomutechJson;
import tech.komutech.native_scripts.support.KomutechPaths;
import tech.komutech.native_scripts.support.KomutechSupport;
import tech.komutech.native_scripts.support.ParticleBudget;
import tech.komutech.native_scripts.support.PlayerAttributeStore;

public final class ScrollCombatEngine {
   private static Map<String, Object> scrollConfig;
   private static Map<String, Object> staffConfig;
   private static long scrollConfigMtime = Long.MIN_VALUE;
   private static long staffConfigMtime = Long.MIN_VALUE;
   private static final Map<String, Long> scrollReadyAtMs = new HashMap<>();
   private static final Map<UUID, Long> staffReadyAtMs = new HashMap<>();
   private static final long DOUBLE_CLICK_WINDOW_MS = 400L;
   private static final long ATTACK_DELAY_TICKS = 8L;
   private static final int DEFAULT_MIN_CD_TICKS = 20;
   private static final int DEFAULT_POST_CAST_GAP_TICKS = 20;
   private static final Map<UUID, Long> firstRightClick = new HashMap<>();
   private static final Map<UUID, Integer> pendingAttackTasks = new HashMap<>();
   private static final String SCROLL_CFG_RESOURCE = "addon_configs/Komutech/卷轴属性.json";
   private static final String STAFF_CFG_RESOURCE = "addon_configs/Komutech/灵杖属性.json";

   private static Path scrollConfigPath() {
      return KomutechPaths.scrollConfig();
   }

   private static Path staffConfigPath() {
      return KomutechPaths.staffConfig();
   }

   private static Path scrollDataDir() {
      return KomutechPaths.yunZhuanXia();
   }

   private ScrollCombatEngine() {
   }

   public static void castScroll(Player var0, ItemStack var1, Map<String, Object> var2, String var3) {
      String var4 = normalizeSkillId(var3);
      ensureScrollConfig();
      Map var5 = KomutechJson.asMap(scrollConfig.get("公共规则"));
      Map var6 = KomutechJson.asMap(scrollConfig.get(var4));
      if (var6.isEmpty()) {
         reloadScrollConfig();
         var5 = KomutechJson.asMap(scrollConfig.get("公共规则"));
         var6 = KomutechJson.asMap(scrollConfig.get(var4));
      }

      if (var6.isEmpty()) {
         KomutechSupport.send(var0, "§c卷轴配置缺失: " + var4);
      } else {
         double var7 = KomutechJson.getDouble(var6, "基础伤害", 1.0);
         double var9 = KomutechJson.getDouble(var6, "射程", 16.0);
         int var11 = KomutechJson.getInt(var6, "基础灵力消耗", 1);
         int var12 = KomutechJson.getInt(var6, "基础冷却", 20);
         int var13 = KomutechJson.getInt(var6, "熟练度上限", 1000);
         Map var14 = KomutechJson.asMap(var5.get("公式参数"));
         double var15 = realmCoef(var2, KomutechJson.asMap(var5.get("修为倍率")));
         double var17 = KomutechJson.getDouble(KomutechJson.asMap(var5.get("灵根属性倍率")), PlayerAttributeStore.getString(var2, "灵根属性", "单灵根"), 1.0);
         double var19 = PlayerAttributeStore.getDouble(var2, "攻击力_实际", 1.0);
         double var21 = getDamageScale(var1, KomutechJson.asMap(var5.get("品阶伤害倍率")));
         int var23 = getProficiency(var0.getName(), var4);
         double var24 = 1.0 + Math.floor(var23 / 100.0) * KomutechJson.getDouble(var14, "熟练度增伤因子", 0.05);
         double var26 = var19 * var21 * var15 * var17 * var7 * var24;
         if (var26 <= 0.0) {
            var26 = 5.0;
         }

         double var28 = PlayerAttributeStore.getDouble(var2, "根骨", 1.0);
         double var30 = 1.0 + Math.floor(var23 / 100.0) * KomutechJson.getDouble(var14, "熟练度增耗因子", 0.02);
         int var32 = Math.max(1, (int)Math.round(var11 * var15 * var17 * var28 * var30));
         double var33 = PlayerAttributeStore.getDouble(var2, "悟性", 1.0);
         double var35 = Math.min(KomutechJson.getDouble(var14, "悟性最大减免比例", 0.5), (var33 - 1.0) * KomutechJson.getDouble(var14, "冷却减免因子_悟性", 0.15));
         double var37 = Math.min(
            KomutechJson.getDouble(var14, "熟练度最大减免比例", 0.4), Math.floor(var23 / 100.0) * KomutechJson.getDouble(var14, "冷却减免因子_熟练度", 0.015)
         );
         int var39 = KomutechJson.getInt(var14, "最小冷却", 20);
         int var40 = KomutechJson.getInt(var14, "释放后额外冷却", 20);
         int var41 = Math.max(var39, (int)Math.floor(var12 * (1.0 - Math.min(0.7, var35 + var37))));
         int var42 = continuousCastLockTicks(var4);
         int var43 = Math.max(var41, var42 + var40);
         String var44 = var0.getUniqueId() + "_" + var4;
         long var45 = System.currentTimeMillis();
         long var47 = scrollReadyAtMs.getOrDefault(var44, 0L);
         if (var45 < var47) {
            double var51 = (var47 - var45) / 1000.0;
            KomutechSupport.actionBar(var0, "§c✖ 冷却中！剩余 §6" + String.format("%.1f", var51) + " §c秒");
         } else {
            PlayerAttributeStore.Lingli var49 = PlayerAttributeStore.parseLingli(var2.get("灵力"));
            if (var49.current() < var32) {
               KomutechSupport.actionBar(var0, "§c灵力不足！当前 §6" + String.format("%.2f", var49.current()) + "§c / 需要 §6" + var32);
            } else {
               var2.put("灵力", PlayerAttributeStore.formatLingli(new PlayerAttributeStore.Lingli(var49.current() - var32, var49.max(), var49.bonus())));
               scrollReadyAtMs.put(var44, var45 + var43 * 50L);
               KomutechSupport.actionBar(var0, "§a消耗 " + var32 + " 灵力");
               int var50 = addProficiency(var0.getName(), var4, ThreadLocalRandom.current().nextInt(1, 11), var13);
               if (var50 > 0) {
                  KomutechSupport.send(var0, "§b熟练度 +" + var50 + " §e" + var4);
               }

               var0.getWorld().playSound(var0.getLocation(), "entity.evoker.cast_spell", 1.0F, 1.0F);
               ScrollSpecialEffects.execute(var4, var0, var2, var26, var9, KomutechJson.asList(var5.get("白名单生物")), KomutechJson.asList(var5.get("功德变化范围")));
            }
         }
      }
   }

   private static int continuousCastLockTicks(String var0) {
      return switch (var0) {
         case "五行必杀" -> 80;
         case "御龙护身决" -> 250;
         case "寒霜锁" -> 35;
         case "星陨劫" -> 45;
         case "游龙惊鸿诀" -> 40;
         case "碎玉闪" -> 8;
         default -> 0;
      };
   }

   public static String normalizeSkillId(String var0) {
      if (var0 != null && !var0.isBlank()) {
         String var1 = var0.trim();
         String var2 = var1.toUpperCase(Locale.ROOT);
         String var3 = "KOMUTECH_L_JZ_";
         return var2.startsWith(var3) && var1.length() >= var3.length() ? var1.substring(var3.length()) : var1;
      } else {
         return "";
      }
   }

   static int getIntAttr(Map<String, Object> var0, String var1, int var2) {
      return PlayerAttributeStore.getInt(var0, var1, var2);
   }

   public static void staffAttack(Player var0, ItemStack var1, Map<String, Object> var2, String var3) {
      if (tryStaffAttack(var0, var1, var2, var3)) {
         switch (var3) {
            case "蜉蝣梦":
               StaffSpecialAttacks.fuyouMeng(var0, var1, var2);
               break;
            case "空引津":
               StaffSpecialAttacks.kongYinJin(var0, var1, var2);
               break;
            case "归墟":
               StaffSpecialAttacks.guiXu(var0, var1, var2);
               break;
            case "烧火棍":
               staffRayAttack(var0, var2, var1, var3, Color.fromRGB(255, 255, 255), Color.fromRGB(255, 255, 255), 1.0F, 0.2);
               break;
            default:
               staffRayAttack(var0, var2, var1, var3, Color.fromRGB(0, 255, 255), Color.fromRGB(0, 255, 77), 1.0F, 0.1);
         }
      }
   }

   public static void staffRayAttack(Player var0, Map<String, Object> var1, ItemStack var2, String var3, Color var4, Color var5, float var6) {
      staffRayAttack(var0, var1, var2, var3, var4, var5, var6, 0.25);
   }

   public static void staffRayAttack(Player var0, Map<String, Object> var1, ItemStack var2, String var3, Color var4, Color var5, float var6, double var7) {
      ensureStaffConfig();
      double var9 = staffMaxDistance(var3);
      double var11 = calcStaffDamage(var0, var2, var1);
      rayDamage(
         var0, var1, var2, var11, var9, KomutechJson.asList(staffConfig.get("白名单生物")), KomutechJson.asList(staffConfig.get("功德变化范围")), var4, var5, var6, var7
      );
   }

   public static boolean tryStaffAttack(Player var0, ItemStack var1, Map<String, Object> var2, String var3) {
      ensureStaffConfig();
      Map var4 = KomutechJson.asMap(staffConfig.get("灵杖"));
      Map var5 = KomutechJson.asMap(staffConfig.get(var3));
      if (var5.isEmpty() && !"灵杖".equals(var3) && !"烧火棍".equals(var3)) {
         reloadStaffConfig();
         var5 = KomutechJson.asMap(staffConfig.get(var3));
      }

      int var6 = KomutechJson.getInt(var5.isEmpty() ? var4 : var5, "基础冷却", 20);
      int var7 = KomutechJson.getInt(var5.isEmpty() ? var4 : var5, "基础消耗", 3);
      Map var8 = KomutechJson.asMap(staffConfig.get("公式参数"));
      double var9 = PlayerAttributeStore.getDouble(var2, "悟性", 1.0);
      double var11 = KomutechJson.getDouble(var8, "冷却最大减免比例", 0.5);
      double var13 = Math.min(var11, Math.max(0.0, (var9 - 1.0) * KomutechJson.getDouble(var8, "冷却减免因子", 0.08)));
      int var15 = KomutechJson.getInt(var8, "最小冷却", 20);
      int var16 = KomutechJson.getInt(var8, "释放后额外冷却", 20);
      int var17 = Math.max(var15, (int)Math.floor(var6 * (1.0 - var13)));
      int var18 = staffEffectTicks(var3);
      int var19 = Math.max(var17, var18 + var16);
      int var20 = Math.max(
         1,
         (int)Math.round(
            var7
               * realmCoef(var2, KomutechJson.asMap(staffConfig.get("修为倍率")))
               * KomutechJson.getDouble(KomutechJson.asMap(staffConfig.get("灵根属性倍率")), PlayerAttributeStore.getString(var2, "灵根属性", "单灵根"), 1.0)
               * PlayerAttributeStore.getDouble(var2, "根骨", 1.0)
               * KomutechJson.getDouble(var8, "根骨消耗因子", 0.1)
         )
      );
      long var21 = System.currentTimeMillis();
      long var23 = staffReadyAtMs.getOrDefault(var0.getUniqueId(), 0L);
      if (var21 < var23) {
         double var27 = (var23 - var21) / 1000.0;
         KomutechSupport.actionBar(var0, "§c✖ 冷却中！剩余 §6" + String.format("%.1f", var27) + " §c秒");
         return false;
      } else {
         PlayerAttributeStore.Lingli var25 = PlayerAttributeStore.parseLingli(var2.get("灵力"));
         if (var25.current() < var20) {
            KomutechSupport.actionBar(var0, "§c灵力不足！当前 §6" + String.format("%.2f", var25.current()) + "§c / 需要 §6" + var20);
            return false;
         } else {
            var2.put("灵力", PlayerAttributeStore.formatLingli(new PlayerAttributeStore.Lingli(var25.current() - var20, var25.max(), var25.bonus())));
            staffReadyAtMs.put(var0.getUniqueId(), var21 + var19 * 50L);
            KomutechSupport.actionBar(var0, "§a消耗 " + var20 + " 灵力 §7| §f剩余 " + String.format("%.0f", var25.current() - var20) + "/" + (long)var25.totalMax());
            var0.getWorld().playSound(var0.getLocation(), "entity.evoker.cast_spell", 1.0F, 1.0F);
            return true;
         }
      }
   }

   private static int staffEffectTicks(String var0) {
      return switch (var0) {
         case "归墟" -> 9;
         case "花葬" -> 40;
         case "蜉蝣梦" -> 45;
         case "空引津" -> 16;
         default -> 0;
      };
   }

   public static int calcGenericSpiritCost(Map<String, Object> var0, double var1, int var3, double var4) {
      if (var3 <= 0) {
         return 0;
      } else {
         ensureStaffConfig();
         Map var6 = KomutechJson.asMap(staffConfig.get("公式参数"));
         double var7 = KomutechJson.getDouble(var6, "根骨消耗因子", 0.1) * var4;
         double var9 = realmCoef(var0, KomutechJson.asMap(staffConfig.get("修为倍率")));
         double var11 = KomutechJson.getDouble(KomutechJson.asMap(staffConfig.get("灵根属性倍率")), PlayerAttributeStore.getString(var0, "灵根属性", "单灵根"), 1.0);
         double var13 = PlayerAttributeStore.getDouble(var0, "根骨", 1.0);
         return Math.max(1, (int)Math.ceil(var1 * var3 * var9 * var11 * Math.max(0.5, var13 * var7)));
      }
   }

   public static boolean tryConsumeSpiritPower(Player var0, Map<String, Object> var1, int var2) {
      if (var2 <= 0) {
         return true;
      } else {
         PlayerAttributeStore.Lingli var3 = PlayerAttributeStore.parseLingli(var1.get("灵力"));
         if (var3.current() < var2) {
            KomutechSupport.actionBar(var0, "§c灵力不足！需要 §6" + var2 + " §c，当前 §6" + String.format("%.0f", var3.current()));
            return false;
         } else {
            var1.put("灵力", PlayerAttributeStore.formatLingli(new PlayerAttributeStore.Lingli(var3.current() - var2, var3.max(), var3.bonus())));
            return true;
         }
      }
   }

   public static double calcStaffDamage(Player var0, ItemStack var1, Map<String, Object> var2) {
      ensureStaffConfig();
      return PlayerAttributeStore.getDouble(var2, "攻击力_实际", 0.0)
         * getDamageScale(var1, KomutechJson.asMap(staffConfig.get("伤害倍率")))
         * realmCoef(var2, KomutechJson.asMap(staffConfig.get("修为倍率")))
         * KomutechJson.getDouble(KomutechJson.asMap(staffConfig.get("灵根属性倍率")), PlayerAttributeStore.getString(var2, "灵根属性", "单灵根"), 1.0);
   }

   public static double staffMaxDistance(String var0) {
      ensureStaffConfig();
      Map var1 = KomutechJson.asMap(staffConfig.get("灵杖"));
      Map var2 = KomutechJson.asMap(staffConfig.get(var0));
      return KomutechJson.getDouble(var2.isEmpty() ? var1 : var2, "射程", 15.0);
   }

   public static void switchScroll(Player var0, ItemStack var1) {
      List var2 = getOwnedScrollIds(var0.getName());
      if (var2.isEmpty()) {
         KomutechSupport.send(var0, "§c云篆匣中没有卷轴");
      } else {
         String var3 = getBoundSkill(var1);
         int var4 = -1;

         for (int var5 = 0; var5 < var2.size(); var5++) {
            if (extractSkillName((String)var2.get(var5)).equals(var3)) {
               var4 = var5;
               break;
            }
         }

         String var7 = (String)var2.get((var4 + 1) % var2.size());
         String var6 = extractSkillName(var7);
         setBoundSkill(var1, var6);
         KomutechSupport.send(var0, "§a切换卷轴: §f" + var6);
      }
   }

   public static void applyMeritChange(Map<String, Object> var0, String var1) {
      meritChangeDelta(var0, var1);
   }

   public static int meritChangeDelta(Map<String, Object> var0, String var1) {
      ensureStaffConfig();
      List var2 = KomutechJson.asList(staffConfig.get("白名单生物"));
      List var3 = KomutechJson.asList(staffConfig.get("功德变化范围"));
      int var4 = var3.size() >= 2 ? parseIntValue(var3.get(0), 1) : 1;
      int var5 = var3.size() >= 2 ? parseIntValue(var3.get(1), 10) : 10;
      int var6 = ThreadLocalRandom.current().nextInt(var4, var5 + 1);
      boolean var7 = var2.stream().anyMatch(var1x -> var1.equals(String.valueOf(var1x)));
      if (var7) {
         var0.put("煞气", PlayerAttributeStore.getInt(var0, "煞气", 0) + var6);
         var0.put("功德", Math.max(0, PlayerAttributeStore.getInt(var0, "功德", 0) - var6));
         return -var6;
      } else {
         var0.put("功德", PlayerAttributeStore.getInt(var0, "功德", 0) + var6);
         return var6;
      }
   }

   public static boolean handleStaffRightClick(Player var0, Runnable var1) {
      UUID var2 = var0.getUniqueId();
      long var3 = System.currentTimeMillis();
      Long var5 = firstRightClick.get(var2);
      if (var5 != null && var3 - var5 <= 400L) {
         firstRightClick.remove(var2);
         cancelPendingAttack(var2);
         return true;
      } else {
         firstRightClick.put(var2, var3);
         cancelPendingAttack(var2);
         Plugin var6 = KomutechSupport.plugin();
         if (var6 == null) {
            var1.run();
            return false;
         } else {
            int var7 = Bukkit.getScheduler().runTaskLater(var6, () -> {
               pendingAttackTasks.remove(var2);
               firstRightClick.remove(var2);
               if (var0.isOnline()) {
                  var1.run();
               }
            }, 8L).getTaskId();
            pendingAttackTasks.put(var2, var7);
            return false;
         }
      }
   }

   private static void cancelPendingAttack(UUID var0) {
      Integer var1 = pendingAttackTasks.remove(var0);
      if (var1 != null) {
         Bukkit.getScheduler().cancelTask(var1);
      }
   }

   public static String getBoundSkill(ItemStack var0) {
      if (var0 != null && var0.hasItemMeta()) {
         List<String> var1 = var0.getItemMeta().getLore();
         if (var1 == null) {
            return null;
         } else {
            for (String var3 : var1) {
               if (var3.startsWith("§b绑定卷轴：")) {
                  return var3.substring("§b绑定卷轴：§f".length()).trim();
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   public static void setBoundSkill(ItemStack var0, String var1) {
      ItemMeta var2 = var0.getItemMeta();
      ArrayList var3 = var2.getLore() == null ? new ArrayList() : new ArrayList(var2.getLore());
      String var4 = "§b绑定卷轴：§f" + var1;
      boolean var5 = false;

      for (int var6 = 0; var6 < var3.size(); var6++) {
         if (((String)var3.get(var6)).startsWith("§b绑定卷轴：")) {
            var3.set(var6, var4);
            var5 = true;
            break;
         }
      }

      if (!var5) {
         var3.add(var4);
      }

      var2.setLore(var3);
      var0.setItemMeta(var2);
   }

   private static void rayDamage(
      Player var0,
      Map<String, Object> var1,
      ItemStack var2,
      double var3,
      double var5,
      List<Object> var7,
      List<Object> var8,
      Color var9,
      Color var10,
      float var11,
      double var12
   ) {
      Location var14 = var0.getEyeLocation();
      Vector var15 = var14.getDirection();
      Location var16 = var14.clone().add(var15);
      ParticleBudget.spawnRayTrail(var0.getWorld(), var16, var15, var5, var9, var10, var11, var12);
      RayTraceResult var17 = var0.getWorld()
         .rayTrace(
            var16,
            var15,
            var5,
            FluidCollisionMode.NEVER,
            false,
            0.1,
            var1x -> var1x != var0 && var1x instanceof LivingEntity var2x && !var2x.getType().name().equals("ARMOR_STAND")
         );
      if (var17 != null && var17.getHitEntity() instanceof LivingEntity var18) {
         var18.damage(var3, var0);
         if (!(var18 instanceof Player)) {
            applyMeritChange(var1, var18.getType().name());
         }
      }
   }

   private static List<String> getOwnedScrollIds(String var0) {
      Map var1 = loadScrollData(var0);
      if (var1.isEmpty()) {
         return List.of();
      } else {
         ArrayList var2 = new ArrayList();

         for (Object var4 : KomutechJson.asList(var1.get("卷轴数据"))) {
            if (var4 != null) {
               var2.add(String.valueOf(var4));
            }
         }

         return var2;
      }
   }

   private static String extractSkillName(String var0) {
      return normalizeSkillId(var0);
   }

   private static int parseIntValue(Object var0, int var1) {
      if (var0 instanceof Number var2) {
         return var2.intValue();
      } else {
         try {
            return Integer.parseInt(String.valueOf(var0));
         } catch (NumberFormatException var3) {
            return var1;
         }
      }
   }

   private static double getDamageScale(ItemStack var0, Map<String, Object> var1) {
      if (var0 != null && var0.hasItemMeta() && var0.getItemMeta().getLore() != null) {
         for (String var3 : var0.getItemMeta().getLore()) {
            String var4 = var3.replaceAll("§.", "");
            Matcher var5 = Pattern.compile("『([^』]+)阶』").matcher(var4);
            if (var5.find()) {
               return KomutechJson.getDouble(var1, var5.group(1) + "阶", 1.0);
            }
         }

         return 1.0;
      } else {
         return 1.0;
      }
   }

   private static double realmCoef(Map<String, Object> var0, Map<String, Object> var1) {
      String var2 = PlayerAttributeStore.getString(var0, "灵气", "0");
      Matcher var3 = Pattern.compile("^(\\d+)").matcher(var2);
      double var4 = var3.find() ? Double.parseDouble(var3.group(1)) : 0.0;
      if (var4 < 100.0) {
         return 0.1;
      } else if (var4 >= 1.0E11) {
         List var16 = KomutechJson.asList(var1.get("仙人"));
         return !var16.isEmpty() ? Double.parseDouble(String.valueOf(var16.get(0))) : 48.0;
      } else {
         double[] var6 = new double[]{100.0, 1000.0, 10000.0, 100000.0, 1000000.0, 1.0E7, 1.0E8, 1.0E9};
         String[] var7 = new String[]{"引气入体", "练气", "筑基", "金丹", "元婴", "化神", "合体", "渡劫", "飞升", "仙人"};
         int var8 = 0;

         for (int var9 = 1; var9 < var6.length; var8 = var9++) {
            if (var4 < var6[var9]) {
               var8 = var9 - 1;
               break;
            }
         }

         if (var4 >= 1.0E9) {
            var8 = 7;
         }

         List var17 = KomutechJson.asList(var1.get(var7[var8]));
         if (var17.isEmpty() && "合体".equals(var7[var8])) {
            var17 = KomutechJson.asList(var1.get("大成"));
         }

         if (var17.isEmpty() && "仙人".equals(var7[var8])) {
            var17 = KomutechJson.asList(var1.get("神人"));
         }

         double var10 = var8 == 0 ? 100.0 : var6[var8];
         double var12 = var8 == 7 ? 9.9E10 : var6[var8 + 1] - var10;
         int var14 = (int)Math.floor(var12 / 9.0);
         int var15 = (int)Math.floor((var4 - var10) / var14);
         if (var15 >= var17.size()) {
            var15 = var17.size() - 1;
         }

         return var17.isEmpty() ? 1.0 : Double.parseDouble(String.valueOf(var17.get(Math.max(0, var15))));
      }
   }

   private static Map<String, Object> loadScrollData(String var0) {
      try {
         Path var1 = scrollDataDir().resolve("[" + var0 + "]云篆匣.json");
         return (Map<String, Object>)(!Files.exists(var1)
            ? new HashMap<>()
            : KomutechJson.asMap(KomutechJson.parse(Files.readString(var1, StandardCharsets.UTF_8))));
      } catch (IOException var2) {
         return new HashMap<>();
      }
   }

   private static int getProficiency(String var0, String var1) {
      Map var2 = loadScrollData(var0);
      Map var3 = KomutechJson.asMap(var2.get("熟练度记录"));
      return KomutechJson.getInt(var3, var1, 0);
   }

   private static int addProficiency(String var0, String var1, int var2, int var3) {
      Map var4 = loadScrollData(var0);
      HashMap var5 = new HashMap<>(KomutechJson.asMap(var4.get("熟练度记录")));
      int var6 = KomutechJson.getInt(var5, var1, 0);
      int var7 = Math.min(var2, Math.max(0, var3 - var6));
      if (var7 <= 0) {
         return 0;
      } else {
         var5.put(var1, var6 + var7);
         var4.put("熟练度记录", var5);

         try {
            Files.createDirectories(scrollDataDir());
            Files.writeString(scrollDataDir().resolve("[" + var0 + "]云篆匣.json"), KomutechJson.stringify(var4), StandardCharsets.UTF_8);
         } catch (IOException var9) {
         }

         return var7;
      }
   }

   private static void ensureScrollConfig() {
      Path var0 = scrollConfigPath();
      long var1 = fileMtime(var0);
      if (scrollConfig == null || var1 != scrollConfigMtime) {
         reloadScrollConfig();
      }
   }

   private static void reloadScrollConfig() {
      Path var0 = scrollConfigPath();
      scrollConfig = KomutechConfigMerge.loadMerged(var0, "addon_configs/Komutech/卷轴属性.json");
      scrollConfigMtime = fileMtime(var0);
   }

   private static void ensureStaffConfig() {
      Path var0 = staffConfigPath();
      long var1 = fileMtime(var0);
      if (staffConfig == null || var1 != staffConfigMtime) {
         reloadStaffConfig();
      }
   }

   private static void reloadStaffConfig() {
      Path var0 = staffConfigPath();
      staffConfig = KomutechConfigMerge.loadMerged(var0, "addon_configs/Komutech/灵杖属性.json");
      staffConfigMtime = fileMtime(var0);
   }

   private static long fileMtime(Path var0) {
      try {
         if (Files.exists(var0)) {
            return Files.getLastModifiedTime(var0).toMillis();
         }
      } catch (IOException var2) {
      }

      return -1L;
   }

   private static String trim(double var0) {
      return var0 == Math.floor(var0) ? String.valueOf((long)var0) : String.valueOf(var0);
   }
}
