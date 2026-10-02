"""把原工程的脚本层合并搬运到新工程 tech.komutech.native_scripts。

原工程脚本实现分散在两个包：
  tech/komutech/native_scripts/**   实现（101 个文件，包名已是 tech.komutech）
  tech/origintech/native_scripts/**  核心接口 + support（包名是 tech.origintech）

两边 support/ 文件名无冲突，可安全合并到同一目录；唯一重名是
NativeScriptRegistry.java —— 以 tech.komutech 的实现为准（它才是 bootstrap 用的）。

搬运时统一把 import tech.origintech.native_scripts.X 改写为 tech.komutech.native_scripts.X。
"""
import os
import re
import shutil

OLD_ROOT = r"D:/仓库源码/口木科技/KomutechNative-src/src/main/java"
NEW_ROOT = r"D:/仓库源码/口木科技/Komutech-Plugin/src/main/java"
PKG = "tech/komutech/native_scripts"

# (源目录, 是否改写包名)
# 注意：tech/komutech 侧的实现文件 import 的正是 tech.origintech.native_scripts 的
# 接口类，因此两侧都必须改写包名（第一版只对第二侧改写，导致残留 100+ 处）。
SOURCES = [
    (os.path.join(OLD_ROOT, "tech/komutech/native_scripts"), True),
    (os.path.join(OLD_ROOT, "tech/origintech/native_scripts"), True),
]

REWRITE = [
    (re.compile(r"\btech\.origintech\.native_scripts\."), "tech.komutech.native_scripts."),
    (re.compile(r"\bpackage tech\.origintech\.native_scripts"), "package tech.komutech.native_scripts"),
]

stats = {"copied": 0, "skipped_dup": 0, "rewritten": 0}


def walk(src):
    for dirpath, _, files in os.walk(src):
        for f in files:
            if f.endswith(".java"):
                yield os.path.join(dirpath, f)


def main():
    for src_dir, do_rewrite in SOURCES:
        for path in walk(src_dir):
            rel = os.path.relpath(path, src_dir).replace("\\", "/")
            dst = os.path.join(NEW_ROOT, PKG, rel.replace("/", os.sep))
            # 重名的 NativeScriptRegistry 以 tech.komutech 版本为准
            if rel == "NativeScriptRegistry.java" and do_rewrite:
                stats["skipped_dup"] += 1
                continue
            text = open(path, encoding="utf-8").read()
            if do_rewrite:
                for pat, rep in REWRITE:
                    text, n = pat.subn(rep, text)
                    stats["rewritten"] += n
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            open(dst, "w", encoding="utf-8", newline="\n").write(text)
            stats["copied"] += 1

    print("copied:", stats["copied"])
    print("skipped(dup registry):", stats["skipped_dup"])
    print("import rewritten:", stats["rewritten"])

    # 残留检查
    leftover = []
    for dirpath, _, files in os.walk(os.path.join(NEW_ROOT, PKG)):
        for f in files:
            if not f.endswith(".java"):
                continue
            p = os.path.join(dirpath, f)
            t = open(p, encoding="utf-8").read()
            for bad in ["tech.origintech.", "package tech.origintech"]:
                if bad in t:
                    leftover.append(os.path.relpath(p, NEW_ROOT).replace("\\", "/") + " -> " + bad)
    print("--- 残留 tech.origintech 引用 ---")
    for x in sorted(set(leftover)):
        print("  ", x)
    if not leftover:
        print("   无")


if __name__ == "__main__":
    main()
