package com.lexapps.sblgntreader.di

import com.lexapps.sblgntreader.data.prefs.PreferenceStore
import com.lexapps.sblgntreader.data.prefs.AppPrefs
import com.lexapps.sblgntreader.data.prefs.VocabPrefs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PrefsModule {

    @Provides
    @Singleton
    fun provideAppPrefs(store: PreferenceStore): AppPrefs = AppPrefs(store)

    @Provides
    @Singleton
    fun provideVocabPrefs(store: PreferenceStore): VocabPrefs = VocabPrefs(store)
}

