![Indispensable](assets/core-dark.png#only-light)
![Signum](assets/core-light.png#only-dark)

[![Maven Central](https://img.shields.io/maven-central/v/at.asitplus.signum/indispensable?label=maven-central)](https://mvnrepository.com/artifact/at.asitplus.signum.indispensable/)

# Indispensable Core Data Structures and functionality for Cryptographic Material

This [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) library provides platform-independent data
types and functionality related to crypto and PKI applications:

* EC Math
  * EC Point Class
  * EC Curve Class
  * Mathematical operations
  * Bit Length
  * Point Compression
* Public Keys (RSA and EC)
* Algorithm Identifiers (Signatures, Hashing)
* X509 Certificate Class (create, encode, decode)
  * Extensions
  * Alternative Names
  * Distinguished Names
* Certification Request (CSR)
  * CSR Class
  * Attributes
* ObjectIdentifier Class with human-readable notation (e.g. 1.2.9.6245.3.72.13.4.7.6)
* 100% pure Kotlin BitSet
* Exposes Multibase Encoder/Decoder as an API dependency
  including [Matthew Nelson's smashing Base16, Base32, and Base64 encoders](https://github.com/05nelsonm/encoding)
* Generic ASN.1 abstractions to operate on and create arbitrary ASN.1 Data
* Serializability of all ASN.1 classes for debugging **AND ONLY FOR DEBUGGING!!!** *Seriously, do not try to deserialize
    ASN.1 classes through kotlinx.serialization! Use `decodeFromDer()` and its companions!*
* **ASN.1 Parser and Encoder including a DSL to generate ASN.1 structures**

This last bit means that
you can work with X509 Certificates, public keys, CSRs and arbitrary ASN.1 structures on iOS.

**Do check out the full API docs [here](https://a-sit-plus.github.io/signum/)**!

## Using it in your Projects

This library was built for [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html). Currently, it targets
the JVM, Android and iOS.

Simply declare the desired dependency to get going:

```kotlin 
implementation("at.asitplus.signum:indispensable:$version")
```

## Structure and Class Overview
As the name _Indispensable_ implies, this is the base module for all KMP crypto operations.
It includes types, abstractions, and functionality considered absolutely essential to even entertain the thought
of working with and on cryptographic data.

### Package Organisation

#### Fundamental Cryptographic Data Structures
The main package housing all data classes is `at.asitplus.signum.indispensable`.
It contains essentials such as:


* `CryptoPublicKey` representing a public key. Currently, we support RSA and EC public keys on NIST curves.
* `Digest` containing an enumeration of supported
* `ECCurve` representing an EC Curve
* `ECPoint` representing a point on an elliptic curve
* `CryptoSignatre` representing a cryptographic signature including descriptive information regarding the algorithms and signature data
* `SignatureAlgorithm` containing an enumeration of supported signature algorithms
    * `X509SignatureAlgorithm` enumeration of supported X.509 signature algorithms (maps to and from `SignatureAlgorithm`)

#### PKI-Related data Structures
The `pki` package contains data classes relevant in the PKI context:

* `X509Certificate` does what you think it does
    * `X509CertificateExtension` contains a convenience abstraction of X.509 certificate extensions 
    * `AlternativeNames` contains definitions of subject/issuer alternative names
    * `RelativeDistinguishedName` contains definitions of RDNs (Common Name, City, …)
* `Pkcs10CertificateRequest` contains a CSR abstraction
    * `Pcs10CertificateRequestAttributes` contains a CSR attribute extension

#### ASN.1
The `asn1` package contains a 100% pure Kotlin (read: no platform dependencies) ASN.1 engine and data types:

* `Asn1Elements.kt` contains all ANS.1 element types
    * `Asn1Element` is an abstract, generic ASN.1 element. Has a tag and content. Can be DER-encoded
        * `Asn1Element.Tag` representing an ASN.1 tag. Contains user-friendly representations of:
            * Tag number
            * `CONSTRUCTED` bit
            * Tag Class
            * A set of predefined tag constants that are often encountered such as `INTEGER`, `OCTET STRING`, `BIT STRING`, etc…
      * `Asn1Primitive` is an ASN.1 element containing primitive data (string, byte strings, numbers, null, …)
      * `Asn1Structure` is a `CONSTRUCTED` ASN.1 type, containing zero or more child elements
        * `Asn1Sequence` has sequence semantics (order-preserving!)
        * `Asn1SequenceOf` has sequence semantics but allows only child nodes of the same tag
        * `Asn1Set` has set semantics, i.e. sorts all child nodes by tag in accordance with DER
        * `Asn1SetOf` has set semantics but allows only child nodes of the same tag

In addition, some convenience types are also present:

* `Asn1ExplicitlyTagged`, which is essentially a sequence, but with a user-defined `CONTEXT_SPECIFIC` tag
* `Asn1BitString`, wich is an ASN.1 primitive containing bit strings, which are not necessarily byte-aligned.
  Heavily relies on the included `BitSet` type to work its magic.
* `Asn1OctetString`, wich is often encountered in one of two flavours:
    * `Asn1PrimitiveOctetString` containing raw bytes
    * `Asn1EncapsulatingOctetString` containing any number of children. This is a structure, without the `CONSTRUCTED` bit set, using tag number `4`.
* `Asn1CustomStructure` representing a structure with a custom tag, that does not align with any predefined tag.
  Can be constructed to auto-sort children to conform with DER set semantics.
* `ObjectIdentifier` represents an ASN.1 OID
* `Asn1String` contains different String types (printable, UTF-8, numeric, …)
* `Asn1Time` maps from/to kotlinx-datetime `Instant`s and supports both UTC time and generalized time

The `asn1.encoding` package contains the ASN.1 builder DSL, as well as encoding and decoding functions
-- both for whole ASN.1 elements, as wells as for encoding/decoding primitive data types to/from DER-conforming byte arrays.
Most prominently, it comes with ASN.1 unsigned varint and minimum-length encoding of signed numbers.

## Working with Cryptographic Material

### Conversion from/to platform types

## ASN.1 Engine
Relevant classes like `CryptoPublicKey`, `X509Certificate`, `Pkcs10CertificationRequest`, etc. all
implement `Asn1Encodable` and their respective companions implement `Asn1Decodable`.
Which means that you can do things like parsing and examining certificates, creating CSRs, or transferring key
material.


### Generic Patterns
encodable/decodable
encodetoDer
encodeToTlv
Asn1Element vs classes with semantics

### Parsing

Parsing and re-encoding an X.509 certificate works as follows:

```kotlin
val cert = X509Certificate.decodeFromDer(certBytes)

when (val pk = cert.publicKey) {
    is CryptoPublicKey.EC -> println(
        "Certificate with serial no. ${
            cert.tbsCertificate.serialNumber
        } contains an EC public key using curve ${pk.curve}"
    )

    is CryptoPublicKey.Rsa -> println(
        "Certificate with serial no. ${
            cert.tbsCertificate.serialNumber
        } contains a ${pk.bits.number} bit RSA public key"
    )
}

println("Re-encoding it produces the same bytes? ${cert.encodeToDer() contentEquals certBytes}")
```

Which produces the following output:

     Certificate with serial no. 19821EDCA68C59CF contains an EC public key using curve SECP_256_R_1
     Re-encoding it produces the same bytes? true

### Encoding

#### High-Level

#### Low-Level

### Custom Tagging

#### Explicit

#### Implicit

### ASN.1 Builder DSL

While predefined structures are essential for working with cryptographic material in a PKI context,
full control is sometimes required.
Signum directly support this with an ASN.1 builder DSL, including explicit and implicit tagging:

```kotlin
Asn1.Sequence {
    +ExplicitlyTagged(1uL) {
        +Asn1Primitive(Asn1Element.Tag.BOOL, byteArrayOf(0x00)) //or +Asn1.Bool(false)
    }
    +Asn1.Set {
        +Asn1.Sequence {
            +Asn1.SetOf {
                +PrintableString("World")
                +PrintableString("Hello")
            }
            +Asn1.Set {
                +PrintableString("World")
                +PrintableString("Hello")
                +Utf8String("!!!")
            }

        }
    }
    +Asn1.Null()

    +ObjectIdentifier("1.2.603.624.97")

    +(Utf8String("Foo") withImplicitTag (0xCAFEuL withClass TagClass.PRIVATE))
    +PrintableString("Bar")

    //fake Primitive
    +(Asn1.Sequence { +Asn1.Int(42) } withImplicitTag (0x5EUL without CONSTRUCTED))

    +Asn1.Set {
        +Asn1.Int(3)
        +Asn1.Int(-65789876543L)
        +Asn1.Bool(false)
        +Asn1.Bool(true)
    }
    +Asn1.Sequence {
        +Asn1.Null()
        +Asn1String.Numeric("12345")
        +UtcTime(Clock.System.now())
    }
} withImplicitTag (1337uL withClass TagClass.APPLICATION)
```

This produces the following ASN.1 structure:

```
Application 1337 (9 elem)

    [1] (1 elem)
        BOOLEAN false
    SET (1 elem)
        SEQUENCE (2 elem)
            SET (2 elem)
                PrintableString World
                PrintableString Hello
            SET (3 elem)
                UTF8String !!!
                PrintableString World
                PrintableString Hello
    NULL
    OBJECT IDENTIFIER 1.2.603.624.97
    Private 51966 (3 byte) Foo
    PrintableString Bar
    [94] (3 byte) 02012A
    SET (4 elem)
        BOOLEAN false
        BOOLEAN true
        INTEGER 3
        INTEGER (36 bit) -65789876543
    SEQUENCE (3 elem)
        NULL
        NumericString 12345
        UTCTime 2024-09-16 11:53:51 UTC
```