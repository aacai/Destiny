package zhiqiu.app.destiny.platform

import android.content.Context

/** 全局 Application Context 持有者，供非 Composable 的 Room 构造器 actual 使用 */
lateinit var applicationContext: Context
