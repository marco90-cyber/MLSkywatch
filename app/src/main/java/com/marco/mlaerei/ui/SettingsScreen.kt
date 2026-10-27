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
import com.marco.mlaerei.data.Backdrop
import com.marco.mlaerei.data.PaintingStyle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: UiState,
    onRadius: (Int) -> Unit,
    onShowGround: (Boolean) -> Unit,
    onRefreshSec: (Int) -> Unit,
    onPaintingEnabled: (Boolean) -> Unit,
    onPaintingStyle: (PaintingStyle) -> Unit,
    onBackdrop: (Backdrop) -> Unit,
    onClearPaintings: () -> Unit,
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
            RadiusCard(state.radiusKm, onRadius)

            PaintingCard(state, onPaintingEnabled, onPaintingStyle, onBackdrop, onClearPaintings)

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

            CreditsCard()
        }
    }
}

@Composable
private fun PaintingCard(
    state: UiState,
    onEnabled: (Boolean) -> Unit,
    onStyle: (PaintingStyle) -> Unit,
    onBackdrop: (Backdrop) -> Unit,
    onClear: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Dipinti dell'aereo", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Nella scheda dell'aereo compare un dipinto creato dall'AI, oltre alla foto.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.paintingEnabled, onCheckedChange = onEnabled)
            }

            if (state.paintingEnabled) {
                Text("Stile", style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (style in PaintingStyle.values()) {
                        FilterChip(
                            selected = state.paintingStyle == style,
                            onClick = { onStyle(style) },
                            label = { Text(style.label) },
                        )
                    }
                }

                Text("Sfondo", style = MaterialTheme.typography.labelLarge)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (b in Backdrop.values()) {
                        FilterChip(
                            selected = state.backdrop == b,
                            onClick = { onBackdrop(b) },
                            label = { Text(b.label) },
                        )
                    }
                }

                Text(
                    "È gratis, ma il servizio è lento: un dipinto ogni 15-20 secondi circa. " +
                        "L'AI parte da una descrizione e non dalla foto, quindi livrea e dettagli " +
                        "possono essere approssimativi. Ogni dipinto resta salvato sul telefono.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onClear, enabled = state.savedPaintings > 0) {
                    Text("Svuota i dipinti salvati (${state.savedPaintings})")
                }
            }
        }
    }
}

@Composable
private fun RadiusCard(radiusKm: Int, onRadius: (Int) -> Unit) {
    var tmp by remember(radiusKm) { mutableFloatStateOf(radiusKm.toFloat()) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Raggio di ricerca", style = MaterialTheme.typography.titleMedium)
            Text(
                "${tmp.roundToInt()} km",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Slider(
                value = tmp,
                onValueChange = { tmp = it },
                onValueChangeFinished = { onRadius(tmp.roundToInt()) },
                valueRange = AppPrefs.MIN_RADIUS_KM.toFloat()..AppPrefs.MAX_RADIUS_KM.toFloat(),
                steps = 48,
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (km in listOf(10, 25, 50, 100, 250)) {
                    FilterChip(
                        selected = tmp.roundToInt() == km,
                        onClick = {
                            tmp = km.toFloat()
                            onRadius(km)
                        },
                        label = { Text("$km km") },
                    )
                }
            }
            Text(
                "Il valore predefinito è ${AppPrefs.DEFAULT_RADIUS_KM} km: è la distanza fino a cui, " +
                    "con aria limpida e senza ostacoli, un aereo di linea in quota si riesce ancora a " +
                    "distinguere a occhio nudo. È una stima: foschia, nuvole e posizione cambiano molto " +
                    "quello che si vede davvero.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = {
                tmp = AppPrefs.DEFAULT_RADIUS_KM.toFloat()
                onRadius(AppPrefs.DEFAULT_RADIUS_KM)
            }) { Text("Ripristina ${AppPrefs.DEFAULT_RADIUS_KM} km") }
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
                title = "Dipinti",
                text = "Le immagini in stile quadro sono generate dall'intelligenza artificiale tramite " +
                    "Pollinations.ai. Non sono foto e non rappresentano l'aereo con precisione.",
                links = listOf("pollinations.ai" to "https://pollinations.ai"),
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
