#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
清洗已抓取的《滴天髓阐微》文件：去掉古诗文网/古文岛注入的站点导航垃圾
（"古文岛"指纹头、"客户端下载…注释 译文"标签、"第00X章…----"章节导航、
下一章预览块等），保留 诗诀 + 【原注】 + 【任氏曰】 + 命例 正文。

用法
----
    python3 clean_ditiansui.py                 # 就地清洗 books/滴天髓阐微.txt
    python3 clean_ditiansui.py --in x.txt --out y.txt
"""

import argparse
import re


def clean_block(block):
    b = block
    # 1) 去掉 "《滴天髓阐微》上篇第0X章 <标题>" 这一多余的标题行
    b = re.sub(r'《滴天髓阐微》[上下]篇第\d+章\s*\S*', '', b)
    # 2) 去掉 "古文岛" 指纹头（从 "滴天髓阐微·…_古文岛_原古诗文网" 吃到 "注释 译文"）
    b = re.sub(r'滴天髓阐微·[^_]*_古文岛_原古诗文网\s*', '', b)
    b = re.sub(r'古文岛\s*客户端下载.*?注释\s*译文\s*', '', b, flags=re.S)
    # 3) 通用去掉残留的 "注释 译文" / "原文 赏析 注释 译文" 标签
    b = re.sub(r'原文\s*赏析\s*注释\s*译文\s*', '', b)
    b = re.sub(r'注释\s*译文\s*', '', b)
    # 4) 从页脚 / 下一章预览处截断（这些都在正文之后）
    b = re.split(r'目录\s*下一章', b)[0]
    b = re.split(r'©\s*古诗文网', b)[0]
    b = re.split(r'第\d+章\s*《滴天髓阐微》', b)[0]
    # 5) 规范化 **【原注】** -> 【原注】
    b = b.replace('**【原注】**', '【原注】')
    # 6) 收尾：合并空白、去多余空行
    b = re.sub(r'[ \t]+', ' ', b)
    b = re.sub(r'\n{2,}', '\n', b).strip()
    return b


def main():
    ap = argparse.ArgumentParser(description="清洗《滴天髓阐微》抓取文件")
    ap.add_argument("--in", dest="inp", default="books/滴天髓阐微.txt")
    ap.add_argument("--out", default=None, help="输出路径(默认就地覆盖 --in)")
    args = ap.parse_args()
    out = args.out or args.inp

    text = open(args.inp, encoding="utf-8").read()
    # 按 "第00X章 《滴天髓阐微》第 X 章\n----" 切分章节
    parts = re.split(r'\n?第\d+章\s*《滴天髓阐微》第\s*\d+\s*章\s*\n-{10,}\n', text)
    chapters = []
    for p in parts:
        p = p.strip()
        if not p:
            continue
        title = p.splitlines()[0].strip()
        body = clean_block(p)
        if not body:
            continue
        chapters.append((title, body))

    with open(out, "w", encoding="utf-8") as f:
        for title, body in chapters:
            f.write(f"【{title}】\n{body}\n\n")

    print(f"清洗完成 -> {out}  共 {len(chapters)} 章")


if __name__ == "__main__":
    main()
