#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""清洗 composeResources/files/books 里几本脏 txt，就地覆盖。

用法：
  python3 clean_book_texts.py
  python3 clean_book_texts.py --only bazitiyao qianliminggao
"""
from __future__ import annotations

import argparse
import os
import re
import sys

BOOKS_DIR = os.path.normpath(os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    "../../src/commonMain/composeResources/files/books",
))

# 先前 WebFetch 得到的维基「全览」（比 kanripo 四库 OCR 干净得多）
WIKI_LIXU = (
    "/Users/zhiqiu_1/.cursor/projects/"
    "Users-zhiqiu-1-AndroidStudioProjects-Destiny/agent-tools/"
    "45037c96-85f3-444d-9ea7-1114c889bf68.txt"
)


def collapse_blank(text: str) -> str:
    text = re.sub(r"[ \t]+\n", "\n", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip() + "\n"


def clean_lixuzhongmingshu(_raw: str) -> str:
    """优先用维基全览；没有则对 kanripo 脏源做兜底清洗。"""
    if os.path.isfile(WIKI_LIXU):
        raw = open(WIKI_LIXU, encoding="utf-8").read()
        return _clean_lixu_wiki(raw)
    return _clean_lixu_kanripo(_raw)


def _clean_lixu_wiki(raw: str) -> str:
    lines = []
    for line in raw.splitlines():
        t = line.strip()
        if not t:
            continue
        if t.startswith("李虚中命书 (四库") or t.startswith("<李") or t.startswith("{:"):
            continue
        if t.startswith("|") or t.startswith("---"):
            continue
        if "公有领域" in t or "Public domain" in t or t.startswith("检索自"):
            continue
        if t.startswith("搜索") and len(t) < 8:
            continue
        lines.append(t)
    text = "\n".join(lines)

    # 夹注 〈…〉 改为 （…） 并尽量单独成行，方便阅读
    def note_to_paren(m: re.Match) -> str:
        return f"（{m.group(1).strip()}）"

    text = re.sub(r"〈([^〉]+)〉", note_to_paren, text)

    # 卷标拆开
    text = re.sub(
        r"钦定四库全书\s*李虚中命书(卷[上中下])",
        r"\n= 李虚中命书\1 =\n",
        text,
    )
    text = text.replace("钦定四库全书", "")

    # 粗分段：句号后若跟甲乙丙…起头的六十甲子短论，加空行
    text = re.sub(r"。\s*(?=[甲乙丙丁戊己庚辛壬癸][子丑寅卯辰巳午未申酉戌亥])", "。\n\n", text)
    # 卷内专论标题（原文常粘在卷标后；避免「夫三元九限者」类误伤）
    for title in (
        "通理物化", "真假邪正", "升降清浊", "衰旺取时",
        "三元九限", "天承地禄", "水土名用",
    ):
        text = re.sub(
            rf"(?<![=\n夫必分])\s*{title}\s*(?!者|斯可矣)",
            rf"\n= {title} =\n",
            text,
        )

    return collapse_blank(text)


def _clean_lixu_kanripo(raw: str) -> str:
    lines = []
    for line in raw.splitlines():
        if line.startswith("#+") or line.startswith("# -*-"):
            continue
        lines.append(line)
    text = "\n".join(lines)
    text = re.sub(r"<pb:[^>]+>", "", text)
    text = text.replace("¶", "").replace("\u3000", "")
    text = re.sub(r"[ \t]+", "", text)

    def flatten(m: re.Match) -> str:
        return "".join(p.strip() for p in m.group(1).split("/"))

    text = re.sub(r"[（(]([^（）()]*?/[^（）()]*)[）)]", flatten, text)
    text = re.sub(r"(李虛中命書卷[上中下])", r"\n= \1 =\n", text)
    text = re.sub(r"(?<![=\n])(提要)(?![=\n])", r"\n= 提要 =\n", text, count=1)
    return collapse_blank(text)


_CHAPTER_SHELL = re.compile(
    r"^第\d+章\s*(?:字号.*|《千里命稿》第\s*\d+\s*章)?\s*$"
)
_SEP = re.compile(r"^-{5,}\s*$")
_TOC_NUM = re.compile(r"^\d{3}\.\s+")
_SHICHEN = re.compile(
    r"^[子丑寅卯辰巳午未申酉戌亥]月[甲乙丙丁戊己庚辛壬癸]日.+时$"
)
_RI = {"甲日", "乙日", "丙日", "丁日", "戊日", "己日", "庚日", "辛日", "壬日", "癸日"}


def clean_bazitiyao(raw: str) -> str:
    out: list[str] = []
    for line in raw.splitlines():
        t = line.strip()
        if not t:
            if out and out[-1] != "":
                out.append("")
            continue
        if _CHAPTER_SHELL.match(t) or _SEP.match(t) or t in ("原 文", "原文"):
            continue
        if t.startswith("八字提要"):
            continue
        if _TOC_NUM.match(t):
            continue
        if _SHICHEN.match(t):
            out.append(f"= {t} =")
            continue
        if t in _RI:
            out.append(f"= {t} =")
            continue
        out.append(t)
    return collapse_blank("\n".join(out))


_QIANLI_HEADINGS = {
    "目录", "序", "杨叔和序", "骆经畲序", "顾乃平序", "自序",
    "起例问答", "天干篇", "地支篇", "人元篇", "人元之利",
    "格局篇", "用神篇", "神煞篇", "岁运篇", "女命篇", "评断篇",
    "甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸",
    "子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥",
}
_GANZHI_ONE = set("甲乙丙丁戊己庚辛壬癸子丑寅卯辰巳午未申酉戌亥")


def clean_qianliminggao(raw: str) -> str:
    # 先去掉网站壳
    stripped: list[str] = []
    for line in raw.splitlines():
        t = line.strip()
        if not t:
            stripped.append("")
            continue
        if _CHAPTER_SHELL.match(t) or _SEP.match(t) or t in ("原 文", "原文"):
            continue
        if t.startswith("千里命稿"):
            continue
        stripped.append(t)

    out: list[str] = []
    i = 0
    n = len(stripped)
    emitted_toc = False

    def next_nonempty(idx: int) -> str:
        j = idx
        while j < n and not stripped[j]:
            j += 1
        return stripped[j] if j < n else ""

    while i < n:
        t = stripped[i]
        if not t:
            if out and out[-1] != "":
                out.append("")
            i += 1
            continue

        if t == "目录":
            if not emitted_toc:
                out.append("= 目录 =")
                emitted_toc = True
            # 跳过后续短目录项，直到长正文
            i += 1
            while i < n:
                cur = stripped[i]
                if not cur:
                    i += 1
                    continue
                if len(cur) > 40 or cur.startswith(("问：", "答：")):
                    break
                # 短标题且后面仍是短标题/空 → 目录项
                nxt = next_nonempty(i + 1)
                if len(cur) <= 12 and (not nxt or len(nxt) <= 12 or nxt in _QIANLI_HEADINGS):
                    i += 1
                    continue
                break
            continue

        if t == "序":
            # 「序」单独一行常是废标题，看下一行
            nxt = next_nonempty(i + 1)
            if nxt in ("杨叔和序", "骆经畲序", "顾乃平序"):
                i += 1
                continue

        if t in _QIANLI_HEADINGS:
            nxt = next_nonempty(i + 1)
            # 目录残留：短标题后仍是短标题 → 跳过
            if t not in _GANZHI_ONE and len(nxt) <= 12 and nxt in _QIANLI_HEADINGS:
                i += 1
                continue
            out.append(f"= {t} =")
            i += 1
            continue

        out.append(t)
        i += 1

    return collapse_blank("\n".join(out))


CLEANERS = {
    "lixuzhongmingshu": clean_lixuzhongmingshu,
    "bazitiyao": clean_bazitiyao,
    "qianliminggao": clean_qianliminggao,
}


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", nargs="*", help="只清洗指定 id")
    ap.add_argument("--dir", default=BOOKS_DIR)
    args = ap.parse_args()
    ids = args.only or list(CLEANERS)
    for i in ids:
        fn = CLEANERS.get(i)
        if not fn:
            print(f"未知 id: {i}", file=sys.stderr)
            continue
        path = os.path.join(args.dir, f"{i}.txt")
        raw = open(path, encoding="utf-8").read()
        cleaned = fn(raw)
        open(path, "w", encoding="utf-8").write(cleaned)
        print(
            f"[ok] {i}.txt  {len(raw)} -> {len(cleaned)} 字, "
            f"{raw.count(chr(10))} -> {cleaned.count(chr(10))} 行",
            flush=True,
        )
        # 抽查开头
        head = "\n".join(cleaned.splitlines()[:8])
        print(head, flush=True)
        print("---", flush=True)


if __name__ == "__main__":
    main()
