package cash.z.ecc.android.sdk.demoapp.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import cash.z.ecc.android.sdk.demoapp.preference.api.PreferenceProvider
import cash.z.ecc.android.sdk.demoapp.preference.model.entry.Key
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Provides an Android implementation of shared preferences.
 *
 * This class is thread-safe.
 *
 * For a given preference file, it is expected that only a single instance is constructed and that
 * this instance lives for the lifetime of the application. Constructing multiple instances will
 * potentially corrupt preference data and will leak resources.
 *
 * Implementation note: EncryptedSharedPreferences are not thread-safe, so this implementation
 * confines them to a single background thread.
 */
class AndroidPreferenceProvider(
    private val sharedPreferences: SharedPreferences,
    private val dispatcher: CoroutineDispatcher
) : PreferenceProvider {
    override suspend fun hasKey(key: Key) =
        withContext(dispatcher) {
            sharedPreferences.contains(key.key)
        }

    @SuppressLint("ApplySharedPref")
    override suspend fun putString(
        key: Key,
        value: String?
    ) = withContext(dispatcher) {
        val editor = sharedPreferences.edit()

        editor.putString(key.key, value)

        editor.commit()

        Unit
    }

    override suspend fun getString(key: Key) =
        withContext(dispatcher) {
            sharedPreferences.getString(key.key, null)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(key: Key): Flow<Unit> =
        callbackFlow<Unit> {
            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
                    // Callback on main thread
                    trySend(Unit)
                }
            sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

            // Kickstart the emissions
            trySend(Unit)

            awaitClose {
                sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.flowOn(dispatcher)

    companion object {
        suspend fun newStandard(
            context: Context,
            filename: String
        ): PreferenceProvider {
            /*
             * Because of this line, we don't want multiple instances of this object created
             * because we don't clean up the thread afterwards.
             */
            val singleThreadedDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

            val sharedPreferences =
                withContext(singleThreadedDispatcher) {
                    context.getSharedPreferences(filename, Context.MODE_PRIVATE)
                }

            return AndroidPreferenceProvider(sharedPreferences, singleThreadedDispatcher)
        }

        suspend fun newEncrypted(
            context: Context,
            filename: String
        ): PreferenceProvider {
            /*
             * Because of this line, we don't want multiple instances of this object created
             * because we don't clean up the thread afterwards.
             */
            val singleThreadedDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

            val mainKey =
                withContext(singleThreadedDispatcher) {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    MasterKey
                        .Builder(context)
                        .apply {
                            setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        }.build()
                }

            val sharedPreferences =
                withContext(singleThreadedDispatcher) {
                    @Suppress("BlockingMethodInNonBlockingContext")
                    EncryptedSharedPreferences.create(
                        context,
                        filename,
                        mainKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                }

            return AndroidPreferenceProvider(sharedPreferences, singleThreadedDispatcher)
        }
    }
}
