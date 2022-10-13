package cash.z.ecc.android.sdk.internal.db.derived

import androidx.sqlite.db.SupportSQLiteDatabase
import cash.z.ecc.android.sdk.internal.db.queryAndMap
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.util.Locale

internal class BlockTable(private val zcashNetwork: ZcashNetwork, private val sqliteDatabase: SupportSQLiteDatabase) {
    companion object {

        private val SELECTION_MIN_HEIGHT = arrayOf(
            String.format(
                Locale.ROOT,
                "MIN(%s)", // $NON-NLS
                BlockTableDefinition.COLUMN_LONG_HEIGHT
            )
        )

        private val SELECTION_MAX_HEIGHT = arrayOf(
            String.format(
                Locale.ROOT,
                "MAX(%s)", // $NON-NLS
                BlockTableDefinition.COLUMN_LONG_HEIGHT
            )
        )

        private val SELECTION_BLOCK_HEIGHT = String.format(
            Locale.ROOT,
            "%s = ?", // $NON-NLS
            BlockTableDefinition.COLUMN_LONG_HEIGHT
        )

        private val PROJECTION_COUNT = arrayOf("COUNT(*)") // $NON-NLS

        private val PROJECTION_HASH = arrayOf(BlockTableDefinition.COLUMN_BLOB_HASH)
    }

    suspend fun count() = sqliteDatabase.queryAndMap(
        BlockTableDefinition.TABLE_NAME,
        columns = PROJECTION_COUNT,
        cursorParser = { it.getLong(0) }
    ).first()

    suspend fun firstScannedHeight(): BlockHeight {
        // Note that we assume the Rust layer will add the birthday height as the first block
        val heightLong =
            sqliteDatabase.queryAndMap(
                table = BlockTableDefinition.TABLE_NAME,
                columns = SELECTION_MIN_HEIGHT,
                cursorParser = { it.getLong(0) }
            ).first()

        return BlockHeight.new(zcashNetwork, heightLong)
    }

    suspend fun lastScannedHeight(): BlockHeight {
        // Note that we assume the Rust layer will add the birthday height as the first block
        val heightLong =
            sqliteDatabase.queryAndMap(
                table = BlockTableDefinition.TABLE_NAME,
                columns = SELECTION_MAX_HEIGHT,
                cursorParser = { it.getLong(0) }
            ).first()

        return BlockHeight.new(zcashNetwork, heightLong)
    }

    suspend fun findBlockHash(blockHeight: BlockHeight): ByteArray? {
        return sqliteDatabase.queryAndMap(
            table = BlockTableDefinition.TABLE_NAME,
            columns = PROJECTION_HASH,
            selection = SELECTION_BLOCK_HEIGHT,
            selectionArgs = arrayOf(blockHeight.value),
            cursorParser = { it.getBlob(0) }
        ).firstOrNull()
    }
}

object BlockTableDefinition {
    const val TABLE_NAME = "blocks" // $NON-NLS

    const val COLUMN_LONG_HEIGHT = "height" // $NON-NLS

    const val COLUMN_BLOB_HASH = "hash" // $NON-NLS
}
