package cash.z.ecc.android.sdk.demoapp

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.getSystemService
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.viewbinding.ViewBinding
import cash.z.ecc.android.sdk.demoapp.util.fromResources
import cash.z.ecc.android.sdk.internal.service.LightWalletGrpcService
import cash.z.ecc.android.sdk.internal.service.LightWalletService
import cash.z.ecc.android.sdk.model.LightWalletEndpoint
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.model.defaultForNetwork
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView

class MainActivity :
    AppCompatActivity(),
    ClipboardManager.OnPrimaryClipChangedListener,
    DrawerLayout.DrawerListener {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var clipboard: ClipboardManager
    private var clipboardListener: ((String?) -> Unit)? = null
    var fabListener: BaseDemoFragment<out ViewBinding>? = null

    /**
     * The service to use for all demos that interact directly with the service. Since gRPC channels
     * are expensive to recreate, we set this up once per demo. A real app would hardly ever use
     * this object because it would utilize the synchronizer, instead, which exposes APIs that
     * automatically sync with the server.
     */
    var lightwalletService: LightWalletService? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.addPrimaryClipChangedListener(this)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            onFabClicked(view)
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_address, R.id.nav_balance, R.id.nav_block, R.id.nav_private_key,
                R.id.nav_latest_height, R.id.nav_block_range,
                R.id.nav_transactions, R.id.nav_utxos, R.id.nav_send
            ),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        drawerLayout.addDrawerListener(this)

        initService()
    }

    override fun onDestroy() {
        super.onDestroy()
        lightwalletService?.shutdown()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_settings) {
            val navController = findNavController(R.id.nav_host_fragment)
            navController.navigate(R.id.nav_home)
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    //
    // Private functions
    //

    private fun initService() {
        if (lightwalletService != null) {
            lightwalletService?.shutdown()
        }
        val network = ZcashNetwork.fromResources(applicationContext)
        lightwalletService = LightWalletGrpcService.new(
            applicationContext,
            LightWalletEndpoint.defaultForNetwork(network)
        )
    }

    private fun onFabClicked(view: View) {
        fabListener?.onActionButtonClicked()
    }

    //
    // Helpers
    //

    fun getClipboardText(): String? {
        return with(clipboard) {
            if (!hasPrimaryClip()) return null
            return primaryClip!!.getItemAt(0)?.coerceToText(this@MainActivity)?.toString()
        }
    }

    override fun onPrimaryClipChanged() {
        clipboardListener?.invoke(getClipboardText())
    }

    fun setClipboardListener(block: (String?) -> Unit) {
        clipboardListener = block
        block(getClipboardText())
    }

    fun removeClipboardListener() {
        clipboardListener = null
    }

    fun hideKeyboard() {
        val windowToken = window.decorView.rootView.windowToken
        getSystemService<InputMethodManager>()?.hideSoftInputFromWindow(windowToken, 0)
    }

    /* DrawerListener implementation */

    override fun onDrawerStateChanged(newState: Int) {
    }

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
    }

    override fun onDrawerClosed(drawerView: View) {
    }

    override fun onDrawerOpened(drawerView: View) {
        hideKeyboard()
    }
}
