# -*- coding: utf-8 -*-
"""
菜单自检：验证 menus.yml 的条目与「会取用菜单的机器」能否对上。

背景（最易静默失效的点）：
  机器 Loader 用 KT.menus.get(upper(id)) 取菜单；取不到时拿到 null，
  机器**照常注册**，但 UI 退化成 Slimefun 默认布局、进度条槽失效、脚本点击处理器丢失——
  编译、启动、日志全都不报错，只有进游戏才发现机器是「哑」的。
  模板机(11 台)更严格：TemplateMachinesLoader 强制要求菜单存在且已设进度槽，否则整台跳过。

检查项：
  1. menus.yml 每个条目的结构（是否有 slots/matrix/import 之一）
  2. 机器类文件里「预期需要菜单」的条目，其 id 是否能在 menus.yml 中找到
  3. 反向：menus.yml 里无人使用的孤立菜单（仅提示，不算错）
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONTENT = os.path.join(ROOT, "content")

# 会按 upper(id) 从 KT.menus 取菜单的内容文件
MACHINE_FILES = [
    "machines.yml",
    "recipe_machines.yml",
    "workbenches.yml",
    "template_machines.yml",
    "mb_machines.yml",
    "mat_generators.yml",
]


def top_keys(path):
    """取 yml 顶层键（跳过注释与空行）。"""
    keys = []
    if not os.path.isfile(path):
        return keys
    for line in open(path, encoding="utf-8"):
        if not line or line[0] in " #\t":
            continue
        m = re.match(r"^([^\s:#][^:]*):\s*$", line.rstrip("\n"))
        if m:
            keys.append(m.group(1).strip())
    return keys


def has_block(path, key, block):
    """判断某顶层条目下是否存在给定缩进块（简易实现，够用即可）。"""
    if not os.path.isfile(path):
        return False
    lines = open(path, encoding="utf-8").read().split("\n")
    inside = False
    base = None
    for line in lines:
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        m = re.match(r"^(\s*)([^\s:#][^:]*):", line)
        if not m:
            continue
        indent = len(m.group(1))
        name = m.group(2).strip()
        if indent == 0:
            inside = name == key
            continue
        if inside and indent == 2 and name == block:
            return True
    return False


def main():
    menu_path = os.path.join(CONTENT, "menus.yml")
    menu_keys = [k.strip('"').strip("'") for k in top_keys(menu_path)]
    menu_upper = {k.upper(): k for k in menu_keys}
    print("menus.yml 条目:", len(menu_keys))

    # 1. 结构完整性
    broken = []
    for k in menu_keys:
        if not (has_block(menu_path, k, "slots") or has_block(menu_path, k, "matrix")
                or has_block(menu_path, k, "import")):
            broken.append(k)
    print("\n=== 结构异常（无 slots/matrix/import 之一） ===")
    for b in broken:
        print("  ", b)
    if not broken:
        print("   无")

    # 2. 机器需要的菜单是否都存在
    print("\n=== 机器条目是否都有对应菜单 ===")
    missing_total = 0
    hit_total = 0
    for f in MACHINE_FILES:
        p = os.path.join(CONTENT, f)
        if not os.path.isfile(p):
            print(f"  {f}: 文件不存在，跳过")
            continue
        keys = [k.strip('"').strip("'") for k in top_keys(p)]
        missing = [k for k in keys if k.upper() not in menu_upper]
        hit = len(keys) - len(missing)
        hit_total += hit
        missing_total += len(missing)
        print(f"  {f}: 总 {len(keys)}, 命中菜单 {hit}, 缺菜单 {len(missing)}")
        for m in missing[:12]:
            print("      MISSING:", m)
        if len(missing) > 12:
            print(f"      ... 另有 {len(missing) - 12} 条")

    print(f"\n合计：命中 {hit_total}, 缺菜单 {missing_total}")

    # 3. 孤立菜单
    used = set()
    for f in MACHINE_FILES:
        p = os.path.join(CONTENT, f)
        if os.path.isfile(p):
            used.update(k.upper() for k in top_keys(p))
    orphan = [k for k in menu_keys if k.upper() not in used]
    print(f"\n=== 未被机器引用的菜单（{len(orphan)} 条，属正常：纯展示/分页/教程菜单本就不绑定机器） ===")
    for o in orphan[:10]:
        print("  ", o)
    if len(orphan) > 10:
        print(f"   ... 另有 {len(orphan) - 10} 条")

    return 0


if __name__ == "__main__":
    sys.exit(main())
