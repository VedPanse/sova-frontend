package org.sova.design

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import sova.composeapp.generated.resources.Res
import sova.composeapp.generated.resources.manrope_medium
import sova.composeapp.generated.resources.manrope_regular
import sova.composeapp.generated.resources.newsreader_italic
import sova.composeapp.generated.resources.newsreader_regular
import sova.composeapp.generated.resources.newsreader_semibold_italic

@Composable
fun healthTypography(): Typography {
    val newsreader = FontFamily(
        Font(Res.font.newsreader_regular, weight = FontWeight.Normal, style = FontStyle.Normal),
        Font(Res.font.newsreader_italic, weight = FontWeight.Normal, style = FontStyle.Italic),
        Font(Res.font.newsreader_semibold_italic, weight = FontWeight.SemiBold, style = FontStyle.Italic),
    )
    val manrope = FontFamily(
        Font(Res.font.manrope_regular, weight = FontWeight.Normal),
        Font(Res.font.manrope_medium, weight = FontWeight.Medium),
    )

    return Typography(
        headlineLarge = TextStyle(
            fontFamily = newsreader,
            fontSize = 40.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = newsreader,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = manrope,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = manrope,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = manrope,
            fontSize = 7.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
        ),
    )
}
