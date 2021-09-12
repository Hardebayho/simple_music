package tech.smallwonder.simplemusic.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import tech.smallwonder.simplemusic.R

private val lobster = Font(R.font.lobster)

private val yksLight = Font(R.font.yks_light, FontWeight.W300)
private val yksExtraLight = Font(R.font.yks_extralight, FontWeight.W100)
private val yksRegular = Font(R.font.yks_regular, FontWeight.W400)
private val yksBold = Font(R.font.yks_bold, FontWeight.W700)
private val yksSemiBold = Font(R.font.yks_semi_bold, FontWeight.W600)
private val yksMedium = Font(R.font.yks_medium, FontWeight.W500)

val LobsterFontFamily = FontFamily(lobster)
val YKSFontFamily = FontFamily(yksLight, yksExtraLight, yksRegular, yksBold, yksSemiBold, yksMedium)

// Set of Material typography styles to start with
val Typography = Typography(
    defaultFontFamily = YKSFontFamily,
    body1 = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
    )
    /* Other default text styles to override
    button = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp
    ),
    caption = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    )
    */
)