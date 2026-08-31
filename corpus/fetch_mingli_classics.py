#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
从维基文库(zh.wikisource.org)下载命理古籍原文，清洗为纯文本(.txt)。

说明
----
- 维基文库上的这些古籍均属公有领域文献，且提供 API 原文(wikitext)，
  不是 PDF，非常适合拿来当语料 / 解析 / 训练数据使用。
- 脚本会自动：用 search 定位准确条目名(处理繁体/括号差异)
  -> 枚举分卷(卷一..卷N、卷上/中/下) -> 拉取每卷原始 wikitext
  -> 清洗模板/表格/HTML -> 拼接 -> 保存为 UTF-8 纯文本。
- 传输层优先使用系统自带的 curl（macOS/Linux 均预装），未找到 curl 时回退到
  Python 标准库 urllib。因此**无需 pip install**；若走 SOCKS 代理且机器上没有
  curl，则可 `pip install PySocks` 作为 urllib 兜底（推荐场景是让 curl 直接走代理）。

用法
----
    python3 fetch_mingli_classics.py                 # 下载全部，输出到 ./books
    python3 fetch_mingli_classics.py --out mydir
    python3 fetch_mingli_classics.py --only 神峰通考 三命通会
    python3 fetch_mingli_classics.py --list          # 只列出将要下载的条目
    python3 fetch_mingli_classics.py --proxy socks5://127.0.0.1:10808
        # 走 SOCKS5 代理（v2ray 等）；也可不传参而用环境变量 SOCKS_PROXY=...

注意：本脚本需要能访问外网(维基文库)。在无法联网的环境里运行会失败。
"""

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import time
import urllib.parse
import urllib.request

API = "https://zh.wikisource.org/w/api.php"
UA = ("DestinyCorpusFetcher/1.0 (https://github.com/; educational corpus "
      "of public-domain Chinese astrology texts) python-urllib")

# 每本书：name=输出文件名(不含扩展名)，search=搜索词，candidates=已知准确标题候选
BOOKS = [
    {"name": "穷通宝鉴", "search": "穷通宝鉴", "candidates": ["穷通宝鉴"]},
    {"name": "三命通会", "search": "三命通會", "candidates": ["三命通會"]},
    {"name": "渊海子平", "search": "淵海子平", "candidates": ["淵海子平", "渊海子平"]},
    {"name": "神峰通考", "search": "神峰通考", "candidates": ["神峰通考"]},
    {"name": "五行精纪", "search": "五行精紀", "candidates": ["五行精紀", "五行精纪"]},
    {"name": "李虚中命书", "search": "李虛中命書",
     "candidates": ["李虛中命書", "李虚中命书", "李虛中命書 (四庫全書本)"]},
    {"name": "滴天髓", "search": "滴天髓", "candidates": ["滴天髓"]},
    {"name": "五行大义", "search": "五行大義", "candidates": ["五行大義", "五行大义"]},
    {"name": "玉照定真经", "search": "玉照定真經",
     "candidates": ["玉照定真經 (四庫全書本)", "玉照定真經", "玉照定真经"]},
    {"name": "珞琭子消息赋", "search": "珞琭子三命消息賦",
     "candidates": ["珞琭子三命消息賦註 (四庫全書本)", "珞琭子賦註 (四庫全書本)",
                    "珞琭子三命消息賦"]},
    {"name": "星命总括", "search": "星命總括",
     "candidates": ["星命總括 (四庫全書本)", "星命總括", "星命总括"]},
    {"name": "月谈赋", "search": "月談賦", "candidates": ["月談賦", "月谈赋"]},
]

# 这些书在维基文库没有独立条目，脚本不会下载，仅在此注明其它来源(见 README)
NOT_ON_WIKISOURCE = [
    "子平真诠", "千里命稿", "八字提要", "天元巫咸经",
    "星平会海", "命理约言", "兰台妙选", "子平管见",
    "鬼谷遗文", "命理探原", "呱呱集", "造化元钥",
    "滴天髓阐微", "御定子平",
]


# --------------------------------------------------------------------------- #
# 代理支持（默认直连；指定 --proxy 或环境变量 SOCKS_PROXY/HTTPS_PROXY 时启用）
# --------------------------------------------------------------------------- #
def install_proxy(proxy=None):
    """配置代理。

    1) 把代理写进环境变量（all_proxy / socks_proxy / http_proxy / https_proxy），
       供后续的 curl 传输层自动读取；
    2) 同时给 urllib 也装上对应 opener（直连/HTTP 代理场景下 urllib 兜底用）。

    proxy 形如 socks5://127.0.0.1:10808 或 http://host:port。
    未提供时依次读取环境变量 SOCKS_PROXY / HTTPS_PROXY / HTTP_PROXY，
    仍没有则保持直连（便于在能直连外网的机器上直接跑）。
    """
    if proxy is None:
        proxy = os.environ.get("SOCKS_PROXY") or os.environ.get("HTTPS_PROXY") \
            or os.environ.get("HTTP_PROXY")
    if not proxy:
        return
    p = urllib.parse.urlparse(proxy)
    scheme = (p.scheme or "").lower()

    # —— 写入环境变量，curl 会据此走代理 ——
    if scheme in ("socks5", "socks5h", "socks4", "socks4a"):
        os.environ["all_proxy"] = proxy
        os.environ["socks_proxy"] = proxy
        os.environ["SOCKS_PROXY"] = proxy
        print(f"[代理] SOCKS({scheme}) {p.hostname}:{p.port or 1080} "
              f"(via curl all_proxy/socks_proxy)")
    elif scheme in ("http", "https"):
        os.environ["http_proxy"] = proxy
        os.environ["https_proxy"] = proxy
        os.environ["HTTP_PROXY"] = proxy
        os.environ["HTTPS_PROXY"] = proxy
        print(f"[代理] HTTP 代理 {proxy} (via curl *_proxy)")
    else:
        print(f"[警告] 不支持的代理协议: {scheme}", file=sys.stderr)
        return

    # —— urllib 兜底(opener) ——
    if scheme in ("socks5", "socks5h", "socks4", "socks4a"):
        try:
            import socks
            from sockshandler import SocksiPyHandler
            stype = {"socks5": socks.SOCKS5, "socks5h": socks.SOCKS5,
                     "socks4": socks.SOCKS4, "socks4a": socks.SOCKS4}.get(scheme, socks.SOCKS5)
            opener = urllib.request.build_opener(
                SocksiPyHandler(stype, p.hostname or "127.0.0.1", p.port or 1080))
            urllib.request.install_opener(opener)
        except ImportError:
            # 没有 PySocks 时 urllib 走不通 SOCKS，但 curl 兜底仍能工作
            pass


# 礼貌间隔：Wikimedia 对匿名/共享代理 IP 有限流，请求之间留点空隙
REQUEST_GAP = 2.0
_last_req = 0.0


def _throttle():
    global _last_req
    wait = REQUEST_GAP - (time.time() - _last_req)
    if wait > 0:
        time.sleep(wait)
    _last_req = time.time()


def http_get(url, timeout=60, retries=8, backoff=4.0, ua=None):
    """HTTP GET 返回文本。优先 curl（经 SOCKS/HTTP 代理最稳），否则 urllib 兜底。

    处理代理/网络偶发丢包，并针对 Wikimedia 的 429 限流做指数退避重试。
    ua: 自定义 User-Agent（部分站点对手机/桌面 UA 返回不同内容时可指定）。
    """
    import tempfile
    ua = ua or UA
    last_err = None
    for attempt in range(1, retries + 1):
        _throttle()
        try:
            if shutil.which("curl"):
                tf = tempfile.NamedTemporaryFile(delete=False, suffix=".bin")
                tf.close()
                try:
                    r = subprocess.run(
                        ["curl", "-sSL", "--max-time", str(timeout), "-A", ua,
                         "--compressed", "-o", tf.name, "-w", "%{http_code}", url],
                        capture_output=True, env=dict(os.environ), check=True)
                    code = r.stdout.decode("utf-8", "replace").strip()
                    body = open(tf.name, "rb").read().decode("utf-8", "replace")
                finally:
                    try:
                        os.unlink(tf.name)
                    except OSError:
                        pass
                if code == "429" or "Wikimedia Error" in body or "Too Many Requests" in body:
                    last_err = IOError("Wikimedia 429 限流")
                elif body.strip():
                    return body
                else:
                    last_err = IOError("curl 返回空响应")
            else:
                # urllib 兜底（直连或 HTTP 代理；SOCKS 需 PySocks+opener）
                req = urllib.request.Request(url, headers={"User-Agent": ua})
                with urllib.request.urlopen(req, timeout=timeout) as resp:
                    body = resp.read().decode("utf-8", "replace")
                if body.strip():
                    return body
                last_err = IOError("urllib 返回空响应")
        except Exception as e:  # noqa: BLE001  —— 网络抖动，重试
            last_err = e
        if attempt < retries:
            time.sleep(backoff * (2 ** (attempt - 1)))
    raise last_err or IOError("http_get 重试后仍失败")


# --------------------------------------------------------------------------- #
# 基础 API 封装
# --------------------------------------------------------------------------- #
def api(params):
    params = dict(params)
    params["format"] = "json"
    url = API + "?" + urllib.parse.urlencode(params)
    return json.loads(http_get(url, timeout=30))


def _chunk(seq, n=50):
    for i in range(0, len(seq), n):
        yield seq[i:i + n]


def filter_existing(titles):
    """批量判断条目是否存在，返回存在的标题列表。"""
    existing = []
    for part in _chunk(list(dict.fromkeys(titles)), 50):
        data = api({"action": "query", "titles": "|".join(part), "redirects": 1})
        for p in data.get("query", {}).get("pages", {}).values():
            if "missing" not in p:
                existing.append(p["title"])
        time.sleep(0.3)
    return existing


def search_titles(hint, limit=10):
    data = api({"action": "query", "list": "search",
                "srsearch": hint, "srnamespace": 0, "srlimit": limit})
    return [r["title"] for r in data.get("query", {}).get("search", [])]


def fetch_raw(title):
    """拉取单个条目的原始 wikitext。"""
    data = api({"action": "query", "prop": "revisions", "rvprop": "content",
                "rvslots": "main", "titles": title, "redirects": 1})
    for p in data.get("query", {}).get("pages", {}).values():
        revs = p.get("revisions")
        if revs:
            rev = revs[0]
            if "slots" in rev:
                return rev["slots"]["main"]["*"]
            return rev.get("*")
    return None


# --------------------------------------------------------------------------- #
# 标题解析 / 分卷枚举
# --------------------------------------------------------------------------- #
_CN = {c: i for i, c in enumerate("零一二三四五六七八九")}


def cn2int(s):
    if s.isdigit():
        return int(s)
    if s in _CN:
        return _CN[s]
    if "十" in s:
        left, _, right = s.partition("十")
        l = _CN.get(left, 1) if left else 1
        r = _CN.get(right, 0) if right else 0
        return l * 10 + r
    return 0


def guess_subpages(root):
    """根据根标题推断可能的分卷标题(卷一..卷二十 / 卷上中下)。"""
    cands = [root]
    stems = []
    for n in range(1, 21):
        # 1..20 的中文
        if n < 10:
            stems.append("卷" + "一二三四五六七八九"[n - 1])
        elif n == 10:
            stems.append("卷十")
        elif n < 20:
            stems.append("卷十" + "一二三四五六七八九"[n - 11])
        else:
            stems.append("卷二十")
    for s in ["卷上", "卷中", "卷下", "卷首", "卷末"]:
        stems.append(s)
    for st in stems:
        cands.append(root + "/" + st)
    return cands


def order_key(title, root):
    if title == root:
        return (0, 0)
    seg = title.split("/")[-1]
    if seg.startswith("卷"):
        rest = seg[1:]
        if rest == "上":
            return (1, 0)
        if rest == "中":
            return (1, 1)
        if rest == "下":
            return (1, 2)
        try:
            return (2, cn2int(rest))
        except Exception:
            return (3, 0)
    return (4, 0)


def resolve_root(book):
    """确定一本书的根条目标题(优先 candidates，再 search)。"""
    for c in book.get("candidates", []):
        if filter_existing([c]):
            return c
    for h in search_titles(book["search"]):
        if "/" not in h and filter_existing([h]):
            return h
    for h in search_titles(book["search"]):
        if filter_existing([h]):
            return h
    return None


def collect_pages(root):
    cands = guess_subpages(root)
    existing = filter_existing(cands)
    # 跳过“全览/总览/附录”这类汇总页，避免与分卷重复
    pages = [t for t in existing if not re.search(r"[览附总]", t)]
    pages.sort(key=lambda t: order_key(t, root))
    return pages


# --------------------------------------------------------------------------- #
# wikitext 清洗
# --------------------------------------------------------------------------- #
def clean(text):
    text = re.sub(r"<!--.*?-->", "", text, flags=re.S)
    text = re.sub(r"<ref[^>]*>.*?</ref>", "", text, flags=re.S)
    text = re.sub(r"<ref[^>]*/>", "", text)
    text = re.sub(r"<[^>]+>", "", text)
    # 去掉模板 {{...}}（循环处理嵌套）
    prev = None
    while prev != text:
        prev = text
        text = re.sub(r"\{\{[^{}]*\}\}", "", text)
    text = text.replace("'''", "").replace("''", "")
    out = []
    for line in text.splitlines():
        s = line.strip()
        if s.startswith("{|") or s.startswith("|}") or s.startswith("|-") \
           or s.startswith("|+") or s.startswith("!"):
            continue
        if s.startswith("|"):
            s = s[1:].strip()
        out.append(s)
    text = "\n".join(out)
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


# --------------------------------------------------------------------------- #
# 主流程
# --------------------------------------------------------------------------- #
def download_book(book, out_dir):
    root = resolve_root(book)
    if not root:
        print(f"  [跳过] {book['name']}：未在维基文库找到条目", file=sys.stderr)
        return None
    pages = collect_pages(root)
    if not pages:
        print(f"  [跳过] {book['name']} (根: {root})：未获取到正文", file=sys.stderr)
        return None

    print(f"  [下载] {book['name']}  <-  根条目: {root}")
    print(f"          分卷({len(pages)}): " + ", ".join(pages))
    parts = []
    for t in pages:
        raw = fetch_raw(t)
        if raw:
            parts.append(clean(raw))
        time.sleep(0.4)

    full = "\n\n".join(p for p in parts if p)
    if not full:
        print(f"  [跳过] {book['name']}：正文为空", file=sys.stderr)
        return None

    path = os.path.join(out_dir, book["name"] + ".txt")
    with open(path, "w", encoding="utf-8") as f:
        f.write(full)
    print(f"          已保存 -> {path}  ({len(full)} 字)")
    return path


def main():
    ap = argparse.ArgumentParser(description="从维基文库下载命理古籍为纯文本")
    ap.add_argument("--out", default="books", help="输出目录 (默认 ./books)")
    ap.add_argument("--only", nargs="*", help="只下载指定书名")
    ap.add_argument("--list", action="store_true", help="只列出将要下载的条目后退出")
    ap.add_argument("--proxy", default=None,
                    help="代理地址，如 socks5://127.0.0.1:10808 或 http://host:port。"
                         "未指定时读取 SOCKS_PROXY/HTTPS_PROXY 环境变量，仍无则直连。")
    args = ap.parse_args()

    install_proxy(args.proxy)

    books = BOOKS
    if args.only:
        wanted = set(args.only)
        books = [b for b in BOOKS if b["name"] in wanted]
        if not books:
            print("没有匹配的书名。可选：", ", ".join(b["name"] for b in BOOKS),
                  file=sys.stderr)
            return

    print("将处理的书籍（维基文库可用）：")
    for b in books:
        print("  -", b["name"])
    if NOT_ON_WIKISOURCE:
        print("维基文库无独立条目（见 README 其它来源）：",
              ", ".join(NOT_ON_WIKISOURCE))

    if args.list:
        return

    out_dir = args.out
    os.makedirs(out_dir, exist_ok=True)

    ok, skip = [], []
    for b in books:
        print()
        try:
            p = download_book(b, out_dir)
            (ok if p else skip).append(b["name"])
        except Exception as e:
            print(f"  [错误] {b['name']}: {e}", file=sys.stderr)
            skip.append(b["name"])

    print("\n完成。成功 %d 本，跳过 %d 本。" % (len(ok), len(skip)))
    if ok:
        print("已保存:", ", ".join(ok))
    if skip:
        print("未获取:", ", ".join(skip))


if __name__ == "__main__":
    main()
