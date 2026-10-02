"""静态自检：yml 里 script: 引用的名字，是否都能在 NativeScriptRegistry 里找到。

这是阶段 2 最容易「静默失效」的地方：若名字对不上，ScriptLoader 只会返回 null，
物品照常注册但全部行为丢失 —— 编译不报错、启动也不报错，只有玩起来才发现机器是死的。
因此在这里提前比对。
"""
import re
import glob
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# 1) yml 里引用的脚本名
yml_scripts = set()
for f in glob.glob(os.path.join(ROOT, "content", "*.yml")):
    with open(f, encoding="utf-8") as fh:
        for m in re.finditer(r"^\s*script:\s*(.+?)\s*$", fh.read(), re.M):
            v = m.group(1).strip().strip('"').strip("'")
            if v:
                yml_scripts.add(v)

# 2) 注册表里注册的名字
reg = os.path.join(ROOT, "src/main/java/tech/komutech/native_scripts/NativeScriptRegistry.java")
with open(reg, encoding="utf-8") as fh:
    src = fh.read()
registered = set(re.findall(r'register\(\s*"([^"]+)"', src))

print("yml 引用的脚本名:", len(yml_scripts))
print("注册表注册名    :", len(registered))
print()
missing = sorted(s for s in yml_scripts if s not in registered)
if missing:
    print("=== yml 引用但注册表没有（会导致该物品无行为）===")
    for m in missing:
        print("   MISSING:", m)
else:
    print("=== 全部命中，无缺失 ===")

unused = sorted(s for s in registered if s not in yml_scripts)
print()
print("注册了但 yml 未引用:", len(unused))
for u in unused[:15]:
    print("   ", u)
