package tech.smallwonder.simplemusic.utils

import android.content.Context
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import java.io.File

fun Context.favoritesDir() : File {
    val file = File(filesDir, "favorites.txt")
    if (!file.exists()) file.createNewFile()
    return file
}

fun Context.playlistsDir() : File {
    val file = File(filesDir, "playlists.txt")
    if (!file.exists()) file.createNewFile()
    return file
}

fun Context.showToast(text: String, duration: Int) {
    Toast.makeText(this, text, duration).show()
}

fun String?.getOrAny(value: Any) : Any {
    return this?: value
}

infix fun <T> T?.OR(value: T) : T {
    return this?: value
}

fun Color.manipulate(factor: Float) : Color {
    return Color(
        (red * factor).coerceIn(0f, 1f),
        (green * factor).coerceIn(0f, 1f),
        (blue * factor).coerceIn(0f, 1f),
        alpha
    )
}

fun Color.darker() : Color {
    return manipulate(0.5f)
}

fun Color.invert() : Color {
    return Color(
        1.0f - red,
        1.0f - green,
        1.0f - blue,
    )
}

fun Color.isDark() : Boolean {
    val red = red * 255
    val green = green * 255
    val blue = blue * 255

    val darkness = 1 - (0.299 * red + 0.587 * green + 0.114 * blue) / 255

    return darkness >= 0.5
}

fun Color.darkness(): Double {
    val red = red * 255
    val green = green * 255
    val blue = blue * 255

    return 1 - (0.299 * red + 0.587 * green + 0.114 * blue) / 255
}