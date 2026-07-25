package com.eventtickets.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.eventtickets.core.ui.theme.ErrorRed
import com.eventtickets.core.ui.theme.NeutralGray
import com.eventtickets.core.ui.theme.SuccessGreen
import com.eventtickets.core.ui.theme.WarningAmber
import com.eventtickets.domain.model.OrderStatus

/**
 * Badge visual reutilizado em Checkout, Payment e Receipt para exibir o
 * status atual do pedido de forma consistente em todo o app.
 */
@Composable
fun OrderStatusBadge(status: OrderStatus, modifier: Modifier = Modifier) {
    val (label, color) = status.toLabelAndColor()
    Text(
        text = label,
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

private fun OrderStatus.toLabelAndColor(): Pair<String, Color> = when (this) {
    OrderStatus.CREATED -> "Aguardando pagamento" to NeutralGray
    OrderStatus.PROCESSING -> "Processando" to WarningAmber
    OrderStatus.APPROVED -> "Aprovado" to SuccessGreen
    OrderStatus.DENIED -> "Negado" to ErrorRed
    OrderStatus.CANCELED -> "Cancelado" to NeutralGray
    OrderStatus.ERROR -> "Erro" to ErrorRed
}
