package zhiqiu.app.destiny.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import android.content.Context

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val dbFile = context.applicationContext.getDatabasePath("destiny.db")!!
    return Room.databaseBuilder<AppDatabase>(context = context.applicationContext, name = dbFile.absolutePath)
}
