package com.lumera.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumera.app.data.local.AddonDao
import com.lumera.app.data.model.ProfileEntity
import com.lumera.app.data.profile.ProfileConfigurationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dao: AddonDao,
    private val profileConfigurationManager: ProfileConfigurationManager
) : ViewModel() {

    private val _activeProfile = MutableStateFlow<ProfileEntity?>(null)
    val activeProfile: StateFlow<ProfileEntity?> = _activeProfile

    private var profileJob: Job? = null
    private var activeProfileId: Int? = null

    // Call this when user clicks a profile
    fun login(id: Int) {
        viewModelScope.launch {
            profileConfigurationManager.captureStartupRuntimeIfNeeded()

            val previousProfileId = activeProfileId
            if (previousProfileId != null && previousProfileId != id) {
                profileConfigurationManager.saveRuntimeState(previousProfileId)
            }

            profileConfigurationManager.loadRuntimeState(id)
            activeProfileId = id

            profileJob?.cancel()
            profileJob = launch {
                dao.getProfileFlow(id).collect { profile ->
                    _activeProfile.value = profile
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            activeProfileId?.let { profileConfigurationManager.saveRuntimeState(it) }
            profileConfigurationManager.clearLastActiveProfileId()
            activeProfileId = null
            profileJob?.cancel()
            _activeProfile.value = null
        }
    }

    suspend fun persistActiveProfileState() {
        activeProfileId?.let { profileConfigurationManager.saveRuntimeState(it) }
    }
}
