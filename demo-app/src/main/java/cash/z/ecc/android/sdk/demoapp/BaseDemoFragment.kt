package cash.z.ecc.android.sdk.demoapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewbinding.ViewBinding
import cash.z.ecc.android.sdk.demoapp.util.mainActivity
import cash.z.ecc.android.sdk.ext.TroubleshootingTwig
import cash.z.ecc.android.sdk.ext.Twig
import com.google.android.material.snackbar.Snackbar

abstract class BaseDemoFragment<T : ViewBinding> : Fragment() {

    /**
     * Since the lightwalletservice is not a component that apps typically use, directly, we provide
     * this from one place. Everything that can be done with the service can/should be done with the
     * synchronizer because it wraps the service.
     */
    val lightwalletService get() = mainActivity()?.lightwalletService
    
    // contains view information provided by the user
    val sharedViewModel: SharedViewModel by activityViewModels()
    lateinit var binding: T
    
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
    }

    override fun onPause() {
        super.onPause()
        unregisterActionButtonListener()
        (activity as? MainActivity)?.hideKeyboard()
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
        Snackbar.make(requireView(), "Replace with your own action", Snackbar.LENGTH_LONG)
            .setAction("Action") { /* auto-close */ }.show()
    }

    /**
     * Convenience function to the given text to the clipboard.
     */
    open fun copyToClipboard(text: String, description: String = "Copied to clipboard!") {
        (activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)?.let { cm ->
            cm.setPrimaryClip(ClipData.newPlainText("DemoAppClip", text))
        }
        toast(description)
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
}
