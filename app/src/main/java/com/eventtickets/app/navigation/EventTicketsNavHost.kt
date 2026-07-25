package com.eventtickets.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.eventtickets.feature.checkout.CheckoutScreen
import com.eventtickets.feature.events.EventsScreen
import com.eventtickets.feature.payment.PaymentScreen
import com.eventtickets.feature.receipt.ReceiptScreen


@Composable
fun EventTicketsNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.Events.route) {

        // Fluxo 1: Visualizar eventos disponíveis para compra
        composable(Routes.Events.route) {
            EventsScreen(
                onEventSelected = { event ->
                    navController.navigate(Routes.Checkout.createRoute(event.id))
                }
            )
        }

        // Fluxo 2: Selecionar a quantidade de ingressos
        composable(
            route = Routes.Checkout.route,
            arguments = listOf(navArgument(Routes.Checkout.ARG_EVENT_ID) { type = NavType.StringType })
        ) {
            CheckoutScreen(
                onOrderCreated = { orderId ->
                    navController.navigate(Routes.Payment.createRoute(orderId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // Fluxo 3 e 4: Iniciar/concluir pagamento via Cielo + registrar resultado
        composable(
            route = Routes.Payment.route,
            arguments = listOf(navArgument(Routes.Payment.ARG_ORDER_ID) { type = NavType.StringType })
        ) {
            PaymentScreen(
                onPaymentFinished = { orderId ->
                    navController.navigate(Routes.Receipt.createRoute(orderId)) {
                        // Remove a tela de pagamento da pilha para impedir que o
                        // usuário volte para ela via botão "voltar" e tente
                        // reenviar o pagamento de um pedido já finalizado.
                        popUpTo(Routes.Events.route)
                    }
                }
            )
        }

        // Fluxo 5: Exibir comprovante/resumo da compra (+ QR Code, se aprovado)
        composable(
            route = Routes.Receipt.route,
            arguments = listOf(navArgument(Routes.Receipt.ARG_ORDER_ID) { type = NavType.StringType })
        ) {
            ReceiptScreen(
                onBackToEvents = {
                    navController.navigate(Routes.Events.route) {
                        popUpTo(Routes.Events.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
