package com.lexapps.sblgntreader.di

import com.lexapps.sblgntreader.ui.stats.DefaultStatsPreferenceSource
import com.lexapps.sblgntreader.ui.stats.StatsPreferenceSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
@Suppress("unused") // Referenced via Hilt modules
abstract class StatsModule {

    @Binds
    @Suppress("unused")
    abstract fun bindStatsPreferenceSource(
        impl: DefaultStatsPreferenceSource
    ): StatsPreferenceSource
}

