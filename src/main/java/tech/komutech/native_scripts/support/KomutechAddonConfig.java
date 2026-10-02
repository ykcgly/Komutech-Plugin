package tech.komutech.native_scripts.support;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

/**
 * 附属配置读取（{@code plugins/Komutech/config.yml}）。
 *
 * <p>三层取值：磁盘配置 → jar 内 {@code default_config.yml} 默认值 → 代码兜底值。
 * 磁盘文件不存在时会把 jar 内的默认模板释放到 {@code plugins/Komutech/config.yml}，
 * 供管理员直接编辑；已存在的文件永不覆盖，避免冲掉线上已改好的配置。
 */
public final class KomutechAddonConfig {
   /** jar 内的默认配置模板。 */
   public static final String TEMPLATE = "default_config.yml";
   private static File configFile;
   private static YamlConfiguration config;
   private static YamlConfiguration defaults;
   private static long lastModified;

   private KomutechAddonConfig() {
   }

   private static Plugin plugin() {
      return Bukkit.getPluginManager().getPlugin("Komutech");
   }

   /** 加载（并缓存）jar 内的默认模板。 */
   private static synchronized YamlConfiguration defaults() {
      if (defaults == null) {
         Plugin var0 = plugin();
         if (var0 == null) {
            defaults = new YamlConfiguration();
         } else {
            try (InputStream var1 = var0.getResource(TEMPLATE)) {
               defaults = var1 == null ? new YamlConfiguration() : YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(var1, java.nio.charset.StandardCharsets.UTF_8));
            } catch (Exception var2) {
               Bukkit.getLogger().log(Level.WARNING, "[口木科技] 加载默认配置模板失败", var2);
               defaults = new YamlConfiguration();
            }
         }
      }

      return defaults;
   }

   /**
    * 首次访问时把 jar 内模板释放到 {@code plugins/Komutech/config.yml}。
    *
    * @return 配置文件路径
    */
   public static synchronized File ensureConfigFile() {
      File var0 = KomutechPaths.configFile().toFile();
      if (var0.isFile()) {
         return var0;
      }

      File var1 = var0.getParentFile();
      if (var1 != null && !var1.isDirectory() && !var1.mkdirs()) {
         Bukkit.getLogger().warning("[口木科技] 无法创建配置目录: " + var1.getAbsolutePath());
      }

      Plugin var2 = plugin();

      try (InputStream var3 = var2 == null ? null : var2.getResource(TEMPLATE)) {
         if (var3 == null) {
            return var0;
         }

         try (OutputStream var4 = Files.newOutputStream(var0.toPath())) {
            var3.transferTo(var4);
         }

         Bukkit.getLogger().info("[口木科技] 已生成默认配置文件: " + var0.getPath());
      } catch (Exception var4) {
         Bukkit.getLogger().log(Level.WARNING, "[口木科技] 释放默认配置失败: " + var0.getPath(), var4);
      }

      return var0;
   }

   private static YamlConfiguration yaml() {
      File var0 = ensureConfigFile();
      long var1 = var0.exists() ? var0.lastModified() : 0L;
      if (config == null || configFile == null || var1 != lastModified || !var0.equals(configFile)) {
         configFile = var0;
         config = YamlConfiguration.loadConfiguration(var0);
         lastModified = var1;
      }

      return config;
   }

   public static int getInt(String key, int fallback) {
      YamlConfiguration var0 = yaml();
      if (var0.contains(key)) {
         return var0.getInt(key);
      }

      YamlConfiguration var1 = defaults();
      return var1.contains(key) ? var1.getInt(key) : fallback;
   }

   public static String getString(String key, String fallback) {
      YamlConfiguration var0 = yaml();
      String var1 = var0.isSet(key) ? var0.getString(key) : null;
      if (var1 != null) {
         return var1;
      }

      YamlConfiguration var2 = defaults();
      String var3 = var2.isSet(key) ? var2.getString(key) : null;
      return var3 != null ? var3 : fallback;
   }

   public static List<String> getStringList(String key) {
      YamlConfiguration var0 = yaml();
      if (var0.isSet(key)) {
         List var1 = var0.getStringList(key);
         if (var1 != null && !var1.isEmpty()) {
            return var1;
         }
      }

      YamlConfiguration var2 = defaults();
      List var3 = var2.isSet(key) ? var2.getStringList(key) : null;
      return var3 == null ? Collections.emptyList() : var3;
   }

   /** 主动失效缓存，供 /ktadmin reload 之类命令在改完磁盘配置后调用。 */
   public static synchronized void reload() {
      config = null;
      configFile = null;
      lastModified = 0L;
   }
}
