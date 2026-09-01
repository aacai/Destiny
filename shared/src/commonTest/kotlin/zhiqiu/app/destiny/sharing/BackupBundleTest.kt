package zhiqiu.app.destiny.sharing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class BackupBundleTest {

    private fun newFs() = FakeFileSystem()

    @Test
    fun imagePathIsOrganizedByProfileAndCategory() {
        assertEquals(
            "images/p1/bazi/20260901-143022.jpg",
            BackupLayout.imagePath("p1", "bazi", "20260901-143022.jpg"),
        )
        assertEquals(
            "images/p1/ziwei/20260901-143055.jpg",
            BackupLayout.imagePath("p1", "ziwei", "20260901-143055.jpg"),
        )
    }

    @Test
    fun packAndUnpackRoundTrip() {
        val fs = newFs()
        val zip = "/destiny-backup.zip".toPath()
        val json = "{\"app\":\"Destiny\"}"
        val images = mapOf(
            BackupLayout.imagePath("p1", "bazi", "20260901-143022.jpg") to byteArrayOf(1, 2, 3),
            BackupLayout.imagePath("p1", "ziwei", "20260901-143055.jpg") to byteArrayOf(4, 5, 6, 7),
            BackupLayout.imagePath("p2", "bazi", "20260902-101500.jpg") to byteArrayOf(9),
        )

        BackupBundle(fs).pack(zip, json, images)
        val unpacked = BackupBundle(fs).unpack(zip)

        assertEquals(json, unpacked.backupJson)
        // ByteArray 的 equals 是引用比较，需逐项用 contentEquals 比对内容
        assertEquals(images.keys, unpacked.images.keys, "解包后图片路径应完全一致")
        for ((path, bytes) in images) {
            assertTrue(bytes.contentEquals(unpacked.images[path]), "图片内容应一致: $path")
        }
    }

    @Test
    fun packWithoutImagesStillUnpacks() {
        val fs = newFs()
        val zip = "/no-image.zip".toPath()
        BackupBundle(fs).pack(zip, "{}", emptyMap())
        val unpacked = BackupBundle(fs).unpack(zip)
        assertEquals("{}", unpacked.backupJson)
        assertTrue(unpacked.images.isEmpty(), "无图片时解包结果应为空")
    }

    @Test
    fun packOverwritesExistingZip() {
        val fs = newFs()
        val zip = "/overwrite.zip".toPath()
        BackupBundle(fs).pack(zip, "first", mapOf("images/a/b/c.jpg" to byteArrayOf(1)))
        BackupBundle(fs).pack(zip, "second", emptyMap())
        val unpacked = BackupBundle(fs).unpack(zip)
        assertEquals("second", unpacked.backupJson)
        assertTrue(unpacked.images.isEmpty(), "重新打包应覆盖旧内容，不残留旧图片")
    }
}
