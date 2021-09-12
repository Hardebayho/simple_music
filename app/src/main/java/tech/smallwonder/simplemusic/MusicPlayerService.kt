package tech.smallwonder.simplemusic

import android.app.*
import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata.*
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.*
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import tech.smallwonder.simplemusic.models.Song
import tech.smallwonder.simplemusic.utils.OR
import tech.smallwonder.simplemusic.utils.RepeatMode
import tech.smallwonder.simplemusic.utils.ThumbUtils

class MusicPlayerService : Service(), MediaPlayer.OnCompletionListener,
    AudioManager.OnAudioFocusChangeListener {

    private val player: MediaPlayer by lazy { MediaPlayer() }
    val playlist = mutableStateOf<List<Song>>(listOf())
    private val shuffledPlaylist = mutableListOf<Song>()

    private val session by lazy { MediaSession(this, "SimplePlayerSession") }
    private val playbackState by lazy { PlaybackState.Builder() }
    private val metadata by lazy { Builder() }

    val shuffle = mutableStateOf(false)

    val repeatMode = mutableStateOf(RepeatMode.All)

    private val headsetPlugReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // We need to do something if headset is removed. For now, I'll just call pause
            // TODO: Do something else with this and figure out whether it's plugged in or out
            pause()
        }
    }

    fun setShuffle(shuffleEnabled: Boolean) {
        if (shuffleEnabled) {
            shuffledPlaylist.clear()
            shuffledPlaylist.addAll(pl)
            shuffledPlaylist.shuffle()
        } else {
            shuffledPlaylist.clear()
        }
        shuffle.value = shuffleEnabled
        save()
    }

    fun toggleRepeatMode() {
        var currentMode = repeatMode.value!!.ordinal
        if (++currentMode >= RepeatMode.values().size) {
            currentMode = 0
        }

        repeatMode.value = RepeatMode.values()[currentMode]
        save()
    }

    private lateinit var focusRequest: AudioFocusRequest

    private var useShuffle = false

    private var currentIndex: Int = -1
        set(value) {
            val playlist = if (shuffle.value && useShuffle) shuffledPlaylist else pl

            if (value >= 0 && value < playlist.size) {
                field = value
                currentSong.value = playlist[value]
                initPlayer()
            }
        }

    // I have to do this because I know for sure that playlist will never be null!
    // This saves me from having to type "_playlist.value!!" 5000 times
    private val pl: List<Song> get() = playlist.value
    private var canPlay: Boolean = false

    val currentSong = MutableLiveData<Song?>(null)

    val currentTime = mutableStateOf(0)

    val duration = mutableStateOf(0)

    private val timeHandler = Handler(Looper.getMainLooper())

    var isPlaying = mutableStateOf(false)

    fun setPlaying(playing: Boolean) {
        if (playing) {
            play()
        } else {
            pause()
        }
    }

    fun setPlaylist(playlist: List<Song>, index: Int) {
        useShuffle = false
        this.playlist.value = playlist
        currentIndex = index
        useShuffle = true
    }

    private fun initPlayer() {
        val serv = this
        canPlay = currentSong.value?.runCatching {
            player.reset()
            player.setDataSource(serv, uri)
            player.prepare()
            duration.value = player.duration
        }?.isSuccess ?: false
    }

    private fun play() {
        if (canPlay) {
            val manager = getSystemService(AUDIO_SERVICE) as AudioManager
            val res = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.requestAudioFocus(focusRequest)
            } else {
                manager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            }

            if (res != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Toast.makeText(this, "Cannot acquire audio focus! Please try again", Toast.LENGTH_SHORT).show()
                return
            }

            player.start()
            isPlaying.value = player.isPlaying
            startTiming()
            updateNotification()

            save()
        }
    }

    private fun pause(temp: Boolean = false) {
        if (canPlay && player.isPlaying) {
            tempPause = false
            player.pause()

            if (!temp) {
                val manager = getSystemService(AUDIO_SERVICE) as AudioManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    manager.abandonAudioFocusRequest(focusRequest)
                } else {
                    manager.abandonAudioFocus(this)
                }
            }

            isPlaying.value = player.isPlaying

            updateNotification()
            save()
        }
    }

    fun seek(time: Int) {
        player.seekTo(time)
    }

    fun next() {
        val playlist = if (shuffle.value!!) shuffledPlaylist else pl
        var index = currentIndex
        if (++index >= playlist.size) {
            index = 0
        }

        currentIndex = index
        play()
    }

    fun prev() {
        var index = currentIndex
        if (--index < 0) {
            index = pl.size - 1
        }

        currentIndex = index
        play()
    }

    private fun startTiming() {
        timeHandler.postDelayed({
            if (player.isPlaying) {
                currentTime.value = player.currentPosition
                save()
                startTiming()
            }
        }, 200)
    }

    private val CHANNEL_ID = "simple_media_player"

    private fun updateNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Media Notification",
                    NotificationManager.IMPORTANCE_LOW
                )
                manager.createNotificationChannel(channel)
            }
        }

        CoroutineScope(IO).launch {
            currentSong.value?.let {
                val art = ThumbUtils.loadArt(it.uri, it.id)
                var bitmap: Bitmap? = null
                art?.let { art ->
                    bitmap = BitmapFactory.decodeFile(art)
                }

                if (bitmap == null) {
                    bitmap = BitmapFactory.decodeResource(resources, R.drawable.default_album_art)
                }

                val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Notification.Builder(this@MusicPlayerService, CHANNEL_ID)
                } else {
                    Notification.Builder(this@MusicPlayerService)
                }

                val clickIntent = PendingIntent.getActivity(
                    this@MusicPlayerService, 103, Intent(
                        this@MusicPlayerService,
                        MainActivity::class.java
                    ).setAction("OPEN_NOW_PLAYING"), PendingIntent.FLAG_IMMUTABLE
                )

                val prevIntent = PendingIntent.getService(this@MusicPlayerService, 101, Intent(this@MusicPlayerService, MusicPlayerService::class.java).setAction("SKIP_PREVIOUS"), PendingIntent.FLAG_IMMUTABLE)
                val nextIntent = PendingIntent.getService(this@MusicPlayerService, 102, Intent(this@MusicPlayerService, MusicPlayerService::class.java).setAction("SKIP_NEXT"), PendingIntent.FLAG_IMMUTABLE)
                val playIntent = PendingIntent.getService(this@MusicPlayerService, 103, Intent(this@MusicPlayerService, MusicPlayerService::class.java).setAction("PLAY"), PendingIntent.FLAG_IMMUTABLE)
                val pauseIntent = PendingIntent.getService(this@MusicPlayerService, 104, Intent(this@MusicPlayerService, MusicPlayerService::class.java).setAction("PAUSE"), PendingIntent.FLAG_IMMUTABLE)
                val closeIntent = PendingIntent.getService(this@MusicPlayerService, 105, Intent(this@MusicPlayerService, MusicPlayerService::class.java).setAction("CLOSE"), PendingIntent.FLAG_IMMUTABLE)

                val notification = builder.setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(it.title)
                    .setContentText("${it.artist} - ${it.album}")
                    .setLargeIcon(bitmap)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setContentIntent(clickIntent)
                    .addAction(Notification.Action(android.R.drawable.ic_media_previous, "Previous", prevIntent))
                    .addAction(Notification.Action(if (player.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play, "Play/Pause", if (player.isPlaying) pauseIntent else playIntent))
                    .addAction(Notification.Action(android.R.drawable.ic_media_next, "Next", nextIntent))
                    .addAction(Notification.Action(android.R.drawable.ic_menu_close_clear_cancel, "Close", closeIntent))
                    .setStyle(Notification.MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0, 1, 2))
                    .build()

                metadata.apply {
                    putBitmap(METADATA_KEY_ALBUM_ART, bitmap)
                    putString(METADATA_KEY_TITLE, it.title)
                    putString(METADATA_KEY_ALBUM, it.album)
                    putString(METADATA_KEY_ARTIST, it.artist)
                    putLong(METADATA_KEY_DURATION, player.duration.toLong())
                    session.setMetadata(metadata.build())
                    playbackState.setState(if (player.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED, player.currentPosition.toLong(), 1f)
                    session.setPlaybackState(playbackState.build())

                    withContext(Main) {
                        startForeground(10, notification)
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.apply {
            when (action) {
                "SKIP_PREVIOUS" -> prev()
                "SKIP_NEXT" -> next()
                "PLAY" -> play()
                "PAUSE" -> pause()
                "HANDLE_MEDIA_BUTTON" -> {
                    println("Handling media button!")
                }
                "CLOSE" -> {
                    if (!player.isPlaying) {
                        pause()
                        stopForeground(true)
                    }
                }
                else -> println("Unknown action: $action")
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        return MyBinder(this)
    }

    override fun onCreate() {
        super.onCreate()

        registerReceiver(headsetPlugReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(this)
                .build()
        }

        session.isActive = true
        session.setMediaButtonReceiver(PendingIntent.getService(this, 111, Intent("HANDLE_MEDIA_BUTTON"), PendingIntent.FLAG_IMMUTABLE))
        player.setOnCompletionListener(this)

        val flags = PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SEEK_TO or PlaybackState.ACTION_STOP

        playbackState.setActions(flags)
        session.setCallback(object: MediaSession.Callback() {
            override fun onPlay() {
                super.onPlay()
                play()
            }

            override fun onPause() {
                super.onPause()
                pause()
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                next()
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                prev()
            }

            override fun onStop() {
                super.onStop()
                pause()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(headsetPlugReceiver)
        player.release()
    }

    companion object {
        private const val CURRENT_TIME_KEY: String = "CURRENT_TIME"
        private const val CURRENT_SONG_KEY: String = "CURRENT_SONG"
        private const val CURRENT_PLAYLIST_KEY: String = "CURRENT_PLAYLIST"
        private const val CURRENT_REPEAT_MODE_KEY: String = "CURRENT_REPEAT_MODE"
        private const val CURRENT_SHUFFLE_KEY: String = "CURRENT_SHUFFLE"
        private const val TAG: String = "MusicPlayerService"
    }

    private fun save() {
        // Use SharedPreferences
        GlobalScope.launch(IO) {
            val sp = PreferenceManager.getDefaultSharedPreferences(this@MusicPlayerService).edit()
            // Save current time
            sp.putInt(CURRENT_TIME_KEY, player.currentPosition)

            // Save current song (id)
            currentSong.value?.let {
                sp.putInt(CURRENT_SONG_KEY, it.id)
            }

            // Save playlist (ids)
            // Put each song inside a comma-separated list
            val builder = StringBuilder()

            for (song in playlist.value) {
                builder.append("${song.id},")
            }

            val str = builder.trim()

            if (str.isNotEmpty()) {
                sp.putString(CURRENT_PLAYLIST_KEY, str.toString())
            }

            // Save repeat mode
            sp.putInt(CURRENT_REPEAT_MODE_KEY, repeatMode.value.ordinal)

            // Save shuffle mode
            sp.putBoolean(CURRENT_SHUFFLE_KEY, shuffle.value)

            sp.apply()
        }
    }

    var restored = false

    fun restore(songz: List<Song>) {
        if (restored) return
        restored = true
        // Restore current time
        GlobalScope.launch(IO) {
            val sp = PreferenceManager.getDefaultSharedPreferences(this@MusicPlayerService)
            val time = sp.getInt(CURRENT_TIME_KEY, 0)

            // Restore current song
            val currentSongId = sp.getInt(CURRENT_SONG_KEY, -1)

            // Restore current playlist
            val currentPlaylistStr = sp.getString(CURRENT_PLAYLIST_KEY, "")

            if (currentSongId >= 0 && currentPlaylistStr!!.trim().isNotEmpty()) {
                val tempPlaylist = mutableListOf<Song>()

                currentPlaylistStr.trim().split(",").forEach {
                    val id = it.trim().toIntOrNull()
                    id?.let {
                        songz.find { song ->
                            song.id == it
                        }?.let { actualSong ->
                            tempPlaylist.add(actualSong)
                        }
                    }
                }

                playlist.value = tempPlaylist

                val sh = sp.getBoolean(CURRENT_SHUFFLE_KEY, false)
                val rpMode = RepeatMode.values()[sp.getInt(CURRENT_REPEAT_MODE_KEY, RepeatMode.All.ordinal)]

                // Before setting repeat mode & shuffle, set the current song index
                // This will help us set the current song and then we can seek to the new time
                withContext(Main) {
                    currentIndex = playlist.value.indexOfFirst { it.id == currentSongId }
                    seek(time)
                    setShuffle(sh)
                    repeatMode.value = rpMode
                }
            }
        }
    }

    class MyBinder(val service: MusicPlayerService) : Binder()

    override fun onCompletion(p: MediaPlayer?) {
        isPlaying.value = player.isPlaying
        when (repeatMode.value) {
            RepeatMode.All -> next()
            RepeatMode.One -> play()
            else -> {}
        }
    }

    private var tempPause = false

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                tempPause = player.isPlaying
                if (tempPause) {
                    pause(true)
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (tempPause) {
                    play()
                    tempPause = false
                }
            }
        }
    }
}