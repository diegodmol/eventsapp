package com.eventtickets.feature.payment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eventtickets.core.ui.components.OrderStatusBadge
import com.eventtickets.domain.model.OrderStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    onPaymentFinished: (orderId: String) -> Unit,
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val order = uiState.order

    Scaffold(
        topBar = { TopAppBar(title = { Text("Pagamento") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (order == null) {
                CircularProgressIndicator()
                return@Scaffold
            }

            Text(text = order.eventTitle, style = MaterialTheme.typography.titleLarge)
            Text(
                text = "${order.quantity}x ingresso(s) — R$ %.2f".format(order.totalPriceCents / 100.0),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            OrderStatusBadge(status = order.status)
            Spacer(modifier = Modifier.height(24.dp))

            when (order.status) {
                OrderStatus.CREATED, OrderStatus.ERROR -> {
                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                    Button(
                        onClick = { viewModel.pay() },
                        enabled = !uiState.isProcessingPayment,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isProcessingPayment) {
                            CircularProgressIndicator(modifier = Modifier.height(20.dp))
                        } else {
                            Text(
                                if (order.status == OrderStatus.ERROR) "Tentar pagamento novamente"
                                else "Pagar com a Cielo Smart"
                            )
                        }
                    }
                }
                OrderStatus.PROCESSING -> {
                    CircularProgressIndicator()
                    Text(
                        text = "Aguardando confirmação no terminal Cielo Smart…",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                OrderStatus.APPROVED, OrderStatus.DENIED, OrderStatus.CANCELED -> {
                    val message = when (order.status) {
                        OrderStatus.APPROVED -> "Pagamento aprovado!"
                        OrderStatus.DENIED -> "Pagamento negado."
                        OrderStatus.CANCELED -> "Pagamento cancelado."
                        else -> ""
                    }
                    Text(text = message, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onPaymentFinished(order.orderId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ver comprovante")
                    }
                }
            }
        }
    }
}
