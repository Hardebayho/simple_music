package tech.smallwonder.simplemusic.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tech.smallwonder.simplemusic.LocalModel
import tech.smallwonder.simplemusic.models.MusicModel
import tech.smallwonder.simplemusic.models.Song

class FavoritesRepository {
    private lateinit var context: Context
    val songs = mutableStateOf(listOf<Int>())

    fun init(context: Context) {
        this.context = context
        restore()
    }

    @Composable
    fun getSongs() : List<Song> {
        val model = LocalModel.current
        val _songs = mutableListOf<Song>()

        songs.value.forEach { id ->
            model.songs.value.firstOrNull { it.id == id }?.let {
                _songs.add(it)
            }
        }
        return _songs
    }

    fun getSongs(model: MusicModel) : List<Song> {
        val _songs = mutableListOf<Song>()

        songs.value.forEach { id ->
            model.songs.value.firstOrNull { it.id == id }?.let {
                _songs.add(it)
            }
        }
        return _songs
    }

    fun addSong(song: Song) : Boolean {
        if (songs.value.contains(song.id)) {
            return false
        }
        // 3. Else, add to favorites
        val list = mutableListOf(*songs.value.toTypedArray())
        list.add(song.id)
        songs.value = list

        // 4. Save
        save()

        return true
    }

    fun removeSong(song: Song) : Boolean {
        if (!songs.value.contains(song.id)) {
            return false
        }

        val list = mutableListOf(*songs.value.toTypedArray())
        list.remove(song.id)

        songs.value = list
        save()

        return true
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun save() {
        GlobalScope.launch(Dispatchers.IO) {
            val str = StringBuilder()
            for (i in songs.value) {
                str.append("$i\n")
            }
            context.favoritesDir().writeText(str.toString())
        }
    }

    private fun restore() {
        GlobalScope.launch(Dispatchers.IO) {
            val list = mutableListOf<Int>()
            val str = context.favoritesDir().readText()
            if (str.isEmpty()) return@launch
            val tokens = str.split("\n")
            for (token in tokens) {
                if (token.isEmpty()) continue
                list.add(token.toInt())
            }
            songs.value = list
        }
    }

    fun isInFavorites(song: Song) : Boolean {
        return songs.value.contains(song.id)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        val instance = FavoritesRepository()
    }
}