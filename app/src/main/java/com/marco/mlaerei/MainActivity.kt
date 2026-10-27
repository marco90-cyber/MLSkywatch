package com.marco.mlaerei

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.marco.mlaerei.ui.Fmt
import com.marco.mlaerei.ui.MLSkywatchTheme
import com.marco.mlaerei.ui.MainScreen
import com.marco.mlaerei.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private lateinit var locationHelper: LocationHelper
    private lateinit var headingSensor: HeadingSensor

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        val granted = hasLocationPermission()
        vm.setPermission(granted)
        if (granted) locationHelper.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        locationHelper = LocationHelper(
            this,
            onProviderChange = {
                vm.setLocationEnabled(locationHelper.isAnyEnabled())
                if (locationHelper.isAnyEnabled() && hasLocationPermission()) locationHelper.start()
            },
        ) { vm.setLocation(it.latitude, it.longitude) }

        headingSensor = HeadingSensor(this) { vm.setHeading(it) }
        vm.setCompassAvailable(headingSensor.available)

        // il sensore della bussola gira solo quando il radar deve seguirla
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                try {
                    vm.ui.map { it.headingUp }.distinctUntilChanged().collect { on ->
                        if (on) headingSensor.start() else headingSensor.stop()
                    }
                } finally {
                    headingSensor.stop()
                }
            }
        }

        if (savedInstanceState == null && !hasLocationPermission()) requestLocationPermission()

        setContent {
            val themeMode by vm.ui.map { it.themeMode }.distinctUntilChanged()
                .collectAsState(initial = vm.ui.value.themeMode)
            MLSkywatchTheme(themeMode) {
                App(
                    vm = vm,
                    onRequestPermission = { requestLocationPermission() },
                    onOpenLocationSettings = {
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val granted = hasLocationPermission()
        vm.setPermission(granted)
        vm.setLocationEnabled(locationHelper.isAnyEnabled())
        if (granted) locationHelper.start()
        vm.startPolling()
    }

    override fun onStop() {
        super.onStop()
        locationHelper.stop()
        vm.stopPolling()
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermission() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        )
    }
}

@Composable
private fun App(
    vm: MainViewModel,
    onRequestPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit,
) {
    val state by vm.ui.collectAsState()
    val heading = vm.heading.collectAsState()
    Fmt.imperial = state.imperial
    var showSettings by rememberSaveable { mutableStateOf(false) }

    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(
            state = state,
            onRadius = vm::setRadius,
            onShowGround = vm::setShowGround,
            onRefreshSec = vm::setRefreshSec,
            onImperial = vm::setImperial,
            onThemeMode = vm::setThemeMode,
            onBack = { showSettings = false },
        )
    } else {
        MainScreen(
            state = state,
            onSelect = vm::select,
            onRefresh = vm::refreshNow,
            heading = heading,
            onToggleHeading = vm::toggleHeadingUp,
            onOpenSettings = { showSettings = true },
            onRequestPermission = onRequestPermission,
            onOpenLocationSettings = onOpenLocationSettings,
        )
    }
}
