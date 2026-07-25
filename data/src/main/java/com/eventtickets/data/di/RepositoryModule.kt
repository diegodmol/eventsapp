package com.eventtickets.data.di

import com.eventtickets.data.repository.EventRepositoryImpl
import com.eventtickets.data.repository.OrderRepositoryImpl
import com.eventtickets.data.repository.TicketRepositoryImpl
import com.eventtickets.domain.repository.EventRepository
import com.eventtickets.domain.repository.OrderRepository
import com.eventtickets.domain.repository.TicketRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindEventRepository(impl: EventRepositoryImpl): EventRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository

    @Binds
    @Singleton
    abstract fun bindTicketRepository(impl: TicketRepositoryImpl): TicketRepository
}
