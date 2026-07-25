package com.eventtickets.data.remote.cielo
/**
 * Resultado decodificado da resposta da Cielo Smart. Parseado de forma
 * flexível (via JsonObject, em CieloDeeplinkParser) porque a doc mostra
 * campos com tipos inconsistentes entre exemplos (ex.: "amount" ora
 * número, ora string).
 */
sealed class CieloDeeplinkOutcome {
    data class Success(
        val orderIdCielo: String?,
        val reference: String?,
        val status: String?,
        val transactionId: String?,
        val authCode: String?,
        val nsu: String?,
        val brand: String?,
        val amountCents: Long?
    ) : CieloDeeplinkOutcome()

    data class Failure(
        val code: Int,
        val reason: String,
        val isCancellation: Boolean
    ) : CieloDeeplinkOutcome()

    data class IntegrationError(val message: String) : CieloDeeplinkOutcome()
}