package tech.komutech.load;

import org.jetbrains.annotations.Nullable;
import tech.komutech.KT;
import tech.komutech.native_scripts.NativeScript;
import tech.komutech.native_scripts.NativeScriptEval;
import tech.komutech.native_scripts.NativeScriptRegistry;
import tech.komutech.script.ScriptEval;

/**
 * 按脚本名取 Java 单例并包成 {@link ScriptEval}。
 *
 * <p>对应原 {@code ScriptEvalLoader.load}，但去掉了它那层已经失效的 .js 文件逻辑：
 * 原实现先判断 {@code new File(scriptsFolder, name + ".js")} 是否存在，若不存在但注册表
 * 里有该 id，仍然会 {@code new JavaScriptEval(file, addon)} —— 而 JavaScriptEval 内部只把
 * 这个 File 用来算相对路径当 registry key（{@code resolveScriptPath}），文件内容从未被读取。
 * 也就是说 .js 存在与否对行为毫无影响，真正生效的始终是 {@link NativeScriptRegistry} 里的
 * Java 单例。新的单插件形态没有 scripts 目录，因此直接查注册表。
 */
public final class ScriptLoader {

    private ScriptLoader() {}

    /** 加载脚本；找不到时记录一条日志并返回 null（调用方按「无脚本」处理）。 */
    @Nullable
    public static ScriptEval load(@Nullable String name, String kind, String itemId) {
        if (name == null || name.isBlank()) {
            return null;
        }
        NativeScript script = NativeScriptRegistry.get(name);
        if (script == null) {
            KT.log("加载" + kind + " " + itemId + " 时找不到脚本: " + name);
            return null;
        }
        return new NativeScriptEval(name, script);
    }
}
