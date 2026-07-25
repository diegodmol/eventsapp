package com.eventtickets.feature.receipt

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eventtickets.core.ui.components.OrderStatusBadge
import com.eventtickets.domain.model.OrderStatus
import com.eventtickets.domain.model.Ticket
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    onBackToEvents: () -> Unit,
    viewModel: ReceiptViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val order = uiState.order

    Scaffold(
        topBar = { TopAppBar(title = { Text("Comprovante da compra") }) }
    ) { padding ->
        if (order == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = order.eventTitle, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        OrderStatusBadge(status = order.status)
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))
                        ReceiptRow("Quantidade", "${order.quantity} ingresso(s)")
                        ReceiptRow("Valor total", "R$ %.2f".format(order.totalPriceCents / 100.0))
                        order.payment?.let { payment ->
                            ReceiptRow("Transação", payment.transactionId ?: "—")
                            ReceiptRow("NSU", payment.nsu ?: "—")
                            payment.authorizationCode?.let {
                                ReceiptRow("Autorização", it)
                            }
                            payment.brand?.let {
                                ReceiptRow("Bandeira", it)
                            }
                            payment.errorMessage?.let { ReceiptRow("Motivo", it) }
                        }
                        ReceiptRow("Data", order.updatedAt.toString())
                    }
                }
            }

            if (order.status == OrderStatus.APPROVED) {
                if (uiState.isGeneratingTickets && uiState.tickets.isEmpty()) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Gerando ingressos…", modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }

                items(uiState.tickets, key = { it.ticketId }) { ticket ->
                    TicketQrCard(ticket)
                }
            }

            item {
                androidx.compose.material3.Button(
                    onClick = onBackToEvents,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Voltar para eventos")
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun TicketQrCard(ticket: Ticket) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ingresso ${ticket.sequence}/${ticket.totalInOrder}",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            val qrBitmap = remember(ticket.qrPayload) {
                QrCodeGenerator.generate(ticket.qrPayload, sizePx = 480)
            }
            Image(
                bitmap = qrBitmap.asImageBitmap(),
                contentDescription = "QR Code do ingresso ${ticket.sequence}",
                modifier = Modifier.size(220.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = ticket.ticketId,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
