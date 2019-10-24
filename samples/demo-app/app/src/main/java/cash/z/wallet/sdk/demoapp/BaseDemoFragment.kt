package cash.z.wallet.sdk.demoapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import cash.z.wallet.sdk.ext.TroubleshootingTwig
import cash.z.wallet.sdk.ext.Twig
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
        onClear()
    }

    /**
     * Inflate the ViewBinding. Unfortunately, the `inflate` function is not part of the ViewBinding
     * interface so the base class cannot take care of this behavior without some help.
     */
    abstract fun inflateBinding(layoutInflater: LayoutInflater): T
    abstract fun resetInBackground()
    abstract fun onResetComplete()
    abstract fun onClear()
}