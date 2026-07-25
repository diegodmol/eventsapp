package com.eventtickets.app.navigation

/**
 * Rotas de navegação do app, seguindo a recomendação oficial de usar
 * Navigation Compose com argumentos tipados via rota (single-activity app).
 */
sealed class Routes(val route: String) {
    data object Events : Routes("events")

    data object Checkout : Routes("checkout/{eventId}") {
        fun createRoute(eventId: String) = "checkout/$eventId"
        const val ARG_EVENT_ID = "eventId"
    }

    data object Payment : Routes("payment/{orderId}") {
        fun createRoute(orderId: String) = "payment/$orderId"
        const val ARG_ORDER_ID = "orderId"
    }

    data object Receipt : Routes("receipt/{orderId}") {
        fun createRoute(orderId: String) = "receipt/$orderId"
        const val ARG_ORDER_ID = "orderId"
    }
}
