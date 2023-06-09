package dp.datadeling.utils

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.util.*
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar


class LocalDateSerializer : StdSerializer<LocalDate>(LocalDate::class.java) {

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: LocalDate, gen: JsonGenerator, sp: SerializerProvider) {
        gen.writeString(value.format(ISO_LOCAL_DATE))
    }
}

class LocalDateDeserializer : StdDeserializer<LocalDate>(LocalDate::class.java) {

    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): LocalDate {
        return LocalDate.parse(jp.readValueAs(String::class.java))
    }
}

class LocalDateTimeSerializer : StdSerializer<LocalDateTime>(LocalDateTime::class.java) {

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: LocalDateTime, gen: JsonGenerator, sp: SerializerProvider) {
        gen.writeString(value.format(ISO_LOCAL_DATE_TIME))
    }
}

class LocalDateTimeDeserializer : StdDeserializer<LocalDateTime>(LocalDateTime::class.java) {

    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): LocalDateTime {
        return LocalDateTime.parse(jp.readValueAs(String::class.java))
    }
}

fun LocalDate.toXMLDate(): XMLGregorianCalendar {
    return DatatypeFactory.newInstance().newXMLGregorianCalendar(
        GregorianCalendar.from(
            this.atStartOfDay(ZoneId.systemDefault())
        )
    )
}
