package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore: DataStore<Preferences> by preferencesDataStore(name = "vinted_studio_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val CUSTOM_MODEL_REF_PATH = stringPreferencesKey("custom_model_ref_path")
        val CUSTOM_BEDROOM_REF_PATH = stringPreferencesKey("custom_bedroom_ref_path")
    }

    val customModelRefPath: Flow<String?> = context.studioDataStore.data.map { prefs ->
        prefs[Keys.CUSTOM_MODEL_REF_PATH]
    }

    val customBedroomRefPath: Flow<String?> = context.studioDataStore.data.map { prefs ->
        prefs[Keys.CUSTOM_BEDROOM_REF_PATH]
    }

    suspend fun setCustomModelRefPath(path: String?) {
        context.studioDataStore.edit { prefs ->
            if (path == null) {
                prefs.remove(Keys.CUSTOM_MODEL_REF_PATH)
            } else {
                prefs[Keys.CUSTOM_MODEL_REF_PATH] = path
            }
        }
    }

    suspend fun setCustomBedroomRefPath(path: String?) {
        context.studioDataStore.edit { prefs ->
            if (path == null) {
                prefs.remove(Keys.CUSTOM_BEDROOM_REF_PATH)
            } else {
                prefs[Keys.CUSTOM_BEDROOM_REF_PATH] = path
            }
        }
    }
}
