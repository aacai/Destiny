package zhiqiu.app.destiny

import android.app.Application
import zhiqiu.app.destiny.platform.applicationContext

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 限定名：成员作用域里 ContextWrapper 继承了同名的只读 applicationContext 属性
        zhiqiu.app.destiny.platform.applicationContext = this
    }
}
