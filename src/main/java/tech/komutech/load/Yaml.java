package tech.komutech.load;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.file.YamlConfiguration;
import tech.komutech.KT;
import tech.komutech.KomutechPlugin;

/**
 * 从 jar 资源加载 YAML（对齐 WorldTaste 的 Yaml）。
 *
 * <p>加载期按文件名缓存解析结果：preload 与各 Loader 会访问同一批内容文件，
 * 缓存后每个文件单次加载中只解析一次。加载结束后由 {@link Setup#loadAll()} 调用
 * {@link #clearCache()} 释放解析树。
 */
public final class Yaml {

    private Yaml() {}

    private static final Map<String, YamlConfiguration> CACHE = new HashMap<>();

    public static YamlConfiguration loadResource(KomutechPlugin plugin, String name) {
        YamlConfiguration cached = CACHE.get(name);
        if (cached != null) return cached;
        YamlConfiguration cfg = doLoad(plugin, name);
        CACHE.put(name, cfg);
        return cfg;
    }

    private static YamlConfiguration doLoad(KomutechPlugin plugin, String name) {
        try (InputStream in = plugin.getResource(name)) {
            if (in == null) {
                KT.log("资源缺失: " + name);
                return new YamlConfiguration();
            }
            try (InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        } catch (IOException e) {
            KT.log("读取 " + name + " 失败: " + e);
            return new YamlConfiguration();
        }
    }

    public static void clearCache() {
        CACHE.clear();
    }
}
