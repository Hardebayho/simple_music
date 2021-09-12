package tech.smallwonder.simplemusic.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import tech.smallwonder.simplemusic.models.Playlist
import tech.smallwonder.simplemusic.models.Song
import java.lang.StringBuilder

class PlaylistRepository {

    lateinit var context: Context

    val playlists = mutableStateListOf<Playlist>()

    fun init(context: Context, songs: List<Song>) {
        this.context = context
        restore(songs)
    }

    fun updatePlaylist(playlist: Playlist) : Boolean {
        // Update the playlist by removing this one and adding the other one
        if (playlists.isNotEmpty()) {
            for (i in 0..playlists.size) {
                if (playlists[i].title == playlist.title) {
                    playlists.remove(playlists[i])
                    break
                }
            }
        }

        playlists.add(playlist)

        save()
        return true
    }

    fun removePlaylist(playlist: Playlist) : Boolean {
        return playlists.remove(playlist)
    }

    fun exists(title: String) = playlists.find { it.title == title } != null

    private fun save() {
        GlobalScope.launch(Dispatchers.IO) {
            val file = context.playlistsDir()
            val builder = StringBuilder()
            for (playlist in playlists) {
                val b2 = StringBuilder()
                for (song in playlist.songs) {
                    b2.append("${song.id},")
                }
                builder.append("${playlist.title}=${b2}\n")
            }
            file.writeText(builder.toString())
        }
    }

    private fun restore(songs: List<Song>) {
        GlobalScope.launch(Dispatchers.IO) {
            val file = context.playlistsDir()
            val lines = file.readLines()
            for (line in lines) {
                // key=value
                // 1. Split with =
                // 2. Left side is playlist title (trimmed)
                // 3. Right side is values (comma separated)
                val tokens = line.trim().split("=")
                if (tokens.size < 2) continue

                val title = tokens[0].trim()
                val songz = mutableListOf<Song>()

                // 4. Split right side with comma
                val tokens2 = tokens[1].trim().split(",")
                if (tokens2.isEmpty()) continue
                for (token in tokens2) {
                    if (token.trim().isEmpty()) continue
                    // This should be a song ID
                    val id = token.toInt()
                    val song = songs.find {
                        it.id == id
                    }
                    song?.let {
                        songz.add(it)
                    }
                }

                playlists.add(Playlist(title, songz))
            }
        }
    }

    fun renamePlaylist(playlist: Playlist, value: String) : Boolean {
        return if (playlists.contains(playlist)) {
            playlists.remove(playlist)
            playlists.add(playlist.copy(title = value))
            true
        } else false
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        val instance: PlaylistRepository = PlaylistRepository()
    }
}