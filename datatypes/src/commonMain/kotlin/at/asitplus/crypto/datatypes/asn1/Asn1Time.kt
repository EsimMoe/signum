package at.asitplus.crypto.datatypes.asn1

import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * ASN.1 TIME (required since GENERALIZED TIME and UTC TIME exist)
 *
 * @param instant the timestamp to encode
 * @param formatOverride to force either  GENERALIZED TIME or UTC TIME
 */
@Serializable(with = CertTimeStampSerializer::class)
class Asn1Time(val instant: Instant, formatOverride: Format? = null) : Asn1Encodable<Asn1Primitive> {

    /**
     * Indicates whether this timestamp uses UTC TIME or GENERALIZED TIME
     */
    val format: Format

    init {
        format =
            formatOverride ?: if (instant > THRESHOLD_GENERALIZED_TIME) Format.GENERALIZED else Format.UTC
    }

    companion object : Asn1Decodable<Asn1Primitive, Asn1Time> {
        private val THRESHOLD_GENERALIZED_TIME = Instant.parse("2050-01-01T00:00:00Z")

        @Throws(Asn1Exception::class)
        override fun decodeFromTlv(src: Asn1Primitive) =
            Asn1Time(src.readInstant(), if (src.tag == BERTags.UTC_TIME) Format.UTC else Format.GENERALIZED)
    }

    override fun encodeToTlv(): Asn1Primitive =
        when (format) {
            Format.UTC -> instant.encodeToAsn1UtcTime()
            Format.GENERALIZED -> instant.encodeToAsn1GeneralizedTime()
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Asn1Time

        if (instant != other.instant) return false
        if (format != other.format) return false

        return true
    }

    override fun hashCode(): Int {
        var result = instant.hashCode()
        result = 31 * result + format.hashCode()
        return result
    }

    override fun toString(): String {
        return "Asn1Time(instant=$instant, format=$format)"
    }

    /**
     * Enum of supported Time formats
     */
    enum class Format {
        /**
         * UTC TIME
         */
        UTC,

        /**
         * GENERALIZED TIME
         */
        GENERALIZED
    }
}


object CertTimeStampSerializer : KSerializer<Asn1Time> {
    override val descriptor = PrimitiveSerialDescriptor("CertificateTimestamp", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder) =
        Asn1Time.decodeFromTlv(Asn1Element.decodeFromDerHexString(decoder.decodeString()) as Asn1Primitive)

    override fun serialize(encoder: Encoder, value: Asn1Time) {
        encoder.encodeString(value.encodeToTlv().toDerHexString())
    }

}