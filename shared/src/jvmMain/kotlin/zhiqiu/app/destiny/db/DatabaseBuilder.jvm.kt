package zhiqiu.app.destiny.db

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.File

fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbFile = File(System.getProperty("user.home"), ".destiny/destiny.db")
    return Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
}
