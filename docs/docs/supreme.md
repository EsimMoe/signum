![Signum Supreme](assets/supreme-dark-large.png#only-light) ![Signum Supreme](assets/supreme-light-large.png#only-dark)

[![Maven Central](https://img.shields.io/maven-central/v/at.asitplus.signum/supreme?label=maven-central)](https://mvnrepository.com/artifact/at.asitplus.signum/supreme)

# **Supreme** KMP Crypto Provider


This [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) library provides platform-independent data
types and functionality related to crypto and PKI applications:

* **Multiplatform ECDSA and RSA Signer and Verifier** &rarr; Check out the included [CMP demo App](demoapp) to see it in
  action
* Supports Attestation on iOS and Android
* Biometric Authentication on Android and iOS without Callbacks or Activity Passing** (✨Magic!✨)
* Support Attestation on Android and iOS


**Do check out the full API docs [here](https://a-sit-plus.github.io/signum/)**!

## Using it in your Projects

This library was built for [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html). Currently, it targets
the JVM, Android and iOS.

Simply declare the desired dependency to get going:

```kotlin 
implementation("at.asitplus.signum:supreme:$supreme_version")
```
