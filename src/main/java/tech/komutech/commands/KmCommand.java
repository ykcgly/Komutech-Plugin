package tech.komutech.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import tech.komutech.KT;
import tech.komutech.util.Store;

/**
 * {@code /km} —— 口木科技管理指令。
 *
 * <h2>{@code /km import}</h2>
 * <p>把 <b>RSC 版</b>的玩家数据（{@code plugins/RykenSlimefunCustomizer/addon_configs/Komutech/**}）
 * 导进本插件现在的 SQLite 文档库。默认只<b>预览</b>：扫描、统计、列出样例，一个字节都不写；
 * 确认无误后加 {@code --apply} 才会落库。</p>
 *
 * <pre>
 * /km import                        预览 RSC 数据（默认源目录）
 * /km import --apply                真正写入
 * /km import --source &lt;目录&gt;        指定源目录（例如把 RSC 数据拷到别处）
 * /km import --only 玩家属性,纳戒    只导这些命名空间
 * /km import --overwrite            库里已有同键时用 RSC 数据覆盖（默认跳过，保护现有进度）
 * </pre>
 *
 * <p>源目录结构必须是 {@code <命名空间>/<键>.json}，本插件认这四个命名空间：
 * {@code 玩家属性}、{@code 萬象匱}、{@code 云篆匣}、{@code 纳戒}。</p>
 *
 * <p>权限：{@code komutech.command.import} 或 OP，控制台永远可用。</p>
 */
public final class KmCommand implements CommandExecutor, TabCompleter {

    /** 权限节点。 */
    public static final String PERM_IMPORT = "komutech.command.import";

    /** RSC 插件目录名（实测拼写是 RykenSlimefunCustomizer，带 fun）。 */
    private static final String RSC_PLUGIN = "RykenSlimefunCustomizer";

    /** RSC 插件目录的旧拼写兜底。 */
    private static final String RSC_PLUGIN_ALT = "RykenSlimeCustomizer";

    /** RSC 数据在插件目录下的相对路径。 */
    private static final String RSC_DATA = "addon_configs/Komutech";

    /** 本插件认的玩家数据命名空间（用于补全与提示）。 */
    private static final List<String> KNOWN_NS = List.of("玩家属性", "萬象匱", "云篆匣", "纳戒");

    /**
     * 候选源目录（按优先级）：
     * <ol>
     *   <li>{@code plugins/RykenSlimefunCustomizer/addon_configs/Komutech} —— RSC 装在本服时的标准位置；</li>
     *   <li>{@code plugins/RykenSlimeCustomizer/addon_configs/Komutech} —— 旧拼写兜底
     *       （RSC 早期版本的目录名没有 fun，糖然实测踩过这个坑）；</li>
     *   <li>{@code plugins/Komutech/addon_configs/Komutech} —— 把 RSC 数据拷进本插件目录的情形；</li>
     *   <li>{@code plugins/Komutech} —— 直接把数据文件夹丢进插件目录的情形。</li>
     * </ol>
     * 第一个存在的胜出。
     */
    private static List<File> sourceCandidates() {
        List<File> out = new ArrayList<>();
        File plugins = KT.plugin() == null ? null : KT.plugin().getDataFolder().getParentFile();
        if (plugins != null) {
            out.add(new File(plugins, RSC_PLUGIN + "/" + RSC_DATA));
            out.add(new File(plugins, RSC_PLUGIN_ALT + "/" + RSC_DATA));
            out.add(new File(plugins, "Komutech/" + RSC_DATA));
            out.add(new File(plugins, "Komutech"));
        }
        return out;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!checkPerm(sender)) return true;
        if (args.length == 0 || !"import".equalsIgnoreCase(args[0])) {
            usage(sender, label);
            return true;
        }
        return importData(sender, label, args);
    }

    private boolean checkPerm(CommandSender sender) {
        if (!(sender instanceof Player player)) return true;   // 控制台
        if (player.hasPermission(PERM_IMPORT)) return true;
        try {
            if (player.isOp()) return true;
        } catch (Throwable ignored) {
            // 忽略
        }
        sender.sendMessage("§c需要权限 §f" + PERM_IMPORT + " §c（或 OP）才能执行数据导入。");
        return false;
    }

    private void usage(CommandSender sender, String label) {
        sender.sendMessage("§6§l口木科技 §7数据管理");
        sender.sendMessage("§f/" + label + " import §7— 从 RSC 导入玩家数据（默认只预览）");
        sender.sendMessage("§8  --apply §7真正写入  §8--source <目录> §7指定源目录");
        sender.sendMessage("§8  --only <命名空间,...> §7只导指定命名空间  §8--overwrite §7覆盖已有同名文档");
        sender.sendMessage("§7存储后端：§f" + (Store.available()
                ? "SQLite（" + Store.databaseFile().getName() + "）"
                : "文件（SQLite 不可用，已降级）"));
        Map<String, Integer> ns = Store.namespaces();
        if (ns.isEmpty()) {
            sender.sendMessage("§7库内暂无玩家数据。");
        } else {
            sender.sendMessage("§7库内数据：" + describeNamespaces(ns));
        }
        sender.sendMessage("§7源目录结构须为 §f<命名空间>/<键>.json§7，支持："
                + String.join(" / ", KNOWN_NS));
    }

    private boolean importData(CommandSender sender, String label, String[] args) {
        boolean apply = false;
        boolean overwrite = false;
        File source = resolveSource();
        Set<String> only = new LinkedHashSet<>();
        for (int i = 1; i < args.length; i++) {
            String a = args[i];
            switch (a.toLowerCase(Locale.ROOT)) {
                case "--apply" -> apply = true;
                case "--overwrite" -> overwrite = true;
                case "--dry-run" -> apply = false;
                case "--source" -> {
                    if (i + 1 >= args.length) {
                        sender.sendMessage("§c--source 后面要跟目录路径。");
                        return true;
                    }
                    source = new File(args[++i]);
                }
                case "--only" -> {
                    if (i + 1 >= args.length) {
                        sender.sendMessage("§c--only 后面要跟命名空间（逗号分隔，可多个，如 玩家属性,纳戒）。");
                        return true;
                    }
                    for (String part : args[++i].split("[,，]")) {
                        String ns = part.trim();
                        if (!ns.isEmpty()) only.add(ns);
                    }
                }
                default -> {
                    sender.sendMessage("§c未知参数：" + a);
                    usage(sender, label);
                    return true;
                }
            }
        }

        if (!Store.available()) {
            sender.sendMessage("§cSQLite 后端不可用（见启动日志「[存储]」），导入无处可写。"
                    + "请先修好数据库再试。");
            return true;
        }
        if (source == null || !source.isDirectory()) {
            sender.sendMessage("§c没有找到 RSC 玩家数据目录，以下位置都试过了：");
            for (File c : sourceCandidates()) {
                sender.sendMessage("§8 - §7" + c.getAbsolutePath()
                        + " §8(" + (c.isDirectory() ? "存在" : "不存在") + ")");
            }
            sender.sendMessage("§7把 RSC 数据文件夹放到上面任一位置，或用 §f--source <目录> §7直接指定"
                    + "（目录结构须为 §f<命名空间>/<键>.json§7）。");
            return true;
        }

        final File src = source;
        final boolean doApply = apply;
        final boolean doOverwrite = overwrite;
        final Set<String> onlyNs = only;

        sender.sendMessage("§6[导入] §f源目录：§7" + src.getAbsolutePath());
        sender.sendMessage("§6[导入] §f模式：" + (doApply ? "§c写入（--apply）" : "§a预览（不会写入）")
                + (doOverwrite ? " §7+ 覆盖已有" : ""));
        if (!onlyNs.isEmpty()) sender.sendMessage("§6[导入] §f只导：" + String.join(", ", onlyNs));

        // 扫描要读成百上千个文件，放到异步线程；结果回主线程发消息
        Bukkit.getScheduler().runTaskAsynchronously(KT.plugin(), () -> {
            Store.ImportReport rep;
            try {
                rep = Store.importLegacy(src, onlyNs, doOverwrite, !doApply);
            } catch (Throwable t) {
                Bukkit.getScheduler().runTask(KT.plugin(), () ->
                        sender.sendMessage("§c[导入] 失败：" + t));
                return;
            }
            Bukkit.getScheduler().runTask(KT.plugin(), () -> report(sender, rep, doApply, onlyNs));
        });
        return true;
    }

    private void report(CommandSender sender, Store.ImportReport rep, boolean applied, Set<String> onlyNs) {
        if (rep.total() == 0) {
            sender.sendMessage("§7[导入] 源目录里没有可导入的玩家数据（*.json）。");
            return;
        }
        sender.sendMessage("§6[导入] §f" + rep.summary());
        if (!rep.perNs().isEmpty()) {
            List<String> parts = new ArrayList<>();
            int shown = 0;
            for (Map.Entry<String, Integer> e : rep.perNs().entrySet()) {
                parts.add(e.getKey() + " ×" + e.getValue());
                if (++shown >= 15) break;
            }
            sender.sendMessage("§7[导入] 命名空间（扫到的文件数）：" + String.join(" §8| §7", parts));
        }
        // --only 点名的命名空间在源目录里一个文件都没扫到 → 明确说出来，别让用户猜
        if (onlyNs != null && !onlyNs.isEmpty()) {
            List<String> missing = new ArrayList<>();
            for (String ns : onlyNs) {
                if (!rep.perNs().containsKey(ns)) missing.add(ns);
            }
            if (!missing.isEmpty()) {
                sender.sendMessage("§e[导入] 这些命名空间在源目录里没有扫到任何文件："
                        + String.join(", ", missing));
                sender.sendMessage("§e      请确认源目录下是否存在 §f<命名空间>/<键>.json§e。");
            }
        }
        if (!rep.samples().isEmpty()) {
            sender.sendMessage("§7[导入] 样例：" + String.join(", ", rep.samples()));
        }
        if (rep.failed() > 0) {
            sender.sendMessage("§c[导入] " + rep.failed() + " 份文件解析/写入失败，已跳过（原文件未动）。");
        }
        if (rep.skipped() > 0 && rep.created() == 0 && rep.overwritten() == 0) {
            sender.sendMessage("§e[导入] 库内已有同名文档，默认跳过（保护现有进度）。");
            sender.sendMessage("§e      如需用 RSC 数据覆盖库内进度，请加 §f--overwrite §e重新执行。");
        }
        if (applied) {
            Store.flush();   // 立刻落库，别等 5 分钟周期 —— 导入是显式管理动作
            sender.sendMessage("§a[导入] 已写入 SQLite。当前库内："
                    + describeNamespaces(Store.namespaces()));
        } else {
            sender.sendMessage("§e[导入] 这是预览。确认无误后执行 §f/km import --apply §e真正写入。");
        }
    }

    private static String describeNamespaces(Map<String, Integer> ns) {
        if (ns == null || ns.isEmpty()) return "（空）";
        List<String> parts = new ArrayList<>();
        int shown = 0;
        for (Map.Entry<String, Integer> e : ns.entrySet()) {
            parts.add(e.getKey() + " ×" + e.getValue());
            if (++shown >= 12) break;
        }
        return String.join(" §8| §7", parts);
    }

    /** 按候选顺序解析默认源目录；都不存在返回 null。 */
    private static File resolveSource() {
        for (File c : sourceCandidates()) {
            if (c.isDirectory()) return c;
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            if ("import".startsWith(args[0].toLowerCase(Locale.ROOT))) out.add("import");
            return out;
        }
        if (args.length >= 2 && "import".equalsIgnoreCase(args[0])) {
            String cur = args[args.length - 1].toLowerCase(Locale.ROOT);
            for (String opt : new String[]{"--apply", "--overwrite", "--source", "--only"}) {
                if (opt.startsWith(cur) && !Arrays.asList(args).contains(opt)) out.add(opt);
            }
        }
        return out;
    }
}
