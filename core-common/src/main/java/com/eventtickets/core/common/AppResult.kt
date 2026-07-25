package com.eventtickets.core.common

/**
 * Wrapper de resultado usado em toda a aplicação (camadas domain/data/feature) para
 * tornar erros de integração e de pagamento EXPLÍCITOS no tipo de retorno das
 * funções, em vez de depender de exceptions não tratadas subindo a pilha.
 *
 * Isso atende ao requisito não-funcional de "tratamento explícito de erros de
 * integração e pagamento": todo use case que fala com rede/Cielo retorna
 * AppResult<T>, e a UI é obrigada (pelo compilador, via 'when' exaustivo) a
 * tratar os três estados.
 */
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Failure(val error: AppError) : AppResult<Nothing>()
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}

/**
 * Taxonomia de erros da aplicação. Ter subtipos nomeados (em vez de usar
 * Exception genérica ou String) permite que a UI decida a mensagem e a ação de
 * retry corretas para cada caso, e que o Payment flow distinga claramente um
 * erro de REDE (retry seguro) de um erro de NEGÓCIO/PAGAMENTO (não retry
 * automático, requer decisão do usuário).
 */
sealed class AppError(val message: String, val cause: Throwable? = null) {

    // --- Erros de rede / integração genérica ---
    data class Network(val reason: String, val throwable: Throwable? = null) :
        AppError("Falha de comunicação: $reason", throwable)

    data class Timeout(val operation: String) :
        AppError("Tempo esgotado ao executar: $operation")

    data class ServerError(val code: Int, val details: String) :
        AppError("Erro do servidor ($code): $details")

    // --- Erros específicos da integração Cielo Smart ---
    data class CieloSdkNotAvailable(val details: String) :
        AppError("SDK/Terminal Cielo Smart não disponível: $details")

    data class CieloTransactionDenied(val cieloCode: String?, val cieloMessage: String?) :
        AppError("Pagamento negado pela Cielo: ${cieloMessage ?: "sem detalhe"} (código: $cieloCode)")

    data class CieloTransactionCanceled(val reason: String) :
        AppError("Pagamento cancelado: $reason")

    data class CieloIntegrationError(val cieloCode: String?, val details: String) :
        AppError("Erro na integração com a Cielo Smart: $details (código: $cieloCode)")

    // --- Erros de negócio / validação ---
    data class InvalidQuantity(val requested: Int, val available: Int) :
        AppError("Quantidade inválida: solicitado $requested, disponível $available")

    data class EventNotFound(val eventId: String) :
        AppError("Evento não encontrado: $eventId")

    data class OrderNotFound(val orderId: String) :
        AppError("Pedido não encontrado: $orderId")

    data class DuplicatePaymentAttempt(val orderId: String) :
        AppError("Pagamento já foi iniciado/concluído para este pedido: $orderId")

    data class Unknown(val details: String, val throwable: Throwable? = null) :
        AppError("Erro inesperado: $details", throwable)
}
