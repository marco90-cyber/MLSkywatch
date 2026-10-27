package com.marco.mlaerei

import android.Manifest
import android.content.pm.PackageManager
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
import com.marco.mlaerei.ui.MLSkywatchTheme
import com.marco.mlaerei.ui.MainScreen
import com.marco.mlaerei.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private lateinit var locationHelper: LocationHelper

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

        locationHelper = LocationHelper(this) { vm.setLocation(it.latitude, it.longitude) }

        if (savedInstanceState == null && !hasLocationPermission()) requestLocationPermission()

        setContent {
            MLSkywatchTheme {
                App(vm = vm, onRequestPermission = { requestLocationPermission() })
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val granted = hasLocationPermission()
        vm.setPermission(granted)
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
private fun App(vm: MainViewModel, onRequestPermission: () -> Unit) {
    val state by vm.ui.collectAsState()
    var showSettings by rememberSaveable { mutableStateOf(false) }

    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(
            state = state,
            onRadius = vm::setRadius,
            onShowGround = vm::setShowGround,
            onRefreshSec = vm::setRefreshSec,
            onBack = { showSettings = false },
        )
    } else {
        MainScreen(
            state = state,
            onSelect = vm::select,
            onRefresh = vm::refreshNow,
            onOpenSettings = { showSettings = true },
            onRequestPermission = onRequestPermission,
        )
    }
}
