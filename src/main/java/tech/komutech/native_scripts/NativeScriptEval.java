package tech.komutech.native_scripts;

import org.bukkit.plugin.Plugin;
import tech.komutech.script.ScriptEval;

/**
 * 把 {@link ScriptEval} 的函数调用转发给 Java 单例 {@link NativeScript}。
 *
 * <p>行为与原实现逐条对应：init 触发生命周期注册（{@link NativeLifecycleScript#registerLifecycle}），
 * 其余函数名原样转发到 {@code script.invoke}。
 *
 * <p>与原实现的差异：原构造函数签名是 {@code (String name, ProjectAddon addon, NativeScript script)}，
 * 只为拼一个 {@code .native} 文件路径（从未被读取）。新的单插件形态没有附属目录，
 * 因此改为 {@code (String name, NativeScript script)}，脚本名仅用于标识。
 */
public class NativeScriptEval extends ScriptEval {
   private final NativeScript script;

   public NativeScriptEval(String name, NativeScript script) {
      this.script = script;
   }

   @Override
   protected void contextInit() {
   }

   @Override
   public void addThing(String var1, Object var2) {
   }

   @Override
   public String key() {
      return "java";
   }

   @Override
   public Object evalFunction(String var1, Object... var2) {
      if ("init".equals(var1)) {
         if (this.script instanceof NativeLifecycleScript var3) {
            Plugin var5 = NativePluginHelper.getPlugin();
            if (var5 != null) {
               var3.registerLifecycle(var5);
            }
         }

         return null;
      } else {
         return this.script.invoke(var1, var2);
      }
   }

   @Override
   public void close() {
   }
}
