package cash.z.wallet.sdk.sample.memo

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import cash.z.wallet.sdk.data.*
import cash.z.wallet.sdk.ext.convertZatoshiToZec
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch

class MainActivity : ScopedActivity() {

    private lateinit var synchronizer: Synchronizer
    private var progressJob: Job? = null
    private var activeTransaction: TransactionInfo = TransactionInfo()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Turn on simple logging for debug builds
        Twig.plant(TroubleshootingTwig())

        synchronizer = Injection.provideSynchronizer(this.applicationContext)
        synchronizer.start(this)
    }


    override fun onResume() {
        super.onResume()
        progressJob = launchProgressMonitor(synchronizer.progress())
    }

    override fun onPause() {
        super.onPause()
        progressJob?.cancel()
        progressJob = null
    }

    override fun onDestroy() {
        super.onDestroy()
        synchronizer.stop()
    }

    private fun CoroutineScope.launchProgressMonitor(channel: ReceiveChannel<Int>) = launch {
        for (i in channel) {
            onProgress(i)
        }
    }

    private fun CoroutineScope.launchTransactionMonitor(channel: ReceiveChannel<Map<ActiveTransaction, TransactionState>>) = launch {
        for (i in channel) {
            onUpdate(i)
        }
    }

    private fun onProgress(progress: Int) {
        twig("Launching - onProgress $progress")
        val isComplete = progress == 100
        text_status.text = if(isComplete) "Balance: ${synchronizer.getAvailableBalance().convertZatoshiToZec(3)} TAZ" else "Synchronizing...\t$progress%"
        button_send.isEnabled = isComplete
    }

    private fun onUpdate(transactions: Map<ActiveTransaction, TransactionState>) {
        if (transactions.isNotEmpty()) {
            // primary is the last one that was inserted
            val primaryEntry =
                    transactions.entries.toTypedArray()[transactions.size - 1]
            updatePrimaryTransaction(primaryEntry.key, primaryEntry.value)
        }
    }

    private fun updatePrimaryTransaction(transaction: ActiveTransaction,
                                         transactionState: TransactionState) {
        val status = transactionState.toString()
        text_status.text = "Memo Sent!\nAwaiting confirmation...\nstatus: $status"
    }

    fun onSendMemo(v: View) {
        launchTransactionMonitor(synchronizer.activeTransactions())

        activeTransaction = TransactionInfo(memo = input_memo.text.toString())

        launch {
            synchronizer.sendToAddress(
                activeTransaction.amount,
                activeTransaction.toAddress,
                activeTransaction.memo
            )
        }

        button_send.isEnabled = false
        text_status.text = "Memo Sent!\nAwaiting confirmation..."
        input_memo.text = null
        input_memo.clearFocus()
        hideKeyboard()
    }

    private fun hideKeyboard() {
        getSystemService<InputMethodManager>()
                ?.hideSoftInputFromWindow(window.decorView.rootView.windowToken,
                        InputMethodManager.HIDE_NOT_ALWAYS)
    }

    data class TransactionInfo(
        val amount: Long = 20_000L,
        // reference wallet : Carol
        val toAddress: String = "ztestsapling1efxqj5256ywqdc3zntfa0hw6yn4f83k2h7fgngwmxr3h3w7zyd" +
                "lencvh30730ez6p8fwg56htgz",
        val memo: String = "sample memo"
    )
}
