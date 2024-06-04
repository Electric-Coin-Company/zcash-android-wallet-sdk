package cash.z.ecc.android.sdk.demoapp.ui.screen.transactions.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.z.ecc.android.sdk.demoapp.R

@Composable
internal fun Transaction(state: TransactionState) {
    Column(
        modifier = Modifier
            .background(Color.White)
            .clickable(onClick = state.onClick)
            .padding(16.dp),
        verticalArrangement = spacedBy(8.dp)
    ) {
        TransactionRow(
            title = stringResource(R.string.transaction_time),
            value = state.time
        )
        TransactionRow(
            title = stringResource(R.string.transaction_value),
            value = state.value
        )
        TransactionRow(
            title = stringResource(R.string.transaction_fee),
            value = state.fee
        )
        TransactionRow(
            title = stringResource(R.string.transaction_status),
            value = state.status
        )
    }
}

@Composable
private fun TransactionRow(title: String, value: String) {
    Row {
        Text(
            color = Color.Gray,
            modifier = Modifier.weight(.35f),
            text = title
        )
        Text(
            modifier = Modifier.weight(.65f),
            fontWeight = FontWeight.Bold,
            text = value
        )
    }
}

@Immutable
data class TransactionState(
    val time: String,
    val value: String,
    val fee: String,
    val status: String,
    val onClick: () -> Unit
)

@Preview
@Composable
private fun TransactionPreview() = MaterialTheme {
    Transaction(
        state = TransactionState(
            time = "12:00",
            value = "1000",
            fee = "100",
            status = "success",
            onClick = {},
        )
    )
}
