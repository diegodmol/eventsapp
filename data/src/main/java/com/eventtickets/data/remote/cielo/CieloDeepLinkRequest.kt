package com.eventtickets.data.remote.cielo
import kotlinx.serialization.Serializable

@Serializable
data class CieloDeeplinkItem(
    val name: String,
    val quantity: Int,
    val sku: String,
    val unitOfMeasure: String,
    val unitPrice: Long
)
@Serializable
data class CieloDeeplinkPaymentRequest(
    val accessToken: String,
    val clientID: String,
    val reference: String? = null,
    val merchantCode: String? = null,
    val email: String? = null,
    val installments: Int = 0,
    val items: List<CieloDeeplinkItem>,
    val paymentCode: String? = null,
    val value: String
)