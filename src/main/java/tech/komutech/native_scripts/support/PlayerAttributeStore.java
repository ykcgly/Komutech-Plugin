package tech.komutech.native_scripts.support;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tech.komutech.util.Store;

public final class PlayerAttributeStore {
   private static final Pattern LINGLI_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*/\\s*(\\d+(?:\\.\\d+)?|無|无)\\s*(?:\\+\\s*(-?\\d+(?:\\.\\d+)?))?$");

   private static Path dataDir() {
      return KomutechPaths.playerAttributes();
   }

   private PlayerAttributeStore() {
   }

   public static Path fileFor(String var0) {
      return KomutechPaths.safeResolve(dataDir(), "[" + KomutechPaths.safeName(var0) + "].json");
   }

   /**
    * 玩家属性是否有存档。
    *
    * <p>走 {@link KomutechPaths#docExists}：库里查不到时仍会看一眼磁盘上的旧 JSON，
    * 所以「升级到 SQLite 但还没导入」的空档期不会误判为没有存档。</p>
    */
   public static boolean exists(String var0) {
      return KomutechPaths.docExists(fileFor(var0).toFile());
   }

   /**
    * 读一份玩家属性。
    *
    * <p>后端优先 SQLite（{@link Store}），库里没有时回落同名 JSON 文件并顺手入库；
    * 两边都没有才返回 null。</p>
    */
   public static Map<String, Object> load(String var0) {
      return KomutechPaths.readJson(fileFor(var0).toFile());
   }

   /** 写一份玩家属性（进 SQLite 文档库；库不可用时回落文件）。 */
   public static boolean save(String var0, Map<String, Object> var1) {
      return KomutechPaths.writeJson(fileFor(var0).toFile(), var1);
   }

   /** 删一份玩家属性（库与磁盘上的同名文件一并处理）。 */
   public static boolean delete(String var0) {
      return KomutechPaths.deleteDoc(fileFor(var0).toFile());
   }

   public static Map<String, Object> deepCopy(Map<String, Object> var0) {
      return KomutechJson.asMap(KomutechJson.parse(KomutechJson.stringify(var0)));
   }

   public static PlayerAttributeStore.Lingli parseLingli(Object var0) {
      String var1 = var0 == null ? "100/100+0" : String.valueOf(var0).trim();
      Matcher var2 = LINGLI_PATTERN.matcher(var1);
      if (var2.find()) {
         double var5 = var2.group(3) == null ? 0.0 : parseLargeNumber(var2.group(3));
         return new PlayerAttributeStore.Lingli(parseLargeNumber(var2.group(1)), parseMaxNumber(var2.group(2)), var5);
      } else {
         Matcher var3 = Pattern.compile("^(\\d+(?:\\.\\d+)?)").matcher(var1);
         if (var3.find() && var1.contains("/")) {
            String var4 = var1.substring(var1.indexOf(47) + 1).trim();
            if (var4.startsWith("無") || var4.startsWith("无")) {
               return new PlayerAttributeStore.Lingli(parseLargeNumber(var3.group(1)), Double.POSITIVE_INFINITY, 0.0);
            }
         }

         return new PlayerAttributeStore.Lingli(100.0, 100.0, 0.0);
      }
   }

   private static double parseMaxNumber(String var0) {
      if (var0 == null || var0.isBlank()) {
         return 0.0;
      } else {
         return !"無".equals(var0) && !"无".equals(var0) ? parseLargeNumber(var0) : Double.POSITIVE_INFINITY;
      }
   }

   private static double parseLargeNumber(String var0) {
      if (var0 != null && !var0.isBlank()) {
         try {
            return var0.contains(".") ? Double.parseDouble(var0) : Long.parseLong(var0);
         } catch (NumberFormatException var2) {
            return Double.parseDouble(var0);
         }
      } else {
         return 0.0;
      }
   }

   public static String formatSpiritAmount(double var0, double var2) {
      return formatAmount(var0) + "/" + formatMax(var2);
   }

   public static String formatMax(double var0) {
      return Double.isInfinite(var0) ? "無" : trimDouble(var0);
   }

   public static String formatAmount(double var0) {
      if (!Double.isInfinite(var0) && !Double.isNaN(var0)) {
         return Math.abs(var0 - Math.rint(var0)) < 1.0E-9 ? String.valueOf((long)Math.rint(var0)) : String.format("%.2f", var0);
      } else {
         return "0";
      }
   }

   public static String formatLingli(PlayerAttributeStore.Lingli var0) {
      String var1 = formatSpiritAmount(var0.current, var0.max);
      if (var0.bonus == 0.0) {
         return var1;
      } else {
         String var2 = trimDouble(var0.bonus);
         return var1 + (var0.bonus > 0.0 ? "+" + var2 : var2);
      }
   }

   public static double getDouble(Map<String, Object> var0, String var1, double var2) {
      return KomutechJson.getDouble(var0, var1, var2);
   }

   public static int getInt(Map<String, Object> var0, String var1, int var2) {
      return KomutechJson.getInt(var0, var1, var2);
   }

   public static String getString(Map<String, Object> var0, String var1, String var2) {
      return KomutechJson.getString(var0, var1, var2);
   }

   public static boolean getBoolean(Map<String, Object> var0, String var1, boolean var2) {
      return KomutechJson.getBoolean(var0, var1, var2);
   }

   public static Map<String, Object> getMap(Map<String, Object> var0, String var1) {
      if (var0 != null && var0.containsKey(var1)) {
         Object var2 = var0.get(var1);
         return (Map<String, Object>)(var2 instanceof Map var3 ? KomutechJson.asMap(var2) : new LinkedHashMap<>());
      } else {
         return new LinkedHashMap<>();
      }
   }

   private static String trimDouble(double var0) {
      return Math.floor(var0) == var0 ? String.valueOf((long)var0) : String.valueOf(var0);
   }

   private static String prettyJson(Map<String, Object> var0) {
      return KomutechJson.stringify(var0);
   }

   public record Lingli(double current, double max, double bonus) {
      public double totalMax() {
         return Double.isInfinite(this.max) ? Double.POSITIVE_INFINITY : this.max + this.bonus;
      }

      public boolean isUnlimitedMax() {
         return Double.isInfinite(this.max);
      }
   }
}
