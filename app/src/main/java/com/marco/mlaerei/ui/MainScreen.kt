package com.marco.mlaerei.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.marco.mlaerei.UiState
import com.marco.mlaerei.data.Aircraft
import com.marco.mlaerei.data.AircraftDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: UiState,
    onSelect: (String) -> Unit,
    onRefresh: () -> Unit,
    heading: State<Float>,
    onToggleHeading: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenLocationSettings: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    val ready = state.permissionGranted && state.location != null

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("MLSkywatch", fontWeight = FontWeight.SemiBold) },
                    actions = {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Aggiorna")
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Impostazioni")
                        }
                    },
                )
                if (ready) {
                    TabRow(selectedTabIndex = tab) {
                        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Radar") })
                        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Cielo") })
                    }
                }
            }
        },
    ) { inner ->
        when {
            !state.permissionGranted -> ScrollColumn(inner) { PermissionCard(onRequestPermission) }
            state.location == null -> ScrollColumn(inner) { SearchingCard(state.locationEnabled, onOpenLocationSettings) }
            tab == 0 -> ScrollColumn(inner) { RadarTab(state, heading, onSelect, onToggleHeading) }
            else -> SkyScreen(state, onSelect, Modifier.padding(inner))
        }
    }
}

@Composable
private fun ScrollColumn(inner: PaddingValues, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .padding(inner)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun PermissionCard(onRequestPermission: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Serve la tua posizione", style = MaterialTheme.typography.titleMedium)
            Text(
                "MLSkywatch mostra gli aerei che volano intorno a te, quindi ha bisogno di sapere dove sei. " +
                    "La posizione resta sul telefono: viene usata solo per chiedere i voli e il meteo della tua zona.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onRequestPermission) { Text("Consenti la posizione") }
        }
    }
}

@Composable
private fun SearchingCard(locationEnabled: Boolean, onOpenLocationSettings: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!locationEnabled) {
                Text("La posizione del telefono è disattivata", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Attiva la posizione (GPS) nelle impostazioni del telefono, poi torna qui: " +
                        "l'app riparte da sola.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onOpenLocationSettings) { Text("Apri le impostazioni posizione") }
            } else {
                Text("Cerco la tua posizione…", style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(
                    "Al chiuso il GPS può metterci un po': prova vicino a una finestra o all'aperto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ------------------------------------------------------------------------- Radar

@Composable
private fun RadarTab(
    state: UiState,
    heading: State<Float>,
    onSelect: (String) -> Unit,
    onToggleHeading: () -> Unit,
) {
    RadarView(
        aircraft = state.aircraft,
        radiusKm = state.radiusKm,
        selectedHex = state.selected?.hex,
        headingUp = state.headingUp,
        heading = heading,
        onSelect = onSelect,
        modifier = Modifier.fillMaxWidth(),
    )
    FilterChip(
        selected = state.headingUp,
        enabled = state.compassAvailable,
        onClick = onToggleHeading,
        label = {
            Text(
                when {
                    !state.compassAvailable -> "Bussola non disponibile"
                    state.headingUp -> "Bussola on"
                    else -> "Bussola off"
                }
            )
        },
    )
    Legend()

    val time = state.lastUpdate?.let { SimpleDateFormat("HH:mm:ss", Locale.ITALY).format(Date(it)) }
    val count = when (state.aircraft.size) {
        0 -> "Nessun aereo"
        1 -> "1 aereo"
        else -> "${state.aircraft.size} aerei"
    }
    Text(
        text = "$count entro ${Fmt.distWhole(state.radiusKm.toDouble())}" + (time?.let { " · aggiornato alle $it" } ?: ""),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
    state.error?.let {
        Text(
            it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }

    if (state.aircraft.isNotEmpty()) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (a in state.aircraft) {
                FilterChip(
                    selected = a.hex == state.selected?.hex,
                    onClick = { onSelect(a.hex) },
                    label = { Text("${a.callsign} · ${Fmt.km(a.distanceKm)}") },
                )
            }
        }
    }

    state.selected?.let { DetailCard(it, state.details, state.detailsLoading) }
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(Color(0xFFFF7043), if (Fmt.imperial) "< 3.000 ft" else "< 900 m")
        LegendItem(Color(0xFFFFCA28), if (Fmt.imperial) "< 10.000 ft" else "< 3.000 m")
        LegendItem(Color(0xFF66BB6A), if (Fmt.imperial) "< 25.000 ft" else "< 7.600 m")
        LegendItem(Color(0xFF42A5F5), if (Fmt.imperial) "≥ 25.000 ft" else "≥ 7.600 m")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.size(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DetailCard(a: Aircraft, details: AircraftDetails?, loading: Boolean) {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // foto
            val photo = details?.photoUrl
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    photo != null -> AsyncImage(
                        model = photo,
                        contentDescription = "Foto dell'aereo ${a.callsign}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f),
                    )
                    loading -> CircularProgressIndicator()
                    else -> Text(
                        "Nessuna foto disponibile",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (details != null && photo != null && details.photoSource != null) {
                val who = details.photographer?.let { "Foto: $it · " } ?: "Foto: "
                val link = details.photoLink
                Text(
                    text = who + details.photoSource,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = if (link != null) Modifier.clickable { uriHandler.openUri(link) } else Modifier,
                )
            }

            // intestazione
            Text(a.callsign, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            val subtitle = details?.airline ?: details?.owner
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }

            // rotta
            if (details != null && details.originCode != null && details.destCode != null) {
                Text(
                    "${details.originCode}  →  ${details.destCode}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                val cities = listOfNotNull(details.originCity, details.destCity)
                if (cities.size == 2) {
                    Text(
                        "${cities[0]} → ${cities[1]}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            StatsGrid(buildStats(a, details))

            if (details != null && details.problems.isNotEmpty()) {
                Text(
                    "Dati mancanti → " + details.problems.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun buildStats(a: Aircraft, d: AircraftDetails?): List<Pair<String, String>> {
    val items = ArrayList<Pair<String, String>>()

    val alt = when {
        a.onGround -> "A terra"
        a.altitudeM != null -> Fmt.altitude(a.altitudeM!!)
        else -> null
    }
    alt?.let { items.add("Altitudine" to it) }
    a.speedKmh?.let { items.add("Velocità" to Fmt.speed(it)) }
    items.add("Distanza" to "${Fmt.km(a.distanceKm)} verso ${Fmt.degrees(a.bearingDeg)}")
    Fmt.elevation(a.elevationDeg)?.let { items.add("Nel cielo" to it) }
    a.trackDeg?.let { items.add("Direzione di volo" to Fmt.degrees(it)) }
    Fmt.vertRate(a.vertRateFpm)?.let { items.add("Quota" to it) }

    val model = d?.model ?: a.typeDescription ?: a.typeCode
    model?.let { items.add("Modello" to it) }
    d?.manufacturer?.let { items.add("Costruttore" to it) }
    a.registration?.let { items.add("Matricola" to it) }
    d?.owner?.let { items.add("Proprietario" to it) }
    a.squawk?.let { items.add("Squawk" to it) }
    items.add("Codice ICAO" to a.hex)
    return items
}

@Composable
private fun StatsGrid(items: List<Pair<String, String>>) {
    Column {
        for (row in items.chunked(2)) {
            Row(Modifier.fillMaxWidth()) {
                for ((label, value) in row) {
                    Column(Modifier.weight(1f).padding(vertical = 6.dp, horizontal = 2.dp)) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(value, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
