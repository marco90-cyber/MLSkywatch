package com.marco.mlaerei.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.marco.mlaerei.data.UpdateChecker
import com.marco.mlaerei.data.UpdateResult
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.marco.mlaerei.UiState
import com.marco.mlaerei.data.AppPrefs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: UiState,
    onRadius: (Int) -> Unit,
    onShowGround: (Boolean) -> Unit,
    onRefreshSec: (Int) -> Unit,
    onImperial: (Boolean) -> Unit,
    onThemeMode: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Impostazioni", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Unità di misura", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Cambia distanze, quote, velocità e temperatura in tutta l'app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !state.imperial,
                            onClick = { onImperial(false) },
                            label = { Text("Metrico (km, m, km/h, °C)") },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.imperial,
                            onClick = { onImperial(true) },
                            label = { Text("Imperiale (mi, ft, mph, °F)") },
                        )
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tema", style = MaterialTheme.typography.titleMedium)
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(0 to "Sistema", 1 to "Chiaro", 2 to "Scuro").forEach { (m, name) ->
                            FilterChip(
                                selected = state.themeMode == m,
                                onClick = { onThemeMode(m) },
                                label = { Text(name) },
                            )
                        }
                    }
                }
            }

            RadiusCard(state.radiusKm, state.imperial, onRadius)

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Mostra aerei a terra", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Include gli aerei fermi o in rullaggio negli aeroporti vicini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = state.showGround, onCheckedChange = onShowGround)
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Aggiornamento", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Ogni quanti secondi scaricare i voli.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (sec in listOf(5, 10, 20, 30)) {
                            FilterChip(
                                selected = state.refreshSec == sec,
                                onClick = { onRefreshSec(sec) },
                                label = { Text("$sec s") },
                            )
                        }
                    }
                }
            }

            UpdateCard()

            CreditsCard()
        }
    }
}

@Composable
private fun RadiusCard(radiusKm: Int, imperial: Boolean, onRadius: (Int) -> Unit) {
    val f = if (imperial) Fmt.KM_PER_MI else 1.0
    val unit = if (imperial) "mi" else "km"
    fun toUnit(km: Int) = (km / f).roundToInt().toFloat()
    fun toKm(u: Float) = (u * f).roundToInt().coerceIn(AppPrefs.MIN_RADIUS_KM, AppPrefs.MAX_RADIUS_KM)

    var tmp by remember(radiusKm, imperial) { mutableFloatStateOf(toUnit(radiusKm)) }
    val minU = toUnit(AppPrefs.MIN_RADIUS_KM)
    val maxU = toUnit(AppPrefs.MAX_RADIUS_KM)
    val presets = if (imperial) listOf(5, 15, 30, 60, 150) else listOf(10, 25, 50, 100, 250)
    val defaultU = toUnit(AppPrefs.DEFAULT_RADIUS_KM).roundToInt()

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Raggio di ricerca", style = MaterialTheme.typography.titleMedium)
            Text(
                "${tmp.roundToInt()} $unit",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Slider(
                value = tmp,
                onValueChange = { tmp = it },
                onValueChangeFinished = { onRadius(toKm(tmp)) },
                valueRange = minU..maxU,
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (u in presets) {
                    FilterChip(
                        selected = tmp.roundToInt() == u,
                        onClick = {
                            tmp = u.toFloat()
                            onRadius(toKm(tmp))
                        },
                        label = { Text("$u $unit") },
                    )
                }
            }
            Text(
                "Il valore predefinito è $defaultU $unit: è la distanza fino a cui, " +
                    "con aria limpida e senza ostacoli, un aereo di linea in quota si riesce ancora a " +
                    "distinguere a occhio nudo. È una stima: foschia, nuvole e posizione cambiano molto " +
                    "quello che si vede davvero.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = {
                tmp = defaultU.toFloat()
                onRadius(AppPrefs.DEFAULT_RADIUS_KM)
            }) { Text("Ripristina $defaultU $unit") }
        }
    }
}

@Composable
private fun UpdateCard() {
    val context = LocalContext.current
    val current = remember { UpdateChecker.installed(context).second }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Aggiornamenti", style = MaterialTheme.typography.titleMedium)
            Text(
                "Versione installata: $current",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Scarica e installa la nuova versione direttamente nell'app, con Pausa, Riprendi e Annulla.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = { com.marco.mlaerei.AppUpdate.open(context) }) { Text("Cerca aggiornamenti") }
        }
    }
}

@Composable
private fun CreditsCard() {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Informazioni e crediti", style = MaterialTheme.typography.titleMedium)

            CreditItem(
                title = "Posizioni degli aerei",
                text = "Dati ADS-B di adsb.lol (licenza ODbL) e, quando non risponde, di adsb.fi. " +
                    "Sono reti di ricevitori gestite da volontari.",
                links = listOf("adsb.lol" to "https://adsb.lol", "adsb.fi" to "https://adsb.fi"),
            )
            CreditItem(
                title = "Rotte e dati dell'aeromobile",
                text = "Compagnia, partenza, destinazione, modello e proprietario arrivano da adsbdb.",
                links = listOf("adsbdb.com" to "https://www.adsbdb.com"),
            )
            CreditItem(
                title = "Foto degli aerei",
                text = "Le foto appartengono ai rispettivi fotografi, indicati sotto ogni foto, e vengono da " +
                    "planespotters.net. Se manca, può comparire una foto di airport-data.com.",
                links = listOf(
                    "planespotters.net" to "https://www.planespotters.net",
                    "airport-data.com" to "https://www.airport-data.com",
                ),
            )
            CreditItem(
                title = "Meteo",
                text = "Lo sfondo della schermata Cielo segue il meteo di Open-Meteo.com (licenza CC BY 4.0).",
                links = listOf("open-meteo.com" to "https://open-meteo.com"),
            )
            CreditItem(
                title = "Loghi delle compagnie",
                text = "I loghi nella schermata Cielo vengono scaricati da images.kiwi.com. " +
                    "I marchi appartengono alle rispettive compagnie aeree.",
                links = listOf("kiwi.com" to "https://www.kiwi.com"),
            )
            Text(
                "I dati sono forniti così come sono e possono avere errori o ritardi: " +
                    "l'app non va usata per la navigazione o per la sicurezza del volo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "MLSkywatch · fatta con Claude",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CreditItem(title: String, text: String, links: List<Pair<String, String>>) {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text(text, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            for ((name, url) in links) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { uriHandler.openUri(url) },
                )
            }
        }
    }
}
