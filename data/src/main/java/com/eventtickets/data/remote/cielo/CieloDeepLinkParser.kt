package com.eventtickets.data.remote.cielo

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Serialização/desserialização do contrato de deeplink com a Cielo Smart:
 * monta a URI de requisição e decodifica a resposta em Base64.
 */
object CieloDeeplinkParser {

    /**
     * Decodifica o parâmetro "response" (Base64) recebido de volta da
     * Cielo Smart. Diferencia sucesso de erro/cancelamento pela FORMA do
     * JSON: respostas de erro têm apenas {code, reason}; respostas de
     * sucesso têm um objeto Order completo com "payments".
     *
     * ATENÇÃO: a tabela completa de códigos de erro não está documentada
     * publicamente além do exemplo "code=1 -> CANCELADO PELO USUÁRIO".
     * A heurística abaixo (checar se "reason" contém "CANCEL") deve ser
     * validada/ajustada com testes reais no sandbox.
     */
    fun parse(base64Response: String): CieloDeeplinkOutcome {
        return try {
            val decoded =
                String(android.util.Base64.decode(base64Response, android.util.Base64.DEFAULT))
            val json = Json.parseToJsonElement(decoded).jsonObject

            val payments = json["payments"]?.jsonArray
            if (payments == null) {
                val code = json["code"]?.jsonPrimitive?.intOrNull ?: -1
                val reason =
                    json["reason"]?.jsonPrimitive?.content ?: "Motivo não informado pela Cielo"
                CieloDeeplinkOutcome.Failure(
                    code = code,
                    reason = reason,
                    isCancellation = reason.contains("CANCEL", ignoreCase = true)
                )
            } else {
                val firstPayment = payments.firstOrNull()?.jsonObject
                CieloDeeplinkOutcome.Success(
                    orderIdCielo = json["id"]?.jsonPrimitive?.content,
                    reference = json["reference"]?.jsonPrimitive?.content,
                    status = json["status"]?.jsonPrimitive?.content,
                    transactionId = firstPayment?.get("externalId")?.jsonPrimitive?.content,
                    authCode = firstPayment?.get("authCode")?.jsonPrimitive?.content,
                    nsu = firstPayment?.get("cieloCode")?.jsonPrimitive?.content,
                    brand = firstPayment?.get("brand")?.jsonPrimitive?.content,
                    amountCents = firstPayment?.get("amount")?.jsonPrimitive?.longOrNull
                )
            }
        } catch (e: Exception) {
            CieloDeeplinkOutcome.IntegrationError("Falha ao decodificar resposta da Cielo Smart: ${e.message}")
        }
    }

    fun buildRequestUri(request: CieloDeeplinkPaymentRequest): android.net.Uri {
        val json = Json.encodeToString(request)
        val base64 = android.util.Base64.encodeToString(json.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        return android.net.Uri.parse(
            "${CieloDeeplinkContract.PAYMENT_SCHEME}://${CieloDeeplinkContract.PAYMENT_HOST}" +
                    "?request=$base64&urlCallback=${CieloDeeplinkContract.CALLBACK_URI}"
        )
    }
}