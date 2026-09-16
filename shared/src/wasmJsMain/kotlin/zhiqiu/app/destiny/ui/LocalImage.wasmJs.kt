package zhiqiu.app.destiny.ui

// Web：路径为内存 FS 绝对路径或 URL；Coil 按字符串加载（本地文件场景由 FakeFileSystem 配合）
actual fun localImageModel(absolutePath: String): Any = absolutePath
