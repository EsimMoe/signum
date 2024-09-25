![Signum Supreme](assets/supreme-dark-large.png#only-light) ![Signum Supreme](assets/supreme-light-large.png#only-dark)

[![Maven Central](https://img.shields.io/maven-central/v/at.asitplus.signum/supreme?label=maven-central)](https://mvnrepository.com/artifact/at.asitplus.signum/supreme)

# **Supreme** KMP Crypto Provider

This [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) library provides platform-independent data
types and functionality related to crypto and PKI applications:

* **Multiplatform ECDSA and RSA Signer and Verifier** &rarr; Check out the included [CMP demo App](https://github.com/a-sit-plus/signum/tree/main/demoapp) to see it in
  action
* Supports Attestation on iOS and Android
* Biometric Authentication on Android and iOS without Callbacks or Activity Passing** (✨Magic!✨)
* Support Attestation on Android and iOS

**Do check out the full API docs [here](https://a-sit-plus.github.io/signum/supreme/index.html)**!

## Using it in your Projects

This library was built for [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html). Currently, it targets
the JVM, Android and iOS.

Simply declare the desired dependency to get going:

```kotlin 
implementation("at.asitplus.signum:supreme:$supreme_version")
```

## Key Design Principles
The Supreme KMP crypto provider works differently than JCA. It uses a `Provider` to manage private key material and create `Signer` instances,
and a `Verifier`, that is instantiated on a `SignatureAlgorithm`, taking a `CryptoPublicKey` as parameter.
In addition, creating ephemeral keys is a dedicated operation, decoupled from a `Provider`.
The actual implementation of cryptographic functionality is delegated to platform-native implementations.

## Signature Creation

## Signature Verification

To verify a signature, obtain a `Verifier` instance using `verifierFor(k: PublicKey)`, either directly on a
`SignatureAlgorithm`, or on one of the specialized algorithms (`X509SignatureAlgorithm`, `CoseAlgorithm`, ...).
A variety of constants, resembling the well-known JCA names, are also available in `SignatureAlgorithm`'s companion.

As an example, here's how to verify a basic signature using a public key:

```kotlin
val publicKey: CryptoPublicKey.EC = TODO("You have this and trust it.")
val plaintext = "You want to trust this.".encodeToByteArray()
val signature: CryptoSignature = TODO("This was sent alongside the plaintext.")
val verifier = SignatureAlgorithm.ECDSAwithSHA256.verifierFor(publicKey).getOrThrow()
val isValid = verifier.verify(plaintext, signature).isSuccess
println("Looks good? $isValid")
```

There really is not much more to it. This pattern works the same on all platforms.
Details on how to parse cryptographic material can be found in the [section on decoding](indispensable.md#decoding) in
of the Indispensable module description.

## Ephemeral Keys

## Digest Calculation
The Supreme KMP crypto provider introduces a `digest()` extension function on the `Digest` class.
For a list of supported algorithms, check out the [feature matrix](features.md#supported-algorithms).