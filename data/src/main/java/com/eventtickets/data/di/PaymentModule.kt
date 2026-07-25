package com.eventtickets.data.di

import com.eventtickets.data.remote.cielo.CieloPaymentBridge
import com.eventtickets.data.remote.cielo.CieloSmartPaymentGateway
import com.eventtickets.data.remote.cielo.FakeCieloPaymentGateway
import com.eventtickets.domain.repository.PaymentGateway
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Ponto único de decisão sobre qual implementação de [PaymentGateway] é
 * usada: a integração real com a Cielo Smart (via Intent) ou o gateway fake
 * de demonstração, usado quando o emulador/app Cielo Smart não está presente
 * no dispositivo de avaliação.
 *
 * Para alternar: mude USE_REAL_CIELO_GATEWAY para true e garanta que o app
 * Cielo Smart (ou o emulador oficial) esteja instalado no dispositivo/AVD.
 */
@Module
@InstallIn(SingletonComponent::class)
object PaymentModule {

    private const val USE_REAL_CIELO_GATEWAY = true

    @Provides
    @Singleton
    fun providePaymentGateway(
        cieloSmartPaymentGateway: CieloSmartPaymentGateway,
        fakeCieloPaymentGateway: FakeCieloPaymentGateway
    ): PaymentGateway =
        if (USE_REAL_CIELO_GATEWAY) cieloSmartPaymentGateway else fakeCieloPaymentGateway

    @Provides
    @Singleton
    fun provideCieloPaymentBridge(): CieloPaymentBridge = CieloPaymentBridge()
}
