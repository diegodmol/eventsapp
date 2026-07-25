package com.eventtickets.data.remote.cielo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CompletableDeferred
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dispara a Intent(ACTION_VIEW) para a URI da Cielo Smart a partir de uma
 * Activity ativa, e suspende até o resultado chegar via deeplink de volta
 * (ver MainActivity.onNewIntent -> onDeeplinkResponse).
 *
 * Mantém apenas uma WeakReference à Activity para evitar memory leak.
 */
@Singleton
class CieloPaymentBridge @Inject constructor() {

    private var activityRef: WeakReference<Activity>? = null
    private var pendingResult: CompletableDeferred<CieloDeeplinkOutcome>? = null

    fun attachActivity(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    fun detachActivity() {
        activityRef = null
    }

    suspend fun launchPayment(uri: Uri): CieloDeeplinkOutcome {
        val activity = activityRef?.get()
            ?: return CieloDeeplinkOutcome.IntegrationError(
                "Nenhuma tela ativa para iniciar o pagamento na Cielo Smart."
            )

        val deferred = CompletableDeferred<CieloDeeplinkOutcome>()
        pendingResult = deferred
        activity.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        })
        return deferred.await()
    }

    /** Chamado pela MainActivity quando a resposta chega via deeplink. */
    fun onDeeplinkResponse(outcome: CieloDeeplinkOutcome) {
        pendingResult?.complete(outcome)
        pendingResult = null
    }
}