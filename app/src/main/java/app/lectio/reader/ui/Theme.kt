package app.lectio.reader.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.unit.sp
import app.lectio.reader.R

val Literary = FontFamily(Font(R.font.literata))

internal val LectioLight = lightColorScheme(
    primary=Color(0xFF365F99),onPrimary=Color.White,primaryContainer=Color(0xFFD8E6FF),onPrimaryContainer=Color(0xFF102F57),
    secondary=Color(0xFF526176),onSecondary=Color.White,secondaryContainer=Color(0xFFE0E7F2),onSecondaryContainer=Color(0xFF202E40),
    tertiary=Color(0xFF635C7E),onTertiary=Color.White,tertiaryContainer=Color(0xFFE9E1FF),onTertiaryContainer=Color(0xFF302B4A),
    background=Color(0xFFF6F8FC),onBackground=Color(0xFF191D25),surface=Color(0xFFF6F8FC),onSurface=Color(0xFF191D25),
    surfaceVariant=Color(0xFFE2E7EF),onSurfaceVariant=Color(0xFF414B5A),outline=Color(0xFF707C8D),outlineVariant=Color(0xFFC2CAD6),
    surfaceContainerLowest=Color.White,surfaceContainerLow=Color(0xFFF0F3F9),surfaceContainer=Color(0xFFEAEEF5),
    surfaceContainerHigh=Color(0xFFE3E8F0),surfaceContainerHighest=Color(0xFFDCE2EC),surfaceDim=Color(0xFFD6DDE8),surfaceBright=Color(0xFFF6F8FC),
    inverseSurface=Color(0xFF2B3038),inverseOnSurface=Color(0xFFEDF1F8),inversePrimary=Color(0xFFA8C7FA),surfaceTint=Color(0xFF365F99),
    error=Color(0xFFBA1A1A),onError=Color.White,errorContainer=Color(0xFFFFDAD6),onErrorContainer=Color(0xFF410002),scrim=Color.Black
)
internal val LectioDark = darkColorScheme(
    primary=Color(0xFFA8C7FA),onPrimary=Color(0xFF0A305F),primaryContainer=Color(0xFF243E62),onPrimaryContainer=Color(0xFFD6E5FF),
    secondary=Color(0xFFBBC5D6),onSecondary=Color(0xFF263141),secondaryContainer=Color(0xFF303A49),onSecondaryContainer=Color(0xFFE0E7F2),
    tertiary=Color(0xFFC5BFE4),onTertiary=Color(0xFF302B4A),tertiaryContainer=Color(0xFF47415F),onTertiaryContainer=Color(0xFFE9E1FF),
    background=Color(0xFF101216),onBackground=Color(0xFFE3E7EF),surface=Color(0xFF101216),onSurface=Color(0xFFE3E7EF),
    surfaceVariant=Color(0xFF424B59),onSurfaceVariant=Color(0xFFB5BECC),outline=Color(0xFF8691A2),outlineVariant=Color(0xFF424B59),
    surfaceContainerLowest=Color(0xFF0C0E12),surfaceContainerLow=Color(0xFF171A20),surfaceContainer=Color(0xFF1D2129),
    surfaceContainerHigh=Color(0xFF262B35),surfaceContainerHighest=Color(0xFF303641),surfaceDim=Color(0xFF101216),surfaceBright=Color(0xFF363C47),
    inverseSurface=Color(0xFFE3E7EF),inverseOnSurface=Color(0xFF2B3038),inversePrimary=Color(0xFF365F99),surfaceTint=Color(0xFFA8C7FA),
    error=Color(0xFFFFB4AB),onError=Color(0xFF690005),errorContainer=Color(0xFF93000A),onErrorContainer=Color(0xFFFFDAD6),scrim=Color.Black
)
// Reader-only paper palette; the application keeps its own selected appearance.
internal val LectioPaper = LectioLight.copy(
    primary=Color(0xFF5C5B35),onPrimary=Color(0xFFFFFFFF),primaryContainer=Color(0xFFE7E3B6),onPrimaryContainer=Color(0xFF302F13),
    secondary=Color(0xFF675D46),onSecondary=Color(0xFFFFFFFF),secondaryContainer=Color(0xFFE8DDBA),onSecondaryContainer=Color(0xFF332C1C),
    background=Color(0xFFF6EBCF),onBackground=Color(0xFF302B22),surface=Color(0xFFF6EBCF),onSurface=Color(0xFF302B22),
    surfaceVariant=Color(0xFFE8DDBF),onSurfaceVariant=Color(0xFF5F5644),outline=Color(0xFF7D735B),outlineVariant=Color(0xFFCFC1A2),
    surfaceContainerLowest=Color(0xFFFFF8E8),surfaceContainerLow=Color(0xFFF3E7CA),surfaceContainer=Color(0xFFEEE1C0),
    surfaceContainerHigh=Color(0xFFE9DAB5),surfaceContainerHighest=Color(0xFFE4D4AA),surfaceDim=Color(0xFFE4D4AA),surfaceBright=Color(0xFFF6EBCF),surfaceTint=Color(0xFF5C5B35)
)
val LectioTypography = Typography(
    displaySmall=TextStyle(fontFamily=Literary,fontSize=36.sp,lineHeight=44.sp),
    headlineLarge=TextStyle(fontFamily=Literary,fontSize=32.sp,lineHeight=40.sp),
    headlineMedium=TextStyle(fontFamily=Literary,fontSize=28.sp,lineHeight=36.sp),
    headlineSmall=TextStyle(fontFamily=Literary,fontSize=24.sp,lineHeight=32.sp),
    titleLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=22.sp,lineHeight=28.sp,fontWeight=FontWeight.Medium),
    titleMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=16.sp,lineHeight=24.sp,fontWeight=FontWeight.Medium),
    titleSmall=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=14.sp,lineHeight=20.sp,fontWeight=FontWeight.Medium),
    bodyLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=16.sp,lineHeight=25.sp),
    bodyMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=14.sp,lineHeight=21.sp),
    bodySmall=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=12.sp,lineHeight=18.sp),
    labelLarge=TextStyle(fontSize=14.sp,lineHeight=20.sp,fontWeight=FontWeight.Medium)
)
@Composable fun LectioTheme(theme: String, content: @Composable () -> Unit) {
    val dark=when(theme) { "Oscuro"->true; "Claro"->false;else->isSystemInDarkTheme() }
    val view=LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let {window->
            WindowCompat.getInsetsController(window,view).apply {
                isAppearanceLightStatusBars=!dark
                isAppearanceLightNavigationBars=!dark
            }
        }
    }
    MaterialTheme(colorScheme=if(dark) LectioDark else LectioLight,typography=LectioTypography,content=content)
}
