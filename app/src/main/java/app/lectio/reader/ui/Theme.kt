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

private val Light = lightColorScheme(
    primary=Color(0xFF24483F),onPrimary=Color(0xFFFFFFFF),primaryContainer=Color(0xFFE0E9DE),onPrimaryContainer=Color(0xFF213F34),
    secondary=Color(0xFF77513B),secondaryContainer=Color(0xFFF0E1D0),onSecondaryContainer=Color(0xFF553725),
    background=Color(0xFFF8F5EF),onBackground=Color(0xFF242B26),surface=Color(0xFFF8F5EF),onSurface=Color(0xFF242B26),
    surfaceVariant=Color(0xFFEDE9E0),onSurfaceVariant=Color(0xFF62665D),outline=Color(0xFF797C71),outlineVariant=Color(0xFFDCDDD2),
    surfaceContainer=Color(0xFFEFECE4),surfaceContainerLow=Color(0xFFF3F0E9),surfaceContainerHigh=Color(0xFFE9E6DD)
)
private val Dark = darkColorScheme(
    primary=Color(0xFFB2D0BB),onPrimary=Color(0xFF17392F),primaryContainer=Color(0xFF304E42),onPrimaryContainer=Color(0xFFD5E9DA),
    secondary=Color(0xFFE4B99A),secondaryContainer=Color(0xFF543E30),onSecondaryContainer=Color(0xFFF4DCC9),
    background=Color(0xFF191D19),onBackground=Color(0xFFE5E6DC),surface=Color(0xFF191D19),onSurface=Color(0xFFE5E6DC),
    surfaceVariant=Color(0xFF2B302A),onSurfaceVariant=Color(0xFFB5BBAD),outline=Color(0xFF8F9688),outlineVariant=Color(0xFF41483E),
    surfaceContainer=Color(0xFF242A23),surfaceContainerLow=Color(0xFF20251F),surfaceContainerHigh=Color(0xFF30362E)
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
    MaterialTheme(colorScheme=if(dark) Dark else Light,typography=LectioTypography,content=content)
}
