"""把原工程的物品/机器/脚本求值类族搬到新工程。

搬运范围：tech/origintech/objects/{customs,machine,script}  -> tech/komutech/objects/{...}

包名与符号替换规则（每条都对应一个真实依赖，缺一不可）：
  tech.origintech.objects.          -> tech.komutech.objects.
  tech.origintech.utils.            -> tech.komutech.util.
  tech.origintech.libraries.colors. -> tech.komutech.util.colors.
  tech.origintech.native_scripts.   -> tech.komutech.native_scripts.
  tech.origintech.bulit_in.JavaScriptEval -> tech.komutech.script.ScriptEval
  OriginTechPlugin.INSTANCE         -> KT.plugin()          （插件实例）
  import ...OriginTechPlugin        -> import tech.komutech.KT
"""
import os
import re

OLD = r"D:/仓库源码/口木科技/KomutechNative-src/src/main/java/tech/origintech/objects"
NEW = r"D:/仓库源码/口木科技/Komutech-Plugin/src/main/java/tech/komutech/objects"

SUBDIRS = ["customs", "machine", "script"]

RULES = [
    (re.compile(r"\btech\.origintech\.objects\."), "tech.komutech.objects."),
    (re.compile(r"\btech\.origintech\.utils\."), "tech.komutech.util."),
    (re.compile(r"\btech\.origintech\.libraries\.colors\."), "tech.komutech.util.colors."),
    (re.compile(r"\btech\.origintech\.native_scripts\."), "tech.komutech.native_scripts."),
    (re.compile(r"\btech\.origintech\.bulit_in\.JavaScriptEval\b"), "tech.komutech.script.ScriptEval"),
    (re.compile(r"\bpackage tech\.origintech\.objects\b"), "package tech.komutech.objects"),
    # 插件实例：原实现到处用 OriginTechPlugin.INSTANCE 拿 plugin
    (re.compile(r"\bOriginTechPlugin\.INSTANCE\b"), "KT.plugin()"),
    (re.compile(r"^import tech\.origintech\.OriginTechPlugin;$", re.M), "import tech.komutech.KT;"),
    (re.compile(r"^import tech\.origintech\.RscCoexistBridge;$", re.M), ""),
]

count = 0
for sub in SUBDIRS:
    src_root = os.path.join(OLD, sub)
    for dirpath, _, files in os.walk(src_root):
        for f in files:
            if not f.endswith(".java"):
                continue
            src = os.path.join(dirpath, f)
            rel = os.path.relpath(src, src_root).replace("\\", "/")
            dst = os.path.join(NEW, sub, rel.replace("/", os.sep))
            text = open(src, encoding="utf-8").read()
            for pat, rep in RULES:
                text = pat.sub(rep, text)
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            open(dst, "w", encoding="utf-8", newline="\n").write(text)
            count += 1

print("copied:", count)

# 残留检查
leftover = []
for dirpath, _, files in os.walk(NEW):
    for f in files:
        if not f.endswith(".java"):
            continue
        p = os.path.join(dirpath, f)
        t = open(p, encoding="utf-8").read()
        for bad in ["tech.origintech.", "ProjectAddon", "OriginTechPlugin"]:
            if bad in t:
                leftover.append(os.path.relpath(p, NEW).replace("\\", "/") + "  -> " + bad)
print("--- 残留 ---")
for x in sorted(set(leftover)):
    print("  ", x)
if not leftover:
    print("   无")
