# Running Darksidewalletd tests
Some tests are executed against a fake version of the Zcash network, by running a localhost lightwalletd server in a special mode called "darkside".  This is different from the Zcash test network, which is a publicly accessible and deployed network that acts more like a staging network before changes are pushed to the production network.

The module [darkside-test-lib](../../darkside-test-lib) contains a test suite that requires manually launching a localhost lightwalletd instance in darkside mode.

To run these tests

1. clone [lightwalletd](https://github.com/zcash/lightwalletd.git)
`git clone https://github.com/zcash/lightwalletd.git`
1. Install Go.
    1. If you're using homebrew
        ```` zsh
        brew install go
        ````
1. Inside the `lightwalletd` checkout, compile lightwalletd
    ```` zsh
    make
    ````

1. Inside the `lightwalletd` checkout, run the program in _darkside_ mode
    ```` zsh
    ./lightwalletd --log-file /dev/stdout --darkside-very-insecure  --darkside-timeout 1000 --gen-cert-very-insecure --data-dir . --no-tls-very-insecure
    ````
1. Launch an Android emulator. Darkside tests are configured to only run on an Android emulator, as this makes it easy to automate finding the localhost server running on the same computer that's also running the emulator.
1. Run the Android test suite
    1. From the command line
        ```` zsh
        ./gradlew :darkside-test-lib:connectedAndroidTest
        ````
    1. From Android Studio
        1. Choose the run configuration `:darkside-test-lib:connectedAndroidTest`
