# Testing
This documentation outlines our approach to testing. By running tests against our app consistently, we verify the 
SDK's correctness, functional behavior, and usability before releasing it publicly.

## Automated testing

- TBD
<!-- TODO [#807]: Testing documentation update --> 
<!-- TODO [#807]: https://github.com/zcash/zcash-android-wallet-sdk/issues/807 --> 

## Manual testing

We aim to automate as much as we possibly can. Still manual testing is really important for Quality Assurance.

Here you'll find our manual testing scripts. When developing a new feature you can add your own that provide the proper steps to properly test it.

## Gathering Code Coverage
The app consists of different Gradle module types (e.g. Kotlin Multiplatform, Android).  Generating coverage for these different module types requires different command line invocations.

### Kotlin Multiplatform
Kotlin Multiplatform does not support coverage for all platforms.  Most of our code lives under commonMain, with a JVM target.  This effectively allows generation of coverage reports with Jacoco.  Coverage is enabled by default when running `./gradlew check`.

### Android
The Android Gradle plugin supports code coverage with Jacoco.  This integration can sometimes be buggy.  For that reason, coverage is disabled by default and can be enabled on a case-by-case basis, by passing `-PIS_ANDROID_INSTRUMENTATION_TEST_COVERAGE_ENABLED=true` as a command line argument for Gradle builds.  For example: `./gradlew connectedCheck -PIS_ANDROID_INSTRUMENTATION_TEST_COVERAGE_ENABLED=true`.

When coverage is enabled, running instrumentation tests will automatically generate coverage reports stored under `$module/build/reports/coverage`.

## Benchmarking
This section provides information about available benchmark tests integrated in this project as well as how to use them. Currently, we support macrobenchmark tests run locally as described in the Android [documentation](https://developer.android.com/topic/performance/benchmarking/benchmarking-overview).

We provide dedicated benchmark test module `demo-app-benchmark-test` for this. If you want to run these benchmark
tests against our demo application, make sure you have a physical device connected with Android SDK level 29, at least.
Select `benchmark` build variant for this module. Make sure that other modules are set to benchmark
type too. The benchmark tests can be run with Android Studio run configuration
`demo-app-benchmark-test:connectedBenchmarkAndroidTest`. Running the benchmark test this way automatically
provides benchmarking results in Run panel. Or you can run the tests manually from the terminal with `./gradlew connectedBenchmarkAndroidTest` and analyze results with Android Studio's Profiler or [Perfetto](https://ui.perfetto.dev/) tool, as described in this Android [documentation](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview#access-trace).

**Note**: We've enabled benchmarking also for emulators, although it's always better to run the tests on a real physical device. Emulator benchmark improvements might not carry over to a real user's experience (or even regress real device performance).

### Referential benchmark tests results 
Every few months, or before a major SDK release, we run and compare benchmark test results to avoid making the SDK's mechanisms significantly slower.

**Note**: If possible, run the benchmark tests on a physical device with sufficient empty disk space, connected to the
internet and charged or plugged-in to a charger. It's always better to restart the device before approaching to 
running the benchmark tests. Also, please, ensure you're running it on the latest main branch
commits of that date. Generate tests results with the Android Studio run configuration
`demo-app-benchmark-test:connectedBenchmarkAndroidTest` and gather results from the Run panel.

#### Dec 7, 2022:

- SDK version: `1.10.0-beta01`
- Git branch: `789-benchmark-demo-app`
- Device: 
  - Pixel 6 - Android 13:
    ```
    Starting 3 tests on Pixel 6 - 13
    
    StartupBenchmark_appStartup
    timeToInitialDisplayMs   min 388.8,   median 410.9,   max 423.0
    Traces: Iteration 0 1 2 3 4
    
    StartupBenchmark_tracesSdkStartup
    ADDRESS_SCREENMs   min 784.1,   median 900.4,   max 926.9
    SAPLING_ADDRESSMs   min   2.4,   median   4.3,   max   5.9
    TRANSPARENT_ADDRESSMs   min   1.7,   median   2.6,   max   6.0
    UNIFIED_ADDRESSMs   min   1.5,   median   2.2,   max   2.8
    Traces: Iteration 0 1 2 3 4
    
    SyncBlockchainBenchmark_tracesSyncBlockchain
    BALANCE_SCREENMs   min 46,042.2,   median 46,233.0,   max 46,462.2
    BLOCKCHAIN_SYNCMs   min 45,393.5,   median 45,578.3,   max 45,830.3
    DOWNLOADMs   min 34,951.3,   median 35,763.0,   max 35,870.6
    SCANMs   min  9,536.7,   median  9,846.5,   max 10,501.8
    VALIDATIONMs   min     93.1,   median    112.5,   max    124.3
    Traces: Iteration 0 1 2
    
    BUILD SUCCESSFUL in 4m 18s
    ```
  - Pixel 3a - Android 12:
    ```
    Starting 3 tests on Pixel 3a - 12
    
    StartupBenchmark_appStartup
    timeToInitialDisplayMs   min 545.3,   median 565.3,   max 607.2
    Traces: Iteration 0 1 2 3 4
    
    StartupBenchmark_tracesSdkStartup
    ADDRESS_SCREENMs   min   897.1,   median   955.3,   max 1,352.8
    SAPLING_ADDRESSMs   min     3.9,   median     6.1,   max     8.3
    TRANSPARENT_ADDRESSMs   min     2.0,   median     4.2,   max     5.9
    UNIFIED_ADDRESSMs   min     2.4,   median     2.4,   max     5.2    
    Traces: Iteration 0 1 2 3 4
    
    SyncBlockchainBenchmark_tracesSyncBlockchain
    BALANCE_SCREENMs   min 63,403.1,   median 63,716.7,   max 63,993.6
    BLOCKCHAIN_SYNCMs   min 62,739.0,   median 63,060.5,   max 63,345.0
    DOWNLOADMs   min 34,317.3,   median 34,462.8,   max 34,551.3
    SCANMs   min 28,279.3,   median 28,463.3,   max 28,655.8
    VALIDATIONMs   min    133.0,   median    136.4,   max    141.2
    Traces: Iteration 0 1 2
    
    BUILD SUCCESSFUL in 6m 12s
    ```

#### Feb 18, 2023:

- SDK version: `1.14.0-beta01`
- Git branch: `765-Store_blocks_on_disk_instead_of_in_SQLite`
- Note: Switched to storing cache blocks blob files on disk instead of in SQLite database 
- Device:
  - Pixel 6 - Android 13:
    ```
    Starting 3 tests on Pixel 6 - 13

    StartupBenchmark_appStartup
    timeToInitialDisplayMs   min 248.6,   median 287.0,   max 385.7
    Traces: Iteration 0 1 2 3 4
    
    StartupBenchmark_tracesSdkStartup
    ADDRESS_SCREENMs   min   935.5,   median   943.3,   max 1,013.7
    SAPLING_ADDRESSMs   min     2.1,   median     3.7,   max     6.9
    TRANSPARENT_ADDRESSMs   min     2.2,   median     3.3,   max     7.5
    UNIFIED_ADDRESSMs   min     2.2,   median     3.8,   max     5.8
    Traces: Iteration 0 1 2 3 4

    SyncBlockchainBenchmark_tracesSyncBlockchain
    BALANCE_SCREENMs   min 67,376.0,   median 67,614.0,   max 73,651.2
    BLOCKCHAIN_SYNCMs   min 66,449.6,   median 66,624.1,   max 72,865.5
    DOWNLOADMs   min 52,245.6,   median 52,442.0,   max 56,489.1
    SCANMs   min 14,061.2,   median 14,074.5,   max 16,261.6
    VALIDATIONMs   min    114.6,   median    120.7,   max    129.1
    Traces: Iteration 0 1 2

    BUILD SUCCESSFUL in 5m 9s
    ```
  - Pixel 3a - Android 12:
    ```
    Starting 3 tests on Pixel 3a - 12

    StartupBenchmark_appStartup
    timeToInitialDisplayMs   min 475.8,   median 513.6,   max 531.8
    Traces: Iteration 0 1 2 3 4
    
    StartupBenchmark_tracesSdkStartup
    ADDRESS_SCREENMs   min   978.6,   median 1,094.6,   max 1,156.1
    SAPLING_ADDRESSMs   min     4.7,   median     6.9,   max     7.3
    TRANSPARENT_ADDRESSMs   min     5.3,   median     5.5,   max    11.6
    UNIFIED_ADDRESSMs   min     3.9,   median     7.5,   max    10.8
    Traces: Iteration 0 1 2 3 4

    SyncBlockchainBenchmark_tracesSyncBlockchain
    BALANCE_SCREENMs   min 66,203.0,   median 66,274.0,   max 66,472.4
    BLOCKCHAIN_SYNCMs   min 65,279.5,   median 65,417.6,   max 65,570.8
    DOWNLOADMs   min 34,191.6,   median 34,392.5,   max 34,447.2
    SCANMs   min 30,870.4,   median 30,944.6,   max 30,981.0
    VALIDATIONMs   min    142.4,   median    143.2,   max    154.5
    Traces: Iteration 0 1 2

    BUILD SUCCESSFUL in 6m 58s
    ```





