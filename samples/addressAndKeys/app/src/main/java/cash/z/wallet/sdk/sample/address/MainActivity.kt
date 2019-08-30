package cash.z.wallet.sdk.sample.address

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cash.z.wallet.sdk.data.TroubleshootingTwig
import cash.z.wallet.sdk.data.Twig
import cash.z.wallet.sdk.ext.SampleSeedProvider
import cash.z.wallet.sdk.secure.Wallet
import kotlin.properties.Delegates

/**
 * Sample app that shows how to access the address and spending key.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var wallet: Wallet
    private lateinit var addressInfo: TextView

    // Secure storage is out of scope for this example (wallet makers know how to securely store things)
    // However, any class can implement the required interface for these dependencies. The expectation is that a wallet
    // maker would wrap an existing class with something that implements the property interface to access data. These
    // dependencies would then point to those wrappers.
    private val mockSecureStorage = Delegates.notNull<String>()
    private val mockSecureSeedProvider = SampleSeedProvider("testreferencealice")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Twig.plant(TroubleshootingTwig())

        addressInfo = findViewById(R.id.text_address_info)

        wallet = Injection.provideWallet(mockSecureSeedProvider, mockSecureStorage).also { it.initialize() }
    }

    override fun onResume() {
        super.onResume()
        addressInfo.text = createAddressInfo()
    }

    private fun createAddressInfo(): String {
        val address = wallet.getAddress()
        val key by mockSecureStorage
        val info = """
                seed:
                ${mockSecureSeedProvider.seedValue}
                --------------------------------------
                address:
                $address
                --------------------------------------
                spendingKey:
                $key
            """.trimIndent()
        return info
    }

    fun onTestThings(view: View) {
        // This is a good place to insert some test behavior to try out the SDK in response to a click
        // it may help to add objects to the Injection.kt file
        Toast.makeText(this, "Test SDK behavior", Toast.LENGTH_SHORT).show()
    }

}
