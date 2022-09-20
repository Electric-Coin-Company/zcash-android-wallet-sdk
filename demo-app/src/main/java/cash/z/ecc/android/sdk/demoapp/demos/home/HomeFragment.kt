package cash.z.ecc.android.sdk.demoapp.demos.home

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import cash.z.ecc.android.sdk.demoapp.BaseDemoFragment
import cash.z.ecc.android.sdk.demoapp.databinding.FragmentHomeBinding
import cash.z.ecc.android.sdk.demoapp.util.mainActivity
import kotlinx.coroutines.launch

/**
 * The landing page for the demo. Every time the app returns to this screen, it clears all demo
 * data just for sanity. The goal is for each demo to be self-contained so that the behavior is
 * repeatable and independent of pre-existing state.
 */
@Suppress("TooManyFunctions")
class HomeFragment : BaseDemoFragment<FragmentHomeBinding>() {

    override fun inflateBinding(layoutInflater: LayoutInflater) =
        FragmentHomeBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textSeedPhrase.setOnClickListener(::onEditSeedPhrase)
        binding.buttonPaste.setOnClickListener(::onPasteSeedPhrase)
        binding.buttonAccept.setOnClickListener(::onAcceptSeedPhrase)
        binding.buttonCancel.setOnClickListener(::onCancelSeedPhrase)
    }

    override fun onResume() {
        super.onResume()
        mainActivity()?.setClipboardListener(::updatePasteButton)

        lifecycleScope.launch {
            sharedViewModel.seedPhrase.collect {
                binding.textSeedPhrase.text = "Seed Phrase: ${it.toAbbreviatedPhrase()}"
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mainActivity()?.removeClipboardListener()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onEditSeedPhrase(unused: View) {
        setEditShown(true)
        binding.inputSeedPhrase.setText(sharedViewModel.seedPhrase.value)
        binding.textLayoutSeedPhrase.helperText = ""
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onAcceptSeedPhrase(unused: View) {
        if (applySeedPhrase()) {
            setEditShown(false)
            binding.inputSeedPhrase.setText("")
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onCancelSeedPhrase(unused: View) {
        setEditShown(false)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onPasteSeedPhrase(unused: View) {
        mainActivity()?.getClipboardText().let { clipboardText ->
            binding.inputSeedPhrase.setText(clipboardText)
            applySeedPhrase()
        }
    }

    private fun applySeedPhrase(): Boolean {
        val newPhrase = binding.inputSeedPhrase.text.toString()
        return if (!sharedViewModel.updateSeedPhrase(newPhrase)) {
            binding.textLayoutSeedPhrase.helperText = "Invalid seed phrase"
            binding.textLayoutSeedPhrase.setHelperTextColor(ColorStateList.valueOf(Color.RED))
            false
        } else {
            binding.textLayoutSeedPhrase.helperText = "valid seed phrase"
            binding.textLayoutSeedPhrase.setHelperTextColor(ColorStateList.valueOf(Color.GREEN))
            true
        }
    }

    private fun setEditShown(isShown: Boolean) {
        with(binding) {
            textSeedPhrase.visibility = if (isShown) View.GONE else View.VISIBLE
            textInstructions.visibility = if (isShown) View.GONE else View.VISIBLE
            groupEdit.visibility = if (isShown) View.VISIBLE else View.GONE
        }
    }

    private fun updatePasteButton(clipboardText: String? = mainActivity()?.getClipboardText()) {
        clipboardText.let {
            val isEditing = binding.groupEdit.visibility == View.VISIBLE
            if (isEditing && (it != null && it.split(' ').size > 2)) {
                binding.buttonPaste.visibility = View.VISIBLE
            } else {
                binding.buttonPaste.visibility = View.GONE
            }
        }
    }

    private fun String.toAbbreviatedPhrase(): String {
        this.trim().apply {
            val firstSpace = indexOf(' ')
            val lastSpace = lastIndexOf(' ')
            return if (firstSpace != -1 && lastSpace >= firstSpace) {
                "${take(firstSpace)}...${takeLast(length - 1 - lastSpace)}"
            } else {
                this
            }
        }
    }
}
