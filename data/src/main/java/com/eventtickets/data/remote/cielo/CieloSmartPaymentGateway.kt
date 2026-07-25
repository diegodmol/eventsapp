package com.eventtickets.data.remote.cielo

import android.content.Context
import android.content.Intent
import com.eventtickets.core.common.AppError
import com.eventtickets.core.common.AppResult
import com.eventtickets.data.BuildConfig
import com.eventtickets.domain.model.PaymentMethod
import com.eventtickets.domain.model.PaymentResult
import com.eventtickets.domain.model.PaymentStatus
import com.eventtickets.domain.repository.PaymentGateway
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CieloSmartPaymentGateway @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bridge: CieloPaymentBridge
) : PaymentGateway {

    override suspend fun startPayment(
        orderId: String,
        idempotencyKey: String,
        amountCents: Long
    ): AppResult<PaymentResult> {
        val request = CieloDeeplinkPaymentRequest(
            accessToken = BuildConfig.CIELO_ACCESS_TOKEN,
            clientID = BuildConfig.CIELO_CLIENT_ID,
            reference = orderId,
            installments = 0,
            items = listOf(
                CieloDeeplinkItem(
                    name = "Ingresso(s) - evento",
                    quantity = 1,
                    sku = orderId,
                    unitOfMeasure = "unidade",
                    unitPrice = amountCents
                )
            ),
            value = amountCents.toString()
        )
        val uri = CieloDeeplinkParser.buildRequestUri(request)

        if (context.packageManager.resolveActivity(Intent(Intent.ACTION_VIEW, uri), 0) == null) {
            return AppResult.Failure(
                AppError.CieloSdkNotAvailable(
                    "Nenhum app compatível com o esquema lio:// foi encontrado. " +
                            "Instale o emulador/app Cielo Smart."
                )
            )
        }

        return try {
            val outcome = withTimeout(PAYMENT_TIMEOUT_MILLIS) { bridge.launchPayment(uri) }
            when (outcome) {
                is CieloDeeplinkOutcome.Success -> AppResult.Success(outcome.toPaymentResult(amountCents))
                is CieloDeeplinkOutcome.Failure -> AppResult.Success(outcome.toPaymentResult(amountCents))
                is CieloDeeplinkOutcome.IntegrationError -> AppResult.Failure(
                    AppError.CieloIntegrationError(null, outcome.message)
                )
            }
        } catch (e: TimeoutCancellationException) {
            AppResult.Failure(AppError.Timeout("pagamento Cielo Smart (orderId=$orderId)"))
        }
    }

    override suspend fun queryPaymentStatus(idempotencyKey: String): AppResult<PaymentResult?> =
        AppResult.Success(null)

    override suspend fun cancelPayment(transactionId: String): AppResult<Unit> =
        AppResult.Success(Unit) // fluxo de cancelamento via deeplink fica para uma próxima etapa

    companion object {
        private const val PAYMENT_TIMEOUT_MILLIS = 120_000L
    }
}

private fun CieloDeeplinkOutcome.Success.toPaymentResult(amountCents: Long): PaymentResult = PaymentResult(
    transactionId = transactionId,
    nsu = nsu,
    authorizationCode = authCode,
    status = PaymentStatus.APPROVED,
    paymentMethod = PaymentMethod.UNKNOWN, // refinar depois de ver o campo real no sandbox
    brand = brand,
    amountCents = amountCents,
    respondedAt = Instant.now()
)

private fun CieloDeeplinkOutcome.Failure.toPaymentResult(amountCents: Long): PaymentResult = PaymentResult(
    transactionId = null,
    nsu = null,
    authorizationCode = null,
    status = if (isCancellation) PaymentStatus.CANCELED else PaymentStatus.DENIED,
    paymentMethod = null,
    brand = null,
    amountCents = amountCents,
    errorCode = code.toString(),
    errorMessage = reason,
    respondedAt = Instant.now()
)