# 古籍抓取脚本

脚本本身**不会**打进 APK。抓取结果默认写到：

`shared/src/commonMain/composeResources/files/books/*.txt`

这些 `.txt` 是 Compose Multiplatform 公共资源，会随 Android / Desktop / iOS 打包。

## 用法

```bash
cd shared/tools/books
python3 fetch_mingli_classics.py --list
python3 fetch_mingli_classics.py --only sanmingtonghui yuanhaiziping
python3 fetch_mingli_classics.py --proxy socks5://127.0.0.1:10808

python3 fetch_ditiansui.py          # -> ditiunsuichanwei.txt
python3 clean_ditiansui.py          # 清洗阐微正文
```

`--only` 可用中文书名或 ASCII id（与 `BookStore` 一致）。

## 说明

- 维基文库公有领域原文，经脚本清洗为 UTF-8 纯文本。
- 《滴天髓阐微》不在维基文库，用 `fetch_ditiansui.py`（古诗文网）。
- 《子平真诠 / 千里命稿 / 八字提要》等需其它来源，请直接放真实 `.txt` 到 `files/books/`。
- 若 txt 带网站壳（「第xxx章」「字号」）或四库 OCR 脏标记，可跑：

```bash
python3 clean_book_texts.py
python3 clean_book_texts.py --only lixuzhongmingshu bazitiyao qianliminggao
```
