package tech.komutech.native_scripts.support;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import tech.komutech.util.Store;

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

   // ------------------------------------------------------------------
   // 玩家数据文档（后端 = SQLite；不可用时回落文件）
   //
   // 玩家数据一律是 <数据根>/<命名空间>/<键>.json 的形状
   // （玩家属性/[玩家名].json、萬象匱/[玩家名]<存储名>.json、纳戒/…、云篆匣/…）。
   // 这四种走 Store（SQLite 文档库）；其余（数值配置 JSON、萬衍儀绑定、
   // .txt 标记）仍是真实文件。新增玩家数据请一律走这里的 readJson/writeJson，
   // 禁止 Files.write / Files.readString 裸读写 —— 那会写进磁盘而读取方查库，
   // 表现为「存了读不到」。
   // ------------------------------------------------------------------

   /**
    * 读一份玩家数据文档。
    *
    * <p>库可用但该键还没有文档时，会去读同名旧文件并<b>顺手入库</b>，
    * 保证「先升级后导入」与「边跑边迁移」都不会读到空档。</p>
    */
   public static Map<String, Object> readJson(File file) {
      if (file == null) return null;
      String[] doc = docParts(file);
      if (doc != null) {
         Map<String, Object> fromDb = Store.load(doc[0], doc[1]);
         if (fromDb != null) return fromDb;
         Map<String, Object> fromFile = readJsonFile(file);
         if (fromFile != null) {
            Store.save(doc[0], doc[1], fromFile);   // 惰性迁移：只补空缺，不覆盖
         }
         return fromFile;
      }
      return readJsonFile(file);
   }

   /**
    * 读一份玩家数据的<b>原始结构</b>（顶层可能是对象也可能是数组）。
    *
    * <p>纳戒存档是 {@code ["base64", ...]} 这样的顶层数组，用 {@link #readJson}
    * 拿不到，必须走这里。</p>
    */
   public static Object readAny(File file) {
      if (file == null) return null;
      String[] doc = docParts(file);
      if (doc != null) {
         Object fromDb = Store.loadAny(doc[0], doc[1]);
         if (fromDb != null) return fromDb;
         Object fromFile = readAnyFile(file);
         if (fromFile != null) {
            Store.save(doc[0], doc[1], fromFile);   // 惰性迁移：只补空缺，不覆盖
         }
         return fromFile;
      }
      return readAnyFile(file);
   }

   /** 写一份玩家数据文档（玩家数据目录下的 JSON → SQLite；其余 → 真实文件）。 */
   public static boolean writeJson(File file, Object data) {
      if (file == null) return false;
      String[] doc = docParts(file);
      if (doc != null) {
         if (Store.save(doc[0], doc[1], data)) return true;
         // 库不可用 → 继续走文件
      }
      return writeJsonFile(file, data);
   }

   /** 文档是否存在（玩家数据目录下的 JSON 查库，其余查文件）。 */
   public static boolean docExists(File file) {
      if (file == null) return false;
      String[] doc = docParts(file);
      if (doc != null) {
         if (Store.available()) return Store.exists(doc[0], doc[1]) || file.isFile();
         return file.isFile();
      }
      return file.isFile();
   }

   /** 删一份文档（库里删不掉时再删文件）。 */
   public static boolean deleteDoc(File file) {
      if (file == null) return false;
      String[] doc = docParts(file);
      if (doc != null) {
         boolean db = Store.delete(doc[0], doc[1]);
         boolean fs = file.isFile() && file.delete();
         return db || fs;
      }
      return file.isFile() && file.delete();
   }

   /**
    * 列出一个玩家数据目录下的全部文档（库里的键 + 盘上尚未入库的旧文件）。
    *
    * <p>返回的 File 不保证在磁盘上真实存在 —— 对库里的键它只是 {@code (ns, key)}
    * 的路径表达，配合 {@link #readJson} / {@link #deleteDoc} 使用。</p>
    */
   public static List<File> listDocs(File dir) {
      List<File> out = new ArrayList<>();
      if (dir == null) return out;
      Set<String> seen = new LinkedHashSet<>();
      String[] nsProbe = docParts(new File(dir, "probe.json"));
      if (nsProbe != null && Store.available()) {
         for (String key : Store.keys(nsProbe[0])) {
            if (seen.add(key)) out.add(new File(dir, key + ".json"));
         }
      }
      File[] files = dir.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".json"));
      if (files != null) {
         for (File f : files) {
            String key = f.getName().substring(0, f.getName().length() - 5);
            if (seen.add(key)) out.add(f);
         }
      }
      return out;
   }

   /** 文档最后写入时间（毫秒）；库里没有时取文件 mtime。 */
   public static long docUpdatedAt(File file) {
      if (file == null) return 0L;
      String[] doc = docParts(file);
      if (doc != null) {
         long t = Store.updatedAt(doc[0], doc[1]);
         if (t > 0) return t;
      }
      return file.isFile() ? file.lastModified() : 0L;
   }

   /**
    * 把路径拆成 {@code [命名空间, 键]}；不是「玩家数据目录下的 .json」时返回 null。
    *
    * <p>只认 {@code <数据根>/<一级子目录>/<名>.json} 这一种形状 —— 更深层、
    * 或不在数据根下的文件一律当普通文件处理（数值配置、机器坐标绑定、临时件）。</p>
    */
   private static String[] docParts(File file) {
      try {
         String name = file.getName();
         if (!name.toLowerCase(Locale.ROOT).endsWith(".json")) return null;
         File parent = file.getParentFile();
         if (parent == null || parent.getParentFile() == null) return null;
         File root = dataRoot().toFile();
         if (!parent.getParentFile().toPath().normalize().equals(root.toPath().normalize())) {
            return null;
         }
         String key = name.substring(0, name.length() - 5);
         if (key.isEmpty()) return null;
         return new String[]{parent.getName(), key};
      } catch (Throwable t) {
         return null;
      }
   }

   /** 读真实 JSON 文件；不存在或损坏时返回 null。 */
   private static Map<String, Object> readJsonFile(File file) {
      try {
         if (file == null || !file.exists()) return null;
         String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
         if (text.isBlank()) return null;
         Object parsed = KomutechJson.parse(text);
         return parsed instanceof Map<?, ?> ? KomutechJson.asMap(parsed) : null;
      } catch (Throwable t) {
         return null;
      }
   }

   /** 读真实 JSON 文件并保留原始结构（对象或数组）。 */
   private static Object readAnyFile(File file) {
      try {
         if (file == null || !file.exists()) return null;
         String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
         return text.isBlank() ? null : KomutechJson.parse(text);
      } catch (Throwable t) {
         return null;
      }
   }

   /**
    * 写真实 JSON 文件（先写 {@code .tmp} 再原子替换）。
    *
    * <p>直接覆写在进程崩溃 / 服务器被 kill 时会留下半个 JSON，下次读出来就是空的
    * —— 调用方一旦以此为权威就会把玩家存档清零，所以这里必须走临时文件。</p>
    */
   private static boolean writeJsonFile(File file, Object data) {
      try {
         if (file == null) return false;
         File parent = file.getParentFile();
         if (parent != null && !parent.exists() && !parent.mkdirs()) {
            Bukkit.getLogger().warning("[口木科技] 无法创建目录: " + parent.getAbsolutePath());
            return false;
         }
         Path target = file.toPath();
         Path tmp = target.resolveSibling(target.getFileName().toString() + ".tmp");
         Files.writeString(tmp, KomutechJson.stringify(data), StandardCharsets.UTF_8);
         try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
         }
         return true;
      } catch (IOException | RuntimeException e) {
         Bukkit.getLogger().warning("[口木科技] 写 JSON 失败 "
                 + (file == null ? "?" : file.getAbsolutePath()) + ": " + e);
         return false;
      }
   }
}
