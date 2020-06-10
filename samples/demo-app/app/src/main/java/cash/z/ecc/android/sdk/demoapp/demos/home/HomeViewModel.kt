package cash.z.ecc.android.sdk.demoapp.demos.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "\u25c4\tSelect a demo"
    }
    val text: LiveData<String> = _text
}
