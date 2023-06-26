package cash.z.ecc.android.sdk.fixture

import android.content.Context
import cash.z.ecc.android.sdk.internal.Backend
import cash.z.ecc.android.sdk.internal.db.derived.DbDerivedDataRepository
import cash.z.ecc.android.sdk.internal.db.derived.DerivedDataDb
import cash.z.ecc.android.sdk.internal.model.Checkpoint
import cash.z.ecc.android.sdk.model.UnifiedFullViewingKey
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.runBlocking
import java.io.File

internal object DbDerivedDataRepositoryFixture {

    @Suppress("LongParameterList")
    fun new(
        context: Context,
        backend: Backend,
        databaseFile: File,
        zcashNetwork: ZcashNetwork,
        checkpoint: Checkpoint,
        seed: ByteArray?,
        viewingKeys: List<UnifiedFullViewingKey>
    ): DbDerivedDataRepository {
        return DbDerivedDataRepository(
            runBlocking {
                DerivedDataDb.new(
                    context,
                    backend,
                    databaseFile,
                    zcashNetwork,
                    checkpoint,
                    seed,
                    viewingKeys
                )
            }
        )
    }
}
