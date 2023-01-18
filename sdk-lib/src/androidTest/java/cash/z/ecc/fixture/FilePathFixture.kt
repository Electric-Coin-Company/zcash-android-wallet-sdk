package cash.z.ecc.fixture

import java.io.File

object FilePathFixture {
    val rootDir = File(DatabasePathFixture.new())
    val blocksDir = File(rootDir, "blocks")
}
