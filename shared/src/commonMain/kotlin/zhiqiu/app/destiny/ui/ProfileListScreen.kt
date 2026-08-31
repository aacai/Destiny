package zhiqiu.app.destiny.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Plus
import compose.icons.feathericons.BookOpen
import compose.icons.feathericons.Search
import compose.icons.feathericons.Trash2
import compose.icons.feathericons.X
import zhiqiu.app.destiny.profile.Profile
import zhiqiu.app.destiny.time.timeIndexLabel

private val PageBg = Color(0xFFF5F3EE)
private val CardBg = Color(0xFFFFFFFF)
private val Ink = Color(0xFF222222)
private val Muted = Color(0xFF8A8578)
private val Line = Color(0xFFE2DED2)
private val Accent = Color(0xFF26A6A6)
private val MaleDot = Color(0xFF4A90D9)
private val FemaleDot = Color(0xFFE57373)

private enum class SortKey(val label: String) {
    CREATED("创建"), NAME("名字"), BIRTH("生日"),
}

/** 从「公历」串里解析 y/m/d 用于生日排序；解析不了返回 Int.MIN_VALUE 排到最后 */
private fun birthSortValue(profile: Profile): Int {
    val text = profile.solarDateDisplay.ifBlank { profile.birthday }
    val m = Regex("(\\d{4})[-/.年](\\d{1,2})[-/.月](\\d{1,2})").find(text) ?: return Int.MIN_VALUE
    val (y, mo, d) = m.destructured
    return (y.toIntOrNull() ?: 0) * 10000 + (mo.toIntOrNull() ?: 0) * 100 + (d.toIntOrNull() ?: 0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    profiles: List<Profile>,
    onAdd: () -> Unit,
    onOpen: (Profile) -> Unit,
    onDelete: (Profile) -> Unit,
    onOpenBooks: () -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    var sortKey by rememberSaveable { mutableStateOf(SortKey.CREATED) }
    var ascending by rememberSaveable { mutableStateOf(false) }

    val q = query.trim()
    val filtered = profiles.filter { p ->
        q.isBlank() ||
            p.name.contains(q, ignoreCase = true) ||
            p.birthday.contains(q, ignoreCase = true) ||
            p.solarDateDisplay.contains(q, ignoreCase = true) ||
            p.baziSummary.contains(q, ignoreCase = true)
    }
    val sorted = when (sortKey) {
        SortKey.NAME ->
            if (ascending) filtered.sortedBy { it.name } else filtered.sortedByDescending { it.name }
        SortKey.BIRTH ->
            if (ascending) filtered.sortedBy { birthSortValue(it) } else filtered.sortedByDescending { birthSortValue(it) }
        SortKey.CREATED ->
            if (ascending) filtered.sortedBy { it.createdAt } else filtered.sortedByDescending { it.createdAt }
    }

    Scaffold(
        containerColor = PageBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("档案", color = Ink, fontWeight = FontWeight.Bold)
                        Text(
                            if (q.isBlank()) "共 ${profiles.size} 个档案"
                            else "匹配 ${sorted.size} / ${profiles.size}",
                            color = Muted,
                            fontSize = 12.sp,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PageBg),
                actions = {
                    IconButton(onClick = onOpenBooks) {
                        Icon(
                            imageVector = FeatherIcons.BookOpen,
                            contentDescription = "书架",
                            tint = Accent,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = Accent,
                contentColor = Color.White,
            ) {
                Icon(imageVector = FeatherIcons.Plus, contentDescription = "添加档案")
            }
        },
    ) { padding ->
        if (profiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("还没有档案", color = Muted)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onAdd) { Text("添加档案", color = Accent) }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                SearchBar(
                    query = query,
                    onQueryChange = { query = it },
                )
                SortRow(
                    current = sortKey,
                    ascending = ascending,
                    onSelect = { key ->
                        if (sortKey == key) {
                            ascending = !ascending
                        } else {
                            sortKey = key
                            ascending = key != SortKey.BIRTH
                        }
                    },
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (sorted.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("没有匹配「$q」的档案", color = Muted, fontSize = 14.sp)
                            }
                        }
                    }
                    items(sorted, key = { it.id }) { profile ->
                        ProfileRow(
                            profile = profile,
                            onClick = { onOpen(profile) },
                            onDelete = { onDelete(profile) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(CardBg, RoundedCornerShape(10.dp))
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = FeatherIcons.Search,
            contentDescription = null,
            tint = Muted,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text("搜索名字、日期、四柱…", color = Muted, fontSize = 14.sp)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = Ink),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = FeatherIcons.X,
                contentDescription = "清除",
                tint = Muted,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onQueryChange("") },
            )
        }
    }
}

@Composable
private fun SortRow(
    current: SortKey,
    ascending: Boolean,
    onSelect: (SortKey) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SortKey.entries.forEach { key ->
            val active = key == current
            Row(
                modifier = Modifier
                    .clickable { onSelect(key) }
                    .background(
                        if (active) Accent.copy(alpha = 0.12f) else CardBg,
                        RoundedCornerShape(8.dp),
                    )
                    .border(
                        1.dp,
                        if (active) Accent else Line,
                        RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    key.label,
                    color = if (active) Accent else Muted,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
                if (active) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        if (ascending) "↑" else "↓",
                        color = Accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(
    profile: Profile,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 性别徽章
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    (if (profile.gender == "女") FemaleDot else MaleDot).copy(alpha = 0.15f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                (profile.name.ifBlank { "未" }).take(1),
                color = if (profile.gender == "女") FemaleDot else MaleDot,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    profile.name.ifBlank { "未命名" },
                    color = Ink,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    profile.gender,
                    color = if (profile.gender == "女") FemaleDot else MaleDot,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (profile.groupName.isNotBlank() && profile.groupName != "默认") {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "· ${profile.groupName}",
                        color = Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                "${profile.solarDateDisplay.ifBlank { profile.birthday }} · ${timeIndexLabel(profile.timeIndex)}",
                color = Muted,
                fontSize = 12.sp,
            )
            if (profile.baziSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(profile.baziSummary, color = Accent, fontSize = 12.sp)
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = FeatherIcons.Trash2,
                contentDescription = "删除",
                tint = Line,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
