[zcash-android-wallet-sdk](../../index.md) / [cash.z.ecc.android.sdk.ext](../index.md) / [Twig](./index.md)

# Twig

`interface Twig`

A tiny log.

### Functions

| Name | Summary |
|---|---|
| [plus](plus.md) | Bundles twigs together`open operator fun plus(twig: `[`Twig`](./index.md)`): `[`Twig`](./index.md) |
| [twig](twig.md) | Log the message. Simple.`abstract fun twig(logMessage: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)` = ""): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Companion Object Functions

| Name | Summary |
|---|---|
| [clip](clip.md) | Clip a leaf from the bush. Clipped leaves no longer appear in logs.`fun clip(leaf: Leaf): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |
| [plant](plant.md) | Plants the twig, making it the one and only bush. Twigs can be bundled together to create the appearance of multiple bushes (i.e `Twig.plant(twigA + twigB + twigC)`) even though there's only ever one bush.`fun plant(rootTwig: `[`Twig`](./index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [prune](prune.md) | Clip all leaves from the bush.`fun prune(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |
| [sprout](sprout.md) | Generate a leaf on the bush. Leaves show up in every log message as tags until they are clipped.`fun sprout(leaf: Leaf): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html) |

### Extension Functions

| Name | Summary |
|---|---|
| [twig](../twig.md) | Times a tiny log. Execute the block of code on the clock.`fun <R> `[`Twig`](./index.md)`.twig(logMessage: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, block: () -> R): R` |
| [twigTask](../twig-task.md) | A tiny log task. Execute the block of code with some twigging around the outside. For silent twigs, this adds a small amount of overhead at the call site but still avoids logging.`fun <R> `[`Twig`](./index.md)`.twigTask(logMessage: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, block: () -> R): R` |

### Inheritors

| Name | Summary |
|---|---|
| [CompositeTwig](../-composite-twig/index.md) | Since there can only ever be one trunk on the bush of twigs, this class lets you cheat and make that trunk be a bundle of twigs.`open class CompositeTwig : `[`Twig`](./index.md) |
| [SilentTwig](../-silent-twig/index.md) | A tiny log that does nothing. No one hears this twig fall in the woods.`class SilentTwig : `[`Twig`](./index.md) |
| [TroubleshootingTwig](../-troubleshooting-twig/index.md) | A tiny log for detecting troubles. Aim at your troubles and pull the twigger.`open class TroubleshootingTwig : `[`Twig`](./index.md) |
