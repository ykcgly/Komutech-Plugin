package tech.komutech.native_scripts.support;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public final class AttributePointLimits {
   private static final String EXEMPT_KEY = "属性上限豁免";
   private static final double SPIRIT_GAIN_BASE = 100.0;
   private static final double SPIRIT_GAIN_CAP = 500.0;
   private static final double SPIRIT_GAIN_CAP_HUASHEN = 2000.0;
   private static final double HEALTH_CAP_BASE = 1000.0;
   private static final double HEALTH_CAP_STEP = 1000.0;
   private static final double HEALTH_CAP_MAX = 5000.0;
   private static final List<String> LIMITED_ATTRS = List.of("血量", "攻击力", "防御力", "速度", "灵气获取");
   private static final Map<String, Double> DEFAULTS = Map.of("血量", 1000.0, "攻击力", 2000.0, "防御力", 1000.0, "速度", 100.0, "灵气获取", 500.0);
   private static volatile Map<String, Double> cachedLimits = DEFAULTS;
   private static volatile double cachedSpiritGainHuaShenCap = 2000.0;
   private static volatile double cachedHealthBase = 1000.0;
   private static volatile double cachedHealthStep = 1000.0;
   private static volatile double cachedHealthMax = 5000.0;
   private static volatile long cachedAt = 0L;
   private static final long CACHE_MS = 60000L;

   private AttributePointLimits() {
   }

   public static boolean isLimited(String var0) {
      return LIMITED_ATTRS.contains(var0);
   }

   public static double limit(String var0) {
      reloadIfNeeded();
      return cachedLimits.getOrDefault(var0, DEFAULTS.getOrDefault(var0, Double.MAX_VALUE));
   }

   public static double limit(Map<String, Object> var0, String var1) {
      if ("灵气获取".equals(var1)) {
         return spiritGainCap(var0);
      } else {
         return "血量".equals(var1) ? healthCap(var0) : limit(var1);
      }
   }

   public static double spiritGainCap(Map<String, Object> var0) {
      reloadIfNeeded();
      return var0 != null && CultivationMath.isHuaShenOrAbove(var0) ? cachedSpiritGainHuaShenCap : limit("灵气获取");
   }

   public static double healthCap(Map<String, Object> var0) {
      reloadIfNeeded();
      int var1 = var0 == null ? -1 : CultivationMath.majorRealmIndexFromHuaShen(var0);
      return var1 < 0 ? cachedHealthBase : Math.min(cachedHealthMax, cachedHealthBase + var1 * cachedHealthStep);
   }

   public static double computeActual(Map<String, Object> var0, String var1, double var2) {
      if (var2 <= 0.0) {
         return 0.0;
      } else {
         List var4 = CultivationMath.splitLinggen(PlayerAttributeStore.getString(var0, "灵根", ""));
         double var5 = PlayerAttributeStore.getDouble(var0, "总品质", 0.01);
         double var7 = PlayerAttributeStore.getDouble(var0, "根骨", 1.0);
         return CultivationMath.computeActualValue(
            var2,
            CultivationMath.baseGrowth.getOrDefault(var1, 0.0),
            CultivationMath.getLinggenFinalCoefficient(var4, var5, var1),
            CultivationMath.getRealmCoefficient(var0),
            var5,
            var7
         );
      }
   }

   public static double spiritGainPercent(Map<String, Object> var0, int var1) {
      return var1 <= 0 ? 100.0 : Math.min(spiritGainCap(var0), 100.0 + computeActual(var0, "灵气获取", var1));
   }

   public static boolean canAllocatePoint(Map<String, Object> var0, String var1, int var2) {
      if (!isLimited(var1)) {
         return true;
      } else {
         return "灵气获取".equals(var1)
            ? spiritGainPercent(var0, var2 + 1) <= spiritGainCap(var0) + 1.0E-6
            : computeActual(var0, var1, var2 + 1) <= limit(var0, var1) + 1.0E-6;
      }
   }

   public static boolean isAtLimit(Map<String, Object> var0, String var1, int var2) {
      if (!isLimited(var1)) {
         return false;
      } else {
         return "灵气获取".equals(var1)
            ? spiritGainPercent(var0, var2) >= spiritGainCap(var0) - 1.0E-6
            : computeActual(var0, var1, var2) >= limit(var0, var1) - 1.0E-6;
      }
   }

   public static double clampForApply(Map<String, Object> var0, String var1, double var2) {
      return clampForApply(var0, var1, var2, true);
   }

   public static double clampForApply(Map<String, Object> var0, String var1, double var2, boolean var4) {
      if (!isLimited(var1)) {
         return var2;
      } else {
         return var4 && isExempt(var0) ? var2 : Math.min(var2, limit(var0, var1));
      }
   }

   public static boolean isExempt(Map<String, Object> var0) {
      return PlayerAttributeStore.getBoolean(var0, "属性上限豁免", false);
   }

   public static void markAdminExempt(Map<String, Object> var0) {
      var0.put("属性上限豁免", true);
   }

   public static void clearExempt(Map<String, Object> var0) {
      if (var0 != null) {
         var0.remove("属性上限豁免");
      }
   }

   public static String limitLabel(String var0) {
      return formatLimitLabel(var0, limit(var0));
   }

   public static String limitLabel(Map<String, Object> var0, String var1) {
      return formatLimitLabel(var1, limit(var0, var1));
   }

   private static String formatLimitLabel(String var0, double var1) {
      return !"速度".equals(var0) && !"灵气获取".equals(var0) ? String.format("%.0f", var1) : String.format("%.0f%%", var1);
   }

   private static void reloadIfNeeded() {
      long var0 = System.currentTimeMillis();
      if (var0 - cachedAt >= 60000L || cachedLimits == null) {
         try {
            // 缺失时从 jar 内 属性加点限制.json 模板释放一份到 plugins/Komutech/，服主可直接改
            Map var2 = KomutechConfigMerge.ensureTemplate(KomutechPaths.attributePointLimits(), "属性加点限制.json");
            if (var2.isEmpty()) {
               cachedLimits = DEFAULTS;
               cachedSpiritGainHuaShenCap = 2000.0;
               cachedHealthBase = 1000.0;
               cachedHealthStep = 1000.0;
               cachedHealthMax = 5000.0;
            } else {
               cachedLimits = Map.of(
                  "血量",
                  readLimit(var2, "血量", DEFAULTS.get("血量")),
                  "攻击力",
                  readLimit(var2, "攻击力", DEFAULTS.get("攻击力")),
                  "防御力",
                  readLimit(var2, "防御力", DEFAULTS.get("防御力")),
                  "速度",
                  readLimit(var2, "速度", DEFAULTS.get("速度")),
                  "灵气获取",
                  readLimit(var2, "灵气获取", DEFAULTS.get("灵气获取"))
               );
               cachedSpiritGainHuaShenCap = readLimit(var2, "灵气获取_化神", 2000.0);
               cachedHealthBase = readLimit(var2, "血量", 1000.0);
               cachedHealthStep = readLimit(var2, "血量_步进", 1000.0);
               cachedHealthMax = readLimit(var2, "血量_极限", 5000.0);
            }
         } catch (Exception var3) {
            cachedLimits = DEFAULTS;
            cachedSpiritGainHuaShenCap = 2000.0;
            cachedHealthBase = 1000.0;
            cachedHealthStep = 1000.0;
            cachedHealthMax = 5000.0;
         }

         cachedAt = var0;
      }
   }

   private static double readLimit(Map<String, Object> var0, String var1, double var2) {
      double var4 = KomutechJson.getDouble(var0, var1, var2);
      return var4 > 0.0 ? var4 : var2;
   }
}
