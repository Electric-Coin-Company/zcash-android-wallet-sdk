package cash.z.wallet.sdk.demoapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import cash.z.wallet.sdk.ext.TroubleshootingTwig
import cash.z.wallet.sdk.ext.Twig
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

abstract class BaseDemoFragment<T : ViewBinding> : Fragment() {

    lateinit var binding: T

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Twig.plant(TroubleshootingTwig())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = inflateBinding(layoutInflater)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        registerActionButtonListener()
        // just a quick way of enforcing the following for each demo:
        //  - wait until the fragment is created, then run `initInBackground` on a background thread
        //  - wait until init is finished
        //  - and then run `onInitComplete` on the Main thread
        //     - but only once the fragment is resumed
        //     - and if the fragment/activity/app is stopped during any of this, exit cleanly
        //
        // Why?
        // Because this is a demo. In a full-blown app, there would be other appropriate times to
        // load things. Also, we want each demo to stand alone. It is abnormal for a fragment to
        // always recreate application state but that's the behavior we want for the demos.
        // So we use this approach to coordinate two sets of logic and ensure they run sequentially
        // while also respecting the lifecycle. If we didn't do this, the `initInBackground` would
        // freeze the UI while the fragment is created and `onInitComplete` would have no way of
        // knowing when the background thread work is done and thereby could run in the wrong order.
        lifecycleScope.launchWhenCreated {
            withContext(IO) {
                resetInBackground()
                lifecycleScope.launchWhenResumed {
                    onResetComplete()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterActionButtonListener()
        onClear()
    }

    private fun registerActionButtonListener() {
        (activity as? MainActivity)?.fabListener = this
    }

    private fun unregisterActionButtonListener() {
        (activity as? MainActivity)?.apply {
            if (fabListener === this@BaseDemoFragment) fabListener = null
        }
    }

    /**
     * Callback to run whenever the fragment is paused. The intention is to clear out each demo
     * cleanly so that they are always a repeatable experience.
     */
    open fun onClear() {}

    /**
     * Callback that gets invoked on the visible fragment whenever the floating action button is
     * tapped. This provides a convenient placeholder for the developer to extend the
     * behavior for a demo, for instance by copying the address to the clipboard, whenever the FAB
     * is tapped on the address screen.
     */
    open fun onActionButtonClicked() {
        // Show a message so that it's easy for developers to find how to replace this behavior for
        // each fragment. Simply override this [onActionButtonClicked] callback to add behavior to a
        // demo. In other words, this function probably doesn't need to change because desired
        // behavior should go in the child fragment, which overrides this.
        Snackbar.make(view!!, "Replace with your own action", Snackbar.LENGTH_LONG)
            .setAction("Action") { /* auto-close */ }.show()
    }

    /**
     * Convenience function to the given text to the clipboard.
     */
    open fun copyToClipboard(text: String) {
        (activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)?.let { cm ->
            cm.setPrimaryClip(ClipData.newPlainText("DemoAppClip", text))
        }
        toast("Copied to clipboard!")
    }

    /**
     * Convenience function to show a toast in the main activity.
     */
    fun toast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Inflate the ViewBinding. Unfortunately, the `inflate` function is not part of the ViewBinding
     * interface so the base class cannot take care of this behavior without some help.
     */
    abstract fun inflateBinding(layoutInflater: LayoutInflater): T
    abstract fun resetInBackground()
    abstract fun onResetComplete()
}