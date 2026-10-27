package com.marco.mlaerei

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.marco.mlaerei.data.Aircraft
import com.marco.mlaerei.data.AircraftDetails
import com.marco.mlaerei.data.AircraftRepository
import com.marco.mlaerei.data.AppPrefs
import com.marco.mlaerei.data.Geo
import com.marco.mlaerei.data.LatLon
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class UiState(
    val radiusKm: Int,
    val showGround: Boolean,
    val refreshSec: Int,
    val permissionGranted: Boolean = false,
    val location: LatLon? = null,
    val aircraft: List<Aircraft> = emptyList(),
    val selectedHex: String? = null,
    val details: AircraftDetails? = null,
    val detailsLoading: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val lastUpdate: Long? = null,
) {
    /** Aereo selezionato; se non c'è (o è uscito dal raggio) si prende il più vicino. */
    val selected: Aircraft?
        get() = aircraft.firstOrNull { it.hex == selectedHex } ?: aircraft.firstOrNull()
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = AppPrefs(app)
    private val repo = AircraftRepository()

    private val _ui = MutableStateFlow(
        UiState(
            radiusKm = prefs.radiusKm,
            showGround = prefs.showGround,
            refreshSec = prefs.refreshSec,
        )
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var pollJob: Job? = null
    private var detailJob: Job? = null
    private var detailHex: String? = null
    private var active = false

    private val detailCache = HashMap<String, AircraftDetails>()
    private val emptyUntil = HashMap<String, Long>()

    // ------------------------------------------------------------------ ciclo vita

    fun startPolling() {
        active = true
        if (pollJob?.isActive != true) pollJob = launchLoop()
    }

    fun stopPolling() {
        active = false
        pollJob?.cancel()
        pollJob = null
    }

    private fun restartPolling() {
        if (!active) return
        pollJob?.cancel()
        pollJob = launchLoop()
    }

    private fun launchLoop(): Job = viewModelScope.launch {
        while (isActive) {
            refreshOnce()
            delay(_ui.value.refreshSec * 1000L)
        }
    }

    fun refreshNow() = restartPolling()

    // ---------------------------------------------------------------------- stato

    fun setPermission(granted: Boolean) {
        _ui.update { it.copy(permissionGranted = granted) }
    }

    fun setLocation(lat: Double, lon: Double) {
        val old = _ui.value.location
        if (old != null && Geo.distanceKm(old.lat, old.lon, lat, lon) < 0.1) return
        _ui.update { it.copy(location = LatLon(lat, lon)) }
        if (old == null) restartPolling()
    }

    fun select(hex: String) {
        _ui.update { it.copy(selectedHex = hex) }
        ensureDetails()
    }

    fun setRadius(km: Int) {
        prefs.radiusKm = km
        _ui.update { it.copy(radiusKm = km) }
        restartPolling()
    }

    fun setShowGround(value: Boolean) {
        prefs.showGround = value
        _ui.update { it.copy(showGround = value) }
        restartPolling()
    }

    fun setRefreshSec(sec: Int) {
        prefs.refreshSec = sec
        _ui.update { it.copy(refreshSec = sec) }
        restartPolling()
    }

    // --------------------------------------------------------------------- rete

    private suspend fun refreshOnce() {
        val st = _ui.value
        val loc = st.location ?: return
        _ui.update { it.copy(loading = true) }
        try {
            val list = repo.fetchNearby(loc.lat, loc.lon, st.radiusKm, st.showGround)
            _ui.update {
                it.copy(
                    aircraft = list,
                    loading = false,
                    error = null,
                    lastUpdate = System.currentTimeMillis(),
                )
            }
            ensureDetails()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _ui.update {
                it.copy(
                    loading = false,
                    error = "Impossibile scaricare i voli (${e.message ?: "errore di rete"})",
                )
            }
        }
    }

    private fun ensureDetails() {
        val sel = _ui.value.selected
        if (sel == null) {
            detailHex = null
            _ui.update { it.copy(details = null, detailsLoading = false) }
            return
        }
        if (detailHex == sel.hex) return

        detailCache[sel.hex]?.let { cached ->
            detailHex = sel.hex
            _ui.update { it.copy(details = cached, detailsLoading = false) }
            return
        }
        if ((emptyUntil[sel.hex] ?: 0L) > System.currentTimeMillis()) {
            _ui.update { it.copy(details = AircraftDetails.EMPTY, detailsLoading = false) }
            return
        }

        detailHex = sel.hex
        _ui.update { it.copy(details = null, detailsLoading = true) }
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            val d = repo.fetchDetails(sel)
            if (d.isEmpty) {
                emptyUntil[sel.hex] = System.currentTimeMillis() + 60_000L
            } else {
                detailCache[sel.hex] = d
            }
            if (detailHex == sel.hex) {
                if (d.isEmpty) detailHex = null
                _ui.update { it.copy(details = d, detailsLoading = false) }
            }
        }
    }
}
