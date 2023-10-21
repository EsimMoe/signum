@file:OptIn(ExperimentalEncodingApi::class)

package at.asitplus.crypto.datatypes.asn1

import at.asitplus.crypto.datatypes.asn1.BERTags.BIT_STRING
import at.asitplus.crypto.datatypes.asn1.BERTags.BOOLEAN
import at.asitplus.crypto.datatypes.asn1.BERTags.GENERALIZED_TIME
import at.asitplus.crypto.datatypes.asn1.BERTags.INTEGER
import at.asitplus.crypto.datatypes.asn1.BERTags.NULL
import at.asitplus.crypto.datatypes.asn1.BERTags.OBJECT_IDENTIFIER
import at.asitplus.crypto.datatypes.asn1.BERTags.UTC_TIME
import at.asitplus.crypto.datatypes.asn1.DERTags.toExplicitTag
import io.matthewnelson.encoding.base16.Base16
import io.matthewnelson.encoding.core.Decoder.Companion.decodeToByteArray
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString
import kotlinx.datetime.Instant
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Class Providing a DSL for creating arbitrary ASN.1 structures. You will almost certainyl never use it directly, but rather use it as follows:
 * ```kotlin
 * asn1Sequence {
 *   tagged(31u) {
 *     Asn1Primitive(BERTags.BOOLEAN, byteArrayOf(0x00))
 *   }
 *   set {
 *     sequence {
 *       setOf { //note: DER encoding enfoces sorting here, so the result switches those
 *         printableString { "World" }
 *         printableString { "Hello" }
 *       }
 *       set { //note: DER encoding enfoces sorting by tags, so the order changes in the ourput
 *         printableString { "World" }
 *         printableString { "Hello" }
 *         utf8String { "!!!" }
 *       }
 *     }
 *   }
 *   asn1null()
 *
 *   oid { ObjectIdentifier("1.2.60873.543.65.2324.97") }
 *
 *   utf8String { "Foo" }
 *   printableString { "Bar" }
 *
 *   set {
 *     int { 3 }
 *     long { 123456789876543L }
 *     bool { false }
 *     bool { true }
 *   }
 *   sequence {
 *     asn1null()
 *     hexEncoded { "CAFEBABE" }
 *     hexEncoded { "BADDAD" }
 *     utcTime { instant }
 *   }
 * }
 * ```
 */
class Asn1TreeBuilder {
    internal val elements = mutableListOf<Asn1Element>()

    /**
     * appends a single [Asn1Element] to this ASN.1 structure
     */
    fun append(child: Asn1Element) {
        elements += child
    }

    /**
     * appends a single [Asn1Encodable] to this ASN.1 structure
     */
    fun append(child: Asn1Encodable<*>) = append(child.encodeToTlv())

    /**
     * EXPLICITLY tags and encapsulates the result of [init]
     * <br>
     * **NOTE:** automatically calls [at.asitplus.crypto.datatypes.asn1.DERTags.toExplicitTag] on [tag]
     */
    fun tagged(tag: UByte, init: Asn1TreeBuilder.() -> Unit) {
        val seq = Asn1TreeBuilder()
        seq.init()
        elements += Asn1Tagged(tag.toExplicitTag(), seq.elements)
    }

    /**
     * Adds a BOOL [Asn1Primitive] to this ASN.1 structure
     */
    fun bool(block: () -> Boolean) {
        elements += block().encodeToTlv()
    }

    /**
     * Adds an INTEGER [Asn1Primitive] to this ASN.1 structure
     */
    fun int(block: () -> Int) {
        elements += block().encodeToTlv()
    }

    /**
     * Adds an INTEGER [Asn1Primitive] to this ASN.1 structure
     */
    fun long(block: () -> Long) {
        elements += block().encodeToTlv()
    }

    /**
     * Adds the passed bytes as OCTET STRING [Asn1Element] to this ASN.1 structure
     */
    fun octetString(block: () -> ByteArray) = apply { elements += block().encodeToTlvOctetString() }

    /**
     * Adds the passed bytes as BIT STRING [Asn1Primitive] to this ASN.1 structure
     */
    fun bitString(block: () -> ByteArray) {
        elements += block().encodeToTlvBitString()
    }

    /**
     * Shorthand method taking a HEX representation of an OID value, adding it as an OBJECT IDENTIFIER to this ASN.1 structure.
     * Really only useful for quick debugging against other ASN.1 decoders, such as https://lapo.it/asn1js/, so we're keeping it for now
     */
    @Deprecated("Used only for quick debugging. May be removed in the future")
    fun hexEncoded(block: () -> String) {
        elements += block().encodeTolvOid()
    }

    /**
     * Adds the passed string as UTF8 STRING to this ASN.1 structure
     */
    fun utf8String(block: () -> String) {
        elements += Asn1String.UTF8(block()).encodeToTlv()
    }

    /**
     * Adds the passed string as PRINTABLE STRING to this ASN.1 structure
     */
    fun printableString(block: () -> String) {
        elements += Asn1String.Printable(block()).encodeToTlv()
    }

    /**
     * Adds the passed [Asn1String] to this ASN.1 structure
     */
    fun string(block: () -> Asn1String) {
        val str = block()
        str.encodeToTlv()
    }


    /**
     * Adds a NULL to this ASN.1 structure
     */
    fun asn1null() {
        elements += Asn1Primitive(NULL, byteArrayOf())
    }

    /**
     * Adds the passed instant as UTC TIME to this ASN.1 structure
     */
    fun utcTime(block: () -> Instant) {
        elements += block().encodeToAsn1UtcTime()
    }

    /**
     * Adds the passed instant as GENERALIZED TIME to this ASN.1 structure
     */
    fun generalizedTime(block: () -> Instant) {
        elements += block().encodeToAsn1GeneralizedTime()
    }

    private fun nest(type: CollectionType, init: Asn1TreeBuilder.() -> Unit) {
        val seq = Asn1TreeBuilder()
        seq.init()
        elements += if (type == CollectionType.SEQUENCE) Asn1Sequence(seq.elements)
        else if (type == CollectionType.OCTET_STRING) Asn1EncapsulatingOctetString(seq.elements)
        else Asn1Set(seq.elements.let {
            if (type == CollectionType.SET) it.sortedBy { it.tag }
            else {
                if (it.any { elem -> elem.tag != it.first().tag }) throw IllegalArgumentException("SET_OF must only contain elements of the same tag")
                it.sortedBy { it.derEncoded.encodeToString(Base16) } //TODO this is inefficient
            }
        })
    }

    /**
     * Recursive version of this builder. Adds all children as SEQUENCE to this ASN.1 structure.
     * Use as follows:
     *
     * ```kotlin
     *   set {
     *     sequence {
     *       setOf { //note: DER encoding enforces sorting here, so the result switches those
     *         printableString { "World" }
     *         printableString { "Hello" }
     *       }
     *       set { //note: DER encoding enforces sorting by tags, so the order changes in the output
     *         printableString { "World" }
     *         printableString { "Hello" }
     *         utf8String { "!!!" }
     *       }
     *     }
     *   }
     *  ```
     */
    fun sequence(init: Asn1TreeBuilder.() -> Unit) = nest(CollectionType.SEQUENCE, init)

    /**
     * Recursive version of this builder. Adds all children as SET to this ASN.1 structure, meaning they will be sorted by tag.
     * Use as follows:
     *
     * ```kotlin
     *   set {
     *     sequence {
     *       setOf { //note: DER encoding enforces sorting here, so the result switches those
     *         printableString { "World" }
     *         printableString { "Hello" }
     *       }
     *       set { //note: DER encoding enforces sorting by tags, so the order changes in the output
     *         printableString { "World" }
     *         printableString { "Hello" }
     *         utf8String { "!!!" }
     *       }
     *     }
     *   }
     *  ```
     */
    fun set(init: Asn1TreeBuilder.() -> Unit) = nest(CollectionType.SET, init)

    /**
     * Recursive version of this builder. Adds all children as SET OF to this ASN.1 structure, meaning that every element needs to have the same tag and will be sorted.
     * Use as follows:
     *
     * ```kotlin
     *   set {
     *     sequence {
     *       setOf { //note: DER encoding enforces sorting here, so the result switches those
     *         printableString { "World" }
     *         printableString { "Hello" }
     *       }
     *       set { //note: DER encoding enforces sorting by tags, so the order changes in the output
     *         printableString { "World" }
     *         printableString { "Hello" }
     *         utf8String { "!!!" }
     *       }
     *     }
     *   }
     *  ```
     */
    fun setOf(init: Asn1TreeBuilder.() -> Unit) = nest(CollectionType.SET_OF, init)

    /**
     * OCTET STRING builder. The result of [init] is encapsulated into an ASN.1 OCTET STRING and then added to this ASN.1 structure
     * ```kotlin
     *   set {
     *     octetString {
     *       printableString { "Hello" }
     *       printableString { "World" }
     *       sequence {
     *         printableString { "World" }
     *         printableString { "Hello" }
     *         utf8String { "!!!" }
     *       }
     *     }
     *   }
     *  ```
     */
    fun octetStringEncapsulated(init: Asn1TreeBuilder.() -> Unit) = nest(CollectionType.OCTET_STRING, init)

}

private enum class CollectionType {
    SET,
    SEQUENCE,
    SET_OF,
    OCTET_STRING
}

/**
 * Creates a new SEQUENCE as [Asn1Sequence].
 * Use as follows:
 *
 * ```kotlin
 * sequence {
 *   asn1Null()
 *   printableString { "World" }
 *   printableString { "Hello" }
 *   utf8String { "!!!" }
 * }
 *  ```
 */
fun asn1Sequence(root: Asn1TreeBuilder.() -> Unit): Asn1Sequence {
    val seq = Asn1TreeBuilder()
    seq.root()
    return Asn1Sequence(seq.elements)
}

/**
 * Creates a new  SET as [Asn1Set]. Elements are sorted by tag.
 * Use as follows:
 *
 * ```kotlin
 * set {
 *   asn1Null()
 *   printableString { "World" }
 *   printableString { "Hello" }
 *   utf8String { "!!!" }
 * }
 *  ```
 */
fun asn1Set(root: Asn1TreeBuilder.() -> Unit): Asn1Set {
    val seq = Asn1TreeBuilder()
    seq.root()
    return Asn1Set(seq.elements.sortedBy { it.tag })
}

/**
 * Creates a new SET OF as [Asn1Set]. Tags of all added elements need to be the same. Elements are sorted by encoded value
 * Use as follows:
 *
 * ```kotlin
 * setOf {
 *   printableString { "World" }
 *   printableString { "!!!" }
 *   printableString { "Hello" }
 * }
 *  ```
 */
fun asn1SetOf(root: Asn1TreeBuilder.() -> Unit): Asn1Set {
    val seq = Asn1TreeBuilder()
    seq.root()
    return Asn1Set(seq.elements)
}

/**
 * Produces an INTEGER as [Asn1Primitive]
 */
fun Int.encodeToTlv() = Asn1Primitive(INTEGER, encodeToDer())


/**
 * Produces a BOOLEAN as [Asn1Primitive]
 */
fun Boolean.encodeToTlv() = Asn1Primitive(BOOLEAN, byteArrayOf(if (this) 0xff.toByte() else 0))

/**
 * Produces an INTEGER as [Asn1Primitive]
 */
fun Long.encodeToTlv() = Asn1Primitive(INTEGER, encodeToDer())

/**
 * Produces an OCTET STRING as [Asn1Primitive]
 */
fun ByteArray.encodeToTlvOctetString() = Asn1PrimitiveOctetString(this)

/**
 * Produces a BIT STRING as [Asn1Primitive]
 */
fun ByteArray.encodeToTlvBitString() = Asn1Primitive(BIT_STRING, encodeToBitString())

/**
 * Prepends 0x00 to this ByteArray for encoding it into a BIT STRING. Useful for implicit tagging
 */
fun ByteArray.encodeToBitString() = byteArrayOf(0x00) + this

private fun String.encodeTolvOid() = Asn1Primitive(OBJECT_IDENTIFIER, decodeToByteArray(Base16()))


private fun Int.encodeToDer() = if (this == 0) byteArrayOf(0) else
    encodeToByteArray()

private fun Long.encodeToDer() = if (this == 0L) byteArrayOf(0) else
    encodeToByteArray()

/**
 * Produces a UTC TIME as [Asn1Primitive]
 */
fun Instant.encodeToAsn1UtcTime() = Asn1Primitive(UTC_TIME, encodeToAsn1Time().drop(2).encodeToByteArray())

/**
 * Produces a GENERALIZED TIME as [Asn1Primitive]
 */
fun Instant.encodeToAsn1GeneralizedTime() = Asn1Primitive(GENERALIZED_TIME, encodeToAsn1Time().encodeToByteArray())

private fun Instant.encodeToAsn1Time(): String {
    val value = this.toString()
    if (value.isEmpty())
        throw IllegalArgumentException("Instant serialization failed: no value")
    val matchResult = Regex("([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})")
        .matchAt(value, 0)
        ?: throw IllegalArgumentException("Instant serialization failed: $value")
    val year = matchResult.groups[1]?.value
        ?: throw IllegalArgumentException("Instant serialization year failed: $value")
    val month = matchResult.groups[2]?.value
        ?: throw IllegalArgumentException("Instant serialization month failed: $value")
    val day = matchResult.groups[3]?.value
        ?: throw IllegalArgumentException("Instant serialization day failed: $value")
    val hour = matchResult.groups[4]?.value
        ?: throw IllegalArgumentException("Instant serialization hour failed: $value")
    val minute = matchResult.groups[5]?.value
        ?: throw IllegalArgumentException("Instant serialization minute failed: $value")
    val seconds = matchResult.groups[6]?.value
        ?: throw IllegalArgumentException("Instant serialization seconds failed: $value")
    return "$year$month$day$hour$minute$seconds" + "Z"
}

/**
 * Encode as a four-byte array
 */
fun Int.encodeTo4Bytes(): ByteArray = byteArrayOf(
    (this ushr 24).toByte(),
    (this ushr 16).toByte(),
    (this ushr 8).toByte(),
    (this).toByte()
)

/**
 * Encode as an eight-byte array
 */
fun Long.encodeTo8Bytes(): ByteArray = byteArrayOf(
    (this ushr 56).toByte(),
    (this ushr 48).toByte(),
    (this ushr 40).toByte(),
    (this ushr 32).toByte(),
    (this ushr 24).toByte(),
    (this ushr 16).toByte(),
    (this ushr 8).toByte(),
    (this).toByte()
)

/**
 * Encodes a signed Long correctly to a compact byte array
 */
fun Long.encodeToByteArray(): ByteArray {
    //fast case
    if (this >= Byte.MIN_VALUE && this <= Byte.MAX_VALUE) return byteArrayOf(this.toByte())
    if (this >= Short.MIN_VALUE && this <= Short.MAX_VALUE) return byteArrayOf((this ushr 8).toByte(), this.toByte())
    if (this >= -(0x80 shl 16) && this < (0x80 shl 16)) return byteArrayOf(
        (this ushr 16).toByte(),
        (this ushr 8).toByte(),
        this.toByte()
    )
    if (this >= -((0x80L shl 24)) && this < (0x80L shl 24)) return byteArrayOf(
        (this ushr 24).toByte(),
        (this ushr 16).toByte(),
        (this ushr 8).toByte(),
        this.toByte()
    )
    if (this >= Int.MIN_VALUE && this <= Int.MAX_VALUE) return byteArrayOf(
        (this ushr 32).toByte(),
        (this ushr 24).toByte(),
        (this ushr 16).toByte(),
        (this ushr 8).toByte(),
        this.toByte()
    )

    if (this >= -((0x80L shl 40)) && this < (0x80L shl 40)) return byteArrayOf(
        (this ushr 40).toByte(),
        (this ushr 32).toByte(),
        (this ushr 24).toByte(),
        (this ushr 16).toByte(),
        (this ushr 8).toByte(),
        this.toByte()
    )
    if (this >= -((0x80L shl 48)) && this < (0x80L shl 48)) return byteArrayOf(
        (this ushr 48).toByte(),
        (this ushr 40).toByte(),
        (this ushr 32).toByte(),
        (this ushr 24).toByte(),
        (this ushr 16).toByte(),
        (this ushr 8).toByte(),
        this.toByte()
    )
    //-Overflow FTW!
    @Suppress("INTEGER_OVERFLOW")
    if (this >= ((0x80L shl 56)) && this < ((0x80L shl 56) - 1)) return byteArrayOf(
        (this ushr 56).toByte(),
        (this ushr 48).toByte(),
        (this ushr 40).toByte(),
        (this ushr 32).toByte(),
        (this ushr 24).toByte(),
        (this ushr 16).toByte(),
        (this ushr 8).toByte(),
        this.toByte()
    )
    return byteArrayOf(
        (this ushr 64).toByte(),
        (this ushr 56).toByte(),
        (this ushr 48).toByte(),
        (this ushr 40).toByte(),
        (this ushr 32).toByte(),
        (this ushr 24).toByte(),
        (this ushr 16).toByte(),
        (this ushr 8).toByte(),
        this.toByte()
    )
}

/**
 * Encodes a signed Long correctly to a compact byte array
 */
fun Int.encodeToByteArray(): ByteArray {
    if (this >= Byte.MIN_VALUE && this <= Byte.MAX_VALUE) return byteArrayOf(this.toByte())
    if (this >= Short.MIN_VALUE && this <= Short.MAX_VALUE) return byteArrayOf((this ushr 8).toByte(), this.toByte())
    if (this >= -(0x80 shl 16) && this < (0x80 shl 16)) return byteArrayOf(
        (this ushr 16).toByte(),
        (this ushr 8).toByte(),
        this.toByte()
    )
    //-Overflow FTW!
    @Suppress("INTEGER_OVERFLOW")
    if (this >= ((0x80 shl 24)) && this < ((0x80 shl 24) - 1)) return byteArrayOf(
        (this ushr 24).toByte(),
        (this ushr 16).toByte(),
        (this ushr 8).toByte(),
        this.toByte()
    )
    return byteArrayOf(
        (this ushr 32).toByte(),
        (this ushr 24).toByte(),
        (this ushr 16).toByte(),
        (this ushr 8).toByte(),
        this.toByte()
    )

}

/**
 * Strips the leading 0x00 byte of an ASN.1-encoded Integer,
 * that will be there if the first bit of the value is set,
 * i.e. it is over 0x7F (or < 0 if it is signed)
 */
fun ByteArray.stripLeadingSignByte() = if (this[0] == 0.toByte() && this[1] < 0) drop(1).toByteArray() else this

/**
 * The extracted values from ASN.1 may be too short
 * to be simply concatenated as raw values,
 * so we'll need to pad them with 0x00 bytes to the expected length
 */
fun ByteArray.padWithZeros(len: Int): ByteArray = if (size < len) ByteArray(len - size) { 0 } + this else this

/**
 * Drops or adds zero bytes at the start until the [size] is reached
 */
//TODO: This performs horribly!
fun ByteArray.ensureSize(size: UInt): ByteArray = when {
    this.size.toUInt() > size -> this.drop(1).toByteArray().ensureSize(size)
    this.size.toUInt() < size -> (byteArrayOf(0) + this).ensureSize(size)
    else -> this
}

