[zcash-android-wallet-sdk](../../index.md) / [cash.z.wallet.sdk.ext](../index.md) / [Twig](./index.md)

# Twig

`interface Twig`

A tiny log.

### Functions

| Name | Summary |
|---|---|
| [plus](plus.md) | `open operator fun plus(twig: `[`Twig`](./index.md)`): `[`Twig`](./index.md) |
| [twig](twig.md) | `abstract fun twig(logMessage: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)` = ""): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html) |

### Companion Object Functions

| Name | Summary |
|---|---|
| [clip](clip.md) | `fun clip(leaf: Leaf): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Clip a leaf from the bush. Clipped leaves no longer appear in logs. |
| [plant](plant.md) | `fun plant(rootTwig: `[`Twig`](./index.md)`): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Plants the twig, making it the one and only bush. Twigs can be bundled together to create the appearance of multiple bushes (i.e `Twig.plant(twigA + twigB + twigC)`) even though there's only ever one bush. |
| [prune](prune.md) | `fun prune(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)<br>Clip all leaves from the bush. |
| [sprout](sprout.md) | `fun sprout(leaf: Leaf): `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)<br>Generate a leaf on the bush. Leaves show up in every log message as tags until they are clipped. |

### Extension Functions

| Name | Summary |
|---|---|
| [twig](../twig.md) | `fun <R> `[`Twig`](./index.md)`.twig(logMessage: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, block: () -> `[`R`](../twig.md#R)`): `[`R`](../twig.md#R)<br>Times a tiny log. Execute the block of code on the clock. |
| [twigTask](../twig-task.md) | `fun <R> `[`Twig`](./index.md)`.twigTask(logMessage: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`, block: () -> `[`R`](../twig-task.md#R)`): `[`R`](../twig-task.md#R)<br>A tiny log task. Execute the block of code with some twigging around the outside. For silent twigs, this adds a small amount of overhead at the call site but still avoids logging. |

### Inheritors

| Name | Summary |
|---|---|
| [CompositeTwig](../-composite-twig/index.md) | `open class CompositeTwig : `[`Twig`](./index.md)<br>Since there can only ever be one trunk on the bush of twigs, this class lets you cheat and make that trunk be a bundle of twigs. |
| [SilentTwig](../-silent-twig/index.md) | `class SilentTwig : `[`Twig`](./index.md)<br>A tiny log that does nothing. No one hears this twig fall in the woods. |
| [TroubleshootingTwig](../-troubleshooting-twig/index.md) | `open class TroubleshootingTwig : `[`Twig`](./index.md)<br>A tiny log for detecting troubles. Aim at your troubles and pull the twigger. |
