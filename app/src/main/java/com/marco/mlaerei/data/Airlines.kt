package com.marco.mlaerei.data

data class AirlineInfo(val name: String, val iata: String?)

/** Compagnie dal prefisso ICAO (3 lettere) del nominativo di volo. L'IATA serve per scaricare il logo. */
object Airlines {
    private val table: Map<String, AirlineInfo> = listOf(
        "RYR" to "Ryanair|FR", "RUK" to "Ryanair UK|RK", "EZY" to "easyJet|U2", "EJU" to "easyJet Europe|EC",
        "EZS" to "easyJet Switzerland|DS", "WZZ" to "Wizz Air|W6", "WMT" to "Wizz Air Malta|W4",
        "DLH" to "Lufthansa|LH", "CLH" to "Lufthansa CityLine|CL", "EWG" to "Eurowings|EW",
        "ITY" to "ITA Airways|AZ", "BAW" to "British Airways|BA", "AFR" to "Air France|AF", "KLM" to "KLM|KL",
        "TRA" to "Transavia|HV", "TVF" to "Transavia France|TO", "VLG" to "Vueling|VY", "IBE" to "Iberia|IB",
        "IBS" to "Iberia Express|I2", "AEA" to "Air Europa|UX", "NOZ" to "Norwegian|DY", "NAX" to "Norwegian|DY",
        "SAS" to "SAS|SK", "SWR" to "Swiss|LX", "EDW" to "Edelweiss|WK", "AUA" to "Austrian|OS",
        "BEL" to "Brussels Airlines|SN", "TAP" to "TAP Air Portugal|TP", "LOT" to "LOT Polish Airlines|LO",
        "FIN" to "Finnair|AY", "THY" to "Turkish Airlines|TK", "PGT" to "Pegasus|PC", "UAE" to "Emirates|EK",
        "QTR" to "Qatar Airways|QR", "ETD" to "Etihad|EY", "ELY" to "El Al|LY", "MSR" to "EgyptAir|MS",
        "RAM" to "Royal Air Maroc|AT", "DAL" to "Delta|DL", "UAL" to "United|UA", "AAL" to "American Airlines|AA",
        "ACA" to "Air Canada|AC", "SIA" to "Singapore Airlines|SQ", "CPA" to "Cathay Pacific|CX",
        "ANA" to "ANA|NH", "JAL" to "Japan Airlines|JL", "KAL" to "Korean Air|KE", "AIC" to "Air India|AI",
        "ETH" to "Ethiopian|ET", "SAA" to "South African|SA", "TSC" to "Air Transat|TS",
        "VIR" to "Virgin Atlantic|VS", "EIN" to "Aer Lingus|EI", "ICE" to "Icelandair|FI",
        "NJE" to "NetJets Europe|", "BCS" to "European Air Transport (DHL)|QY", "UPS" to "UPS|5X",
        "FDX" to "FedEx|FX", "GEC" to "Lufthansa Cargo|", "CLX" to "Cargolux|CV", "ABR" to "ASL Airlines|",
        "BOX" to "AeroLogic|", "TAY" to "ASL/TNT|3V", "VOE" to "Volotea|V7", "BTI" to "airBaltic|BT",
        "CTN" to "Croatia Airlines|OU", "ASL" to "Air Serbia|JU", "ROT" to "Tarom|RO", "AEE" to "Aegean|A3",
        "SEH" to "Sky Express|GQ", "LDM" to "Laudamotion|OE", "LDA" to "Lauda Europe|OE", "DLA" to "Air Dolomiti|EN",
        "ENT" to "Enter Air|E4", "SXS" to "SunExpress|XQ", "TUI" to "TUI Airways|BY", "TOM" to "TUI Airways|BY",
        "TFL" to "TUI fly Netherlands|OR", "CND" to "Corendon|XC", "JAF" to "TUI fly Belgium|TB",
        "SKV" to "Sky Alps|", "NOS" to "Neos|NO", "EXS" to "Jet2|LS", "JST" to "Jetstar|JQ", "QFA" to "Qantas|QF",
        "ANZ" to "Air New Zealand|NZ", "AFL" to "Aeroflot|SU", "CCA" to "Air China|CA", "CES" to "China Eastern|MU",
        "CSN" to "China Southern|CZ", "HVN" to "Vietnam Airlines|VN", "THA" to "Thai Airways|TG",
        "MAS" to "Malaysia Airlines|MH", "GIA" to "Garuda Indonesia|GA", "PIA" to "Pakistan Intl|PK",
        "SVA" to "Saudia|SV", "KAC" to "Kuwait Airways|KU", "GFA" to "Gulf Air|GF", "OMA" to "Oman Air|WY",
        "AMX" to "Aeromexico|AM", "AVA" to "Avianca|AV", "LAN" to "LATAM|LA", "TAM" to "LATAM Brasil|JJ",
        "ARG" to "Aerolineas Argentinas|AR", "CMP" to "Copa|CM", "AZU" to "Azul|AD", "GLO" to "Gol|G3",
        "WJA" to "WestJet|WS", "ASA" to "Alaska Airlines|AS", "JBU" to "JetBlue|B6", "SWA" to "Southwest|WN",
        "NKS" to "Spirit|NK", "FFT" to "Frontier|F9", "HAL" to "Hawaiian|HA", "VOZ" to "Virgin Australia|VA",
    ).associate { (k, v) ->
        val (name, iata) = v.split("|")
        k to AirlineInfo(name, iata.ifBlank { null })
    }

    fun info(callsign: String): AirlineInfo? {
        val c = callsign.trim().uppercase()
        if (c.length < 4 || !c.take(3).all { it.isLetter() } || !c[3].isDigit()) return null
        return table[c.take(3)]
    }

    fun fromCallsign(callsign: String): String? = info(callsign)?.name

    fun logoUrl(iata: String): String = "https://images.kiwi.com/airlines/64/$iata.png"
}
