package com.eventtickets.data.remote.cielo

/**
 * Contrato de integração via Deeplink com a Cielo Smart, conforme
 * documentação oficial (modelo recomendado pela Cielo desde a migração
 * LIO On -> Cielo Smart, uso de SDK embarcado descontinuado).
 * Fonte: https://github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local
 */
object CieloDeeplinkContract {
    const val PAYMENT_SCHEME = "lio"
    const val PAYMENT_HOST = "payment"
    const val CANCEL_HOST = "payment-reversal"

    // Nosso app declara este esquema/host no Manifest para receber o
    // resultado de volta da Cielo Smart.
    const val CALLBACK_SCHEME = "order"
    const val CALLBACK_HOST = "response"
    const val CALLBACK_URI = "$CALLBACK_SCHEME://$CALLBACK_HOST"
}