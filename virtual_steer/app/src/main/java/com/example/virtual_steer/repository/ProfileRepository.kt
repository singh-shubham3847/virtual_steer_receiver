package com.example.virtual_steer.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.virtual_steer.model.Profile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.profileDataStore by preferencesDataStore(name = "profiles")

class ProfileRepository(private val context: Context) {

    private object Keys {
        val PROFILES_JSON = stringPreferencesKey("profiles_json")
        val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
    }

    val profilesFlow: Flow<List<Profile>> = context.profileDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val json = prefs[Keys.PROFILES_JSON] ?: "[]"
            try {
                Json.decodeFromString<List<Profile>>(json)
            } catch (e: Exception) {
                emptyList()
            }
        }

    val activeProfileIdFlow: Flow<String?> = context.profileDataStore.data
        .map { prefs -> prefs[Keys.ACTIVE_PROFILE_ID] }

    suspend fun saveProfile(profile: Profile) {
        context.profileDataStore.edit { prefs ->
            val json = prefs[Keys.PROFILES_JSON] ?: "[]"
            val profiles = try {
                Json.decodeFromString<MutableList<Profile>>(json)
            } catch (e: Exception) {
                mutableListOf()
            }
            
            val index = profiles.indexOfFirst { it.id == profile.id }
            if (index != -1) {
                profiles[index] = profile
            } else {
                profiles.add(profile)
            }
            
            prefs[Keys.PROFILES_JSON] = Json.encodeToString(profiles)
        }
    }

    suspend fun deleteProfile(profileId: String) {
        context.profileDataStore.edit { prefs ->
            val json = prefs[Keys.PROFILES_JSON] ?: "[]"
            val profiles = try {
                Json.decodeFromString<MutableList<Profile>>(json)
            } catch (e: Exception) {
                mutableListOf()
            }
            
            profiles.removeAll { it.id == profileId }
            prefs[Keys.PROFILES_JSON] = Json.encodeToString(profiles)
            
            if (prefs[Keys.ACTIVE_PROFILE_ID] == profileId) {
                prefs.remove(Keys.ACTIVE_PROFILE_ID)
            }
        }
    }

    suspend fun setActiveProfile(profileId: String) {
        context.profileDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_PROFILE_ID] = profileId
        }
    }

    suspend fun duplicateProfile(profileId: String) {
        context.profileDataStore.edit { prefs ->
            val json = prefs[Keys.PROFILES_JSON] ?: "[]"
            val profiles = try {
                Json.decodeFromString<MutableList<Profile>>(json)
            } catch (e: Exception) {
                mutableListOf()
            }
            
            val original = profiles.find { it.id == profileId }
            if (original != null) {
                val copy = original.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    name = "${original.name} (Copy)"
                )
                profiles.add(copy)
                prefs[Keys.PROFILES_JSON] = Json.encodeToString(profiles)
            }
        }
    }

    suspend fun renameProfile(profileId: String, newName: String) {
        context.profileDataStore.edit { prefs ->
            val json = prefs[Keys.PROFILES_JSON] ?: "[]"
            val profiles = try {
                Json.decodeFromString<MutableList<Profile>>(json)
            } catch (e: Exception) {
                mutableListOf()
            }
            
            val index = profiles.indexOfFirst { it.id == profileId }
            if (index != -1) {
                profiles[index] = profiles[index].copy(name = newName)
                prefs[Keys.PROFILES_JSON] = Json.encodeToString(profiles)
            }
        }
    }
}
