package com.marco.mlaerei.data

/** Compagnie dal prefisso ICAO (3 lettere) del nominativo di volo, usate quando adsbdb non conosce la rotta. */
object Airlines {
    private val names = mapOf(
        "RYR" to "Ryanair", "RUK" to "Ryanair UK", "EZY" to "easyJet", "EJU" to "easyJet Europe",
        "EZS" to "easyJet Switzerland", "WZZ" to "Wizz Air", "WMT" to "Wizz Air Malta",
        "DLH" to "Lufthansa", "CLH" to "Lufthansa CityLine", "EWG" to "Eurowings", "AZA" to "ITA Airways",
        "BAW" to "British Airways", "AFR" to "Air France", "KLM" to "KLM", "TRA" to "Transavia",
        "VLG" to "Vueling", "IBE" to "Iberia", "IBS" to "Iberia Express", "AEA" to "Air Europa",
        "TVF" to "Transavia France", "NOZ" to "Norwegian", "NAX" to "Norwegian", "SAS" to "SAS",
        "SWR" to "Swiss", "EDW" to "Edelweiss", "AUA" to "Austrian", "BEL" to "Brussels Airlines",
        "TAP" to "TAP Air Portugal", "LOT" to "LOT Polish Airlines", "FIN" to "Finnair",
        "THY" to "Turkish Airlines", "PGT" to "Pegasus", "UAE" to "Emirates", "QTR" to "Qatar Airways",
        "ETD" to "Etihad", "ELY" to "El Al", "MSR" to "EgyptAir", "RAM" to "Royal Air Maroc",
        "DAL" to "Delta", "UAL" to "United", "AAL" to "American Airlines", "ACA" to "Air Canada",
        "SIA" to "Singapore Airlines", "CPA" to "Cathay Pacific", "ANA" to "ANA", "JAL" to "Japan Airlines",
        "KAL" to "Korean Air", "AIC" to "Air India", "ETH" to "Ethiopian", "SAA" to "South African",
        "TSC" to "Air Transat", "VIR" to "Virgin Atlantic", "EIN" to "Aer Lingus", "ICE" to "Icelandair",
        "NJE" to "NetJets Europe", "BCS" to "European Air Transport (DHL)", "UPS" to "UPS",
        "FDX" to "FedEx", "GEC" to "Lufthansa Cargo", "CLX" to "Cargolux", "ABR" to "ASL Airlines",
        "BOX" to "AeroLogic", "TAY" to "TNT/FedEx", "VOE" to "Volotea", "BTI" to "airBaltic",
        "CTN" to "Croatia Airlines", "ADR" to "Adria Airways", "ASL" to "Air Serbia", "ROT" to "Tarom",
        "AEE" to "Aegean", "SEH" to "Sky Express", "LDM" to "Laudamotion", "LDA" to "Lauda Europe",
        "BBD" to "Blue Air", "NSZ" to "Norwegian Air Sweden", "DLA" to "Air Dolomiti",
        "ENT" to "Enter Air", "SXS" to "SunExpress", "TUI" to "TUI Airways", "TOM" to "TUI Airways",
        "TFL" to "TUI fly Netherlands", "CND" to "Corendon", "JAF" to "TUI fly Belgium",
        "SKV" to "Sky Alps", "NEOS" to "Neos", "NOS" to "Neos", "MXP" to "Air Italy",
    )

    fun fromCallsign(callsign: String): String? {
        val c = callsign.trim().uppercase()
        if (c.length < 4 || !c.take(3).all { it.isLetter() } || !c[3].isDigit()) return null
        return names[c.take(3)]
    }
}
