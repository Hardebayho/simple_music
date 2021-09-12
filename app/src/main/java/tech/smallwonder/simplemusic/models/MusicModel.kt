package tech.smallwonder.simplemusic.models

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.BlendMode.Companion.Screen
import androidx.compose.ui.graphics.Color
import androidx.core.database.getIntOrNull
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.*
import androidx.navigation.NavHostController
import androidx.palette.graphics.Palette
import kotlinx.coroutines.*
import tech.smallwonder.simplemusic.MainActivity
import tech.smallwonder.simplemusic.MusicPlayerService
import tech.smallwonder.simplemusic.R
import tech.smallwonder.simplemusic.Screens
import tech.smallwonder.simplemusic.ui.components.Option
import tech.smallwonder.simplemusic.ui.components.OptionTile
import tech.smallwonder.simplemusic.ui.components.SongsListInfoState
import tech.smallwonder.simplemusic.utils.*

data class Song(
    val id: Int,
    val title: String,
    val artist: String,
    val artistId: Int,
    val album: String,
    val albumId: Int,
    val uri: Uri,
    val trackNo: Int,
)

data class Album(
    val id: Int,
    val title: String,
    val songCount: Int,
    val artist: String,
    val artistId: Int,
)

data class Artist(
    val id: Int,
    val title: String,
    val songCount: Int,
    val albumCount: Int,
)

data class Playlist(
    val title: String,
    val songs: List<Song>,
)

class MusicModelFactory(private val contentResolver: ContentResolver, val service: LiveData<MusicPlayerService?>) : ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MusicModel(contentResolver, service) as T
    }
}

class MusicModel(private val contentResolver: ContentResolver, val _service: LiveData<MusicPlayerService?>) : ViewModel() {
    private val _serviceObserver = Observer<MusicPlayerService?> {
        it?.let {
            it.restore(songs.value)
            it.currentSong.observeForever(_currentSongObserver)
            serviceStarted.value = true
            refresh()
        }
    }

    val serviceStarted = mutableStateOf(false)

    private val _currentSongObserver = Observer<Song?> {
        it?.let {
            viewModelScope.launch(Dispatchers.IO) {
                generatePalette(it)
            }
        }
    }

    init {
        _service.observeForever(_serviceObserver)
    }

    val songs = mutableStateOf<List<Song>>(listOf())

    private val service : MusicPlayerService? get() = _service.value

    val albums = mutableStateOf<List<Album>>(listOf())

    val artists = mutableStateOf<List<Artist>>(listOf())

    /// Whether we're currently refreshing our data
    val refreshing = MutableLiveData(false)

    private val _primaryDark = MutableLiveData(defaultDarkColor)
    val primaryDark: LiveData<Color> get() = _primaryDark

    private val _primaryLight = MutableLiveData(defaultLightColor)
    val primaryLight: LiveData<Color> get() = _primaryLight

    val songsListState = mutableStateOf(SongsListInfoState(listOf(), {}, {}, {}))

    @OptIn(ExperimentalMaterialApi::class)
    val sheetState = ModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val sheetContent = mutableStateOf<@Composable ColumnScope.() -> Unit>({
        OptionTile(Option(Icons.Default.Share, "")) {}
    })

    val sortOrder = MutableLiveData<SortOrder>(SortOrder.Title())

    val isFav = mutableStateOf(false)

    val currentSongFilter = mutableStateOf("")
    val currentAlbumFilter = mutableStateOf("")
    val currentPlaylistFilter = mutableStateOf("")
    val currentArtistFilter = mutableStateOf("")

    @OptIn(ExperimentalMaterialApi::class)
    fun showBottomSheet(scope: CoroutineScope, content: @Composable ColumnScope.() -> Unit) {
        sheetContent.value = content
        scope.launch {
            sheetState.show()
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    fun hideBottomSheet(scope: CoroutineScope) {
        scope.launch {
            sheetState.hide()
            sheetContent.value = {
                OptionTile(Option(Icons.Default.Share, "")) {}
            }
        }
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch(Dispatchers.Main) {
            sortOrder.value = order
            songs.value = order.sort(songs.value, true)
        }
    }

    fun songsSortGroup() : Map<String, List<Song>> {
        val songsToGroup = songs.value.filter {
            val query = currentSongFilter.value.trim().lowercase()
            currentSongFilter.value.trim().isEmpty()
                    || it.title.lowercase().contains(query)
                    || it.album.lowercase().contains(query)
                    || it.artist.lowercase().contains(query)
        }
        return sortOrder.value!!.groupBy(songsToGroup)
    }

    fun filteredAlbums() : List<Album> {
        val filter = currentAlbumFilter.value.trim().lowercase()
        if (filter.isEmpty()) return albums.value
        return albums.value.filter { it.title.lowercase().contains(filter) || it.artist.lowercase().contains(filter) }
    }

    fun filteredArtists() : List<Artist> {
        val filter = currentArtistFilter.value.trim().lowercase()
        if (filter.isEmpty()) return artists.value
        return artists.value.filter { it.title.lowercase().contains(filter) }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            loadData()
        }
    }

    fun playSongsSong(song: Song) {
        val index = songs.value.indexOf(song)
        if (index < 0) return

        service?.apply {
            setPlaylist(songs.value, index)
            setPlaying(true)
        }
    }

    fun playPlaylistSong(song: Song) {
        val index = playlist.value.indexOf(song)
        if (index < 0) return

        service?.apply {
            setPlaylist(playlist.value, index)
            setPlaying(true)
        }
    }

    fun playAlbumSongs(album: Album, index: Int) {
        // Get the album songs
        val values = songs.value.filter { it.albumId == album.id }.sortedBy { it.trackNo }
        service?.apply {
            setPlaylist(values, index)
            setPlaying(true)
        }
    }

    fun playListSong(songs: List<Song>, song: Song) {
        if (!songs.contains(song)) return
        service?.apply {
            setPlaylist(songs, songs.indexOf(song))
            setPlaying(true)
        }
    }

    fun playArtistSongs(artist: Artist, index: Int) {
        val values = songs.value.filter { it.artistId == artist.id }.sortedBy { it.trackNo }
        service?.apply {
            setPlaylist(values, index)
            setPlaying(true)
        }
    }

    fun stop() {
        service?.apply {
            stop()
        }
    }

    val isPlaying get() = service!!.isPlaying

    val currentSong get() = service!!.currentSong
    val playlist get() = service!!.playlist

    val currentTime get() = service!!.currentTime
    val duration get() = service!!.duration

    fun next() {
        service?.apply {
            next()
        }
    }

    fun prev() {
        service?.apply {
            prev()
        }
    }

    fun setPlaying(playing: Boolean) {
        service?.apply {
            setPlaying(playing)
        }
    }

    fun seek(position: Int) {
        service?.apply {
            seek(position)
        }
    }

    private fun loadData() {
        // Songs
        refreshing.postValue(true)

        val songs = sortOrder.value!!.sort(loadSongs(), true)

        // Refresh playlist repo here
        PlaylistRepository.instance.init(MainActivity.appContext!!, songs)

        val albums = loadAlbums()
        val artists = loadArtists()

        this.songs.value = songs
        this.albums.value = albums.sortedBy { it.title }
        this.artists.value = artists.sortedBy { it.title }

        refreshing.postValue(false)
    }

    private fun loadSongs() : List<Song> {
        val songs = mutableListOf<Song>()

        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.TRACK,
            ),
            null,
            null,
            null,
        )

        fun Cursor.getStringOrDefault(index: Int, default: String = "") : String {
            if (index < 0) return default
            return getString(index)
        }

        cursor?.apply {
            if (moveToFirst()) {
                do {
                    val idIndex = getColumnIndex(MediaStore.Audio.Media._ID)
                    val titleIndex = getColumnIndex(MediaStore.Audio.Media.TITLE)
                    val artistIndex = getColumnIndex(MediaStore.Audio.Media.ARTIST)
                    val artistIdIndex = getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)
                    val albumIndex = getColumnIndex(MediaStore.Audio.Media.ALBUM)
                    val albumIdIndex = getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                    val trackNoIndex = getColumnIndex(MediaStore.Audio.Media.TRACK)

                    if (idIndex >= 0) {
                        val id = getInt(idIndex)
                        val title = getStringOrDefault(titleIndex, "Unknown Title")
                        val artist = getStringOrDefault(artistIndex, "Unknown Artist")
                        val artistId = getInt(artistIdIndex)
                        val album = getStringOrDefault(albumIndex, "Unknown Album")
                        val albumId = getInt(albumIdIndex)
                        val trackNo = getIntOrNull(trackNoIndex) OR 0

                        val song = Song(id, title, artist, artistId, album, albumId, ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toLong()), trackNo)

                        songs.add(song)
                    }
                } while (moveToNext())
            }
            close()
        }

        return songs
    }

    private fun loadAlbums() : List<Album> {
        val albums = mutableListOf<Album>()

        val cursor = contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ARTIST,
            "artist_id",
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
        ), null, null, null)

        fun Cursor.getStringOrDefault(index: Int, default: String = "") : String {
            if (index < 0) return default
            return getString(index)
        }

        cursor?.apply {
            if (moveToFirst()) {
                do {
                    val idIndex = getColumnIndex(MediaStore.Audio.Albums._ID)
                    val titleIndex = getColumnIndex(MediaStore.Audio.Albums.ALBUM)
                    val artistIndex = getColumnIndex(MediaStore.Audio.Albums.ARTIST)
                    val artistIdIndex = getColumnIndex("artist_id")
                    val numberOfSongsIndex = getColumnIndex(MediaStore.Audio.Albums.NUMBER_OF_SONGS)

                    if (idIndex >= 0) {
                        val id = getInt(idIndex)
                        val title = getStringOrDefault(titleIndex, "Unknown Title")
                        val artist = getStringOrDefault(artistIndex, "Unknown Artist")
                        val artistId = getInt(artistIdIndex)
                        val numberOfSongs = getInt(numberOfSongsIndex)

                        val song = Album(id, title, numberOfSongs, artist, artistId)

                        albums.add(song)
                    }
                } while (moveToNext())
            }
            close()
        }

        return albums.sortedBy {
            it.title
        }
    }

    private fun loadArtists() : List<Artist> {
        val artists = mutableListOf<Artist>()

        val cursor = contentResolver.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
        ), null, null, null)

        fun Cursor.getStringOrDefault(index: Int, default: String = "") : String {
            if (index < 0) return default
            return getString(index)
        }

        cursor?.apply {
            if (moveToFirst()) {
                do {
                    val idIndex = getColumnIndex(MediaStore.Audio.Artists._ID)
                    val titleIndex = getColumnIndex(MediaStore.Audio.Artists.ARTIST)
                    val numberOfAlbumsIndex = getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)
                    val numberOfSongsIndex = getColumnIndex(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)

                    if (idIndex >= 0) {
                        val id = getInt(idIndex)
                        val title = getStringOrDefault(titleIndex, "Unknown Artist")
                        val numberOfAlbums = getInt(numberOfAlbumsIndex)
                        val numberOfSongs = getInt(numberOfSongsIndex)

                        val artist = Artist(id, title, numberOfSongs, numberOfAlbums)

                        artists.add(artist)
                    }
                } while (moveToNext())
            }
            close()
        }

        return artists.sortedBy {
            it.title
        }
    }

    private fun generatePalette(song: Song) {
        var valid = false
        ThumbUtils.loadArt(song.uri, song.id)?.let {
            BitmapFactory.decodeFile(it)?.let { bitmap ->
                valid = true
                doBitmap(bitmap)
            }
        }
        if (!valid) {
            BitmapFactory.decodeResource(MainActivity.appContext!!.resources, R.drawable.default_album_art)?.let { bitmap ->
                doBitmap(bitmap)
            }
        }
    }

    private fun doBitmap(bitmap: Bitmap) {
        val builder = Palette.Builder(bitmap)
        val palette = builder.generate()

        val swatch = palette.dominantSwatch

		swatch?.let { swatch ->
			val darkColor = Color(swatch.rgb).darker()
		    val lightColor = Color(palette.lightMutedSwatch!!.rgb)

		    _primaryDark.postValue(darkColor)
		    _primaryLight.postValue(lightColor)
		}
    }

    override fun onCleared() {
        super.onCleared()
        _service.removeObserver(_serviceObserver)
        _service.value!!.currentSong.removeObserver(_currentSongObserver)
    }

    val shuffle get() = _service.value!!.shuffle
    val repeatMode get() = _service.value!!.repeatMode

    fun toggleShuffle() {
        _service.value!!.setShuffle(!_service.value!!.shuffle.value)
    }

    fun toggleRepeat() {
        _service.value!!.toggleRepeatMode()
    }

    fun toggleFavorite(song: Song) {
        if (FavoritesRepository.instance.isInFavorites(song)) {
            FavoritesRepository.instance.removeSong(song)
        } else {
            FavoritesRepository.instance.addSong(song)
        }
    }

    fun addToFavorites(song: Song) = FavoritesRepository.instance.addSong(song)

    @SuppressLint("QueryPermissionsNeeded")
    fun shareFile(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val doc = DocumentFile.fromSingleUri(context, uri)
        intent.type = doc!!.type
        val chooserIntent = Intent.createChooser(intent, null)
        val resInfoList: List<ResolveInfo> = context
            .packageManager.queryIntentActivities(chooserIntent, PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            context
                .grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun showToast(text: String, length: Int) {
        MainActivity.appContext?.apply {
            showToast(text, length)
        }
    }

    fun onSongOptionSelected(option: Option, song: Song, scope: CoroutineScope) {
        when (option.title) {
            SHARE -> shareFile(MainActivity.appContext!!, song.uri)
            ADD_TO_FAVORITES -> if (addToFavorites(song)) {
                showToast("Song added to favorites", Toast.LENGTH_LONG)
            } else {
                showToast("Song is already in favorites!", Toast.LENGTH_LONG)
            }
            ADD_TO_PLAYLIST -> {
                showBottomSheet(scope) {
                    val playlists = remember { PlaylistRepository.instance.playlists }
                    for (playlist in playlists) {
                        OptionTile(option = Option(Icons.Default.PlaylistAddCheck, playlist.title)) {
                            hideBottomSheet(scope)
                            if (playlist.songs.contains(song)) {
                                showToast("Song is already in playlist!", Toast.LENGTH_LONG)
                                return@OptionTile
                            }
                            val pl = playlist.copy(songs = listOf(song, *playlist.songs.toTypedArray()))
                            if (PlaylistRepository.instance.updatePlaylist(pl)) {
                                showToast("Song added to playlist", Toast.LENGTH_LONG)
                            } else {
                                showToast("Unable to add song to playlist!", Toast.LENGTH_LONG)
                            }
                        }
                    }
                }
            }
        }
    }

    fun onPlaylistSongOptionSelected(playlist: Playlist, song: Song, option: Option) {
        if (playlist.songs.contains(song)) {
            when (option.title) {
                REMOVE_FROM_PLAYLIST -> {
                    val songs = mutableListOf(*playlist.songs.toTypedArray())
                    songs.remove(song)
                    val pl = playlist.copy(songs = songs)
                    PlaylistRepository.instance.updatePlaylist(pl)
                    songsListState.value = songsListState.value.copy(songs = pl.songs)
                }
            }
        }
    }

    fun onFavoriteSongOptionSelected(option: Option, song: Song) {
        if (FavoritesRepository.instance.isInFavorites(song)) {
            when (option.title) {
                REMOVE_FROM_PLAYLIST -> {
                    FavoritesRepository.instance.removeSong(song)
                    songsListState.value = songsListState.value.copy(songs = FavoritesRepository.instance.getSongs(this))
                }
            }
        }
    }

    val properLight: Color get() = if (_primaryLight.value!!.isDark()) _primaryLight.value!!.invert() else _primaryLight.value!!

    companion object {
        val defaultDarkColor = Color(23, 24, 28)
        val defaultLightColor = Color(37, 50, 57)
        private const val ADD_TO_PLAYLIST = "Add To Playlist"
        private const val DELETE = "Delete"
        private const val SHARE = "Share"
        private const val ADD_TO_FAVORITES = "Add To Favorites"
        private const val REMOVE_FROM_PLAYLIST = "Remove from playlist"

        val songOptions = listOf(
            Option(Icons.Default.Share, SHARE),
            Option(Icons.Default.PlaylistAdd, ADD_TO_PLAYLIST),
            Option(Icons.Default.Favorite, ADD_TO_FAVORITES),
        )

        val playlistOptions = listOf(
            Option(Icons.Default.Info, REMOVE_FROM_PLAYLIST)
        )

        val favoriteOptions = listOf(
            Option(Icons.Default.Info, REMOVE_FROM_PLAYLIST)
        )
    }
}
