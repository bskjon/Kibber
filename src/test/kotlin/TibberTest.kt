import kotlinx.coroutines.*
import no.iktdev.kibber.Tibber
import no.iktdev.kibber.TibberListener
import no.iktdev.tibber.TibberPulseSubscription
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TibberTest {
    val token =  System.getenv("tibberTestToken")
    val scope = CoroutineScope(Job() + Dispatchers.IO)
    val tibber = Tibber(token, "test")

    @BeforeEach
    fun closeOpen() {
        tibber.close()
        tibber.registeredLiveListener = null
    }

    @Test
    @DisplayName("Can get homes")
    fun homesQuery() = runBlocking {
        val homes = tibber.homes()
        assertTrue { homes.isNotEmpty() }
    }

    @Test
    @DisplayName("Can get homes with live reading")
    fun homesWithLiveReaderQuery() = runBlocking {
        val homes = tibber.homesWithLiveReader()
        assertTrue { homes.isNotEmpty() }
    }

    @Test
    @DisplayName("Can read live value")
    fun readLiveValueFromHome() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + Job())
        val events = object: TibberListener {
            var _measurement: TibberPulseSubscription.LiveMeasurement? = null
                private set
            override fun onMeasurementReceived(measurement: TibberPulseSubscription.LiveMeasurement) {
                launch(Dispatchers.Default) {
                    _measurement = measurement
                }
            }

            override fun onInvalidMeasurementReceived() {

            }
        }
        val first = tibber.homesWithLiveReader().first()
        scope.launch {
            println("Calling read live")
            tibber.registeredLiveListener = events
            tibber.readLiveWattage(first.id)
        }
        val endTime = System.currentTimeMillis() + 15000
        while (System.currentTimeMillis() < endTime && events._measurement == null) {
            Thread.sleep(100)
        }
        withContext(Dispatchers.IO) {
            tibber.close()
        }
        assertNotNull(events._measurement)
        assertTrue ( (events._measurement?.power ?: -1) != -1 )
        println(events._measurement)
    }

}