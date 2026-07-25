package com.eventtickets.feature.checkout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eventtickets.core.ui.components.ErrorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onOrderCreated: (orderId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.createdOrderId) {
        uiState.createdOrderId?.let { onOrderCreated(it) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Confirmar ingressos") }) }
    ) { padding ->
        val event = uiState.event
        if (event == null && uiState.errorMessage != null) {
            ErrorState(
                message = uiState.errorMessage!!,
                onRetry = null,
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }
        if (event == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Text(text = event.title, style = MaterialTheme.typography.titleLarge)
            Text(text = event.venue, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Quantidade de ingressos", style = MaterialTheme.typography.bodyLarge)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.decrementQuantity() },
                    enabled = uiState.canDecrement
                ) { Text("-") }

                Text(
                    text = uiState.quantity.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                OutlinedButton(
                    onClick = { viewModel.incrementQuantity() },
                    enabled = uiState.canIncrement
                ) { Text("+") }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Total: R$ %.2f".format(uiState.totalPriceCents / 100.0),
                style = MaterialTheme.typography.titleLarge
            )

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { viewModel.confirmSelection() },
                enabled = !uiState.isCreatingOrder,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isCreatingOrder) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                } else {
                    Text("Ir para pagamento")
                }
            }
        }
    }
}
