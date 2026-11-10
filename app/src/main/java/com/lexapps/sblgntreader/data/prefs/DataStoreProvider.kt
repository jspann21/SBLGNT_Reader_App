package com.lexapps.sblgntreader.data.prefs

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.appDataStore by preferencesDataStore(name = "settings")


