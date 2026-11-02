package zhiqiu.meihua

import zhiqiu.liuyao.Trigram

/**
 * 说卦取象：只录经文所列，不做现代附会。
 * 性情一句 + 「为…」物象列表。
 */
fun xiangOf(t: Trigram): XiangEntry = when (t) {
    Trigram.QIAN -> XiangEntry(
        t, "健",
        listOf("天", "圜", "君", "父", "玉", "金", "寒", "冰", "大赤", "良马", "老马", "瘠马", "驳马", "木果", "马", "首"),
    )
    Trigram.KUN -> XiangEntry(
        t, "顺",
        listOf("地", "母", "布", "釜", "吝啬", "均", "子母牛", "大舆", "文", "众", "柄", "黑", "牛", "腹"),
    )
    Trigram.ZHEN -> XiangEntry(
        t, "动",
        listOf("雷", "龙", "玄黄", "旉", "大涂", "长子", "决躁", "苍筤竹", "萑苇", "足"),
    )
    Trigram.XUN -> XiangEntry(
        t, "入",
        listOf("木", "风", "长女", "绳直", "工", "白", "长", "高", "进退", "不果", "臭", "鸡", "股"),
    )
    Trigram.KAN -> XiangEntry(
        t, "陷",
        listOf("水", "沟渎", "隐伏", "矫輮", "弓轮", "加忧", "心病", "耳痛", "血卦", "赤", "通", "月", "盗", "豕", "耳"),
    )
    Trigram.LI -> XiangEntry(
        t, "丽",
        listOf("火", "日", "电", "中女", "甲胄", "戈兵", "大腹", "鳖", "蟹", "蠃", "蚌", "龟", "雉", "目"),
    )
    Trigram.GEN -> XiangEntry(
        t, "止",
        listOf("山", "径路", "小石", "门阙", "果蓏", "阍寺", "指", "狗", "鼠", "黔喙之属", "手"),
    )
    Trigram.DUI -> XiangEntry(
        t, "说",
        listOf("泽", "少女", "巫", "口舌", "毁折", "附决", "刚卤", "妾", "羊", "口"),
    )
}
