package com.eventtickets.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.eventtickets.app.navigation.EventTicketsNavHost
import com.eventtickets.core.ui.theme.EventTicketsTheme
import com.eventtickets.data.remote.cielo.CieloDeeplinkParser
import com.eventtickets.data.remote.cielo.CieloPaymentBridge
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var cieloPaymentBridge: CieloPaymentBridge

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingDeeplink(intent)

        setContent {
            EventTicketsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EventTicketsNavHost()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingDeeplink(intent)
    }

    override fun onResume() {
        super.onResume()
        cieloPaymentBridge.attachActivity(this)
    }

    override fun onPause() {
        cieloPaymentBridge.detachActivity()
        super.onPause()
    }

    private fun handleIncomingDeeplink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "order" && uri.host == "response") {
            val responseParam = uri.getQueryParameter("response") ?: return
            val outcome = CieloDeeplinkParser.parse(responseParam)
            cieloPaymentBridge.onDeeplinkResponse(outcome)
        }
    }
}