package cash.z.wallet.sdk.ext

import android.content.Context
import android.content.SharedPreferences
import cash.z.wallet.sdk.BuildConfig

fun SharedPrefs(context: Context, name: String = "prefs"): SharedPreferences {
    val fileName = "${BuildConfig.FLAVOR}.${BuildConfig.BUILD_TYPE}.$name".toLowerCase()
    return context.getSharedPreferences(fileName, Context.MODE_PRIVATE)!!
}

inline fun SharedPreferences.edit(block: (SharedPreferences.Editor) -> Unit) {
    edit().run {
        block(this)
        apply()
    }
}

operator fun SharedPreferences.set(key: String, value: Any?) {
    when (value) {
        is String? -> edit { it.putString(key, value) }
        is Int -> edit { it.putInt(key, value) }
        is Boolean -> edit { it.putBoolean(key, value) }
        is Float -> edit { it.putFloat(key, value) }
        is Long -> edit { it.putLong(key, value) }
        else -> throw UnsupportedOperationException("Not yet implemented")
    }
}

inline operator fun <reified T : Any> SharedPreferences.get(key: String, defaultValue: T? = null): T? {
    return when (T::class) {
        String::class -> getString(key, defaultValue as? String) as T?
        Int::class -> getInt(key, defaultValue as? Int ?: -1) as T?
        Boolean::class -> getBoolean(key, defaultValue as? Boolean ?: false) as T?
        Float::class -> getFloat(key, defaultValue as? Float ?: -1f) as T?
        Long::class -> getLong(key, defaultValue as? Long ?: -1) as T?
        else -> throw UnsupportedOperationException("Not yet implemented")
    }
}