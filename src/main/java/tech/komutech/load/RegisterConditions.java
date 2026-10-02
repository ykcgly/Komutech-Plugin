package tech.komutech.load;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import tech.komutech.KT;

/**
 * register.conditions 判定（对齐原 YamlReader.checkForRegistration）。
 * 当前内容文件未使用 conditions，实现保留以兼容后续内容。
 */
public final class RegisterConditions {

    private RegisterConditions() {}

    /** 返回 true 表示允许注册。 */
    public static boolean pass(ConfigurationSection s) {
        ConfigurationSection reg = s.getConfigurationSection("register");
        if (reg == null) return true;
        if (reg.getBoolean("unfinished", false)) return false;
        boolean warn = reg.getBoolean("warn", false);
        for (String cond : reg.getStringList("conditions")) {
            String[] parts = cond.split(" ");
            String op = parts[0];
            if (op.equalsIgnoreCase("hasplugin")) {
                if (parts.length != 2) continue;
                if (!Bukkit.getPluginManager().isPluginEnabled(parts[1])) {
                    if (warn) KT.log("需要插件 " + parts[1] + " 才能注册，跳过");
                    return false;
                }
            } else if (op.equalsIgnoreCase("!hasplugin")) {
                if (parts.length != 2) continue;
                if (Bukkit.getPluginManager().isPluginEnabled(parts[1])) {
                    if (warn) KT.log("需要卸载插件 " + parts[1] + " 才能注册，跳过");
                    return false;
                }
            } else if (op.equalsIgnoreCase("itemexist")) {
                if (parts.length != 2) continue;
                if (SlimefunItem.getById(parts[1]) == null && !KT.preload.containsKey(KT.upper(parts[1]))) {
                    if (warn) KT.log("需要物品 " + parts[1] + " 才能注册，跳过");
                    return false;
                }
            } else if (op.equalsIgnoreCase("!itemexist")) {
                if (parts.length != 2) continue;
                if (SlimefunItem.getById(parts[1]) != null || KT.preload.containsKey(KT.upper(parts[1]))) {
                    if (warn) KT.log("需要物品 " + parts[1] + " 不存在才能注册，跳过");
                    return false;
                }
            }
        }
        return true;
    }

    /** version 条件使用（保留给后续内容）。 */
    static boolean versionCheck(int major, int minor, String op, int wantMajor, int wantMinor) {
        return switch (op) {
            case ">" -> major > wantMajor || (major == wantMajor && minor > wantMinor);
            case "<" -> major < wantMajor || (major == wantMajor && minor < wantMinor);
            case ">=" -> major > wantMajor || (major == wantMajor && minor >= wantMinor);
            case "<=" -> major < wantMajor || (major == wantMajor && minor <= wantMinor);
            case "==" -> major == wantMajor && minor == wantMinor;
            case "!=" -> major != wantMajor || minor != wantMinor;
            default -> true;
        };
    }

    static String lower(String v) {
        return v.toLowerCase(Locale.ROOT);
    }
}
