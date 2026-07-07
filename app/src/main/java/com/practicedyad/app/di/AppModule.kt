package com.practicedyad.app.di

import com.practicedyad.app.data.remote.FirebaseService
import com.practicedyad.app.data.repository.AppRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseService(): FirebaseService = FirebaseService()

    @Provides
    @Singleton
    fun provideAppRepository(firebase: FirebaseService): AppRepository = AppRepository(firebase)
}
