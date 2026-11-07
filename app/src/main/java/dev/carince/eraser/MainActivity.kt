package dev.carince.eraser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.carince.eraser.data.EraserDatabase
import dev.carince.eraser.data.RoomItemCache
import dev.carince.eraser.feed.EncryptedFeedUrlStore
import dev.carince.eraser.feed.EraserApp
import dev.carince.eraser.feed.FeedRepository
import dev.carince.eraser.feed.FeedViewModel
import dev.carince.eraser.feed.OkHttpIcsFeedSource
import dev.carince.eraser.ui.theme.EraserTheme
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = applicationContext
            val zone = ZoneId.systemDefault()
            val feedViewModel: FeedViewModel = viewModel {
                FeedViewModel(
                    repository = FeedRepository(OkHttpIcsFeedSource()),
                    store = EncryptedFeedUrlStore(context),
                    cache = RoomItemCache(EraserDatabase.get(context).items()),
                    zone = zone,
                )
            }
            val state by feedViewModel.state.collectAsStateWithLifecycle()

            EraserTheme {
                EraserApp(
                    state = state,
                    zone = zone,
                    onSaveUrl = feedViewModel::saveAndLoad,
                    onRetry = feedViewModel::refresh,
                    onEditFeed = feedViewModel::editFeed,
                )
            }
        }
    }
}
