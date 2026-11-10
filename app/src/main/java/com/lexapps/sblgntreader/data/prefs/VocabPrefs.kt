package com.lexapps.sblgntreader.data.prefs

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class VocabPrefs(private val store: PreferenceStore) {
    private fun levelKey(name: String) = intPreferencesKey("vocab_${name}_level")
    private fun activeKey(name: String) = booleanPreferencesKey("vocab_${name}_active")

    suspend fun getLevel(name: String): Int {
        val prefs = store.data.first()
        return prefs[levelKey(name)] ?: 1
    }

    suspend fun isActive(name: String): Boolean {
        val prefs = store.data.first()
        return prefs[activeKey(name)] ?: false
    }

    fun levelsFlow(name: String): Flow<Int> = store.data.map { it[levelKey(name)] ?: 1 }
    fun activeFlow(name: String): Flow<Boolean> = store.data.map { it[activeKey(name)] ?: false }

    suspend fun setLevel(name: String, level: Int) = store.edit { it[levelKey(name)] = level }
    suspend fun setActive(name: String, active: Boolean) = store.edit { it[activeKey(name)] = active }
}


