#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
从古诗文网(gushiwen.cn) / 古文岛(guwendao.net) 抓取《滴天髓阐微》全文，清洗为纯文本。

《滴天髓阐微》= 清·沈孝瞻 原著，徐乐吾(任铁樵) 评注。维基文库无此条目，
故改用古诗文网。该站结构：
  - 落地页  https://www.gushiwen.cn/guwen/book_74c064ea85bf.aspx  (阅读器壳)
  - 正文在移动版 iframe:  https://m.guwendao.net/guwen/book_74c064ea85bf.aspx
    该页的目录列出全部 64 章，每章是独立页 /guwen/bookv_<hash>.aspx
  - 每章页正文在 <div class="bookcont"> 内，含：诗诀 + 【原注】 + 【任氏曰】注评

用法
----
    python3 fetch_ditiansui.py                      # 下载到 ./books/滴天髓阐微.txt
    python3 fetch_ditiansui.py --out books
    python3 fetch_ditiansui.py --proxy socks5://127.0.0.1:10808

传输层复用 fetch_mingli_classics 的 http_get（优先 curl，支持 SOCKS/HTTP 代理与重试）。
"""

import argparse
import json
import os
import re
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from fetch_mingli_classics import http_get, install_proxy  # noqa: E402

BOOK_ID = "74c064ea85bf"
TOC_URL = f"https://m.guwendao.net/guwen/book_{BOOK_ID}.aspx"
MOBILE_UA = ("Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) "
             "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148")
OUT_NAME = "滴天髓阐微"


def fetch_chapter_list():
    """返回 [(标题, bookv_path), ...] 共 64 章，按目录顺序。"""
    html = http_get(TOC_URL, ua=MOBILE_UA)
    pairs = re.findall(
        r'href="/guwen/(bookv_[0-9a-f]+\.aspx)"[^>]*>([^<]+)</a>', html)
    # 去重保序（目录有时重复渲染）
    seen, out = set(), []
    for path, title in pairs:
        if path in seen:
            continue
        seen.add(path)
        out.append((title.strip(), path))
    return out


def chapter_title(html):
    """从 <title> 取章节名，如 '滴天髓阐微·上篇·天道第二_古文岛...' -> '上篇·天道第二'。"""
    m = re.search(r"<title>([^<]+)</title>", html, re.I)
    if not m:
        return ""
    t = m.group(1)
    t = t.replace("滴天髓阐微·", "").replace("_古文岛_原古诗文网", "")
    return t.strip()


def clean_chapter(html):
    """提取单章正文：去脚本/样式 -> 剥标签 -> 去掉站名指纹头/标签/页脚/下一章预览。"""
    html = re.sub(r"<script[\s\S]*?</script>", "", html, flags=re.I)
    html = re.sub(r"<style[\s\S]*?</style>", "", html, flags=re.I)
    m = re.search(r'<div class="bookcont"[^>]*>([\s\S]*?)(?:<div class="|</div>\s*<div)',
                  html, re.I)
    seg = m.group(1) if m else html
    b = re.sub(r"<[^>]+>", "", seg)
    b = b.replace(" ", " ")  # 归一化不间断空格(NBSP)
    # 去 "《滴天髓阐微》上篇第0X章 <标题>" 多余标题行
    b = re.sub(r"《滴天髓阐微》[上下]篇第\d+章\s*\S*", "", b)
    # 去 "古文岛" 指纹头（吃到 "注释 译文" 标签之后）
    b = re.sub(r"滴天髓阐微·[^_]*_古文岛_原古诗文网\s*", "", b)
    b = re.sub(r"古文岛\s*客户端下载.*?注释\s*译文\s*", "", b, flags=re.S)
    # 去残留标签
    b = re.sub(r"原文\s*赏析\s*注释\s*译文\s*", "", b)
    b = re.sub(r"注释\s*译文\s*", "", b)
    # 从页脚 / 下一章预览处截断
    b = re.split(r"目录\s*下一章", b)[0]
    b = re.split(r"©\s*古诗文网", b)[0]
    b = re.split(r"第\d+章\s*《滴天髓阐微》", b)[0]
    # 规范化 **【原注】** -> 【原注】
    b = b.replace("**【原注】**", "【原注】")
    b = re.sub(r"[ \t]+", " ", b)
    b = re.sub(r"\n{2,}", "\n", b).strip()
    return b


def main():
    ap = argparse.ArgumentParser(description="抓取《滴天髓阐微》为纯文本")
    ap.add_argument("--out", default="books", help="输出目录 (默认 ./books)")
    ap.add_argument("--proxy", default=None,
                    help="代理，如 socks5://127.0.0.1:10808；否则读 SOCKS_PROXY/HTTPS_PROXY 环境变量")
    args = ap.parse_args()

    install_proxy(args.proxy)

    print(f"[1/3] 获取章节目录: {TOC_URL}")
    chapters = fetch_chapter_list()
    if not chapters:
        print("未解析到任何章节，退出。", file=sys.stderr)
        return
    print(f"      共 {len(chapters)} 章")

    out_dir = args.out
    os.makedirs(out_dir, exist_ok=True)
    path = os.path.join(out_dir, OUT_NAME + ".txt")

    # 断点续传：读取已落盘章节标题，跳过已抓取的
    done = set()
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            done = set(re.findall(r"【(.+?)】", f.read()))

    print("[2/3] 逐章抓取正文 ...")
    n_new = 0
    with open(path, "a", encoding="utf-8") as f:
        for i, (title, p) in enumerate(chapters, 1):
            url = f"https://m.guwendao.net/guwen/{p}"
            try:
                html = http_get(url, ua=MOBILE_UA)
                ctitle = chapter_title(html) or title
                if ctitle in done:
                    continue
                body = clean_chapter(html)
                if not body:
                    print(f"      [{i}/{len(chapters)}] {ctitle}: 正文为空，跳过",
                          file=sys.stderr)
                    continue
                f.write(f"【{ctitle}】\n{body}\n\n")
                f.flush()
                done.add(ctitle)
                n_new += 1
                print(f"      [{i}/{len(chapters)}] {ctitle}: {len(body)} 字")
            except Exception as e:
                print(f"      [{i}/{len(chapters)}] {title}: 失败 {e}", file=sys.stderr)

    if n_new == 0 and os.path.exists(path):
        print("没有新增章节（可能已全部抓取过）。")
    elif not os.path.exists(path) or os.path.getsize(path) == 0:
        print("未获取到任何正文。", file=sys.stderr)
        return
    size = os.path.getsize(path)
    print(f"[3/3] 完成 -> {path}  ({size} 字节, 本次新增 {n_new} 章)")


if __name__ == "__main__":
    main()
