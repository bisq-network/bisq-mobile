<p align="center">
  <img src="https://bisq.network/images/bisq-logo.svg"/>
</p>

# Bisq Mobile

This project aims to make Bisq Network accesible in Mobile Platforms following the philosofy of Bisq2 - to make it
easier for both, experienced and newcomers, to trade Bitcoin in a decentralized way.

Currently, this project is a playground to start a POC with the following goals:

- Focus on Android first
- Have a general look and feel native to Android and respectful of Bisq2
- Minimal usable functionality for the mobile world: focus on transacting in Bisq using Mobile and just that and its
  UX (e.g. add push notifications).
- Able to connect to Bisq Network using clearnet (and soon after, with Tor)

For more info please refer to [Bisq Mobile Discussions](https://github.com/bisq-network/bisq2/discussions/2665)

## How to contribute

- As a general rule, please follow same Bisq guidelines for contributions
- Please reachout on Matrix for questions, this project is kickstarting

Once you downloaded the code and are ready to run it and play with it, we highly recommend that you
install [Fleet IDE](https://www.jetbrains.com/fleet/).
**This IDE makes it a breeze to download and install everything you need to run a KMP project**

Of course you can stick to XCode for iOS and Android Studio for Android or whichever IDE you prefer.

## Why KMP

- Native Performance
- Allows us to focus on the "easiest" platform first (Because of Apple restrictions on Tor and networking in general)
- Flexibility without the security/privacy concerns of its competitors
- JVM language allows us to port much of the optimised Bisq code already existing in the Desktop apps

For a reference to the sample code used to kickstart this POC, see below

## App Designs

// TODO - link to doc folder with the selected initial design (figma?)

<-################################################################################################################################################################################################################################->

## Kotlin Multiplatform app template

[![official project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

This is a basic Kotlin Multiplatform app template for Android and iOS. It includes shared business logic and data
handling, and native UI implementations using Jetpack Compose and SwiftUI.

> The template is also
> available [with shared UI written in Compose Multiplatform](https://github.com/kotlin/KMP-App-Template).
>
> The [`amper` branch](https://github.com/Kotlin/KMP-App-Template-Native/tree/amper) showcases the same project
> configured with [Amper](https://github.com/JetBrains/amper).

![Screenshots of the app](images/screenshots.png)

### Technologies

The data displayed by the app is from [The Metropolitan Museum of Art Collection API](https://metmuseum.github.io/).

The app uses the following multiplatform dependencies in its implementation:

- [Ktor](https://ktor.io/) for networking
- [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) for JSON handling
- [Koin](https://github.com/InsertKoinIO/koin) for dependency injection
- [KMP-ObservableViewModel](https://github.com/rickclephas/KMP-ObservableViewModel) for shared ViewModel implementations
  in common code
- [KMP-NativeCoroutines](https://github.com/rickclephas/KMP-NativeCoroutines)

> These are just some of the possible libraries to use for these tasks with Kotlin Multiplatform, and their usage here
> isn't a strong recommendation for these specific libraries over the available alternatives. You can find a wide
> variety
> of curated multiplatform libraries in the [kmp-awesome](https://github.com/terrakok/kmp-awesome) repository.

And the following Android-specific dependencies:

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Navigation component](https://developer.android.com/jetpack/compose/navigation)
- [Coil](https://github.com/coil-kt/coil) for image loading
