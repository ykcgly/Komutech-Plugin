#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
saveditem 内联展开工具（一次性内容转换）。

把 Komutech 原附属里任意位置的 `material_type: saveditem` + `material: <模板相对路径>`
配对，展开为内嵌的 Bukkit 序列化 ItemStack（material_type: serialized + serialized: 段）。
覆盖物品自身的 item: 段与配方 recipe: 槽位（原统计 101 处引用分布在两处）。

行为等价性（与 CommonUtils.readItem 的 saveditem 分支逐一对应）：
  1. 基础堆 = 模板文件 item: 段反序列化出的 ItemStack（含附魔/不可破坏/属性修饰等全部 meta）
  2. 再用引用处自身的 name / lore 覆盖（原实现 RSCItemStack：name 非空则 setDisplayName，
     lore 非空则 setLore）
  3. 数量强制为 1（原实现 RSCItemStack.setAmount(1)，仅作用于物品主体；配方槽位按 countable 走 amount）
因此转换保留引用处原有的 name/lore 键，由加载器按同样规则叠加。
"""
import os
import re
import shutil

SRC_ADDON = r"D:/仓库源码/口木科技/KomutechNative-src/src/main/resources/addon"
DST_CONTENT = r"D:/仓库源码/口木科技/Komutech-Plugin/content"

# 参与转换的内容文件（含可能出现 saveditem 的全部文件）
CONTENT_FILES = [
    "items.yml", "machines.yml", "foods.yml", "mob_drops.yml", "geo_resources.yml",
    "recipe_machines.yml", "mb_machines.yml", "linked_recipe_machines.yml",
    "template_machines.yml", "workbenches.yml", "menus.yml", "groups.yml",
    "recipe_types.yml", "armors.yml", "capacitors.yml", "mat_generators.yml",
    "generators.yml", "solar_generators.yml", "simple_machines.yml", "supers.yml",
    "generations.yml", "researches.yml",
]

_cache = {}


def read_template(path_no_ext):
    """读取 saveditems/<path>.yml 的 item: 段原始行"""
    if path_no_ext in _cache:
        return _cache[path_no_ext]
    f = os.path.join(SRC_ADDON, "saveditems", path_no_ext + ".yml")
    if not os.path.isfile(f):
        _cache[path_no_ext] = None
        return None
    with open(f, "r", encoding="utf-8") as fh:
        lines = fh.read().split("\n")
    start = None
    for i, ln in enumerate(lines):
        if re.match(r"^item:\s*$", ln):
            start = i + 1
            break
    if start is None:
        _cache[path_no_ext] = None
        return None
    body = []
    for ln in lines[start:]:
        if ln.strip() == "":
            continue
        if re.match(r"^\S", ln):  # 顶层键出现即结束
            break
        body.append(ln)
    _cache[path_no_ext] = body
    return body


def template_type(body):
    for ln in body:
        m = re.match(r"^\s*type:\s*(\S+)", ln)
        if m:
            return m.group(1)
    return "STONE"


def convert_text(text, stats):
    lines = text.split("\n")
    out = []
    i = 0
    n = len(lines)
    while i < n:
        ln = lines[i]
        m = re.match(r"^(\s*)material_type:\s*saveditem\s*$", ln)
        if not m:
            out.append(ln)
            i += 1
            continue
        indent = m.group(1)
        # 找紧随的 material: 行（允许中间空行）
        j = i + 1
        mat_val = None
        while j < n and j <= i + 3:
            cur = lines[j]
            if cur.strip() == "":
                j += 1
                continue
            m2 = re.match(r"^" + indent + r"material:\s*(.+?)\s*$", cur)
            if m2:
                mat_val = m2.group(1).strip().strip("'\"")
                break
            break
        if not mat_val:
            out.append(ln)
            i += 1
            continue
        body = read_template(mat_val)
        if body is None:
            stats["missing"].append(mat_val)
            out.append(ln)
            i += 1
            continue
        base = " " * len(indent)
        out.append(f"{base}material_type: serialized")
        out.append(f"{base}material: {template_type(body)}")
        out.append(f"{base}serialized:")
        # 保留模板内部的相对缩进：以模板主体最小缩进为基准平移到目标子级缩进，
        # 否则 meta: 下的子键会被压平到同一层，Bukkit 反序列化将失败
        target_child = len(indent) + 2
        body_indents = [len(b) - len(b.lstrip(" ")) for b in body if b.strip()]
        min_ind = min(body_indents) if body_indents else 0
        for b in body:
            if not b.strip():
                out.append(b)
                continue
            cur = len(b) - len(b.lstrip(" "))
            out.append(" " * (target_child + (cur - min_ind)) + b.lstrip(" "))
        stats["inline"] += 1
        stats["templates"].add(mat_val)
        i = j + 1  # 跳过被替换的 material 行
    return "\n".join(out)


def main():
    os.makedirs(DST_CONTENT, exist_ok=True)
    stats = {"inline": 0, "missing": [], "templates": set()}
    for name in CONTENT_FILES:
        src = os.path.join(SRC_ADDON, name)
        if not os.path.isfile(src):
            continue
        with open(src, "r", encoding="utf-8") as fh:
            text = fh.read()
        new = convert_text(text, stats)
        with open(os.path.join(DST_CONTENT, name), "w", encoding="utf-8", newline="\n") as fh:
            fh.write(new)
    for name in os.listdir(SRC_ADDON):
        if name == "saveditems":
            continue
        p = os.path.join(SRC_ADDON, name)
        if os.path.isfile(p) and not os.path.exists(os.path.join(DST_CONTENT, name)):
            shutil.copy2(p, os.path.join(DST_CONTENT, name))
    print("内联 saveditem 引用数:", stats["inline"])
    print("涉及模板数:", len(stats["templates"]))
    if stats["missing"]:
        print("缺失模板:", stats["missing"][:20])


if __name__ == "__main__":
    main()
