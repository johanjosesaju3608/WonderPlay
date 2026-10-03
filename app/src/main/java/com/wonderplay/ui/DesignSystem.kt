package com.wonderplay.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wonderplay.domain.AppSettings
import com.wonderplay.domain.ThemeMode
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal object Space {
    val tiny = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val page = 24.dp
    val section = 32.dp
    val large = 48.dp
}
internal object Shape {
    val artwork = RoundedCornerShape(8.dp)
    val control = RoundedCornerShape(14.dp)
    val panel = RoundedCornerShape(22.dp)
}
internal val LocalReducedMotion = staticCompositionLocalOf { false }
private val Dark = darkColorScheme(
    primary = Color(0xFFF1DBC5), onPrimary = Color(0xFF231D17),
    primaryContainer = Color(0xFF383028), onPrimaryContainer = Color(0xFFF1DBC5),
    secondary = Color(0xFFC9AF96), onSecondary = Color(0xFF201B17),
    background = Color(0xFF101010), onBackground = Color(0xFFF2EFE9),
    surface = Color(0xFF181818), onSurface = Color(0xFFF2EFE9),
    surfaceVariant = Color(0xFF262523), onSurfaceVariant = Color(0xFFA7A39D),
    surfaceContainer = Color(0xFF20201F), surfaceContainerHigh = Color(0xFF2B2A28),
    outline = Color(0xFF5A5752), outlineVariant = Color(0xFF33322F),
    error = Color(0xFFE3AD9F), onError = Color(0xFF341913),
)
private val Light = lightColorScheme(
    primary = Color(0xFF6B4930), onPrimary = Color(0xFFFFFAF4),
    primaryContainer = Color(0xFFE8D8C8), onPrimaryContainer = Color(0xFF342519),
    secondary = Color(0xFF725B46), onSecondary = Color.White,
    background = Color(0xFFF5F1EB), onBackground = Color(0xFF24221F),
    surface = Color(0xFFFCF8F2), onSurface = Color(0xFF24221F),
    surfaceVariant = Color(0xFFE5E0D8), onSurfaceVariant = Color(0xFF6E685F),
    surfaceContainer = Color(0xFFEDE7DF), surfaceContainerHigh = Color(0xFFE2DCD3),
    outline = Color(0xFF938A7E), outlineVariant = Color(0xFFD8D0C5),
    error = Color(0xFF924A36), onError = Color.White,
)
private val WonderTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 41.sp, letterSpacing = (-1.4).sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 27.sp, lineHeight = 33.sp, letterSpacing = (-0.6).sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.4).sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 1.3.sp),
)

@Composable
internal fun WonderTheme(settings: AppSettings, artwork: String? = null, content: @Composable () -> Unit) {
    val dark = when (settings.theme) { ThemeMode.DARK -> true; ThemeMode.LIGHT -> false; ThemeMode.SYSTEM -> isSystemInDarkTheme() }
    val view = androidx.compose.ui.platform.LocalView.current
    val context = androidx.compose.ui.platform.LocalContext.current
    SideEffect {
        val activity = context as? android.app.Activity
        if (activity != null) {
            val bars = androidx.core.view.WindowCompat.getInsetsController(activity.window, view)
            bars.isAppearanceLightStatusBars = !dark
            bars.isAppearanceLightNavigationBars = !dark
            if (android.os.Build.VERSION.SDK_INT >= 29) activity.window.isNavigationBarContrastEnforced = false
        }
    }
    CompositionLocalProvider(LocalReducedMotion provides settings.reducedMotion) {
        val base = if (dark) Dark else Light
        val accent = albumAccent(artwork, true, dark)
        val container = androidx.compose.ui.graphics.lerp(base.surface, accent, if(dark) .18f else .12f)
        val scheme = base.copy(primary=accent, primaryContainer=container, onPrimaryContainer=accent, secondary=accent)
        CompositionLocalProvider(LocalPlayerGradient provides listOf(container, base.surface)) {
            MaterialTheme(colorScheme = scheme, typography = WonderTypography, content = content)
        }
    }
}

/** The supplied wonderPlay identity remains coffee on black in both appearance modes. */
@Composable
internal fun WonderMark(modifier: Modifier = Modifier, color: Color = Coffee) {
    Icon(androidx.compose.ui.res.painterResource(com.wonderplay.R.drawable.ic_brand),
        contentDescription="wonderPlay logo", modifier=modifier, tint=color)
}

@Composable
internal fun ArtFallback(seed: String, modifier: Modifier = Modifier) {
    val hash = seed.hashCode()
    val hues = listOf(Color(0xFF695247), Color(0xFF465A58), Color(0xFF575161), Color(0xFF626345), Color(0xFF735C48))
    val tone = hues[(hash and Int.MAX_VALUE) % hues.size]
    Canvas(modifier.background(Brush.linearGradient(listOf(tone, tone.copy(red = tone.red * .3f, green = tone.green * .3f, blue = tone.blue * .3f))))) {
        val radius = size.minDimension * .38f
        val center = Offset(size.width * .5f, size.height * .48f)
        repeat(14) { n ->
            drawArc(Color.White.copy(alpha = .09f + (n % 3) * .018f), 208f, 262f, false,
                topLeft = center - Offset(radius - n * radius / 19, radius - n * radius / 19),
                size = Size((radius - n * radius / 19) * 2, (radius - n * radius / 19) * 2),
                style = Stroke(size.minDimension * .0035f, cap = StrokeCap.Round))
        }
        drawCircle(Color(0xFFE8D9C2).copy(alpha = .65f), size.minDimension * .022f, center)
    }
}

@Composable
internal fun TactileIcon(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    selected: Boolean = false, enabled: Boolean = true, filled: Boolean = false, event: HapticEvent = HapticEvent.TAP) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduced = LocalReducedMotion.current
    val scale by animateFloatAsState(if (pressed && !reduced) .92f else 1f, spring(stiffness = 750f), label = "control press")
    val haptics = LocalWonderHaptics.current
    Box(modifier.size(48.dp).graphicsLayer { scaleX = scale; scaleY = scale }
        .background(if (filled) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)) {
        IconButton(onClick = { haptics.perform(event); onClick() }, enabled = enabled, interactionSource = interaction, modifier = Modifier.fillMaxSize()) {
            Icon(icon, label, tint = if (!enabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .4f)
                else if (filled) MaterialTheme.colorScheme.onPrimary else if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(if (filled) 30.dp else 24.dp))
        }
    }
}
