package tech.smallwonder.simplemusic.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.smallwonder.simplemusic.MainActivity
import java.io.File

val Context.thumbDir : File
    get() = File(filesDir, "albumArts").apply { mkdirs() }

object ThumbUtils {
    /// Loads the thumbnail and caches it on the device storage
    @Composable
    fun loadAlbumArt(url: Uri, id: Int) : State<String?> {
        return produceState<String?>(initialValue = null, id) {
            var res: String?
            withContext(Dispatchers.IO) {
                res = loadArt(url, id)
            }
            value = res
        }
    }

    fun loadArt(url: Uri, id: Int): String? {
        val path = File(MainActivity.appContext!!.thumbDir, id.toString())
        if (path.exists()) {
            return path.absolutePath
        }

        return MediaMetadataRetriever().runCatching {
            setDataSource(MainActivity.appContext!!, url)
            val pic = embeddedPicture
            if (pic != null) {
                path.writeBytes(pic)
                path.absolutePath
            } else {
                null
            }
        }.getOrNull()
    }
}