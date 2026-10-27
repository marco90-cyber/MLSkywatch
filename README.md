# MLSkywatch

App Android che mostra gli aerei che volano intorno a te: un radar con tutti i voli vicini e, toccando un aereo, la scheda con foto, compagnia, rotta, altitudine, velocità e dove guardare nel cielo.

- Raggio di ricerca regolabile dalle impostazioni (predefinito 50 km, da 5 a 250 km)
- Posizioni: adsb.lol (di riserva adsb.fi), rotte e dati aeromobile: adsbdb, foto: planespotters.net
- Kotlin + Jetpack Compose + Material You, in italiano

## APK
Ogni push su `main` compila l'APK con GitHub Actions e lo pubblica nelle **Releases** (l'ultima è sempre in cima).
La firma usa `app/debug.keystore` (chiave di test pubblica), così ogni versione si installa sopra la precedente.

## Crediti
Dati ADS-B: [adsb.lol](https://adsb.lol) (ODbL) e [adsb.fi](https://adsb.fi) · Rotte: [adsbdb.com](https://www.adsbdb.com) · Foto: [planespotters.net](https://www.planespotters.net) e i loro fotografi
