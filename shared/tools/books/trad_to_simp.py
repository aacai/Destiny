#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""将 composeResources/files/books 下古籍 txt 繁体转为简体（OpenCC t2s）。

用法：
  python3 trad_to_simp.py
  python3 trad_to_simp.py --only shenfengtongkao wuxingjingji
"""
from __future__ import annotations

import argparse
import os
import sys

try:
    from opencc import OpenCC
except ImportError:
    print("需要 opencc-python-reimplemented：pip3 install --user opencc-python-reimplemented",
          file=sys.stderr)
    sys.exit(1)

BOOKS_DIR = os.path.normpath(os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "../../src/commonMain/composeResources/files/books",
))

# OpenCC 偶发漏网的异体/旧字形
EXTRA = str.maketrans({
    "蔵": "藏",
    "徳": "德",
    "眞": "真",
    "逹": "达",
    "髙": "高",
    "竝": "并",
    "飮": "饮",
    "囘": "回",
    "𤣥": "玄",
    "㳺": "游",
    "熖": "焰",
    "専": "专",
    "衆": "众",
    "覇": "霸",
})


def convert_file(path: str, cc: OpenCC) -> tuple[int, int]:
    raw = open(path, encoding="utf-8").read()
    out = cc.convert(raw).translate(EXTRA)
    if out == raw:
        return 0, len(raw)
    open(path, "w", encoding="utf-8").write(out)
    # 粗估变化量
    changed = sum(1 for a, b in zip(raw, out) if a != b) + abs(len(raw) - len(out))
    return changed, len(out)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", nargs="*", help="只转换指定文件名（不含 .txt）")
    ap.add_argument("--dir", default=BOOKS_DIR)
    args = ap.parse_args()

    cc = OpenCC("t2s")
    names = args.only
    files = []
    for fn in sorted(os.listdir(args.dir)):
        if not fn.endswith(".txt"):
            continue
        stem = fn[:-4]
        if names and stem not in names and fn not in names:
            continue
        files.append(os.path.join(args.dir, fn))

    if not files:
        print("没有匹配的 txt", file=sys.stderr)
        return

    for path in files:
        changed, size = convert_file(path, cc)
        print(f"[ok] {os.path.basename(path)}  ~{changed} 处变化, {size} 字", flush=True)


if __name__ == "__main__":
    main()
