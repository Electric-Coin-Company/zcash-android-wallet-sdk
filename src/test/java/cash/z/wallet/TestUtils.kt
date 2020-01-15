package cash.z.wallet

import org.mockito.Mockito

/**
 * Use in place of `any()` to fix the issue with mockito `any` returning null (so you can't pass it to functions that
 * take a non-null param)
 *
 * TODO: perhaps submit this function to the mockito kotlin project because it allows the use of non-null 'any()'
 */
internal fun <T> anyNotNull() = Mockito.any<T>() as T