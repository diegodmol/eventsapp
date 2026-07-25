package com.eventtickets.domain.model

import java.time.Instant

/**
 * Um ingresso individual emitido para um pedido APROVADO.
 * Cada unidade comprada gera um Ticket com um [qrPayload] único, evitando que
 * a mesma compra gere ingressos indistinguíveis entre si.
 *
 * O QR só é gerado a partir de um pedido com status APPROVED (ver
 * GenerateTicketsUseCase) — portanto o ingresso está sempre vinculado a uma
 * compra concluída, nunca a uma compra pendente ou negada.
 */
data class Ticket(
    val ticketId: String,
    val orderId: String,
    val eventId: String,
    val eventTitle: String,
    val sequence: Int,       // posição do ingresso dentro do pedido (1..quantity)
    val totalInOrder: Int,
    val qrPayload: String,   // conteúdo assinado/serializado codificado no QR
    val issuedAt: Instant
)
