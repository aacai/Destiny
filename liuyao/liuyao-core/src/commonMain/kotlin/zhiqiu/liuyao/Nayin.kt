package zhiqiu.liuyao

/** 六十甲子纳音，两柱一对，自甲子乙丑海中金起。 */
data class NayinEntry(val pair: String, val name: String)

val NAYIN_TABLE: List<NayinEntry> = listOf(
    NayinEntry("甲子 乙丑", "海中金"),
    NayinEntry("丙寅 丁卯", "炉中火"),
    NayinEntry("戊辰 己巳", "大林木"),
    NayinEntry("庚午 辛未", "路旁土"),
    NayinEntry("壬申 癸酉", "剑锋金"),
    NayinEntry("甲戌 乙亥", "山头火"),
    NayinEntry("丙子 丁丑", "涧下水"),
    NayinEntry("戊寅 己卯", "城头土"),
    NayinEntry("庚辰 辛巳", "白蜡金"),
    NayinEntry("壬午 癸未", "杨柳木"),
    NayinEntry("甲申 乙酉", "泉中水"),
    NayinEntry("丙戌 丁亥", "屋上土"),
    NayinEntry("戊子 己丑", "霹雳火"),
    NayinEntry("庚寅 辛卯", "松柏木"),
    NayinEntry("壬辰 癸巳", "长流水"),
    NayinEntry("甲午 乙未", "沙中金"),
    NayinEntry("丙申 丁酉", "山下火"),
    NayinEntry("戊戌 己亥", "平地木"),
    NayinEntry("庚子 辛丑", "壁上土"),
    NayinEntry("壬寅 癸卯", "金箔金"),
    NayinEntry("甲辰 乙巳", "覆灯火"),
    NayinEntry("丙午 丁未", "天河水"),
    NayinEntry("戊申 己酉", "大驿土"),
    NayinEntry("庚戌 辛亥", "钗钏金"),
    NayinEntry("壬子 癸丑", "桑柘木"),
    NayinEntry("甲寅 乙卯", "大溪水"),
    NayinEntry("丙辰 丁巳", "沙中土"),
    NayinEntry("戊午 己未", "天上火"),
    NayinEntry("庚申 辛酉", "石榴木"),
    NayinEntry("壬戌 癸亥", "大海水"),
)
