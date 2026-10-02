package tech.komutech.native_scripts.support;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tech.komutech.native_scripts.cultivation.JianLingShiSupport;

public final class CultivationMath {
   public static final List<String> SWITCHABLE_ATTRS = List.of("血量", "攻击力", "防御力", "速度", "灵识");
   public static final List<String> VALID_LINGGEN = List.of("金", "木", "水", "火", "土", "雷", "风", "冰", "光", "暗");
   private static final double[][] REALM_BONUS = new double[][]{
      {0.1, 0.13, 0.16, 0.19, 0.22, 0.25, 0.28, 0.31, 0.34},
      {0.5, 0.55, 0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.9},
      {1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2.0},
      {2.5, 2.7, 2.9, 3.1, 3.3, 3.5, 3.7, 3.9, 4.1},
      {4.5, 4.8, 5.1, 5.4, 5.7, 6.0, 6.3, 6.6, 6.9},
      {7.5, 8.0, 8.5, 9.0, 9.5, 10.0, 10.5, 11.0, 11.5},
      {12.5, 13.5, 14.5, 15.5, 16.5, 17.5, 18.5, 19.5, 20.5},
      {22.0, 23.5, 25.0, 26.5, 28.0, 29.5, 31.0, 32.5, 34.0},
      {36.0, 38.0, 40.0, 42.0, 44.0, 46.0, 48.0, 50.0, 52.0}
   };
   private static final Map<Integer, Double> DECAY_FACTOR = Map.of(1, 1.0, 2, 0.8, 3, 0.5, 4, 0.3, 5, 0.1);
   private static final Map<String, Boolean> MUTATED = Map.of("雷", true, "风", true, "冰", true, "光", true, "暗", true);
   private static final Map<String, Map<String, Double>> LINGGEN_BONUS = new HashMap<>();
   public static Map<String, Double> baseGrowth = Map.of("血量", 3.0, "攻击力", 1.0, "防御力", 1.0, "速度", 0.1, "灵识", 0.01, "灵力", 0.1, "灵气获取", 2.5);

   private CultivationMath() {
   }

   private static void putBonus(String var0, double var1, double var3, double var5, double var7, double var9, double var11) {
      LINGGEN_BONUS.put(var0, Map.of("血量", var1, "攻击力", var3, "防御力", var5, "速度", var7, "灵识", var9, "灵力", var11, "灵气获取", var11));
   }

   public static List<String> splitLinggen(String var0) {
      return var0 != null && !var0.isBlank() ? Arrays.stream(var0.split("[、，\\s]+")).filter(var0x -> !var0x.isBlank()).toList() : List.of();
   }

   private static long parseSpiritValue(String var0) {
      if (var0 != null && !var0.isBlank()) {
         Matcher var1 = Pattern.compile("^(\\d+(?:\\.\\d+)?)").matcher(var0.trim());
         if (!var1.find()) {
            return 0L;
         } else {
            String var2 = var1.group(1);

            try {
               return var2.contains(".") ? (long)Double.parseDouble(var2) : Long.parseLong(var2);
            } catch (NumberFormatException var4) {
               return Long.MAX_VALUE;
            }
         }
      } else {
         return 0L;
      }
   }

   public static double getRealmCoefficient(Map<String, Object> var0) {
      long var1 = parseSpiritValue(PlayerAttributeStore.getString(var0, "灵气", ""));
      if (var1 < 100L) {
         return 0.0;
      } else if (var1 >= 100000000000L) {
         return 18.0 + Math.floor((var1 - 100000000000L) / 1.0E11) * 2.0;
      } else {
         long[] var3 = new long[]{100L, 1000L, 10000L, 100000L, 1000000L, 10000000L, 100000000L, 1000000000L};
         int var4 = 0;

         for (int var5 = 0; var5 < var3.length; var4 = var5++) {
            if (var1 < var3[var5]) {
               var4 = var5 - 1;
               break;
            }
         }

         if (var4 < 0) {
            var4 = 0;
         }

         if (var1 >= 1000000000L) {
            var4 = 7;
         }

         long var12 = var3[var4];
         long var7 = var4 == 7 ? 90000000000L : var3[var4 + 1] - var12;
         long var9 = Math.max(1L, var7 / 9L);
         int var11 = (int)Math.floor((double)(var1 - var12) / var9);
         if (var11 >= 9) {
            var11 = 8;
         }

         return REALM_BONUS[var4][var11];
      }
   }

   public static CultivationMath.CultivationInfo getCultivationInfo(long var0) {
      if (var0 < 100L) {
         return new CultivationMath.CultivationInfo("『引气入体』", 100L, "100");
      } else {
         record Bracket(int base, long next, String name) {
         }

         Bracket[] var2 = new Bracket[]{
            new Bracket(100, 1000L, "练气"),
            new Bracket(1000, 10000L, "筑基"),
            new Bracket(10000, 100000L, "金丹"),
            new Bracket(100000, 1000000L, "元婴"),
            new Bracket(1000000, 10000000L, "化神"),
            new Bracket(10000000, 100000000L, "合体"),
            new Bracket(100000000, 1000000000L, "渡劫"),
            new Bracket(1000000000, 100000000000L, "飞升")
         };

         for (Bracket var6 : var2) {
            if (var0 < var6.next) {
               long var7 = var6.next - var6.base;
               long var9 = Math.max(1L, var7 / 9L);
               int var11 = (int)Math.floor((double)(var0 - var6.base) / var9);
               if (var11 >= 9) {
                  var11 = 8;
               }

               long var12 = Math.round(var6.next * ((var11 + 1) * 0.1));
               return new CultivationMath.CultivationInfo("『" + var6.name + "·" + (var11 + 1) + "阶』", var12, String.valueOf(var6.next));
            }
         }

         int var14 = (int)Math.floor((var0 - 100000000000L) / 1.0E11) + 1;
         if (var14 < 1) {
            var14 = 1;
         }

         long var15 = Math.round(1.0E11 * (var14 * 0.1));
         return new CultivationMath.CultivationInfo("『仙人·" + var14 + "阶』", var15, "無");
      }
   }

   public static CultivationMath.CultivationInfo getCultivationInfo(String var0) {
      return getCultivationInfo(parseSpiritValue(var0));
   }

   public static String computeLinggenAttr(List<String> var0, double var1) {
      int var3 = var0.size();
      long var4 = var0.stream().filter(MUTATED::containsKey).count();
      if (var3 == 1) {
         String var6 = (String)var0.get(0);
         if (MUTATED.containsKey(var6)) {
            return "变异灵根";
         } else {
            return var1 > 0.9 ? "天灵根" : "单灵根";
         }
      } else if (var3 >= 5) {
         return "杂灵根";
      } else if (var3 > 1 && var4 > 0L) {
         return "变异多灵根";
      } else {
         return switch (var3) {
            case 2 -> "双灵根";
            case 3 -> "三灵根";
            case 4 -> "四灵根";
            default -> "未知";
         };
      }
   }

   public static double getLinggenFinalCoefficient(List<String> var0, double var1, String var3) {
      double var4 = calculateCoefficients(var0, var1).getOrDefault(var3, 0.0);
      String var6 = computeLinggenAttr(var0, var1);

      double var7 = switch (var6) {
         case "杂灵根" -> 0.1;
         case "四灵根" -> 0.3;
         case "三灵根" -> 0.5;
         case "双灵根" -> 0.8;
         case "单灵根" -> 1.0;
         case "天灵根" -> 1.2;
         case "变异灵根" -> 1.3;
         default -> 1.0;
      };
      return (1.0 + var4) * var7;
   }

   public static Map<String, Double> calculateCoefficients(List<String> var0, double var1) {
      HashMap<String, Double> var3 = new HashMap<>();

      for (String var5 : List.of("血量", "攻击力", "防御力", "速度", "灵识", "灵力", "灵气获取")) {
         var3.put(var5, 0.0);
      }

      if (var0.isEmpty()) {
         return var3;
      } else {
         int var13 = var0.size();
         double var14 = DECAY_FACTOR.getOrDefault(var13, 0.1);
         HashMap<String, Double> var7 = new HashMap<>();

         for (String var9 : var3.keySet()) {
            var7.put(var9, 0.0);
         }

         for (String var17 : var0) {
            Map<String, Double> var10 = LINGGEN_BONUS.getOrDefault(var17, Map.of());

            for (String var12 : var7.keySet()) {
               var7.merge(var12, var10.getOrDefault(var12, 0.0), Double::sum);
            }
         }

         for (String var18 : var3.keySet()) {
            var3.put(var18, (Double)var7.get(var18) / var13 * var1 * var14);
         }

         return var3;
      }
   }

   public static double computeActualValue(double var0, double var2, double var4, double var6, double var8, double var10) {
      double var12 = var0 * var2 * (var4 + var6) * var8 * (var10 / 10.0);
      return Math.max(0.0, Math.round(var12 * 100.0) / 100.0);
   }

   public static int computeBaseMaxLingli(Map<String, Object> var0) {
      List var1 = splitLinggen(PlayerAttributeStore.getString(var0, "灵根", ""));
      double var2 = PlayerAttributeStore.getDouble(var0, "总品质", 0.01);
      double var4 = getLinggenFinalCoefficient(var1, var2, "灵力");
      long var6 = parseSpiritValue(PlayerAttributeStore.getString(var0, "灵气", "0"));
      CultivationMath.CultivationInfo var8 = getCultivationInfo(var6);
      double var9 = DECAY_FACTOR.getOrDefault(var1.size(), 0.1);
      double var11 = PlayerAttributeStore.getDouble(var0, "根骨", 1.0);
      double var13 = baseGrowth.getOrDefault("灵力", 0.1);
      double var15 = Math.floor(var8.spiritCap() * var4 * var9 * var2 * var13 * (var11 / 10.0)) + 100.0;
      if (var15 >= 2.147483647E9) {
         return Integer.MAX_VALUE;
      } else {
         return var15 <= 0.0 ? 100 : (int)var15;
      }
   }

   public static void ensureDataComplete(String var0, Map<String, Object> var1) {
      boolean var2 = false;

      for (String var4 : SWITCHABLE_ATTRS) {
         if (!var1.containsKey(var4 + "_启用")) {
            var1.put(var4 + "_启用", true);
            var2 = true;
         }

         if (!var1.containsKey(var4 + "_实际")) {
            var1.put(var4 + "_实际", 0.0);
            var2 = true;
         }
      }

      if (!var1.containsKey("属性点")) {
         var1.put("属性点", 3);
         var2 = true;
      }

      if (!var1.containsKey("灵气获取")) {
         var1.put("灵气获取", 0);
         var2 = true;
      }

      if (!var1.containsKey("灵气获取_实际")) {
         var1.put("灵气获取_实际", 100.0);
         var2 = true;
      }

      if (!var1.containsKey("攻击力")) {
         var1.put("攻击力", 0);
         var2 = true;
      }

      if (JianLingShiSupport.completeLinggenFieldsIfNeeded(var1)) {
         var2 = true;
      }

      String var5 = PlayerAttributeStore.getString(var1, "灵力", "");
      if (var5.isBlank() || !var5.contains("+")) {
         ensureLingliFormat(var1);
         var2 = true;
      }

      if (var2) {
         PlayerAttributeStore.save(var0, var1);
      }
   }

   public static void ensureLingliFormat(Map<String, Object> var0) {
      String var1 = PlayerAttributeStore.getString(var0, "灵力", "");
      Matcher var2 = Pattern.compile("^(\\d+)/(\\d+)$").matcher(var1);
      if (var2.find()) {
         var0.put("灵力", var2.group(1) + "/" + var2.group(2) + "+0");
      } else {
         int var3 = computeBaseMaxLingli(var0);
         var0.put("灵力", var3 + "/" + var3 + "+0");
      }
   }

   public static boolean isImmortalRealm(Map<String, Object> var0) {
      return parseSpiritValue(PlayerAttributeStore.getString(var0, "灵气", "")) >= 100000000000L;
   }

   public static boolean isGoldenCoreRealm(Map<String, Object> var0) {
      return spiritValue(var0) >= 10000L;
   }

   public static boolean isHuaShenOrAbove(Map<String, Object> var0) {
      return spiritValue(var0) >= 1000000L;
   }

   public static long spiritValue(Map<String, Object> var0) {
      return parseSpiritValue(PlayerAttributeStore.getString(var0, "灵气", "0"));
   }

   public static int majorRealmIndexFromHuaShen(Map<String, Object> var0) {
      long var1 = spiritValue(var0);
      if (var1 < 1000000L) {
         return -1;
      } else if (var1 >= 100000000000L) {
         return 4;
      } else if (var1 >= 1000000000L) {
         return 3;
      } else if (var1 >= 100000000L) {
         return 2;
      } else {
         return var1 >= 10000000L ? 1 : 0;
      }
   }

   static {
      putBonus("金", 0.8, 1.2, 1.0, 0.5, 0.8, 1.0);
      putBonus("木", 1.2, 0.8, 1.0, 0.8, 1.2, 1.2);
      putBonus("水", 1.0, 0.9, 0.8, 0.8, 1.0, 1.5);
      putBonus("火", 1.0, 1.0, 0.8, 0.8, 0.8, 0.8);
      putBonus("土", 1.0, 0.8, 1.2, 0.5, 0.9, 0.9);
      putBonus("雷", 1.2, 1.8, 1.0, 1.5, 1.5, 1.5);
      putBonus("风", 1.0, 1.2, 0.8, 1.8, 1.0, 1.3);
      putBonus("冰", 1.2, 1.5, 1.0, 1.2, 1.2, 1.4);
      putBonus("光", 1.3, 1.5, 1.1, 1.5, 1.5, 1.5);
      putBonus("暗", 1.1, 1.3, 1.4, 1.2, 1.5, 1.3);
   }

   public record CultivationInfo(String stage, long spiritCap, String maxStr) {
   }
}
