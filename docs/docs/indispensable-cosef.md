![indispensable-cosef](assets/cosef-dark.png#only-light) ![indispensable-cosef](assets/cosef-light.png#only-dark)

[![Maven Central](https://img.shields.io/maven-central/v/at.asitplus.signum/indispensable-cosef?label=maven-central)](https://mvnrepository.com/artifact/at.asitplus.signum.indispensable-cosef/)

# Indispensable COSE Data Structures

This [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) library provides platform-independent COSE data
types and utility functions:

* Public Keys (RSA and EC)
* Algorithm Identifiers (Signatures, Hashing)
* X509 Certificate Class (create, encode, decode)
* Certification Request (CSR)
* ObjectIdentifier Class with human-readable notation (e.g. 1.2.9.6245.3.72.13.4.7.6)
* Generic ASN.1 abstractions to operate on and create arbitrary ASN.1 Data
* Serializability of all ASN.1 classes for debugging **AND ONLY FOR DEBUGGING!!!** *Seriously, do not try to deserialize
  ASN.1 classes through kotlinx.serialization! Use `decodeFromDer()` and its companions!*
* 100% pure Kotlin BitSet
* Exposes Multibase Encoder/Decoder as an API dependency
  including [Matthew Nelson's smashing Base16, Base32, and Base64 encoders](https://github.com/05nelsonm/encoding)
* **ASN.1 Parser and Encoder including a DSL to generate ASN.1 structures**

This last bit means that
**you can work with X509 Certificates, public keys, CSRs and arbitrary ASN.1 structures on iOS.**.

**Do check out the full API docs [here](https://a-sit-plus.github.io/signum/)**!

## Using it in your Projects

This library was built for [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html). Currently, it targets
the JVM, Android and iOS.

Simply declare the desired dependency to get going:

```kotlin 
implementation("at.asitplus.signum:indispensable-cosef:$version")
```
