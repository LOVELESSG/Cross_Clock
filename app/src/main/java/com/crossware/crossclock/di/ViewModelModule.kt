package com.crossware.crossclock.di

import com.crossware.crossclock.data.StopWatchViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ViewModelModule {

    @Singleton
    @Provides
    fun provideStopWatchViewModel(): StopWatchViewModel {
        return StopWatchViewModel()
    }
}
