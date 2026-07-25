package com.eventtickets.data.remote.cielo

import com.eventtickets.core.common.AppResult
import com.eventtickets.domain.model.PaymentMethod
import com.eventtickets.domain.model.PaymentResult
import com.eventtickets.domain.model.PaymentStatus
import com.eventtickets.domain.repository.PaymentGateway
import kotlinx.coroutines.delay
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Implementação de [PaymentGateway] para build de DEBUG/demonstração, usada
 * quando o emulador Cielo Smart não está disponível no ambiente de teste
 * (ex.: avaliação sem o terminal físico/emulador instalado).
 *
 * Simula latência real de rede/terminal e distribui resultados
 * (aprovado/negado/erro) de forma determinística por valor, para facilitar
 * testes manuais e demonstração de todos os fluxos exigidos:
 *  - Valor termina em ".00" centavos múltiplo de 100 -> aprovado
 *  - amountCents % 777 == 0 -> negado (simula recusa do emissor)
 *  - amountCents % 1313 == 0 -> erro de integração simulado
 *
 * Trocar [FakeCieloPaymentGateway] por [CieloSmartPaymentGateway] é feito em
 * um único ponto: o módulo Hilt de binding (ver di/PaymentModule.kt),
 * tipicamente selecionado por build variant (debug vs release) ou por
 * BuildConfig flag.
 */
@Singleton
class FakeCieloPaymentGateway @Inject constructor() : PaymentGateway {

    override suspend fun startPayment(
        orderId: String,
        idempotencyKey: String,
        amountCents: Long
    ): AppResult<PaymentResult> {
        delay(1800) // simula tempo de interação no terminal (inserir cartão/senha)

        return when {
            amountCents % 1313L == 0L -> AppResult.Failure(
                com.eventtickets.core.common.AppError.CieloIntegrationError(
                    cieloCode = "TERMINAL_BUSY",
                    details = "Terminal ocupado (simulado pelo FakeCieloPaymentGateway)"
                )
            )
            amountCents % 777L == 0L -> AppResult.Success(
                PaymentResult(
                    transactionId = "fake-tx-${UUID.randomUUID()}",
                    nsu = (100000..999999).random().toString(),
                    authorizationCode = null,
                    status = PaymentStatus.DENIED,
                    paymentMethod = PaymentMethod.CREDIT,
                    brand = "MASTERCARD",
                    amountCents = amountCents,
                    errorCode = "51",
                    errorMessage = "Saldo insuficiente (simulado)",
                    respondedAt = Instant.now()
                )
            )
            else -> AppResult.Success(
                PaymentResult(
                    transactionId = "fake-tx-${UUID.randomUUID()}",
                    nsu = (100000..999999).random().toString(),
                    authorizationCode = (100000..999999).random().toString(),
                    status = PaymentStatus.APPROVED,
                    paymentMethod = PaymentMethod.CREDIT,
                    brand = "VISA",
                    amountCents = amountCents,
                    respondedAt = Instant.now()
                )
            )
        }
    }

    override suspend fun queryPaymentStatus(idempotencyKey: String): AppResult<PaymentResult?> =
        AppResult.Success(null)

    override suspend fun cancelPayment(transactionId: String): AppResult<Unit> =
        AppResult.Success(Unit)
}
