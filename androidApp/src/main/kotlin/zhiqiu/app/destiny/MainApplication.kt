package zhiqiu.app.destiny

import android.app.Application
import zhiqiu.app.destiny.platform.applicationContext

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        applicationContext = this
    }
}
