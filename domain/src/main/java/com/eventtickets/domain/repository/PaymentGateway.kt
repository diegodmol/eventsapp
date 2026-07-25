package com.eventtickets.domain.repository

import com.eventtickets.core.common.AppResult
import com.eventtickets.domain.model.PaymentResult

/**
 * Porta (interface) que abstrai a integração com o ecossistema Cielo Smart.
 *
 * A implementação real (em :data, pacote remote/cielo) conversa com o SDK/
 * intent da Cielo Smart no terminal Android (LIO). Manter essa abstração no
 * domain permite:
 *  - Testar toda a lógica de negócio (use cases) com um FakeCieloPaymentGateway,
 *    sem precisar do terminal físico/emulador Cielo Smart.
 *  - Trocar a versão do SDK Cielo ou a estratégia de integração (Intent vs SDK
 *    vs biblioteca .aar) sem tocar em regras de negócio.
 */
interface PaymentGateway {

    /**
     * Inicia uma transação de pagamento no terminal Cielo Smart.
     *
     * @param orderId identificador do pedido local, propagado como referência
     *        externa para a transação Cielo (campo orderId/referenceLabel do
     *        SDK), permitindo conciliação posterior.
     * @param idempotencyKey chave estável por pedido; implementações devem
     *        recusar (AppError.DuplicatePaymentAttempt) uma nova chamada com a
     *        mesma chave se já houver uma transação em andamento/concluída,
     *        evitando cobrança duplicada em reenvio de ação.
     * @param amountCents valor total em centavos.
     */
    suspend fun startPayment(
        orderId: String,
        idempotencyKey: String,
        amountCents: Long
    ): AppResult<PaymentResult>

    /** Consulta o status de uma transação já iniciada (reconciliação após crash/timeout). */
    suspend fun queryPaymentStatus(idempotencyKey: String): AppResult<PaymentResult?>

    /** Solicita o cancelamento/estorno de uma transação, quando aplicável. */
    suspend fun cancelPayment(transactionId: String): AppResult<Unit>
}
