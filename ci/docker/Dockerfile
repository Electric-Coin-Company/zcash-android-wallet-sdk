FROM debian:stretch
RUN apt-get update \
  && apt-get install -y openjdk-8-jdk cmake curl wget git unzip python

ARG BUILD_USER=builder
ARG BUILD_UID=2001
ARG ANDROID_COMPILE_SDK=29
ARG ANDROID_BUILD_TOOLS=
ARG ANDROID_SDK_TOOLS=4333796 
ARG ANDROID_NDK_TOOLS=21.1.6352462
RUN useradd --home-dir /srv/$BUILD_USER \
            --shell /bin/bash \
            --create-home \
            --uid $BUILD_UID\
            $BUILD_USER
USER $BUILD_USER
WORKDIR /srv/$BUILD_USER

RUN wget --quiet --output-document=/tmp/sdk-tools-linux.zip https://dl.google.com/android/repository/sdk-tools-linux-$ANDROID_SDK_TOOLS.zip \
    && unzip /tmp/sdk-tools-linux.zip -d $HOME/.android \
    && mkdir -p $HOME/.android \ 
    && touch $HOME/.android/repositories.cfg \
    && mkdir $HOME/.android/licenses \
    && echo y | $HOME/.android/tools/bin/sdkmanager "ndk;$ANDROID_NDK_TOOLS" >/dev/null \
    && echo y | $HOME/.android/tools/bin/sdkmanager "platform-tools" "platforms;android-$ANDROID_COMPILE_SDK" >/dev/null \
    && echo y | $HOME/.android/tools/bin/sdkmanager "system-images;android-$ANDROID_COMPILE_SDK;default;x86" >/dev/null 

RUN curl https://sh.rustup.rs -sSf | sh -s -- -y

ENV ANDROID_HOME=/srv/$BUILD_USER/.android
ENV ANDROID_SDK_ROOT=/srv/$BUILD_USER/.android
ENV PATH=/srv/$BUILD_USER/.cargo/bin/:/srv/$BUILD_USER/.android:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin

ENV RUSTUP_HOME=/srv/$BUILD_USER/.cargo/bin/
ENV CARGO_HOME=/srv/$BUILD_USER/.cargo

RUN rustup install stable \
    && rustup default stable \
    && rustup target add armv7-linux-androideabi \
    && rustup target add aarch64-linux-android \
    && rustup target add i686-linux-android \
    && rustup target add x86_64-linux-android
