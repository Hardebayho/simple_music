package tech.smallwonder.simplemusic

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import tech.smallwonder.simplemusic.models.BottomNavItem
import tech.smallwonder.simplemusic.models.MusicModel
import tech.smallwonder.simplemusic.models.MusicModelFactory
import tech.smallwonder.simplemusic.ui.components.App
import tech.smallwonder.simplemusic.ui.components.BottomNav
import tech.smallwonder.simplemusic.ui.components.HomePage
import tech.smallwonder.simplemusic.ui.components.SettingsPage
import tech.smallwonder.simplemusic.ui.theme.SimpleMusicTheme
import tech.smallwonder.simplemusic.utils.FavoritesRepository
import tech.smallwonder.simplemusic.utils.invert

val LocalModel = staticCompositionLocalOf<MusicModel>{ error("No model provided!") }

class MainActivity : ComponentActivity() {

    private val serviceInternal = MutableLiveData<MusicPlayerService?>()

    private val service: LiveData<MusicPlayerService?> get() {
        return serviceInternal
    }

    private lateinit var model: MusicModel

    private val connection = object: ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            p1?.let {
                if (it is MusicPlayerService.MyBinder) {
                    serviceInternal.postValue(it.service)
                }
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            serviceInternal.postValue(null)
        }
    }

    @ExperimentalPermissionsApi
    @ExperimentalPagerApi
    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentIntent = intent

        val i = Intent(this, MusicPlayerService::class.java)
        startService(i)
        FavoritesRepository.instance.init(applicationContext)

        bindService(
            i,
            connection,
            BIND_AUTO_CREATE
        )

        appContext = applicationContext

        val model: MusicModel by viewModels {
            MusicModelFactory(contentResolver, service)
        }

        this.model = model

        setContent {
            CompositionLocalProvider(
                LocalModel provides model
            ) {
                SimpleMusicTheme(darkTheme = true) {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        color = MaterialTheme.colors.background,
                    ) {
                        val started by model.serviceStarted
                        if (started) {
                            App()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)
    }

    companion object {
        var appContext: Context? = null
        var currentIntent: Intent? = null
    }
}
