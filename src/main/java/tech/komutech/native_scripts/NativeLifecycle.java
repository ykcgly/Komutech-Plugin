package tech.komutech.native_scripts;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.plugin.Plugin;

public final class NativeLifecycle {
   /**
    * 已完成生命周期注册的脚本实例（identity 语义，脚本自身可能重写 equals）。
    *
    * <p>为什么需要它：{@code registerLifecycle} 有<b>两个</b>调用入口，且历史上都存在——
    * <ol>
    *    <li>{@link NativeScriptEval#evalFunction} 的 {@code "init"} 分支：每条 yml 条目在构造
    *        机器/菜单时都会 {@code doInit()} 一次。同一脚本被 N 条 yml 引用就会被调 N 次，
    *        而 {@code PuTuanScript} 等脚本在 registerLifecycle 里直接 {@code registerEvents}，
    *        于是监听器会注册 N 遍（玩家潜行事件被处理 N 次）。</li>
    *    <li>{@code NativeScriptRegistry.registerLifecycleListeners} → {@code registerAll}：
    *        用于兜住那些<b>只被 groups.yml 的 actions 引用</b>的脚本（如 {@code L-菜单/玩家属性}），
    *        它们走 {@code evalFunction("onButtonGroupClick")}，不经过 {@code doInit}，
    *        因此注册入口 1 覆盖不到。</li>
    * </ol>
    * 两个入口都必须保留（缺一则功能缺失），但都必须幂等，否则叠加注册会重复注册监听器。
    */
   private static final Set<NativeLifecycleScript> REGISTERED =
      Collections.newSetFromMap(new IdentityHashMap<>());

   private NativeLifecycle() {
   }

   /** 幂等注册入口：同一脚本实例只会真正注册一次。 */
   public static void registerLifecycleOnce(NativeLifecycleScript script, Plugin plugin) {
      if (script != null && plugin != null && REGISTERED.add(script)) {
         script.registerLifecycle(plugin);
      }
   }

   /** 仅供测试/重载场景使用：清空注册记录。 */
   public static void resetRegistered() {
      REGISTERED.clear();
   }

   public static void registerAll(Map<String, NativeScript> var0, Plugin var1) {
      for (NativeScript var3 : var0.values()) {
         if (var3 instanceof NativeLifecycleScript var4) {
            registerLifecycleOnce(var4, var1);
         }
      }
   }

   public static void registerAll(Collection<NativeScript> var0, Plugin var1) {
      for (NativeScript var3 : var0) {
         if (var3 instanceof NativeLifecycleScript var4) {
            registerLifecycleOnce(var4, var1);
         }
      }
   }
}
