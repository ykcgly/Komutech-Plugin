package tech.komutech.native_scripts.support;

import java.io.File;
import java.nio.file.Path;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class KomutechPaths {
   private KomutechPaths() {
   }

   /**
    * 插件配置根目录：{@code plugins/Komutech/}（即本插件的数据目录）。
    *
    * <p>目录不存在时会自动创建，管理员可直接在其中编辑 {@code config.yml} 等配置。
    */
   public static File configRoot() {
      Plugin var0 = Bukkit.getPluginManager().getPlugin("Komutech");
      File var1 = var0 != null ? var0.getDataFolder() : new File("plugins/Komutech");
      if (!var1.isDirectory() && !var1.mkdirs()) {
         Bukkit.getLogger().warning("[口木科技] 无法创建配置目录: " + var1.getAbsolutePath());
      }

      return var1;
   }

   /**
    * 旧版 RSC（RykenSlimefunCustomizer）附属路径，仅用于兼容存量服务器的数据。
    *
    * <p>若该目录下已经存在本插件的数据子目录，则继续沿用，避免升级后玩家数据"消失"。
    */
   public static File legacyRoot() {
      File var0 = new File("plugins/RykenSlimefunCustomizer/addon_configs/Komutech");
      if (!var0.isDirectory()) {
         return null;
      }
      for (String var1 : new String[]{"玩家属性", "萬象匱", "云篆匣", "纳戒"}) {
         if (new File(var0, var1).isDirectory()) {
            return var0;
         }
      }

      return null;
   }

   /** 玩家数据根目录：优先沿用旧版 RSC 路径（存量数据），否则用本插件目录。 */
   public static Path dataRoot() {
      File var0 = legacyRoot();
      return var0 != null ? var0.toPath() : configRoot().toPath();
   }

   /** 主配置文件：{@code plugins/Komutech/config.yml}。 */
   public static Path configFile() {
      return configRoot().toPath().resolve("config.yml");
   }

   /**
    * 防路径穿越：在 {@code base} 下安全拼接子路径。
    *
    * <p>玩家名等外部输入可能含 {@code ../}，直接 {@code resolve} 会写到数据目录之外。
    * 这里统一 normalize 后校验结果仍在 {@code base} 内，否则抛异常拒绝。
    *
    * @param base     基准目录
    * @param segments 待拼接的路径片段（可为 null，自动跳过）
    * @throws IllegalArgumentException 拼接结果逃出 {@code base} 时抛出
    */
   public static Path safeResolve(Path base, String... segments) {
      Path root = base.toAbsolutePath().normalize();
      Path current = root;

      for (String segment : segments) {
         if (segment == null || segment.isEmpty()) {
            continue;
         }

         current = current.resolve(segment).normalize();
         if (!current.startsWith(root)) {
            throw new IllegalArgumentException("非法路径（疑似目录穿越）: " + segment);
         }
      }

      return current;
   }

   /**
    * 净化外部输入（玩家名、管理员输入的存储名等），使其可安全用作文件名。
    *
    * <p>去除 Windows 非法字符与控制字符，并把 {@code ..} 折叠掉，杜绝目录穿越。
    * 中文名不受影响，会原样保留。
    */
   public static String safeName(String raw) {
      if (raw == null || raw.isBlank()) {
         return "_";
      }

      String cleaned = raw.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1F]", "_");
      while (cleaned.contains("..")) {
         cleaned = cleaned.replace("..", "_");
      }

      String trimmed = cleaned.trim();
      if (trimmed.isEmpty() || ".".equals(trimmed) || "..".equals(trimmed)) {
         return "_";
      }

      return trimmed;
   }

   /**
    * 生成 {@code <base>/[<playerName>]suffix.json} 形式的玩家数据文件路径。
    *
    * <p>玩家名先经 {@link #safeName(String)} 净化，再由 {@link #safeResolve} 兜底校验，
    * 两层防护确保绝不会写到 {@code base} 之外。
    *
    * @param base       基准目录（如 {@code plugins/Komutech/云篆匣}）
    * @param playerName 玩家名或外部传入的名称
    * @param suffix     文件名后缀（如 {@code 云篆匣}、{@code 纳戒}）
    */
   public static Path playerFile(Path base, String playerName, String suffix) {
      return safeResolve(base, "[" + safeName(playerName) + "]" + safeName(suffix) + ".json");
   }

   public static Path playerAttributes() {
      return dataRoot().resolve("玩家属性");
   }

   public static Path wanXiangGui() {
      return dataRoot().resolve("萬象匱");
   }

   public static Path yunZhuanXia() {
      return dataRoot().resolve("云篆匣");
   }

   public static Path naJie() {
      return dataRoot().resolve("纳戒");
   }

   public static Path scrollConfig() {
      return dataRoot().resolve("卷轴属性.json");
   }

   public static Path staffConfig() {
      return dataRoot().resolve("灵杖属性.json");
   }

   public static Path puTuanConfig() {
      return dataRoot().resolve("蒲团配置.json");
   }

   public static Path attributePointLimits() {
      return dataRoot().resolve("属性加点限制.json");
   }
}
