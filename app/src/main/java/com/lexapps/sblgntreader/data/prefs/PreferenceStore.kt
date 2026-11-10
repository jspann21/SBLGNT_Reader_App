package com.lexapps.sblgntreader.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.lexapps.sblgntreader.ui.UiMessages
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch

@Singleton
class PreferenceStore(private val store: DataStore<Preferences>, private val messages: UiMessages) {
    @Inject constructor(@ApplicationContext context: Context, messages: UiMessages) : this(context.appDataStore, messages)

    val data = store.data.catch { error ->
        if (error is IOException) {
            messages.report("Could not read saved settings. Temporary defaults are in use; try reopening the app.", error)
            emit(emptyPreferences())
        } else throw error
    }

    suspend fun edit(transform: suspend (MutablePreferences) -> Unit): Preferences = store.edit(transform)
}
