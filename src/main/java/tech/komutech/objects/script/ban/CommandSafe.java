package tech.komutech.objects.script.ban;

import java.util.List;
import java.util.Locale;

/**
 * 控制台命令安全校验（物品组按钮的 {@code console} 动作）。
 *
 * <p>历史缺陷：旧实现直接拿配置里的整段字符串与黑名单做<b>全等</b>匹配，
 * 导致 {@code minecraft:op}、{@code bukkit:ban}、{@code luckperms:user ...} 这类
 * 带命名空间前缀的写法可以完整绕过黑名单，以控制台身份执行高危命令。
 *
 * <p>现在统一归一化后再比对：去前导 {@code /} → 取首个 token（剥离参数）→
 * 剥离 {@code namespace:} 前缀 → 转小写。因此黑名单只维护<b>裸命令名</b>，
 * 命名空间前缀不再影响拦截结果。
 */
public class CommandSafe {
   private static final List<String> badCommands = List.of(
      // ── 服务器生命周期 ──────────────────────────────
      "stop",
      "restart",
      "reload",
      "rl",
      "save-all",
      "save-off",
      "save-on",
      // ── 权限 / 提权 ────────────────────────────────
      "op",
      "deop",
      "sudo",
      "execute",
      // ── 封禁 / 踢出 ────────────────────────────────
      "ban",
      "ban-ip",
      "banlist",
      "pardon",
      "pardon-ip",
      "unban",
      "kick",
      // ── 白名单 ─────────────────────────────────────
      "whitelist",
      // ── 权限插件 ───────────────────────────────────
      "luckperms",
      "lp",
      "permissions",
      "perms",
      "groupmanager",
      "manuadd",
      // ── 插件管理（可加载任意 jar）───────────────────
      "plugman",
      "plugmanx",
      "plugmanb",
      "pluginmanager",
      // ── 综合工具插件的高危子命令 ────────────────────
      "jail",
      "unjail",
      "mute",
      "unmute",
      "vanish",
      "invsee",
      "enderchest",
      "clearchat",
      // ── 以控制台身份给玩家能力（等价于变相提权）──────
      "gamemode",
      "give",
      "xp",
      "experience",
      "effect",
      "attribute",
      "sethealth",
      "heal",
      "feed",
      "kill",
      "tp",
      "tphere",
      // ── 原版高危（可造成破坏或大量实体）──────────────
      "function",
      "datapack",
      "setblock",
      "fill",
      "clone",
      "summon",
      "setworldspawn",
      "difficulty",
      "worldborder"
   );

   /**
    * 归一化命令名：去前导 {@code /} → 取首段 → 剥离 {@code namespace:} → 小写。
    *
    * <p>例：{@code "/minecraft:op Notch"} → 命中 {@code op}；
    * {@code "luckperms:user x parent add admin"} → 命中 {@code luckperms}。
    */
   static String normalize(String raw) {
      return firstToken(raw).toLowerCase(Locale.ROOT);
   }

   /** 取出命令名本体（去前导 {@code /}、取首个 token、保留 {@code namespace:} 原样）。 */
   private static String firstToken(String raw) {
      if (raw == null) {
         return "";
      }
      String trimmed = raw.trim();
      while (trimmed.startsWith("/")) {
         trimmed = trimmed.substring(1).trim();
      }
      if (trimmed.isEmpty()) {
         return "";
      }
      // 只取命令名本体，参数不参与匹配
      return trimmed.split("\\s+")[0];
   }

   /**
    * 判断是否为高危命令。
    *
    * <p>对带命名空间的写法（{@code namespace:sub}）同时比对三段，任一命中黑名单即拦截：
    * <ol>
    *    <li>完整 token，如 {@code cmi:sudo}</li>
    *    <li>命名空间（{@code namespace}），如 {@code luckperms:user} → {@code luckperms}</li>
    *    <li>子命令（{@code sub}），如 {@code minecraft:op} → {@code op}</li>
    * </ol>
    * 只比对「子命令」会漏掉 {@code luckperms:user} 这类以子命令开头的写法，
    * 只比对「命名空间」又会漏掉 {@code minecraft:op}，因此三段都必须查。
    *
    * <p>命令为空或无法解析时按<b>拒绝</b>处理（fail-closed）。
    */
   public static boolean isBadCommand(String command) {
      String head = firstToken(command);
      if (head.isEmpty()) {
         return true;
      }

      int colon = head.indexOf(':');
      if (colon < 0) {
         return badCommands.contains(head.toLowerCase(Locale.ROOT));
      }

      String namespace = head.substring(0, colon);
      String sub = head.substring(colon + 1);
      return badCommands.contains(head.toLowerCase(Locale.ROOT))
         || badCommands.contains(namespace.toLowerCase(Locale.ROOT))
         || badCommands.contains(sub.toLowerCase(Locale.ROOT));
   }
}
