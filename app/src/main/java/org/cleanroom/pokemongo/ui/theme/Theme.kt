package org.cleanroom.pokemongo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PokemonBlue,
    secondary = PokemonYellow,
    tertiary = AccentCyan,
    background = DarkBackground,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onSecondary = PokeDarkBlue,
    onBackground = TextWhite,
    onSurface = TextWhite
)

@Composable
fun PokemonGoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
