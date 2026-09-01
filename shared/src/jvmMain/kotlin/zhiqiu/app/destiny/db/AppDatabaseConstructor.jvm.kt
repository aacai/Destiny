// AppDatabaseConstructor 的 actual 由 Room 3 通过 @ConstructedBy + expect object 自动生成，
// 不要手写 actual（否则 KSP 报 "must be an 'expect' declaration"）。运行时也请勿直接调用
// AppDatabaseConstructor.initialize()（其生成体是未初始化连接的裸 AppDatabase_Impl），
// 应通过 getRoomDatabase(getDatabaseBuilder()) 取得数据库实例。
