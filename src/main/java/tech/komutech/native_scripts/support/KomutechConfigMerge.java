package tech.komutech.native_scripts.support;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class KomutechConfigMerge {
   private KomutechConfigMerge() {
   }

   /**
    * 确保磁盘上的数值配置文件存在，缺失时从 jar 内模板释放一份。
    *
    * <p><b>为什么不能是「合并」语义</b>：早期版本名为 {@code loadMerged}，会在磁盘值与 jar 默认值
    * 不同时<b>用 jar 值覆盖磁盘值</b>。对数值配置（卷轴伤害、灵杖消耗、修炼速度）而言这是灾难性的
    * ——服主在 {@code plugins/Komutech/} 调好的参数会在下次读取时被默认值悄悄改回去。
    * 因此这里只做「缺失才释放」：磁盘文件一旦存在就<b>永不改动</b>，服主改动始终优先。
    *
    * @param diskPath       磁盘目标路径（通常在 {@code plugins/Komutech/} 下）
    * @param resourceName   jar 内模板资源名（jar 根目录，如 {@code 卷轴属性.json}）
    * @return 磁盘配置内容；文件不存在且模板也缺失时返回空 Map
    */
   public static Map<String, Object> ensureTemplate(Path diskPath, String resourceName) {
      Map<String, Object> disk = readDisk(diskPath);
      // 磁盘已有内容（哪怕解析出空对象）即视为服主已配置，绝不覆盖
      if (diskPath != null && Files.exists(diskPath)) {
         return disk;
      }

      Map<String, Object> template = readClasspath(resourceName);
      if (template.isEmpty() || diskPath == null) {
         return disk;
      }

      try {
         Path parent = diskPath.getParent();
         if (parent != null) {
            Files.createDirectories(parent);
         }

         try (OutputStream out = Files.newOutputStream(diskPath)) {
            out.write(KomutechJson.stringify(template).getBytes(StandardCharsets.UTF_8));
         }

         Bukkit.getLogger().info("[口木科技] 已生成配置: " + diskPath);
      } catch (Exception e) {
         Bukkit.getLogger().warning("[口木科技] 释放配置失败 " + diskPath + ": " + e);
      }

      return template;
   }

   private static Map<String, Object> readDisk(Path path) {
      try {
         if (path != null && Files.exists(path)) {
            return KomutechJson.asMap(KomutechJson.parse(Files.readString(path, StandardCharsets.UTF_8)));
         }
      } catch (Exception ignored) {
      }

      return new LinkedHashMap<>();
   }

   private static Map<String, Object> readClasspath(String resourceName) {
      Plugin plugin = Bukkit.getPluginManager().getPlugin("Komutech");
      if (plugin == null || resourceName == null || resourceName.isBlank()) {
         return new LinkedHashMap<>();
      }

      try (InputStream in = plugin.getResource(resourceName)) {
         if (in == null) {
            return new LinkedHashMap<>();
         }

         return KomutechJson.asMap(KomutechJson.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8)));
      } catch (Exception e) {
         return new LinkedHashMap<>();
      }
   }
}
