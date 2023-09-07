package cash.z.ecc.android.sdk.internal.db.derived

import androidx.sqlite.db.SupportSQLiteDatabase
import cash.z.ecc.android.sdk.internal.db.queryAndMap
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.flow.first
import java.util.Locale

internal class BlockTable(private val zcashNetwork: ZcashNetwork, private val sqliteDatabase: SupportSQLiteDatabase) {
    companion object {

        private val SELECTION_MAX_HEIGHT = arrayOf(
            String.format(
                Locale.ROOT,
                "MAX(%s)", // $NON-NLS
                BlockTableDefinition.COLUMN_LONG_HEIGHT
            )
        )
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
}

internal object BlockTableDefinition {
    const val TABLE_NAME = "blocks" // $NON-NLS

    const val COLUMN_LONG_HEIGHT = "height" // $NON-NLS
}
