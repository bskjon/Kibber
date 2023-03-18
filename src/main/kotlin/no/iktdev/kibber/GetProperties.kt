package no.iktdev.kibber

import java.util.*

class GetProperties {
    fun version(): String {
        val properties = Properties()
        properties.load(javaClass.classLoader.getResourceAsStream("version.properties"))
        return properties.getProperty("version")
    }
}