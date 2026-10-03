package tech.komutech.jeg;

/**
 * JEG（JustEnoughGuide）集成入口：仅负责两件事——
 * <ul>
 *   <li>检测 JEG 是否安装（供 {@link JegGuideListener} 决定是否注册指南事件拦截）；</li>
 *   <li>打开 JEG 指南主菜单（大配方菜单的返回按钮用；反射调用，JEG 未装时静默）。</li>
 * </ul>
 *
 * <p>对齐尘世百味 {@code com.haiman233.worldtaste.jeg.JegHook} 的形态。
 * 不集成 JEG 的 RecipeCompletable（机器内补全）：本附属的联动配方绑定槽大量超出
 * 3x3，JEG 的补全按钮放不下也展示不全，统一走自定义的
 * {@link tech.komutech.listeners.BigRecipeMenu}。</p>
 */
public final class JegHook {

    private JegHook() {}

    /**
     * JEG 是否可用。检测其指南事件类是否存在——本插件的 JEG 集成只依赖
     * {@code GuideEvents} 事件与 {@code GuideUtil} 的指南打开 API。
     *
     * <p>不能用「插件存在」判定： {@code JegGuideListener} 直接 import JEG API，
     * 只有事件类真的在 classpath 上时才允许类加载，否则 {@code NoClassDefFoundError}。</p>
     */
    public static boolean available() {
        try {
            Class.forName("com.balugaq.jeg.api.objects.events.GuideEvents");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * 打开 JEG 指南主菜单（异步入口，避免阻塞主线程渲染整本指南）。
     * JEG 未安装或 API 变更时静默——返回按钮属于锦上添花，不应报错刷屏。
     */
    public static void openGuide(org.bukkit.entity.Player p) {
        try {
            Class<?> clazz = Class.forName("com.balugaq.jeg.utils.GuideUtil");
            Class<?> modeCls = Class.forName("io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuideMode");
            Object mode = io.github.thebusybiscuit.slimefun4.core.guide.SlimefunGuide.class
                    .getMethod("getDefaultMode").invoke(null);
            clazz.getMethod("openMainMenuAsync", org.bukkit.entity.Player.class, modeCls, int.class)
                    .invoke(null, p, mode, 1);
        } catch (Throwable ignored) {
            // JEG 未安装或 API 变更：静默
        }
    }
}
