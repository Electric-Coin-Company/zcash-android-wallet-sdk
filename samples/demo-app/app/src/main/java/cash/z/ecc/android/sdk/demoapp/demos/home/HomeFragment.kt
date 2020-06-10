package cash.z.ecc.android.sdk.demoapp.demos.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import cash.z.ecc.android.sdk.demoapp.App
import cash.z.ecc.android.sdk.demoapp.R
import cash.z.ecc.android.sdk.ext.twig

/**
 * The landing page for the demo. Every time the app returns to this screen, it clears all demo
 * data just for sanity. The goal is for each demo to be self-contained so that the behavior is
 * repeatable and independent of pre-existing state.
 */
class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProviders.of(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        val textView: TextView = root.findViewById(R.id.text_home)
        homeViewModel.text.observe(viewLifecycleOwner, Observer {
            textView.text = it
        })
        return root
    }

    override fun onResume() {
        super.onResume()
        twig("CLEARING DATA: Visiting the home screen clears the default databases, for sanity" +
                    " sake, because each demo is intended to be self-contained.")
        App.instance.getDatabasePath("unusued.db").parentFile.listFiles().forEach { it.delete() }
    }
}
