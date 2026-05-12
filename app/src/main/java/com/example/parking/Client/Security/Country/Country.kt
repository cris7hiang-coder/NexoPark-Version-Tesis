package com.example.parking.Client.Security.Country

val countries = listOf(
    Country("US", "United States", "+1", "🇺🇸", 10, 10),
    Country("CA", "Canada", "+1", "🇨🇦", 10, 10),
    Country("GB", "United Kingdom", "+44", "🇬🇧", 10, 10),
    Country("ES", "Spain", "+34", "🇪🇸", 9, 9),
    Country("MX", "Mexico", "+52", "🇲🇽", 10, 10),
    Country("AR", "Argentina", "+54", "🇦🇷", 10, 10),
    Country("BO", "Bolivia", "+591", "🇧🇴", 8, 8, validStart = listOf('6','7')),
    Country("BR", "Brazil", "+55", "🇧🇷", 10, 11),
    Country("CL", "Chile", "+56", "🇨🇱", 9, 9),
    Country("CO", "Colombia", "+57", "🇨🇴", 10, 10),
    Country("EC", "Ecuador", "+593", "🇪🇨", 9, 9),
    Country("PE", "Peru", "+51", "🇵🇪", 9, 9),
    Country("PY", "Paraguay", "+595", "🇵🇾", 9, 9),
    Country("UY", "Uruguay", "+598", "🇺🇾", 8, 8),
    Country("VE", "Venezuela", "+58", "🇻🇪", 10, 10)
)
data class Country(
    val code: String,
    val name: String,
    val dialCode: String,
    val flagEmoji: String,
    val minLength: Int,
    val maxLength: Int,
    val validStart: List<Char> = emptyList() // caracteres válidos iniciales opcional

)
