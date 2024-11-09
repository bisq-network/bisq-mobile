import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class PriceRepository {

    // Ktor client setup with ContentNegotiation plugin
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true }) // Configures JSON serialization
        }
    }

    /**
     * Fetches the current BTC price in USD from CoinGecko API.
     */
    suspend fun fetchBtcPrice(): String {
        println("fetchBtcPrice()")
        return withContext(Dispatchers.IO) {
            try {
                val response: CoinGeckoResponse = client.get("https://api.coingecko.com/api/v3/simple/price") {
                    parameter("ids", "bitcoin")
                    parameter("vs_currencies", "usd")
                }.body() // Updated call to get the deserialized response body directly
                println(response.toString())
                "$${response.bitcoin.usd}"
            } catch (e: Exception) {
                println("Error fetching BTC price: ${e.message}")
                e.printStackTrace()
                "Error fetching price"
            }
        }
    }

    fun getValue() = "$70,000"
}

@Serializable
data class CoinGeckoResponse(
    val bitcoin: BitcoinPrice
)

@Serializable
data class BitcoinPrice(
    val usd: Double
)
