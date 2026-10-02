package tech.komutech.script;

import org.jetbrains.annotations.Nullable;

/**
 * 脚本求值抽象层：机器 / 菜单 / 物品通过它调用行为脚本。
 *
 * <p>与原 {@code tech.origintech.objects.script.ScriptEval} 的语义完全一致（这是「不改变
 * 物品行为」的保证）：{@code doInit()} 触发 init、{@code evalFunction(name, args)} 分派行为。
 *
 * <p>与原实现的差异（有意为之，不影响行为）：原基类持有 {@code File} + {@code ProjectAddon}
 * 并读取 .js 文件内容到 {@code fileContext}。但本插件的实际生效路径是
 * {@code JavaScriptEval → NativeScriptEval → NativeScript.invoke}，{@code contextInit()} 是
 * 空实现、文件内容从未被使用——.js 文件只被用来拼 registry key。新的单插件形态里没有附属目录
 * 与 .js 资源，因此这两个字段连同文件读取一并去掉，直接按脚本名查注册表。
 */
public abstract class ScriptEval {

    protected ScriptEval() {}

    /** 实现标识（原实现返回 "java"）。 */
    public abstract String key();

    protected void contextInit() {}

    /** 原实现用于向脚本上下文注入变量；Java 路径下为空实现。 */
    public abstract void addThing(String name, Object value);

    public final void doInit() {
        evalFunction("init");
    }

    /** 调用脚本函数；返回 null 表示脚本无此函数或无需返回值。 */
    @Nullable
    public abstract Object evalFunction(String name, Object... args);

    public abstract void close();
}
