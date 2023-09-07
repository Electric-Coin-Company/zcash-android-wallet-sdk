package cash.z.ecc.android.sdk.internal.db.derived

import androidx.sqlite.db.SupportSQLiteDatabase
import cash.z.ecc.android.sdk.model.ZcashNetwork

internal class BlockTable(private val zcashNetwork: ZcashNetwork, private val sqliteDatabase: SupportSQLiteDatabase) {
    companion object
}
