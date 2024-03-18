@file:Suppress("UNCHECKED_CAST")

package at.asitplus.crypto.datatypes.asn1

import at.asitplus.KmmResult
import at.asitplus.KmmResult.Companion.wrap
import at.asitplus.crypto.datatypes.asn1.DERTags.toImplicitTag

/**
 * Interface providing methods to encode to ASN.1
 */
interface Asn1Encodable<A : Asn1Element> {

    /**
     * Encodes the implementing object into an [A]
     * @throws Asn1Exception in case an illegal ASN.1 Object was to be constructed
     */
    @Throws(Asn1Exception::class)
    fun encodeToTlv(): A

    /**
     * Exception-free version of [encodeToTlv]
     */
    fun encodeToTlvOrNull() = runCatching { encodeToTlv() }.getOrNull()

    /**
     * Safe version of [encodeToTlv], wrapping the result into a [KmmResult]
     */
    fun encodeToTlvSafe() = kotlin.runCatching { encodeToTlv() }.wrap()

    /**
     * Convenience function to directly get the DER-encoded representation of the implementing object
     */
    @Throws(Asn1Exception::class)
    fun encodeToDer() = encodeToTlv().derEncoded

    /**
     * Exception-free version of [encodeToDer]
     */
    fun encodeToDerOrNull() = runCatching { encodeToDer() }.getOrNull()

    /**
     * Safe version of [encodeToDer], wrapping the result into a [KmmResult]
     */
    fun encodeToDerSafe() = kotlin.runCatching { encodeToDer() }.wrap()
}

/**
 * Interface providing convenience methods to decode from ASN.1.
 * Especially useful when companion objects of classes implementing [Asn1Encodable] implement it.
 */
interface Asn1Decodable<A : Asn1Element, T : Asn1Encodable<A>> {
    /**
     * Processes an [A], parsing it into an instance of [T]
     * @throws Asn1Exception if invalid data is provided
     */
    @Throws(Asn1Exception::class)
    fun decodeFromTlv(src: A): T

    /**
     * Exception-free version of [decodeFromTlv]
     */
    fun decodeFromTlvOrNull(src: A) = runCatching { decodeFromTlv(src) }.getOrNull()

    /**
     * Safe version of [decodeFromTlv], wrapping the result into a [KmmResult]
     */
    fun decodeFromTlvSafe(src: A) = kotlin.runCatching { decodeFromTlv(src) }.wrap()

    /**
     * Convenience method, directly DER-decoding a byte array to [T]
     * @throws Asn1Exception if invalid data is provided
     */
    @Throws(Asn1Exception::class)
    fun decodeFromDer(src: ByteArray): T = decodeFromTlv(Asn1Element.parse(src) as A)

    /**
     * Exception-free version of [decodeFromDer]
     */
    fun decodeFromDerOrNull(src: ByteArray) = runCatching { decodeFromDer(src) }.getOrNull()

    /**
     * Safe version of [decodeFromDer], wrapping the result into a [KmmResult]
     */
    fun decodeFromDerSafe(src: ByteArray) = runCatching { decodeFromDer(src) }.wrap()
}

interface Asn1TagVerifyingDecodable<T : Asn1Encodable<Asn1Primitive>> :
    Asn1Decodable<Asn1Primitive, T> {

    /**
     * Same as [Asn1Decodable.decodeFromTlv], but allows overriding the tag, should the implementing class verify it.
     * Useful for implicit tagging, in which case you will want to call [at.asitplus.crypto.datatypes.asn1.DERTags.toImplicitTag] on [tagOverride].
     * @throws Asn1Exception
     */
    @Throws(Asn1Exception::class)
    fun decodeFromTlv(src: Asn1Primitive, tagOverride: UByte?): T

    /**
     * Exception-free version of [decodeFromTlv]
     */
    fun decodeFromTlvOrNull(src: Asn1Primitive, tagOverride: UByte?) =
        runCatching { decodeFromTlv(src, tagOverride) }.getOrNull()

    /**
     * Safe version of [decodeFromTlv], wrapping the result into a [KmmResult]
     */
    fun decodeFromTlvSafe(src: Asn1Primitive, tagOverride: UByte?) =
        kotlin.runCatching { decodeFromTlv(src, tagOverride) }.wrap()


    /**
     * Same as [Asn1Decodable.decodeFromDer], but allows overriding the tag, should the implementing class verify it.
     * Useful for implicit tagging.
     */
    @Throws(Asn1Exception::class)
    fun decodeFromDer(src: ByteArray, tagOverride: UByte?): T =
        decodeFromTlv(Asn1Element.parse(src) as Asn1Primitive, tagOverride)

    /**
     * Exception-free version of [decodeFromDer]
     */
    fun decodeFromDerOrNull(src: ByteArray, tagOverride: UByte?) =
        runCatching { decodeFromDer(src, tagOverride) }.getOrNull()

    /**
     * Safe version of [decodeFromDer], wrapping the result into a [KmmResult]
     */
    fun decodeFromDerSafe(src: ByteArray, tagOverride: UByte?)= runCatching { decodeFromDer(src, tagOverride) }.wrap()
}
