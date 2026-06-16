package com.nousresearch.hermes.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.nousresearch.hermes.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = R.array.com_google_android_gms_fonts_certs,
)

private val InterFont = GoogleFont("Inter")

val InterFamily = FontFamily(
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = InterFont, fontProvider = provider, weight = FontWeight.Bold),
)

private val JetBrainsMonoFont = GoogleFont("JetBrains Mono")
val MonoFamily = FontFamily(
    Font(googleFont = JetBrainsMonoFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = JetBrainsMonoFont, fontProvider = provider, weight = FontWeight.Medium),
)

val HermesTypography = Typography(
    displayLarge  = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Bold,     fontSize = 57.sp,  lineHeight = 64.sp,  letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Bold,     fontSize = 45.sp,  lineHeight = 52.sp),
    displaySmall  = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 36.sp,  lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 32.sp,  lineHeight = 40.sp),
    headlineMedium= TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 28.sp,  lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp,  lineHeight = 32.sp),
    titleLarge    = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp,  lineHeight = 28.sp),
    titleMedium   = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium,   fontSize = 16.sp,  lineHeight = 24.sp,  letterSpacing = 0.15.sp),
    titleSmall    = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium,   fontSize = 14.sp,  lineHeight = 20.sp,  letterSpacing = 0.1.sp),
    bodyLarge     = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal,   fontSize = 16.sp,  lineHeight = 26.sp,  letterSpacing = 0.15.sp),
    bodyMedium    = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal,   fontSize = 14.sp,  lineHeight = 22.sp,  letterSpacing = 0.25.sp),
    bodySmall     = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Normal,   fontSize = 12.sp,  lineHeight = 18.sp,  letterSpacing = 0.4.sp),
    labelLarge    = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium,   fontSize = 14.sp,  lineHeight = 20.sp,  letterSpacing = 0.1.sp),
    labelMedium   = TextStyle(fontFamily = InterFamily, fontWeight = FontWeight.Medium,   fontSize = 12.sp,  lineHeight = 16.sp,  letterSpacing = 0.5.sp),
    labelSmall    = TextStyle(fontFamily = MonoFamily,  fontWeight = FontWeight.Normal,   fontSize = 11.sp,  lineHeight = 16.sp,  letterSpacing = 0.5.sp),
)
