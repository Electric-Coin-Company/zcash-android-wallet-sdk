@if "%DEBUG%" == "" @echo off

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

@rem check for cargo installation
cargo --version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto cargoFound

echo Cargo appears to be missing.
echo Go to https://rustup.rs/ to install it, and then run this script again.
exit /b 1

:cargoFound
echo Cargo found!

@rem update android targets
echo Updating android targets...
echo     rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android
rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android
echo Done.

@rem check for standalone NDK
call build-ndk-standalone.bat
if %ERRORLEVEL% neq 0 exit /b 1


echo Building...
echo    building aarch64...
cargo build --target aarch64-linux-android --release
if %ERRORLEVEL% neq 0 exit /b 10

echo    building i686...
cargo build --target i686-linux-android --release
if %ERRORLEVEL% neq 0 exit /b 20

echo    building armv7...
cargo build --target armv7-linux-androideabi --release
if %ERRORLEVEL% neq 0 exit /b 30

echo Done.
