package zhiqiu.app.destiny

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform