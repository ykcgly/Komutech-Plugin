package tech.komutech.native_scripts.support;

import org.bukkit.Bukkit;

/**
 * 管理员高危操作密码（集中管理，唯一事实来源）。
 *
 * <p>配置位置：{@code plugins/Komutech/config.yml}（首次启动自动从 jar 内
 * {@code default_config.yml} 模板生成）。
 *
 * <p>安全设计：两个密码<b>默认留空</b>，留空即视为「未配置」，对应的高危操作
 * （清空全部存储 / 删除玩家存储）会被直接拒绝。这样即使服主忘记改密码，
 * 也不会退化成「空密码 == 无需密码」或「弱默认密码」被他人利用。
 */
public final class KomutechAdminPassword {
   /** 萬象匱「清空所有存储」确认密码。 */
   public static final String KEY_CLEAR_ALL = "L_ZJ_WXG_MM";
   /** 管理员「删除玩家存储」确认密码。 */
   public static final String KEY_ADMIN_DELETE = "KOMUTECH_WJSX_MM";

   private KomutechAdminPassword() {
   }

   private static String read(String key) {
      String var0 = KomutechAddonConfig.getString(key, "");
      return var0 == null ? "" : var0.trim();
   }

   /** 萬象匱全局清除密码；未配置时返回空串。 */
   public static String clearAllPassword() {
      return read(KEY_CLEAR_ALL);
   }

   /** 管理员删除密码；未配置时返回空串。 */
   public static String adminDeletePassword() {
      return read(KEY_ADMIN_DELETE);
   }

   /** 是否已配置「清空所有存储」密码。未配置则该操作应被拒绝。 */
   public static boolean isClearAllAllowed() {
      return !clearAllPassword().isEmpty();
   }

   /** 是否已配置「管理员删除」密码。未配置则该操作应被拒绝。 */
   public static boolean isAdminDeleteAllowed() {
      return !adminDeletePassword().isEmpty();
   }

   /** 校验「清空所有存储」密码；未配置时恒为 false。 */
   public static boolean verifyClearAll(String input) {
      return isClearAllAllowed() && clearAllPassword().equals(normalize(input));
   }

   /** 校验「管理员删除」密码；未配置时恒为 false。 */
   public static boolean verifyAdminDelete(String input) {
      return isAdminDeleteAllowed() && adminDeletePassword().equals(normalize(input));
   }

   private static String normalize(String input) {
      return input == null ? "" : input.trim();
   }

   /** 启动自检：若高危操作密码未配置，向控制台输出醒目告警。 */
   public static void warnIfUnconfigured() {
      StringBuilder var0 = new StringBuilder();

      if (!isClearAllAllowed()) {
         var0.append("  - ").append(KEY_CLEAR_ALL).append("（清空所有存储）未配置，该操作已被禁用").append('\n');
      }

      if (!isAdminDeleteAllowed()) {
         var0.append("  - ").append(KEY_ADMIN_DELETE).append("（删除玩家存储）未配置，该操作已被禁用").append('\n');
      }

      if (var0.length() > 0) {
         Bukkit.getLogger().warning("========== 口木科技 安全提示 ==========");
         Bukkit.getLogger().warning("以下高危操作密码未配置，相关功能处于禁用状态：");
         Bukkit.getLogger().warning(var0.toString().stripTrailing());
         Bukkit.getLogger().warning("请编辑 plugins/Komutech/config.yml 填写密码后重启服务器。");
         Bukkit.getLogger().warning("=======================================");
      }
   }
}
