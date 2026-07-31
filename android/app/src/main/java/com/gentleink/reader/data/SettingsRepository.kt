package com.gentleink.reader.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.gentleink.reader.filter.FilterMode
import com.gentleink.reader.filter.FilterProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "gentleink_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val MODE = stringPreferencesKey("filter_mode")
        val PROFILE = stringPreferencesKey("filter_profile")
        val ONBOARDING_DONE = stringPreferencesKey("onboarding_done")
    }

    val filterMode: Flow<FilterMode> = context.settingsDataStore.data.map { prefs ->
        runCatching { FilterMode.valueOf(prefs[Keys.MODE] ?: FilterMode.SUBSTITUTE.name) }
            .getOrDefault(FilterMode.SUBSTITUTE)
    }

    val filterProfile: Flow<FilterProfile> = context.settingsDataStore.data.map { prefs ->
        FilterProfile.fromKey(prefs[Keys.PROFILE] ?: FilterProfile.FAMILY.key)
    }

    val onboardingDone: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_DONE] == "true"
    }

    suspend fun setFilterMode(mode: FilterMode) {
        context.settingsDataStore.edit { it[Keys.MODE] = mode.name }
    }

    suspend fun setFilterProfile(profile: FilterProfile) {
        context.settingsDataStore.edit { it[Keys.PROFILE] = profile.key }
    }

    suspend fun setOnboardingDone() {
        context.settingsDataStore.edit { it[Keys.ONBOARDING_DONE] = "true" }
    }
}
