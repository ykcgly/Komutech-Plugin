# -*- coding: utf-8 -*-
"""
菜单自检：验证 menus.yml 与「会取用菜单的机器」能否对上。

背景（最易静默失效的点）：
  机器 Loader 用 KT.menus.get(upper(id)) 取菜单；取不到时拿到 null，机器**照常注册**，
  但 UI 会退化成 Slimefun 默认布局（CustomNoEnergyMachine 等类均有 `if (menu != null)` 兜底，
  不会崩、不会注册失败）。所以「缺菜单」绝大多数情况是**设计如此**，不是 bug。

判定分级（重要，勿简单地把所有缺菜单都当错误）：
  [错误] menus.yml 里有条目，却没有任何机器按 id 引用它
         → 极可能是 id 拼写错/大小写不符，导致该菜单永远不会被挂上，属于真 bug。
  [错误] template_machines.yml 缺菜单
         → TemplateMachinesLoader 强制要求菜单存在且已设进度槽，缺了整台机器直接跳过。
  [提示] machines.yml 中 hidden:true 或无 script 的条目缺菜单
         → 隐藏占位物品、纯展示/材料，走默认布局属正常。
  [信息] 其余缺菜单条目
         → 多为「手持右键触发脚本」的物品（悟道石、蒲团、灵珠等），
            本就不需要 BlockMenu；mb_machines.yml 更是完全不支持菜单（走多方块专属 UI）。

用法：python tools/check_menus.py
退出码：0 = 无错误（可能仍有提示/信息）；1 = 存在 [错误] 级问题
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONTENT = os.path.join(ROOT, "content")

# 按 upper(id) 从 KT.menus 取菜单的内容文件
MACHINE_FILES = [
    "machines.yml",
    "recipe_machines.yml",
    "workbenches.yml",
    "template_machines.yml",
    "mb_machines.yml",
    "mat_generators.yml",
]

# 不支持 BlockMenu 的文件：多方块机器走自己的专属 UI（MultiBlockMachinesLoader /
# CustomMultiBlockMachine 中完全没有 menu 概念）
NO_BLOCKMENU_FILES = {"mb_machines.yml"}

# 强制要求菜单存在的文件：缺菜单则整台机器被跳过
STRICT_FILES = {"template_machines.yml"}

WS = r"[ \t]"


def parse_entries(path):
    """解析 yml 顶层条目 -> {id: {hidden, script, has_menu}}"""
    res = {}
    cur = None
    if not os.path.isfile(path):
        return res
    for line in open(path, encoding="utf-8"):
        raw = line.rstrip("\r\n")
        if not raw.strip() or raw.lstrip().startswith("#"):
            continue
        m = re.match(r"^([^\s:#][^:]*):" + WS + r"*$", raw)
        if m:
            cur = m.group(1).strip().strip('"').strip("'")
            res[cur] = {"hidden": False, "script": None, "has_menu": False}
        elif cur:
            h = re.match(r"^" + WS + r"+hidden:" + WS + r"*(.*)$", raw)
            if h:
                res[cur]["hidden"] = h.group(1).strip().lower() == "true"
            s = re.match(r"^" + WS + r"+script:" + WS + r"*(.+?)$", raw)
            if s:
                res[cur]["script"] = s.group(1).strip().strip('"').strip("'")
            if re.match(r"^" + WS + r"+menu:", raw):
                res[cur]["has_menu"] = True
    return res


def has_block(path, key, block):
    """判断某顶层条目下是否存在给定缩进块。"""
    if not os.path.isfile(path):
        return False
    inside = False
    for line in open(path, encoding="utf-8").read().split("\n"):
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        m = re.match(r"^(" + WS + r"*)([^\s:#][^:]*):", line)
        if not m:
            continue
        indent = len(m.group(1).expandtabs(1))
        name = m.group(2).strip()
        if indent == 0:
            inside = name == key
            continue
        if inside and indent == 2 and name == block:
            return True
    return False


def main():
    menu_path = os.path.join(CONTENT, "menus.yml")
    menus = parse_entries(menu_path)
    menu_upper = {k.upper() for k in menus}
    print("menus.yml 条目:", len(menus))

    errors = 0
    warns = 0
    infos = 0

    # ── 1. 菜单结构完整性 ────────────────────────────
    print("\n=== [错误] 结构异常（无 slots/matrix/import 之一）===")
    broken = [
        k for k in menus
        if not (has_block(menu_path, k, "slots") or has_block(menu_path, k, "matrix")
                or has_block(menu_path, k, "import"))
    ]
    for b in broken:
        print("   ", b)
    if not broken:
        print("    无")
    errors += len(broken)

    # ── 2. 正向：各机器文件的菜单命中情况（分级）──────
    print("\n=== 正向：机器条目与菜单的对应情况 ===")
    all_refs = set()
    for f in MACHINE_FILES:
        p = os.path.join(CONTENT, f)
        if not os.path.isfile(p):
            print(f"  {f}: 文件不存在，跳过")
            continue
        entries = parse_entries(p)
        all_refs |= {k.upper() for k in entries}
        miss = [k for k in entries if k.upper() not in menu_upper]
        hit = len(entries) - len(miss)

        if f in NO_BLOCKMENU_FILES:
            print(f"  {f}: 总 {len(entries)}，命中 {hit}，未配菜单 {len(miss)}"
                  f"  —— 该文件走多方块专属 UI，不使用 BlockMenu，无需菜单")
            continue

        if f in STRICT_FILES:
            bad = miss
            print(f"  {f}: 总 {len(entries)}，命中 {hit}，缺菜单 {len(bad)}"
                  f"  —— 强制要求菜单，缺则整台机器跳过")
            for m in bad[:12]:
                print("      [错误] MISSING:", m)
            if len(bad) > 12:
                print(f"      ... 另有 {len(bad) - 12} 条")
            errors += len(bad)
            continue

        # 普通机器文件：区分「提示」与「信息」
        soft = [k for k in miss if entries[k]["hidden"] or not entries[k]["script"]]
        info = [k for k in miss if k not in soft]
        print(f"  {f}: 总 {len(entries)}，命中 {hit}，未配菜单 {len(miss)}"
              f"  （提示 {len(soft)} / 信息 {len(info)}）")
        if soft:
            print("      [提示] 隐藏占位或无脚本，走默认布局属正常：")
            for m in soft[:6]:
                print("         ", m, "(hidden)" if entries[m]["hidden"] else "(无 script)")
            if len(soft) > 6:
                print(f"         ... 另有 {len(soft) - 6} 条")
        if info:
            print("      [信息] 手持脚本类物品，本就不需要 BlockMenu：")
            for m in info[:6]:
                print("         ", m)
            if len(info) > 6:
                print(f"         ... 另有 {len(info) - 6} 条")
        warns += len(soft)
        infos += len(info)

    # ── 3. 反向：孤立菜单（这才是真错误）──────────────
    print("\n=== [错误] 反向检查：menus.yml 中无任何机器按 id 引用的条目 ===")
    print("    （若这里非空，极可能是 id 拼写/大小写不符，导致菜单永远挂不上）")
    orphan = [k for k in menus if k.upper() not in all_refs]
    for o in sorted(orphan):
        print("   ", o)
    if not orphan:
        print("    无")
    errors += len(orphan)

    # ── 汇总 ────────────────────────────────────────
    print("\n=== 汇总 ===")
    print(f"  [错误] {errors} 项 —— 需修复")
    print(f"  [提示] {warns} 项 —— 隐藏/无脚本条目走默认布局，正常")
    print(f"  [信息] {infos} 项 —— 手持脚本类物品本就不需要 BlockMenu，正常")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
